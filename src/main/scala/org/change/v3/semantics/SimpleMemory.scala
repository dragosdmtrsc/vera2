package org.change.v3.semantics

import org.change.v2.interval.IntervalOps
import org.change.v3.syntax._
import z3.scala.Z3AST

import scala.collection.immutable.SortedMap


case class SimpleMemoryObject(ast : Option[Z3AST], size : Int)
object SimpleMemoryObject {
  def apply(sz : Int) : SimpleMemoryObject = SimpleMemoryObject(None, sz)
  def apply(ast : Z3AST, sz : Int) : SimpleMemoryObject = SimpleMemoryObject(Some(ast), sz)
  def apply(varname : String, sz : Int) : SimpleMemoryObject = SimpleMemoryObject(
    context.mkConst(varname, context.mkBVSort(sz)), sz
  )
}
case class SimpleMemory(
                         pathCondition : List[Z3AST] = Nil,
                         errorCause : Option[String] = None,
                         history : List[String] = Nil,
                         memTags : SortedMap[String, Int] = SortedMap.empty,
                         rawObjects : SortedMap[Int, SimpleMemoryObject] = SortedMap.empty,
                         symbols : SortedMap[String, SimpleMemoryObject] = SortedMap.empty,
                         done : Boolean = false
                       ) {
  override def toString: String = symbols.keys.mkString(",")
  def finish() : SimpleMemory = copy(done = true)
  def finished() : Boolean = done || errorCause.nonEmpty
  def forwardTo(loc : String): SimpleMemory = copy(history = loc :: history)
  def location : String = history.head
  def createTag(name: String, value: Int): SimpleMemory =
    copy(memTags = memTags + (name -> value))
  def destroyTag(name : String) : Option[SimpleMemory] =
    memTags.get(name).map(_ => copy(memTags = memTags - name))
  def eval(tag: Intable): Option[Int] = tag match {
    case v: IntImprovements => Some(v.value)
    case Tag(name)          => memTags.get(name)
    case TagExp(plusTags, minusTags, rest) =>
      plusTags
        .foldLeft(Some(rest): Option[Int])((acc, r) => {
          acc.flatMap(f => eval(r).map(_ + f))
        })
        .flatMap(f => {
          minusTags.foldLeft(Some(f): Option[Int])((acc, r) => {
            acc.flatMap(f => eval(r).map(_ - f))
          })
        })
    case _ => ???
  }
  def getAST(sym : String) : Z3AST = symbols(sym).ast.get

  def canRead(s: String): Boolean = symbols.contains(s)
  def canRead(t: Int): Boolean = rawObjects.contains(t)

  def fail(because: String): SimpleMemory = copy(errorCause = Some(because))
  def error: String = errorCause.getOrElse("")
  def addCondition(condition: Condition): SimpleMemory =
    copy(pathCondition = condition :: pathCondition)
  def addCondition(condition: SimplePathCondition): SimpleMemory =
    copy(pathCondition = pathCondition ++ condition)

  def Tag(name: String, value: Int): Option[SimpleMemory] =
    Some(copy(memTags = memTags + (name -> value)))
  def UnTag(name: String): Option[SimpleMemory] =
    Some(copy(memTags = memTags - name))
  def canModifyExisting(a: Int, size: Int): Boolean =
    rawObjects.contains(a) && rawObjects(a).size == size
  def canModifyExisting(a : Int) : Boolean = rawObjects.contains(a)

  def doesNotOverlap(a: Int, size: Int): Boolean = {
    (!rawObjects.contains(a)) &&
      rawObjects.forall(kv =>
        !IntervalOps
          .intervalIntersectionIsInterval(a, a + size, kv._1, kv._1 + kv._2.size))
  }
  def canModify(a: Int, size: Int): Boolean =
    doesNotOverlap(a, size) ||
      (rawObjects.contains(a) && rawObjects(a).size == size)
  def Allocate(a: Int, size: Int): Option[SimpleMemory] =
    if (canModifyExisting(a, size))
      Some(
        copy(
          rawObjects = rawObjects + (a -> rawObjects(a).copy(size = size))
        ))
    else if (canModify(a, size))
      Some(
        copy(
          rawObjects = rawObjects + (a -> SimpleMemoryObject(size))
        ))
    else None
  def Allocate(id: String, sz: Int): SimpleMemory =
    copy(symbols = symbols + (id -> SimpleMemoryObject(sz)))

  def assignNewValue(a: Int, exp: Z3AST): Option[SimpleMemory] =
    if (canRead(a)) {
      Some(
        copy(
          rawObjects = rawObjects + (a -> rawObjects(a).copy(ast = Some(exp)))
        ))
    } else {
      None
    }
  def assignNewValue(lv : LVExpr, exp: Z3AST) : Option[SimpleMemory] = lv match {
    case Symbol(id) => assignNewValue(id, exp)
    case Reference(i) => eval(i).flatMap(v => assignNewValue(v, exp))
  }
  def assignNewValue(id: String, exp: Z3AST): Option[SimpleMemory] =
    symbols
      .get(id)
      .map(h => copy(symbols = symbols + (id -> h.copy(ast = Some(exp)))))
  def deallocate(s: String): Option[SimpleMemory] =
    if (canRead(s))
      Some(copy(symbols = symbols - s))
    else
      None
  def deallocate(a: Int): Option[SimpleMemory] =
    if (canModifyExisting(a))
      Some(copy(rawObjects = rawObjects - a))
    else
      None
}
