package com.twitter.finagle.parser

import org.jboss.netty.buffer.ChannelBuffer


object EncodingHelpers {
  def encodeDecimalInt(int: Int) = {
    DecimalIntCodec.encode(int)
  }

  def encodeDecimalInt(int: Int, dest: ChannelBuffer) {
    DecimalIntCodec.encode(int, dest)
  }
}


object DecodingHelpers {
  def decodeDecimalInt(buf: ChannelBuffer) = {
    DecimalIntCodec.decode(buf)
  }

  def decodeDecimalInt(buf: ChannelBuffer, numBytes: Int) = {
    DecimalIntCodec.decode(buf, numBytes)
  }

  def decodeFlags(num: Byte): Array[Boolean] = {
    val flags = new Array[Boolean](8)
    var i     = 0

    do {
      flags(i) = (num & (1 << i)) != 0
      i = i + 1
    } while (i < flags.length)

    flags
  }

  def decodeFlags(num: Short): Array[Boolean] = {
    val flags = new Array[Boolean](16)
    var i     = 0

    do {
      flags(i) = (num & (1 << i)) != 0
      i = i + 1
    } while (i < flags.length)

    flags
  }

  def decodeFlags(num: Int): Array[Boolean] = {
    val flags = new Array[Boolean](32)
    var i     = 0

    do {
      flags(i) = (num & (1 << i)) != 0
      i = i + 1
    } while (i < flags.length)

    flags
  }

  def decodeFlags(num: Long): Array[Boolean] = {
    val flags = new Array[Boolean](64)
    var i     = 0

    do {
      flags(i) = (num & (1 << i)) != 0
      i = i + 1
    } while (i < flags.length)

    flags
  }
}
