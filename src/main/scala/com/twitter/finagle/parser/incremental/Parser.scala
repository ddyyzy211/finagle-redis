package com.twitter.finagle.parser.incremental

import org.jboss.netty.buffer.ChannelBuffer
import com.twitter.finagle.ParseException


// states: continue (wait), return, error

sealed abstract class ParseResult[+Output]

case class Continue[+T](next: Parser[T]) extends ParseResult[T]
case class Return[+T](ret: T) extends ParseResult[T]
case class Throw(err: ParseException) extends ParseResult[Nothing]

abstract class Parser[+Out] {
  def decode(buffer: ChannelBuffer): ParseResult[Out]

  def map[T](f: Out => T): Parser[T] = flatMap { t =>
    try {
      new ConstParser(f(t))
    } catch {
      case e: ParseException => new FailParser(e)
    }
  }

  def flatMap[T](f: Out => Parser[T]): Parser[T] = {
    new FlatMapParser(this, f)
  }

  def orElse[T >: Out](other: Parser[T]) = {
    if (isSafe) {
      new OrElseParser(this, other)
    } else {
      new BacktrackingParser(this) orElse other
    }
  }

  // if true, then it is safe to assume that either parsing cannot
  // normally fail, or if it does, then it does not consume any bytes
  def isSafe = true
}

class FailParser(err: ParseException) extends Parser[Nothing] {
  def decode(buffer: ChannelBuffer) = Throw(err)
}

class ConstParser[+Out](out: Out) extends Parser[Out] {
  def decode(buffer: ChannelBuffer) = Return(out)
}

abstract class UnsafeParser[+Out] extends Parser[Out] {
  override def isSafe = false
}

class FlatMapParser[A,+B](lhs: Parser[A], f: A => Parser[B]) extends UnsafeParser[B] {
  def decode(buffer: ChannelBuffer) = {
    lhs.decode(buffer) match {
      case e: Throw       => e
      case r: Return[A]   => f(r.ret).decode(buffer)
      case c: Continue[A] => if (c.next eq lhs) {
        Continue(this)
      } else {
        Continue(new FlatMapParser(c.next, f))
      }
    }
  }
}

class OrElseParser[+Out](lhs: Parser[Out], rhs: Parser[Out]) extends Parser[Out] {
  def decode(buffer: ChannelBuffer) = {
    lhs.decode(buffer) match {
      case e: Throw         => rhs.decode(buffer)
      case r: Return[Out]   => r
      case c: Continue[Out] => if (c.next eq lhs) {
        Continue(this)
      } else {
        Continue(new OrElseParser(c.next, rhs))
      }
    }
  }

  // override to be right-associative
  override def orElse[T >: Out](other: Parser[T]) = {
    new OrElseParser(lhs, rhs orElse other)
  }
}

class BacktrackingParser[+Out](inner: Parser[Out], offset: Int) extends Parser[Out] {

  def this(inner: Parser[Out]) = this(inner, 0)

  def decode(buffer: ChannelBuffer) = {
    val start = buffer.readerIndex

    buffer.setReaderIndex(start + offset)

    // complains that Out is unchecked here, but this cannot fail, so
    // live with the warning.
    inner.decode(buffer) match {
      case e: Throw => {
        buffer.setReaderIndex(start)
        e
      }
      case r: Return[Out] => r
      case c: Continue[Out] => {
        if (c.next == inner && buffer.readerIndex == (start + offset)) {
          buffer.setReaderIndex(start)
          Continue(this)
        } else {
          val newOffset = buffer.readerIndex - start
          buffer.setReaderIndex(start)
          Continue(new BacktrackingParser(c.next, newOffset))
        }
      }
    }
  }
}
