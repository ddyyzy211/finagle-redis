package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import com.twitter.finagle.util.DelimiterIndexFinder
import com.twitter.finagle.util.DecimalIntCodec.{decode => decodeInt}


object Parsers {
  def readUntil(delimiter: String) = new DelimiterParser(delimiter)

  val readLine = readUntil("\r\n")

  val readDecimalInt = readLine map { decodeInt(_) }

  val skipCRLF = skipBytes(2)

  def const[T](t: T) = new ConstParser(t)

  val unit = const(())

  def readBytes(size: Int) = new FixedBytesParser(size)

  def skipBytes(size: Int) = readBytes(size) map { _ => () }

  def guard[T](matcher: String)(parser: Parser[T]) = {
    AlternateParser.stringMatchers(matcher -> parser)
  }

  def choice[T](choices: (String, Parser[T])*) = {
    AlternateParser.stringMatchers(choices: _*)
  }

  def times[T](total: Int)(decoder: Parser[T]) = {
    def go(i: Int, prev: List[T]): Parser[Seq[T]] = {
      if (i == total) {
        const(prev.reverse)
      } else {
        decoder flatMap { rv =>
          go(i + 1, rv :: prev)
        }
      }
    }

    go(0, Nil)
  }
}
