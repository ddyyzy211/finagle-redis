package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.parser.util._


class DelimiterParser(matcher: Matcher) extends Parser[ChannelBuffer] {

  def this(bytes: Array[Byte]) = this(new DelimiterMatcher(bytes))

  def this(string: String) = this(new DelimiterMatcher(string))

  def decode(buffer: ChannelBuffer) = {
    val frameLength = buffer.bytesBefore(matcher)

    if (frameLength < 0) {
      Continue(this)
    } else {
      Return(buffer.readSlice(frameLength))
    }
  }
}

class ConsumingDelimiterParser(matcher: Matcher) extends Parser[ChannelBuffer] {

  def this(bytes: Array[Byte]) = this(new DelimiterMatcher(bytes))

  def this(string: String) = this(new DelimiterMatcher(string))

  def decode(buffer: ChannelBuffer) = {
    val frameLength = buffer.bytesBefore(matcher)

    if (frameLength < 0) {
      Continue(this)
    } else {
      val frame = buffer.readSlice(frameLength)
      buffer.skipBytes(matcher.bytesMatching(buffer, buffer.readerIndex))
      Return(frame)
    }
  }
}
