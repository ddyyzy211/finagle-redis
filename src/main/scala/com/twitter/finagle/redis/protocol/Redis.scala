package com.twitter.finagle.redis.protocol

import org.jboss.netty.channel._
import com.twitter.finagle.{CodecFactory, Codec}


object Redis {
  def apply() = new Redis
  def get() = apply()
}

class Redis extends CodecFactory[Command, Reply] {
  def client = Function.const {
    new Codec[Command, Reply] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline() = {
          val pipeline = Channels.pipeline()

          pipeline.addLast("decoder", new ReplyDecoder)
          pipeline.addLast("encoder", new CommandEncoder)
          pipeline
        }
      }
    }
  }

  def server = Function.const {
    new Codec[Command, Reply] {
      def pipelineFactory = new ChannelPipelineFactory {
        def getPipeline() = {
          val pipeline = Channels.pipeline()

          // pipeline.addLast("decoder", CommandDecoder())

          pipeline
        }
      }
    }
  }
}
