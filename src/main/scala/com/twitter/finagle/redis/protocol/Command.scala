package com.twitter.finagle.redis.protocol

import scala.collection.immutable.Queue
import java.nio.charset.Charset
import org.jboss.netty.channel._
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBufferIndexFinder, ChannelBuffer}
import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.handler.codec.replay.ReplayingDecoder
import com.twitter.util.{Time, Duration, StateMachine}
import com.twitter.finagle.Codec


object Command extends Enumeration {
  type Argument = Array[Byte]
  type Name     = Value

  val APPEND, AUTH, BGWRITEAOF, BGSAVE, BLPOP, BRPOP, BRPOPLPUSH, CONFIG_GET, CONFIG_SET, CONFIG_RESETSTAT, DBSIZE, DEBUG_OBJECT, DEBUG_SEGFAULT, DECR, DECRBY, DEL, DISCARD, ECHO, EXISTS, EXPIRE, EXPIREAT, FLUSHALL, FLUSHDB, GET, GETBIT, GETRANGE, GETSET, HDEL, HEXISTS, HGET, HGETALL, HINCRBY, HKEYS, HLEN, HMGET, HMSET, HSET, HSETNX, HVALS, INCR, INCRBY, INFO, KEYS, LASTSAVE, LINDEX, LINSERT, LLEN, LPOP, LPUSH, LPUSHX, LRANGE, LREM, LSET, LTRIM, MGET, MONITOR, MOVE, MSET, MSETNX, MULTI, OBJECT, PERSIST, PING, PSUBSCRIBE, PUBLISH, PUNSUBSCRIBE, QUIT, RANDOMKEY, RENAME, RENAMENX, RPOP, RPOPLPUSH, RPUSH, RPUSHX, SADD, SAVE, SCARD, SDIFF, SDIFFSTORE, SELECT, SET, SETBIT, SETEX, SETNX, SETRANGE, SHUTDOWN, SINTER, SINTERSTORE, SISMEMBER, SLAVEOF, SLOWLOG, SMEMBERS, SMOVE, SORT, SPOP, SRANDOMMEMBER, SREM, STRLEN, SUBSCRIBE, SUNION, SUNIONSTORE, SYNC, TTL, TYPE, UNSUBSCRIBE, UNWATCH, WATCH, ZADD, ZCARD, ZCOUNT, ZINCRBY, ZINTERSTORE, ZRANGE, ZRANGEBYSCORE, ZRANK, ZREM, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREVRANGE, ZREVRANGEBYSCORE, ZREVRANK, ZSCORE, ZUNIONSTORE = Value

  private[redis] val byteRepresentations = values map { v =>
    v -> ChannelBuffers.wrappedBuffer(v.toString.getBytes("US-ASCII"))
  } toMap
}


case class Command(name: Command.Name, arguments: Array[Command.Argument]) = {
  def toBuffer = {
    ChannelBuffers.unmodifiableBuffer(
      ChannelBuffers.wrappedBuffer
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
