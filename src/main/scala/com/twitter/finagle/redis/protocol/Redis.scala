package com.twitter.finagle.redis.protocol

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.{CodecFactory, Codec}


class Redis extends Codec[Command, Reply] {
  def pipelineFactory = new ChannelPipelineFactory {
    def getPipeline() = {
      val pipeline = Channels.pipeline()

      pipeline.addLast("decoder", new ReplyDecoder)
      pipeline.addLast("encoder", new CommandEncoder)

      pipeline
    }
  }
}
