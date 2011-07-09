package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.util.DelimiterIndexFinder


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
