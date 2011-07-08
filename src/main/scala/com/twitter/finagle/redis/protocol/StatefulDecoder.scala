package com.twitter.finagle.redis.protocol

import java.nio.charset.Charset
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import com.twitter.finagle.Codec


// abstract class StatefulDecoder[State, Output](initialState: => State)
// extends ReplayingDecoder[State](initialState) {

//   def decode(buffer: ChannelBuffer, state: State): Option[Either[State, Output]]

//   def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer, currState: State): AnyRef = {

//     decode(buffer, currState) match {
//       case Some(Right(out)) => {
//         checkpoint(initialState)
//         out
//       }
//       case Some(Left(newState)) => {
//         checkpoint(newState)
//         null.asInstanceOf[Output]
//       }
//       case None => null.asInstanceOf[Output]
//     }
//   }

//   // helper methods

//   protected def wait(): Option[Either[State, Output]] = {
//     None
//   }

//   protected def continue(state: State): Option[Either[State, Output]] = {
//     Some(Left(state))
//   }

//   protected def emit(out: Output): Option[Either[State, Output]] = {
//     Some(Right(out))
//   }
// }
