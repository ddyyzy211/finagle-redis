package com.twitter.finagle.redis.test.memcached

import org.jboss.netty.channel._
import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.Codec
import com.twitter.finagle.parser.incremental._


sealed abstract class Response
case class NotFound()                     extends Response
case class Exists()                       extends Response
case class Stored()                       extends Response
case class NotStored()                    extends Response
case class Deleted()                      extends Response
case class Error(cause: String)    extends Response

case class Values(values: Seq[Value]) extends Response
case class Number(value: Long)         extends Response

case class Value(key: ChannelBuffer, value: ChannelBuffer)


object ResponseDecoder {
  import Parsers._

  val readErrorCause = readByte map { space => //ignore
    readLine map { bytes => Error(bytes.toString("UTF-8")) }
  }

  val readError = choice(
    "ERROR\r\n" -> const(Error("")),
    "SERVER_ERROR" -> readErrorCause,
    "CLIENT_ERROR" -> readErrorCause
  )

  val readStorageResponse = choice(
    "STORED\r\n"     -> const(Stored()),
    "NOT_STORED\r\n" -> const(NotStored()),
    "EXISTS\r\n"     -> const(Exists()),
    "NOT_FOUND\r\n"  -> const(NotFound())
  ) orElse readError

  val readRetrievalResponse =

  val readResponse = choice(
    "STORED\r\n"     -> const(Stored()),
    "NOT_STORED\r\n" -> const(NotStored()),
    "DELETED\r\n"    -> const(Deleted()),
    "NOT_FOUND\r\n"  -> const(NotFound()),
    "EXISTS\r\n"     -> const(Exists())
  )
}


// class Memcached extends Codec[Command, Response] {
//   def pipelineFactory = new ChannelPipelineFactory {
//     def getPipeline() = {
//       val pipeline = Channels.pipeline()

//       pipeline
//     }
//   }
// }
