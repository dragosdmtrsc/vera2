package org.change.p4.control.queryimpl
import org.change.p4.model.Switch
import com.microsoft.z3._
import org.change.utils.Z3Helper._
import scala.collection.mutable
import scala.collection.JavaConverters._

class PacketWrapper(implicit context: Context) {
  var axioms: List[Expr] = List.empty[Expr]
  def declareAxiom(AST: Expr): Unit = {
    axioms = AST :: axioms
  }
  val takeFuns = mutable.Map.empty[(Int, Int), FuncDecl]
  val popFuns = mutable.Map.empty[Int, FuncDecl]
  private var appends: mutable.TreeSet[Int] = mutable.TreeSet.empty[Int]

  def declareAppend(sz: Int): Unit = {
    appends = appends + sz
  }
  def nilPacket(): AST = {
    constructors.head.ConstructorDecl().apply()
  }

  // assumes that this guy is already resolved and can
  // be simply evald to concrete values
  def takeKind(v: Expr): Option[Int] = {
    appends.find(r => {
      val idx = indexing(r)
      constructors(idx).getTesterDecl.apply(v).getBool.getOrElse(false)
    })
  }
  def isNil(v: Expr): Boolean = {
    constructors
      .head
      .getTesterDecl
      .apply(v)
      .simplify()
      .getBool
      .getOrElse(false)
  }

  def unwrap(v: Expr, of: Int): (Expr, Expr) = {
    val idx = indexing(of)
    val oldone = constructors(idx).getAccessorDecls.head.apply(v).simplify()
    val appd = constructors(idx).getAccessorDecls()(1).apply(v).simplify()
    (oldone, appd)
  }

  def getTakeFun(start: Int, end: Int): FuncDecl = {
    takeFuns.getOrElseUpdate(
      (start, end),
      context.mkFuncDecl(
        s"take_${start}_$end",
        packetSort,
        context.mkBVSort(end - start)
      )
    )
  }

  private def mkConstructor(x : Int) : Constructor = {
    context.mkConstructor(s"prepend_$x", s"isprepend_$x",
      Array(s"pin_$x", s"x_$x"),
      Array(null, context.mkBitVecSort(x).asInstanceOf[Sort]),
      Array(0, 0)
    )
  }

  lazy val constructors: List[Constructor] =
    context.mkConstructor("nil",
      "isnil",
      null,
      null,
      null) :: appends.toList.zipWithIndex.map(xwi => {
    val x = xwi._1
    mkConstructor(x)
  })
  lazy val indexing: Map[Int, Int] = appends.toList.zipWithIndex.toMap.mapValues(_ + 1)
  lazy val packetSort: DatatypeSort =
    context.mkDatatypeSort("Packet", constructors.toArray)

  def prepend(packet: Expr, n: Int, v: Expr): Expr = {
    if (packet.getSort != sort()) {
      throw new IllegalArgumentException(
        s"cannot apply append_$n on $packet, " +
          s"expecting something of sort ${sort()}"
      )
    }
    if (v.getSort != context.mkBitVecSort(n))
      throw new IllegalArgumentException(
        s"cannot apply append_$n with argument $v of sort ${v.getSort}"
      )
    val consIdx = indexing(n)
    constructors(consIdx).ConstructorDecl().apply(packet, v)
  }

  def zero(): Expr = constructors.head.ConstructorDecl().apply()
  def sort(): Sort = packetSort
  def take(p: Expr, start: Int, end: Int): Expr = {
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
  val contextBound = mutable.Map.empty[Context, PacketWrapper]
  def apply(context: Context): PacketWrapper = {
    contextBound(context)
  }

  def nilPacket(context: Context): AST = {
    apply(context).nilPacket()
  }

  def initialize(switch: Switch, context: Context): Unit = {
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
