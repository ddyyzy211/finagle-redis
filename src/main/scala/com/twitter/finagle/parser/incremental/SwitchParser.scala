package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.ParseException


object SwitchParser {
  def stringMatchers[T](pairs: (String, Parser[T])*) = {
    new SwitchParser(pairs map {
      case (s, p) => (s.getBytes("US-ASCII"), p)
    } toArray)
  }
}

class SwitchParser[+Out](choices: Array[(Array[Byte], Parser[Out])]) extends UnsafeParser[Out] {

  assert(choices.length > 0)

  val maxMatcherLength = choices map { _._1.length } max

  def decode(buffer: ChannelBuffer): ParseResult[Out] = {
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

      new Throw(new ParseException(
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
