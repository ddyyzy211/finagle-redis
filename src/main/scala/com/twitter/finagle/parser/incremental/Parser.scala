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

  def map[T](f: Out => T): Parser[T] = this into { out =>
    try {
      new ConstParser(f(out))
    } catch {
      case e: ParseException => new FailParser(e)
    }
  }

  def into[T](f: Out => Parser[T]): Parser[T] = {
    new FlatMapParser(this, f)
  }

  def then[T](next: Parser[T]): Parser[T] = {
    new ChainedParser(this, next)
  }

  def flatMap[T](f: Out => Parser[T]) = this into f

  def flatMap[T](next: Parser[T]) = this then next

  def or[T >: Out](other: Parser[T]): Parser[T] = {
    new OrElseParser(this, other)
  }
}

class FailParser(err: ParseException) extends Parser[Nothing] {
  def decode(buffer: ChannelBuffer) = Throw(err)
}

class ConstParser[+Out](out: Out) extends Parser[Out] {
  def decode(buffer: ChannelBuffer) = Return(out)
}

abstract class AbstractChainedParser[A,+B] extends Parser[B] {
  protected def left: Parser[A]
  protected def right(ret: A): Parser[B]
  protected def continued(next: Parser[A]): Parser[B]

  def decode(buffer: ChannelBuffer) = left.decode(buffer) match {
    case e: Throw       => e
    case r: Return[A]   => right(r.ret).decode(buffer)
    case c: Continue[A] => if (c.next eq left) {
      Continue(this)
    } else {
      Continue(continued(c.next))
    }
  }
}

class ChainedParser[A,+B](protected val left: Parser[A], rhs: Parser[B])
extends AbstractChainedParser[A,B] {
  protected def right(ret: A) = rhs
  protected def continued(next: Parser[A]) = new ChainedParser(next, rhs)

  // override to be right-associative
  override def then[T](other: Parser[T]) = {
    new ChainedParser(left, rhs then other)
  }
}

class FlatMapParser[A,+B](protected val left: Parser[A], f: A => Parser[B])
extends AbstractChainedParser[A,B] {
  protected def right(ret: A) = f(ret)
  protected def continued(next: Parser[A]) = new FlatMapParser(next, f)
}

class OrElseParser[+Out](lhs: Parser[Out], rhs: Parser[Out]) extends Parser[Out] {
  def decode(buffer: ChannelBuffer) = {
    val start = buffer.readerIndex

    lhs.decode(buffer) match {
      case e: Throw => if (start == buffer.readerIndex) rhs.decode(buffer) else e
      case r: Return[Out] => r
      case c: Continue[Out] => if (c.next eq lhs) {
        Continue(this)
      } else {
        Continue(c.next or rhs)
      }
    }
  }

  // override to be right-associative
  override def or[T >: Out](other: Parser[T]) = {
    new OrElseParser(lhs, rhs or other)
  }
}

class BacktrackingParser[+Out](inner: Parser[Out], offset: Int) extends Parser[Out] {

  def this(inner: Parser[Out]) = this(inner, 0)

  def decode(buffer: ChannelBuffer) = {
    val start = buffer.readerIndex

    buffer.readerIndex(start + offset)

    // complains that Out is unchecked here, but this cannot fail, so
    // live with the warning.
    inner.decode(buffer) match {
      case e: Throw => {
        buffer.readerIndex(start)
        e
      }
      case r: Return[Out] => r
      case c: Continue[Out] => {
        if (c.next == inner && buffer.readerIndex == (start + offset)) {
          buffer.readerIndex(start)
          Continue(this)
        } else {
          val newOffset = buffer.readerIndex - start
          buffer.readerIndex(start)
          Continue(new BacktrackingParser(c.next, newOffset))
        }
      }
    }
  }
}
