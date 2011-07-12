package com.twitter.finagle.parser.test

import scala.annotation.tailrec
import java.nio.charset.Charset
import org.specs.Specification
import org.specs.matcher.Matcher
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.twitter.finagle.ParseException
import com.twitter.finagle.parser.incremental._


class ParserSpecification extends Specification {
  class RichParser[Out](p: Parser[Out]) {
    def mustParse(source: ChannelBuffer) = {
      new ParserTest(p, source)
    }

    def mustParse(source: String) = {
      new ParserTest(p, ChannelBuffers.wrappedBuffer(source.getBytes("UTF-8")))
    }
  }

  class ParserTest[Out](p: Parser[Out], source: ChannelBuffer) {
    val in = ChannelBuffers.dynamicBuffer

    @tailrec private def go(rv: ParseResult[Out]): ParseResult[Out] = rv match {
      case e: Throw       => e
      case Return(o)      => Return(o)
      case Continue(next) => if (source.readableBytes > 0) {
        in.writeByte(source.readByte)
        go(next.decode(in))
      } else {
        Continue(next)
      }
    }

    // start with an empty buffer
    lazy val rv = go(p.decode(in))

    def andReturn(out: Out) = {
      rv mustEqual Return(out)
      this
    }

    def andThrow(err: ParseException) = {
      rv mustEqual Throw(err)
      this
    }

    def andThrow() = {
      rv must haveClass[Throw]
      this
    }

    def andThrow(msg: String) = {
      rv match {
        case Throw(err) => err.getMessage mustEqual msg
        case _ => fail(p.toString +" did not throw.")
      }
      this
    }

    def andContinue(n: Parser[Out]) = {
      rv mustEqual Continue(n)
      this
    }

    def andContinue() = {
      rv must haveClass[Continue[Out]]
      this
    }

    def readingBytes(c: Int) {
      rv
      in.readerIndex mustEqual c
    }

    def leavingBytes(r: Int) {
      rv
      (source.writerIndex - in.readerIndex) mustEqual r
    }
  }

  implicit def parser2Test[T](parser: Parser[T]) = new RichParser(parser)

  def asString(b: ChannelBuffer) = {
    b.toString(Charset.forName("UTF-8"))
  }
}
