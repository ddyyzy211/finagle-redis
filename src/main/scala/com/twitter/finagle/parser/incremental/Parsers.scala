package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import com.twitter.finagle.util.DelimiterIndexFinder


object Parsers {
  def readUntil(delimiter: String) = new DelimiterParser(delimiter)

  val readLine = readUntil("\r\n")

  val readDecimalInt = readLine map { buf => parseDecimalInt(buf, buf.readableBytes) }

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

  // helpers

  private def pow(x: Int, p: Int) = {
    var rv = 1
    var j = 0

    while (j < p) {
      rv = rv * x
      j  = j + 1
    }

    rv
  }

  private def parseDecimalInt(arr: ChannelBuffer, numBytes: Int) = {
    val last  = numBytes - 1
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
}
