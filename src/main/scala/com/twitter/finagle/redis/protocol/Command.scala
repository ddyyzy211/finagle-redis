package com.twitter.finagle.redis.protocol

import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import com.twitter.finagle.util.DecimalIntCodec.{encode => encodeInt}


object Command extends Enumeration {
  private[redis] val CRLF = "\r\n".getBytes("US-ASCII")

  type Argument = Array[Byte]
  type Name     = Value

  val APPEND, AUTH, BGWRITEAOF, BGSAVE, BLPOP, BRPOP, BRPOPLPUSH, CONFIG_GET, CONFIG_SET, CONFIG_RESETSTAT, DBSIZE, DEBUG_OBJECT, DEBUG_SEGFAULT, DECR, DECRBY, DEL, DISCARD, ECHO, EXISTS, EXPIRE, EXPIREAT, FLUSHALL, FLUSHDB, GET, GETBIT, GETRANGE, GETSET, HDEL, HEXISTS, HGET, HGETALL, HINCRBY, HKEYS, HLEN, HMGET, HMSET, HSET, HSETNX, HVALS, INCR, INCRBY, INFO, KEYS, LASTSAVE, LINDEX, LINSERT, LLEN, LPOP, LPUSH, LPUSHX, LRANGE, LREM, LSET, LTRIM, MGET, MONITOR, MOVE, MSET, MSETNX, MULTI, OBJECT, PERSIST, PING, PSUBSCRIBE, PUBLISH, PUNSUBSCRIBE, QUIT, RANDOMKEY, RENAME, RENAMENX, RPOP, RPOPLPUSH, RPUSH, RPUSHX, SADD, SAVE, SCARD, SDIFF, SDIFFSTORE, SELECT, SET, SETBIT, SETEX, SETNX, SETRANGE, SHUTDOWN, SINTER, SINTERSTORE, SISMEMBER, SLAVEOF, SLOWLOG, SMEMBERS, SMOVE, SORT, SPOP, SRANDOMMEMBER, SREM, STRLEN, SUBSCRIBE, SUNION, SUNIONSTORE, SYNC, TTL, TYPE, UNSUBSCRIBE, UNWATCH, WATCH, ZADD, ZCARD, ZCOUNT, ZINCRBY, ZINTERSTORE, ZRANGE, ZRANGEBYSCORE, ZRANK, ZREM, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREVRANGE, ZREVRANGEBYSCORE, ZREVRANK, ZSCORE, ZUNIONSTORE = Value

  private[redis] val byteRepresentations = values map { v =>
    v -> v.toString.getBytes("US-ASCII")
  } toMap
}


case class Command(name: Command.Name, arguments: Array[Command.Argument])

class CommandEncoder extends OneToOneEncoder {
  private def writeArgumentCount(buffer: ChannelBuffer, count: Int) {
    buffer.writeByte('*')
    encodeInt(count, buffer)
    buffer.writeBytes(Command.CRLF)
  }

  private def writeArgument(buffer: ChannelBuffer, arg: Array[Byte]) {
    buffer.writeByte('$')
    encodeInt(arg.length, buffer)
    buffer.writeBytes(Command.CRLF)
  }

  def encode(context: ChannelHandlerContext, channel: Channel, message: AnyRef) = {
    val Command(cmd, arguments) = message
    val buffer = ChannelBuffers.dynamicBuffer(11 * arguments.length)

    writeArgumentCount(buffer, arguments.length + 1) // include the command itself
    writeArgument(buffer, Command.byteRepresentations(cmd))

    arguments foreach { writeArgument(buffer, _) }

    buffer
  }
}


// object Redis {
//   def apply() = new Redis
//   def get() = apply()
// }

// class Redis extends CodecFactory[Command, Reply] {
//   def client = Function.const {
//     Codec.ofPipelineFactory[Command, Reply] {
//       val pipeline = Channels.pipeline()

//       pipeline.addLast("decoder", new ClientDecoder)
//       pipeline.addLast("decoding2reply", new DecodingToResponse)

//       pipeline.addLast("encoder", new Encoder)
//       pipeline.addLast("command2encoding", new CommandToEncoding)

//       pipeline
//     }
//   }

//   def server = Function.const {
//     Codec.ofPipelineFactory[Command, Reply] {
//       val pipeline = Channels.pipeline()

//       pipeline.addLast("decoder", new ServerDecoder)
//       pipeline.addLast("decoding2reply", new DecodingToResponse)

//       pipeline.addLast("encoder", new Encoder)
//       pipeline.addLast("command2encoding", new CommandToEncoding)

//       pipeline
//     }
//   }
// }
