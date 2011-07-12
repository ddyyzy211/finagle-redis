package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}


object FixedBytesParser {
  val ChunkSize = 256
}

class FixedBytesParser(bytesLeft: Int, dataOpt: Option[ChannelBuffer]) extends Parser[ChannelBuffer] {
  def this(bytes: Int) = this(bytes, None)

  import FixedBytesParser._

  def decode(buffer: ChannelBuffer) = {
    val readable = buffer.readableBytes

    if (readable >= ChunkSize || readable >= bytesLeft) {
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
