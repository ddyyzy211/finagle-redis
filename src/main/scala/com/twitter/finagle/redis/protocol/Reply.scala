package com.twitter.finagle.redis.protocol

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.parser.incremental._
import com.twitter.finagle.parser.util.DecodingHelpers._


sealed abstract class Reply

object Reply {
  case class Status(message: ChannelBuffer) extends Reply
  case class Error(message: ChannelBuffer) extends Reply
  case class Integer(integer: Int) extends Reply
  case class Bulk(data: Option[ChannelBuffer]) extends Reply
  case class MultiBulk(data: Option[Seq[Bulk]]) extends Reply
}

object ReplyDecoder {
  import Reply._
  import Parsers._

  private val readDecimalInt = readLine map { decodeDecimalInt(_) }

  private val readStatusReply = readLine map { Status(_) }

  private val readErrorReply = readLine map { Error(_) }

  private val readIntegerReply = readDecimalInt map { Integer(_) }

  private val readBulkReply = readDecimalInt flatMap { size =>
    if (size < 0) {
      const(Bulk(None))
    } else {
      readBytes(size) flatMap { bytes =>
        skipBytes(2) map { _ =>
          Bulk(Some(bytes))
        }
      }
    }
  }

  private val readBulkForMulti = guard("$") { readBulkReply }

  private val readMultiBulkReply = readDecimalInt flatMap { count =>
    if (count < 0) {
      const(MultiBulk(None))
    } else {
      times(count) { readBulkForMulti } map { bulks =>
        MultiBulk(Some(bulks))
      }
    }
  }

  val parser = choice(
    "+" -> readStatusReply,
    "-" -> readErrorReply,
    ":" -> readIntegerReply,
    "$" -> readBulkReply,
    "*" -> readMultiBulkReply
  )
}

class ReplyDecoder extends ParserDecoder[Reply](ReplyDecoder.parser)
