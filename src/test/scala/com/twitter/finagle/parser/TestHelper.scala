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
    def mustParse(input: String) = new ParserTest(p, input)
  }

  class ParserTest[Out](p: Parser[Out], sourceString: String) {
    val source = ChannelBuffers.wrappedBuffer(sourceString.getBytes("UTF-8"))
    val in     = ChannelBuffers.dynamicBuffer

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

    def andReturn(out: Out) = { go(Continue(p)) mustEqual Return(out); this }
    def andThrow(err: ParseException) = { go(Continue(p)) mustEqual Throw(err); this }
    def andContinue() = { go(Continue(p)) must haveClass[Continue[Out]]; this }
    def readingBytes(c: Int) { in.readerIndex mustEqual c }
    def leavingBytes(r: Int) { (source.writerIndex - in.readerIndex) mustEqual r }
  }

  implicit def parser2Test[T](parser: Parser[T]) = new RichParser(parser)

  def asString(b: ChannelBuffer) = {
    b.toString(Charset.forName("UTF-8"))
  }
}
