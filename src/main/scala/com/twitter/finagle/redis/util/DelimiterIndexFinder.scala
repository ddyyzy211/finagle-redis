package com.twitter.finagle.redis.util

import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import org.jboss.netty.handler.codec.replay.ReplayingDecoder


// lifted and generalized from finagle-memcached. should be moved to core
class DelimiterIndexFinder(val delimiter: Array[Byte]) extends ChannelBufferIndexFinder {

  def this(s: String) = this(s.getBytes("US-ASCII"))

  val delimiterLength = delimiter.length

  def find(buffer: ChannelBuffer, guessedIndex: Int): Boolean = {
    val enoughBytes = guessedIndex + delimiterLength
    if (buffer.writerIndex < enoughBytes) return false

    var i = 0
    while (i < delimiterLength) {
      if (delimiter(i) != buffer.getByte(guessedIndex + i)) return false
      i = i + 1
    }

    true
  }
}
