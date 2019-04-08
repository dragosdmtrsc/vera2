package org.change.p4.control

import org.change.p4.control.types.P4Type
import org.change.p4.model.{HeaderInstance, Switch}

trait P4Query {
  def as[T <: P4Query]: T = asInstanceOf[T]
  def ok(): P4Query = this
  def err(): P4Query = ???
  def errorCause(): P4Query = ???
  def fails(because: String): P4Query = ???
  def field(name: String): P4Query = ???
  def len(): LiteralExprValue = ???
  def valid(): P4Query = ???
  def next(): P4Query = ???
  def fresh(prefix : String = "fresh"): P4Query = ???
  def zeros(): P4Query = ???
  def set(src: P4Query, value: P4Query): P4Query = ???
  def ite(thn: P4Query, els: P4Query): P4Query = ???
  def int(value: BigInt): P4Query = ???
  def int(value: BigInt, p4Type: P4Type): P4Query = ???
  def isArray: Boolean = false

  def toInt: Option[BigInt] = None
  def toBool: Option[Boolean] = None

  def fields(): Iterable[String] = ???

  def ===(other: P4Query): P4Query = ???
  def <(other: P4Query): P4Query = ???
  def >(other: P4Query): P4Query = ???
  def >=(other: P4Query): P4Query = ???
  def <=(other: P4Query): P4Query = ???
  def !=(other: P4Query): P4Query = !(this === other)

  def apply(idx: P4Query): P4Query = ???
  def &&(other: P4Query): P4Query = ???
  def ||(other: P4Query): P4Query = ???
  def &(other: P4Query): P4Query = ???
  def |(other: P4Query): P4Query = ???
  def ^(other: P4Query): P4Query = ???
  def +(other: P4Query): P4Query = ???
  def <<(other: P4Query): P4Query = ???
  def >>(other: P4Query): P4Query = ???
  def -(other: P4Query): P4Query = ???

  def unary_~(): P4Query = ???
  def unary_-(): P4Query = ???
  def unary_!(): P4Query = unary_~()
  def boolVal(bv: Boolean): P4Query = ???
}
trait P4TableQuery extends P4Query {
  def isDefault: P4Query = ???
  def isAction(act: String): P4Query = ???
  def getParam(act: String, parmName: String): P4Query = ???
}
class P4Memory(val switch: Switch) extends P4Query {
  def when(whenCases: Iterable[(P4Query, P4Query)]): P4Query = {
    if (whenCases.isEmpty)
      throw new IllegalArgumentException("when must have at least one case")
    val hd = whenCases.head
    val zero = hd._2.zeros()
    whenCases.foldLeft(zero)((acc, x) => {
      x._1.ite(x._2, acc)
    })
  }
  def or(qries: Iterable[P4Query]): P4Query = {
    if (qries.isEmpty)
      boolVal(false)
    else if (qries.size == 1) qries.head
    else qries.tail.foldLeft(qries.head)((acc, qry) => acc || qry)
  }
  def and(qries: Iterable[P4Query]): P4Query = {
    if (qries.isEmpty)
      boolVal(false)
    else if (qries.size == 1) qries.head
    else qries.tail.foldLeft(qries.head)((acc, qry) => acc && qry)
  }

  def root(): P4Query = ???
  def where(p4Query: P4Query): P4Memory = ???
  def update(dst: P4Query, src: P4Query): P4Memory = ???
  def packet(): PacketQuery = ???
  def standardMetadata(): P4Query = field("standard_metadata")
  def query(table: String, params: Iterable[P4Query]): P4TableQuery = ???
  def lastQuery(table: String): P4TableQuery = ???

  def ingressAllowed(p4Query: P4Query): P4Query = ???
  def egressAllowed(p4Query: P4Query): P4Query = ???

  def cloneSession(cloneSpec: P4Query): P4Query = ???
  def multicastSession(mgid: P4Query): P4Query = ???
}
trait PacketQuery extends P4Query {
  def apply(from: P4Query, to: P4Query): P4Query = ???
  def pop(n: P4Query): PacketQuery = ???
  def prepend(what: P4Query): PacketQuery = ???
  def contents() : P4Query = ???
  def packetLength() : P4Query = ???
}
case class HeaderQuery(header: HeaderInstance) extends P4Query {
  // returns an ORDERED sequence of fields as queries
}
trait P4Value extends P4Query
trait LiteralExprValue extends P4Query {
  def nr: BigInt
}
