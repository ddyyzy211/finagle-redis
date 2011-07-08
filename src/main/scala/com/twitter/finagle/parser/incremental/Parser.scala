package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import com.twitter.finagle.util.DelimiterIndexFinder


// states: continue (wait), return, error

// XXX: better root class?
class ParseFailError(msg: String) extends Exception(msg)

sealed abstract class ParseResult[+Output]

case class Continue[+T](next: Parser[T]) extends ParseResult[T]
case class Return[+T](ret: T) extends ParseResult[T]
class Error(_err: => Throwable) extends ParseResult[Nothing] {
  lazy val err = _err
}

abstract class Parser[+Output] {
  def decode(buffer: ChannelBuffer): ParseResult[Output]

  def map[T](f: Output => T): Parser[T] = flatMap { t =>
    new ConstParser(f(t))
  }

  def flatMap[T](f: Output => Parser[T]): Parser[T] = {
    new FlatMapParser(this, f)
  }
}


class ConstParser[+T](out: T) extends Parser[T] {
  def decode(buffer: ChannelBuffer) = Return(out)
}


class FlatMapParser[A,+B](lhs: Parser[A], f: A => Parser[B]) extends Parser[B] {
  def decode(buffer: ChannelBuffer) = {
    lhs.decode(buffer) match {
      case e: Error       => e
      case r: Return[A]   => f(r.ret).decode(buffer)
      case c: Continue[A] => if (c.next eq lhs) {
        Continue(this)
      } else {
        Continue(new FlatMapParser(c.next, f))
      }
    }
  }
}


object FixedBytesParser {
  val ChunkSize = 256
}

class FixedBytesParser(bytesLeft: Int, dataOpt: Option[ChannelBuffer]) extends Parser[ChannelBuffer] {
  def this(bytes: Int) = this(bytes, None)

  import FixedBytesParser._

  def decode(buffer: ChannelBuffer) = {
    val readable = buffer.readableBytes

    if (readable > ChunkSize || readable >= bytesLeft) {
      val data = dataOpt getOrElse ChannelBuffers.buffer(bytesLeft)

      val newLeft = (bytesLeft - readable) match {
        case l if l < 0 => 0
        case l          => l
      }

      if (bytesLeft > 0) buffer.readBytes(data, bytesLeft - newLeft)

      if (newLeft == 0) {
        Return(data)
      } else {
        Continue(new FixedBytesParser(newLeft, Some(data)))
      }
    } else {
      Continue(this)
    }
  }
}


class DelimiterParser(finder: DelimiterIndexFinder) extends Parser[ChannelBuffer] {

  def this(delimiter: String) = this(new DelimiterIndexFinder(delimiter))

  def decode(buffer: ChannelBuffer) = {
    val frameLength = buffer.bytesBefore(finder)

    if (frameLength < 0) {
      Continue(this)
    } else {
      val frame = buffer.slice(buffer.readerIndex, frameLength)
      buffer.skipBytes(frameLength + finder.delimiterLength)

      Return(frame)
    }
  }
}


object AlternateParser {
  def stringMatchers[T](pairs: (String, Parser[T])*) = {
    new AlternateParser(pairs map {
      case (s, p) => (s.getBytes("US-ASCII"), p)
    } toArray)
  }
}

class AlternateParser[+T](choices: Array[(Array[Byte], Parser[T])]) extends Parser[T] {

  assert(choices.length > 0)

  val maxMatcherLength = choices map { _._1.length } max

  def decode(buffer: ChannelBuffer): ParseResult[T] = {
    if (buffer.readableBytes < maxMatcherLength) {
      Continue(this)
    } else {
      var i = 0

      do {
        val (matcher, parser) = choices(i)

        if (bufferBeginsWith(buffer, matcher)) {
          buffer.skipBytes(matcher.length)
          return parser.decode(buffer)
        }

        i = i + 1
      } while (i < choices.length)

      new Error(new ParseFailError(
        "No match for: "+ (choices map { _._1.toString } mkString(", "))
      ))
    }
  }

  private def bufferBeginsWith(buffer: ChannelBuffer, matcher: Array[Byte]): Boolean = {
    var i = 0

    do {
      if (buffer.getByte(buffer.readerIndex + i) != matcher(i)) return false
      i = i + 1
    } while (i < matcher.length)

    true
  }
}
