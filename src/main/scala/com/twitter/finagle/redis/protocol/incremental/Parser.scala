package com.twitter.finagle.redis.protocol.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import com.twitter.finagle.redis.util.DelimiterIndexFinder


// states: continue, return, error

class ParseFailError(msg: String) extends Exception(msg)

sealed abstract class ParseResult[+Output]

case class Continue[+T](next: Parser[T]) extends ParseResult[T]
case class Return[+T](ret: T) extends ParseResult[T]
class Error(_err: => Throwable) extends ParseResult[Nothing] {
  lazy val err = _err
}

object Parsers {
  def readUntil(delimiter: String) = new DelimiterParser(delimiter)

  val readLine = readUntil("\r\n")

  val skipCRLF = skipBytes(2)

  def readBytes(size: Int) = new FixedBytesParser(size)

  def skipBytes(size: Int) = readBytes(size) map { _ => () }

  def guard(matcher: String) = AlternateParser.stringMatchers(matcher -> unitParser)

  def times[T](total: Int, decoder: Parser[T]) = {
    def go(i: Int): Parser[List[T]] = {
      if (i == total - 1) {
        decoder map { List(_) }
      } else {
        decoder flatMap { rv =>
          go(i + 1) map { rv :: _ }
        }
      }
    }

    go(0)

    // def go(i: Int, prev: List[T]): Parser[Seq[T]] = {
    //   if (i == total) {
    //     new ConstParser(prev.reverse)
    //   } else {
    //     decoder flatMap { rv =>
    //       go(i + 1, rv :: prev)
    //     }
    //   }
    // }

    // go(0, Nil)
  }

  def switch[T](choices: (String, Parser[T])*) = {
    AlternateParser.stringMatchers(choices: _*)
  }

  // helpers

  private def unitParser = new ConstParser[Unit](())
}

abstract class Parser[+Output] {
  def decode(buffer: ChannelBuffer): ParseResult[Output]

  def map[T](f: Output => T): Parser[T] = flatMap { t =>
    new ConstParser(f(t))
  }

  def andThen[T](f: Unit => Parser[T]): Parser[T] = flatMap { _ => f() }

  def flatMap[T](f: Output => Parser[T]): Parser[T] = {
    new FlatMapParser(this, f)
  }

  // internal helper methods

  protected def bufferBeginsWith(buffer: ChannelBuffer, matcher: Array[Byte]): Boolean = {
    var i = 0

    do {
      if (buffer.getByte(buffer.readerIndex + i) != matcher(i)) return false
      i = i + 1
    } while (i < matcher.length)

    true
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

      new Error(new ParseFailError("No match for: "+ (choices map { _._1.toString } mkString(", "))))
    }
  }
}


object Test {
  val status = "+OK\r\n"
  val error = "-Oops\r\n"
  val integer = ":123\r\n"
  val bulk = "$3\r\nfoo\r\n"
  val multiBulk3 = "*3\r\n" + ("$3\r\nfoo\r\n" * 3)
  val multiBulk100 = "*100\r\n" + ("$3\r\nfoo\r\n" * 100)

  import Parsers._

  def pow(x: Int, p: Int) = {
    var rv = 1
    var j = 0

    while (j < p) {
      rv = rv * x
      j  = j + 1
    }

    rv
  }

  def parseDecimalInt(arr: ChannelBuffer) = {
    val last  = arr.readableBytes - 1
    var i     = last
    var rv    = 0
    var lower = 0
    var isNegative = false

    if (arr.getByte(arr.readerIndex) == '-') {
      lower = 1
      isNegative = true
    } else if (arr.getByte(arr.readerIndex) == '+') {
      lower = 1
    }

    while (i >= lower) {
      val c = arr.getByte(arr.readerIndex + i) - 48

      if (c < 0 || c > 9) throw new ParseFailError("byte out of bounds")
      rv = rv + c * pow(10, last - i)
      i = i - 1
    }

    if (isNegative) rv * -1 else rv
  }

  val readInteger = readLine map { parseDecimalInt(_) }

  val readBulk = readInteger flatMap { length =>
    readBytes(length) flatMap { bytes =>
      skipCRLF map { _ => bytes }
    }
  }

  val readBulkForMulti = guard("$") flatMap { _ => readBulk }

  val readMultiBulk = readInteger flatMap { count =>
    times(count, readBulkForMulti) map { bulks =>
      (count, bulks)
    }
  }

  // val readBulk = for {
  //   length <- readInteger
  //   bytes  <- readBytes(length)
  //   crlf   <- readBytes(2)
  // } yield bytes.toString("UTF-8")


  val redisParser = switch(
    "+" -> readLine,
    "-" -> readLine,
    ":" -> readInteger,
    "$" -> readBulk,
    "*" -> readMultiBulk
  )
}
