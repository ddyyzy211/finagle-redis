package com.twitter.finagle.redis.protocol

import org.jboss.netty.channel._
import com.twitter.finagle.{CodecFactory, Codec}


object Redis {
  def apply() = new Redis
  def get() = apply()
}

class Redis extends CodecFactory[Command, Reply] {
  def client = Function.const {
    Codec.ofPipelineFactory[Command, Reply] {
      val pipeline = Channels.pipeline()

      pipeline.addLast("decoder", ReplyDecoder())
      pipeline.addLast("encoder", CommandEncoder())
      pipeline
    }
  }

  def server = Function.const {
    Codec.ofPipelineFactory[Command, Reply] {
      val pipeline = Channels.pipeline()

      // pipeline.addLast("decoder", CommandDecoder())
      pipeline
    }
  }
}
