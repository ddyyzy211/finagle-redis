package com.twitter.finagle.redis.protocol

import scala.collection.immutable.Queue
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import com.twitter.util.{Time, Duration, StateMachine}
import com.twitter.finagle.Codec


// // XXX: inherit from some better base
// class ReplyParseException(message: String) extends Exception


// sealed abstract class Reply

// object Reply {
//   case class Status(message: ChannelBuffer) extends Reply
//   case class Error(message: ChannelBuffer) extends Reply
//   case class Integer(integer: Int) extends Reply
//   case class Bulk(data: Option[ChannelBuffer]) extends Reply
//   case class MultiBulk(data: Option[Seq[Option[ChannelBuffer]]]) extends Reply
// }


// sealed abstract class ReplyState

// object ReplyState {
//   type MultiBulkState = Option[(Queue[Option[ChannelBuffer]], Int)]

//   case object Start extends ReplyState
//   case object ReadStatus extends ReplyState
//   case object ReadError extends ReplyState
//   case object ReadInteger extends ReplyState
//   case object ReadBulkLength extends ReplyState
//   case class ReadBulkData(bytes: Int, data: ChannelBuffer) extends ReplyState
//   case object ReadMultiBulkCount extends ReplyState
// }


// class ReplyDecoder extends StatefulDecoder[ReplyState, Reply](ReplyState.Start) {

//   import Reply._
//   import ReplyState._
//   import BufferUtils._

//   @volatile private var multiState: MultiBulkState = None

//   def decode(buffer: ChannelBuffer, state: ReplyState) = state match {
//     case Start => {
//       val byte = buffer.readByte()

//       if (byte != '$' && !multiState.isEmpty) {
//         throw new ReplyParseException("Expected a multibulk reply part but didn't get it!")
//       }

//       byte match {
//         case '+' => continue(ReadStatus)
//         case '-' => continue(ReadError)
//         case ':' => continue(ReadInteger)
//         case '$' => continue(ReadBulkLength)
//         case '*' => continue(ReadMultiBulkCount)
//         case x   => throw new ReplyParseException("Unknown reply start byte: "+ x.toInt)
//       }
//     }

//     case ReadStatus => readLine(buffer) flatMap { msg => emit(Status(msg)) }

//     case ReadError => readLine(buffer) flatMap { msg => emit(Error(msg)) }

//     case ReadInteger => readInteger(buffer) flatMap { i => emit(Integer(i)) }

//     case ReadBulkLength => readInteger(buffer) flatMap { bytes =>
//       if (bytes == -1) {
//         emitOrContinueMultiBulk(None)
//       } else {
//         continue(ReadBulkData(bytes, ChannelBuffers.buffer(bytes)))
//       }
//     }

//     case ReadBulkData(bytesLeft, data) => {
//       val newLeft = (bytesLeft - buffer.readableBytes) match {
//         case l if l < 0 => 0
//         case l          => l
//       }

//       if (bytesLeft > 0) buffer.readBytes(data, bytesLeft - newLeft)

//       if (newLeft == 0 && buffer.readableBytes >= 2) {
//         buffer.skipBytes(2)

//         emitOrContinueMultiBulk(Some(data))
//       } else {
//         continue(ReadBulkData(newLeft, data))
//       }
//     }

//     case ReadMultiBulkCount => readInteger(buffer) flatMap { count =>
//       if (count == -1) {
//         emit(MultiBulk(None))
//       } else if (count == 0) {
//         emit(MultiBulk(Some(Nil)))
//       } else {
//         multiState = Some(Queue(), count)
//         continue(ReadBulkLength)
//       }
//     }
//   }

//   protected def emitOrContinueMultiBulk(reply: Option[ChannelBuffer]) = {
//     multiState map { case (parts, left) =>
//       if (left == 1) {
//         multiState = None
//         emit(MultiBulk(Some(parts :+ reply)))
//       } else {
//         multiState = Some(parts :+ reply, left - 1)
//         continue(ReadBulkLength)
//       }
//     } getOrElse {
//       emit(Bulk(reply))
//     }
//   }
// }


