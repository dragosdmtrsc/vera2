package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser.{Expression, FieldRef}
import org.change.v2.p4.model.table.TableDeclaration
import org.change.v2.p4.model.{ArrayInstance, Field, HeaderInstance, Switch}
import org.change.v3.semantics.SimpleMemory

import scala.collection.JavaConverters._

trait P4Query {
  def as[T<:P4Query] : T = asInstanceOf[T]
  def ok() : P4Query = this
  def err() : P4Query = ???
  def errorCause() : P4Query = ???
  def fails(because : String) : P4Query = ???
  def field(name : String) : P4Query = ???
  def len() : LiteralExprValue = ???
  def valid() : P4Query = ???
  def next() : P4Query = ???
  def fresh() : P4Query = ???
  def zeros() : P4Query = ???
  def set(src : P4Query, value : P4Query) : P4Query = ???
  def ite(thn : P4Query, els : P4Query): P4Query = ???
  def int(value : BigInt) : P4Query = ???
  def isArray: Boolean = false

  def ===(other : P4Query) : P4Query = ???
  def <(other : P4Query) : P4Query = ???
  def >(other : P4Query) : P4Query = ???
  def >=(other : P4Query) : P4Query = ???
  def <=(other : P4Query) : P4Query = ???
  def !=(other : P4Query) : P4Query = !(this === other)

  def apply(idx : P4Query) : P4Query = ???
  def &&(other : P4Query) : P4Query = ???
  def ||(other : P4Query) : P4Query = ???
  def &(other : P4Query) : P4Query = ???
  def |(other : P4Query) : P4Query = ???
  def ^(other : P4Query) : P4Query = ???
  def +(other : P4Query) : P4Query = ???
  def <<(other : P4Query) : P4Query = ???
  def -(other : P4Query) : P4Query = ???

  def unary_~() : P4Query = ???
  def unary_-() : P4Query = ???
  def unary_!() : P4Query = unary_~()
  def boolVal(bv : Boolean): P4Query = new LiteralBoolValue(bv)
}
case class P4TableQuery(tableDeclaration: TableDeclaration) extends P4Query {
  def isDefault() : P4Query = ???
  def exists() : P4Query = ???
  def isAction(act : String) : P4Query = ???
  def getParam(act : String, parmName : String) : P4Query = ???
}
class P4Memory(switch : Switch) extends P4Query {
  def when(whenCases : Iterable[(P4Query, P4Query)]): P4Query = ???
  def or(qries : Iterable[P4Query]) : P4Query = {
    if (qries.isEmpty)
      boolVal(false)
    else if (qries.size == 1) qries.head
    else qries.tail.foldLeft(qries.head)((acc, qry) => acc || qry)
  }
  def and(qries : Iterable[P4Query]) : P4Query = {
    if (qries.isEmpty)
      boolVal(false)
    else if (qries.size == 1) qries.head
    else qries.tail.foldLeft(qries.head)((acc, qry) => acc && qry)
  }

  def where(p4Query: P4Query) : P4Memory = ???
  def update(dst: P4Query, src : P4Query) : P4Memory = (dst, src) match {
    case (hq : HeaderQuery, hqsrc : HeaderQuery) =>
      if (hq.header != hqsrc.header)
        throw new IllegalArgumentException(s"can't assign headers of different kinds $dst := $src")
      else {
        hq.header.getLayout.getFields.asScala.foldLeft(this)((acc, f) => {
          acc.update(hq.field(f.getName), hqsrc.field(f.getName))
        }).update(hq.valid(), hqsrc.valid())
      }
  }
  def packet() : PacketQuery = ???
  def standardMetadata() : P4Query = field("standard_metadata")
  def query(table : String) : P4TableQuery = ???
  def lastQuery(table : String) : P4TableQuery = ???

  def fieldList() : P4Query = ???
}
class PacketQuery extends P4Query {
  def apply(from : P4Query, to : P4Query) : P4Query = ???
  def pop(n : P4Query) : PacketQuery = ???
  def append(what : P4Query) : PacketQuery = ???
}
case class HeaderQuery(header : HeaderInstance) extends P4Query {
  // returns an ORDERED sequence of fields as queries
  def fields() : Iterable[HeaderFieldQuery] = header.getLayout.getFields.asScala.map(HeaderFieldQuery(header, _))
}
case class ArrayIndex(h : ArrayInstance, idx : P4Query) extends HeaderQuery(h)
case class HeaderFieldQuery(header : HeaderInstance, fieldInstance : Field) extends P4Query
class ArrayQuery(header : ArrayInstance) extends P4Query {
  override val len: LiteralExprValue = new LiteralExprValue(header.getLength, -1)
  override val isArray: Boolean = true
  override def apply(idx: P4Query): HeaderQuery = ArrayIndex(header, idx)
}

trait P4Value extends P4Query
class LiteralBoolValue(v : Boolean) extends P4Query {
  override def unary_!(): P4Query = new LiteralBoolValue(!v)
}
class LiteralExprValue(val nr : BigInt, val width : Int) extends P4Query

trait Instantiator[Backend] {
  def getQuery(backend: Backend) : P4Memory = ???
  def instantiate(p4Query: P4Query) : P4Value
  def instantiateAs[T<:P4Value](p4Query: P4Query) : T = {
    instantiate(p4Query).asInstanceOf[T]
  }
}
class MemoryWrapper[T](t : T) extends P4Value
case class RichContext(ctx : P4Memory) {
  def validityFailure(p4Expr: P4Expr) : P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(p4Expr: P4BExpr) : P4Query = {
    MkQuery.validityFailure(ctx, p4Expr)
  }
  def validityFailure(expr: Expression) : P4Query = {
    MkQuery.validityFailure(ctx, expr)
  }
  def validityFailure(fref: FieldRef) : P4Query = {
    MkQuery.validityFailure(ctx, fref)
  }
  def apply(p4Expr: P4BExpr) : P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(p4Expr: P4Expr) : P4Query = {
    MkQuery(ctx, p4Expr)
  }
  def apply(fref: FieldRef) : P4Query = {
    MkQuery(ctx, new FieldRefExpr(fref))
  }
  def apply(expression: Expression) : P4Query = ???
}
object SMInstantiator {
  implicit def apply(ctx : P4Memory) : RichContext = RichContext(ctx)
  implicit def apply(simpleMemory : List[SimpleMemory]): Instantiator[List[SimpleMemory]] =
    new Instantiator[List[SimpleMemory]] {
      override def instantiate(p4Query: P4Query): P4Value = ???
  }
}