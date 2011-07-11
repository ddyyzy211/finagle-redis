package com.twitter.finagle.parser.incremental

import org.specs.Specification
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.twitter.finagle.ParseException
import com.twitter.finagle.parser.test._


object ParserSpec extends ParserSpecification {
  import Parsers._

  "FailParser" in {
    val err = new ParseException("whoops")

    Parsers.fail(err) mustParse "foo" andThrow err readingBytes(0)
  }

  "ConstParser" in {
    const("as always") mustParse "blah" andReturn "as always" readingBytes(0)
  }

  "DelimiterParser" in {
    val parser = readUntil("\r\n") map asString

    parser mustParse "hello world\r\n" andReturn "hello world" leavingBytes(0)
    parser mustParse "one\r\ntwo\r\n" andReturn "one" readingBytes(5)
    times(2) { parser } mustParse "one\r\ntwo\r\n" andReturn Seq("one", "two") leavingBytes(0)
  }
}
