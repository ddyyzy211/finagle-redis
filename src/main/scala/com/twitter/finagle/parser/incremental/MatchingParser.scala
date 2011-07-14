package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.parser.util._
import com.twitter.finagle.ParseException


abstract class AbstractMatchParser[+Out](matcher: Matcher) extends Parser[Out] {

  protected def matchSucceeded(buffer: ChannelBuffer, matchSize: Int): ParseResult[Out]

  def decode(buffer: ChannelBuffer): ParseResult[Out] = {
    matcher.bytesMatching(buffer, buffer.readerIndex) match {
      case -1        => matchFailed(buffer)
      case matchSize => matchSucceeded(buffer, matchSize)
    }
  }

  protected def matchFailed(buffer: ChannelBuffer): ParseResult[Out] = {
    if (buffer.readableBytes < matcher.bytesNeeded) {
      Continue(this)
    } else {
      new Throw(new ParseException("Match failed."))
    }
  }
}

class MatchParser(matcher: Matcher) extends AbstractMatchParser[Unit](matcher) {
  def this(bytes: Array[Byte]) = this(new DelimiterMatcher(bytes))
  def this(string: String) = this(new DelimiterMatcher(string))

  def matchSucceeded(buffer: ChannelBuffer, matchSize: Int) = {
    Return(())
  }
}

class ConsumingMatchParser(matcher: Matcher)
extends AbstractMatchParser[ChannelBuffer](matcher) {

  def this(bytes: Array[Byte]) = this(new DelimiterMatcher(bytes))

  def this(string: String) = this(new DelimiterMatcher(string))

  def matchSucceeded(buffer: ChannelBuffer, matchSize: Int) = {
    Return(buffer.readSlice(matchSize))
  }
}

object SwitchParser {
  def stringMatchers[T](pairs: (String, Parser[T])*) = {
    new SwitchParser[T](pairs map { case (s, p) =>
      new DelimiterMatcher(s) -> p
    } toArray)
  }
}

class SwitchParser[+Out](choices: Array[(Matcher, Parser[Out])]) extends Parser[Out] {

  val maxMatcherLength = (choices map { _._1.bytesNeeded }) ++ Array(0) max

  def decode(buffer: ChannelBuffer): ParseResult[Out] = {
    var i = 0

    while (i < choices.length) {
      val (matcher, parser) = choices(i)

      matcher.bytesMatching(buffer, buffer.readerIndex) match {
        case -1        => i = i + 1
        case matchSize => {
          buffer.skipBytes(matchSize)
          return parser.decode(buffer)
        }
      }
    }

    if (buffer.readableBytes < maxMatcherLength) {
      Continue(this)
    } else {
      new Throw(new ParseException("Match failed."))
    }
  }
}
