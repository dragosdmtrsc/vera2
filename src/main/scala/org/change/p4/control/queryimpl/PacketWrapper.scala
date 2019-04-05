package org.change.p4.control.queryimpl
import org.change.p4.model.Switch
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.mutable
import scala.collection.JavaConverters._

class PacketWrapper(implicit context: Z3Context) {
  var axioms: List[Z3AST] = List.empty[Z3AST]
  def declareAxiom(z3AST: Z3AST): Unit = {
    axioms = z3AST :: axioms
  }
  val takeFuns = mutable.Map.empty[(Int, Int), Z3FuncDecl]
  val popFuns = mutable.Map.empty[Int, Z3FuncDecl]
  private var appends: mutable.TreeSet[Int] = mutable.TreeSet.empty[Int]

  def declareAppend(sz: Int): Unit = {
    appends = appends + sz
  }
  def nilPacket(): Z3AST = {
    packetSort._2.head()
  }

  // assumes that this guy is already resolved and can
  // be simply evald to concrete values
  def takeKind(v: Z3AST): Option[Int] = {
    appends.find(r => {
      val idx = indexing(r)
      val simpd =
        context.getBoolValue(context.simplifyAst(packetSort._3(idx)(v)))
      if (simpd.nonEmpty) simpd.get
      else false
    })
  }
  def isNil(v: Z3AST): Boolean = {
    context
      .getBoolValue(context.simplifyAst(packetSort._3.head(v)))
      .getOrElse(false)
  }

  def unwrap(v: Z3AST, of: Int): (Z3AST, Z3AST) = {
    val idx = indexing(of)
    val oldone = context.simplifyAst(packetSort._4(idx).head(v))
    val appd = context.simplifyAst(packetSort._4(idx)(1)(v))
    (oldone, appd)
  }

  def getPopFun(of: Int): Z3FuncDecl = {
    popFuns.getOrElseUpdate(
      of,
      context.mkFuncDecl(s"pop_$of", packetSort._1, packetSort._1)
    )
  }

  def getTakeFun(start: Int, end: Int): Z3FuncDecl = {
    takeFuns.getOrElseUpdate(
      (start, end),
      context.mkFuncDecl(
        s"take_${start}_$end",
        packetSort._1,
        context.mkBVSort(end - start)
      )
    )
  }
  lazy val (packetSort, indexing) = {
    val (names, params, mapping) = appends.toList.zipWithIndex
      .map(xwi => {
        val x = xwi._1
        (
          s"prepend_$x",
          Seq(
            (s"pin_$x", RecursiveType(0)),
            (s"x_$x", RegularSort(context.mkBVSort(x)))
          ),
          xwi.copy(_2 = xwi._2 + 1)
        )
      })
      .unzip3
    (
      context
        .mkADTSorts(Seq(("Packet", "nil" :: names, Seq() :: params)))
        .head,
      mapping.toMap
    )
  }

  def prepend(packet: Z3AST, n: Int, v: Z3AST): Z3AST = {
    if (packet.getSort != sort()) {
      throw new IllegalArgumentException(
        s"cannot apply append_$n on $packet, " +
          s"expecting something of sort ${sort()}"
      )
    }
    if (v.getSort != context.mkBVSort(n))
      throw new IllegalArgumentException(
        s"cannot apply append_$n with argument $v of sort ${v.getSort}"
      )
    val consIdx = indexing(n)
    packetSort._2(consIdx)(packet, v)
  }

  def pop(packet: Z3AST, nr: Int): Z3AST = {
    if (packet.getSort != sort()) {
      throw new IllegalArgumentException(
        s"cannot apply pop on $packet, " +
          s"expecting something of sort ${sort()}"
      )
    }
    getPopFun(nr)(packet)
  }

  def zero(): Z3AST = packetSort._2.head()
  def sort(): Z3Sort = packetSort._1
  def take(p: Z3AST, start: Int, end: Int): Z3AST = {
    if (p.getSort != sort()) {
      throw new IllegalArgumentException(
        s"cannot apply take on $p, " +
          s"expecting something of sort ${sort()}"
      )
    }
    getTakeFun(start, end)(p)
  }
}

object PacketWrapper {
  val contextBound = mutable.Map.empty[Z3Context, PacketWrapper]
  def apply(context: Z3Context): PacketWrapper = {
    contextBound(context)
  }

  def nilPacket(context: Z3Context): Z3AST = {
    apply(context).nilPacket()
  }

  def initialize(switch: Switch, context: Z3Context): Unit = {
    val tm = new PacketWrapper()(context)
    switch.getInstances.asScala.foreach(i => {
      i.getLayout.getFields.asScala.foreach(f => {
        if (f.getLength > 0)
          tm.declareAppend(f.getLength)
      })
    })
    contextBound.put(context, tm)
  }
}
