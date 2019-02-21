package org.change.v2.analysis.constraint

import java.io.{Closeable, File, PrintStream}
import java.util.UUID

import org.change.utils.ToDot

import scala.collection.mutable
import scala.util.hashing.MurmurHash3

trait Node {
  def id() : String
}
class TerminalNode(val v: Boolean) extends Node {
  def id() : String = v.toString
}
object TrueNode extends TerminalNode(true)
object FalseNode extends TerminalNode(false)

object TerminalNode {
  def apply(v: Boolean): TerminalNode = if (v) TrueNode else FalseNode
}
case class VarNode(op: OP, low: Node, high: Node) extends Node {
  def id() : String = {
    op.toString + "," + System.identityHashCode(low) + "," + System.identityHashCode(high)
  }
}

object cachedefs {
  var crt = 0
  def next() : Int = {
    crt += 1
    crt - 1
  }

  class CacheKey(val op: OP, val low: Node, val high: Node) {
    override def hashCode(): Int =
      MurmurHash3.arrayHash(
        Array(op.hashCode,
              System.identityHashCode(low),
              System.identityHashCode(high)))

    override def equals(o: scala.Any): Boolean = o match {
      case v: CacheKey => op.equals(v.op) && low.eq(v.low) && high.eq(v.high)
      case _           => false
    }
  }

  implicit def fromTuple(x: (OP, Node, Node)): CacheKey = {
    new CacheKey(x._1, x._2, x._3)
  }

  type Cache = Map[CacheKey, Node]

}

import cachedefs._
import cachedefs.fromTuple

case class ITEPathCondition(root: Node, cache: Cache)
    extends PathCondition[ITEPathCondition] {
  override def &&(condition: Condition): ITEPathCondition = {
    val fc = ITEPathCondition.fromCondition(condition, cache)(OPOrdering)
    val combined = ITEPathCondition.and(fc._1, root, fc._2)(OPOrdering)
    val pc = ITEPathCondition(combined._1, combined._2)
//    if (!pc.root.isInstanceOf[TerminalNode]) {
//      val rnd = "%06d".format(next())
//      val f = new File(s"dots/${rnd}_andc/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_andc/full.dot")
//      val ps1 = new PrintStream(s"dots/${rnd}_andc/op1.dot")
//      val ps2 = new PrintStream(s"dots/${rnd}_andc/op2.dot")
//      ps.println(pc.toDot)
//      ps.close()
//
//      ps1.println(this.toDot)
//      ps1.close()
//
//      ps2.println(ITEPathCondition(fc._1, fc._2).toDot)
//      ps2.close()
//    }
    pc
  }

  override def ||(pathCondition: ITEPathCondition): ITEPathCondition = {
    val combined = ITEPathCondition.or(pathCondition.root,
                                       root,
                                       pathCondition.cache ++ cache)(OPOrdering)
    val pc = ITEPathCondition(combined._1, combined._2)
//    if (!pc.root.isInstanceOf[TerminalNode]) {
//      val rnd = "%06d".format(next())
//      val f = new File(s"dots/${rnd}_or/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_or/full.dot")
//      val ps1 = new PrintStream(s"dots/${rnd}_or/op1.dot")
//      val ps2 = new PrintStream(s"dots/${rnd}_or/op2.dot")
//      ps.println(pc.toDot)
//      ps.close()
//
//      ps1.println(this.toDot)
//      ps1.close()
//
//      ps2.println(pathCondition.toDot)
//      ps2.close()
//    }
    pc
  }

  override def &&(pathCondition: ITEPathCondition): ITEPathCondition = {
    val combined =
      ITEPathCondition.and(pathCondition.root,
                           root,
                           pathCondition.cache ++ cache)(OPOrdering)
    val pc = ITEPathCondition(combined._1, combined._2)
//    if (!pc.root.isInstanceOf[TerminalNode]) {
//      val rnd = "%06d".format(next())
//      val f = new File(s"dots/${rnd}_and/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_and/full.dot")
//      val ps1 = new PrintStream(s"dots/${rnd}_and/op1.dot")
//      val ps2 = new PrintStream(s"dots/${rnd}_and/op2.dot")
//      ps.println(pc.toDot)
//      ps.close()
//
//      ps1.println(this.toDot)
//      ps1.close()
//
//      ps2.println(pathCondition.toDot)
//      ps2.close()
//    }
    pc
  }

  def toDot: String = {
    val sb = new mutable.StringBuilder()

    sb.append("digraph BDD {\n")
    val visited = mutable.Set.empty[Node]
    def go(node: Node) : Unit =
      if (!visited.contains(node)) {
        visited += node
        node match {
          case vn : VarNode => sb.append(s"${ToDot.normalize(vn.id())} -> ${ToDot.normalize(vn.low.id())} [style=dashed];\n")
            sb.append(s"${ToDot.normalize(vn.id())} -> ${ToDot.normalize(vn.high.id())};\n")
            go(vn.low)
            go(vn.high)
          case tn : TerminalNode => sb.append(s"${tn.v} [shape=box];\n")
        }
      }
    go(root)
    sb.append("}")
    sb.toString()
  }


}

object OPOrdering extends Ordering[OP] {
  override def compare(x: OP, y: OP): Int = {
    x.toString.compareTo(y.toString)
  }
}

object ITEPathCondition {

  def and(left: Node, right: Node, cache: Cache)(
      ordering: Ordering[OP]): (Node, Cache) =
    if (left == FalseNode)
      (FalseNode, cache)
    else if (right == FalseNode)
      (FalseNode, cache)
    else if (left == TrueNode)
      (right, cache)
    else if (right == TrueNode)
      (left, cache)
    else {
      val v1 = left.asInstanceOf[VarNode]
      val v2 = right.asInstanceOf[VarNode]
      if (ordering.equiv(v1.op, v2.op)) {
        val lcomb = and(v1.low, v2.low, cache)(ordering)
        val rcomb = and(v1.high, v2.high, lcomb._2)(ordering)
        create(rcomb._2, v1.op, lcomb._1, rcomb._1)
      } else if (ordering.lt(v1.op, v2.op)) {
        val lcomb = and(v1.low, v2, cache)(ordering)
        val rcomb = and(v1.high, v2, lcomb._2)(ordering)
        create(rcomb._2, v1.op, lcomb._1, rcomb._1)
      } else {
        val lcomb = and(v1, v2.low, cache)(ordering)
        val rcomb = and(v1, v2.high, lcomb._2)(ordering)
        create(rcomb._2, v2.op, lcomb._1, rcomb._1)
      }
    }
  def or(left: Node, right: Node, cache: Cache)(
      ordering: Ordering[OP]): (Node, Cache) =
    if (left == TrueNode)
      (TrueNode, cache)
    else if (right == TrueNode)
      (TrueNode, cache)
    else if (left == FalseNode)
      (right, cache)
    else if (right == FalseNode)
      (left, cache)
    else {
      val v1 = left.asInstanceOf[VarNode]
      val v2 = right.asInstanceOf[VarNode]
      if (ordering.equiv(v1.op, v2.op)) {
        val lcomb = or(v1.low, v2.low, cache)(ordering)
        val rcomb = or(v1.high, v2.high, lcomb._2)(ordering)
        create(rcomb._2, v1.op, lcomb._1, rcomb._1)
      } else if (ordering.lt(v1.op, v2.op)) {
        val lcomb = or(v1.low, v2, cache)(ordering)
        val rcomb = or(v1.high, v2, lcomb._2)(ordering)
        create(rcomb._2, v1.op, lcomb._1, rcomb._1)
      } else {
        val lcomb = or(v1, v2.low, cache)(ordering)
        val rcomb = or(v1, v2.high, lcomb._2)(ordering)
        create(rcomb._2, v2.op, lcomb._1, rcomb._1)
      }
    }

  def not(left: Node, cache: Cache)(ordering: Ordering[OP]): (Node, Cache) =
    if (left == TrueNode)
      (FalseNode, cache)
    else if (left == FalseNode)
      (TrueNode, cache)
    else {
      val v1 = left.asInstanceOf[VarNode]
      val nl = not(v1.low, cache)(ordering)
      val nh = not(v1.high, nl._2)(ordering)
      create(nh._2, v1.op, nl._1, nh._1)
    }

  def create(cache: Cache, op: OP, low: Node, high: Node): (Node, Cache) =
    if (low.equals(high)) {
      (low, cache)
    } else {
      val varNode = VarNode(op, low = low, high = high)
      (varNode, cache)
//      if (cache.contains((op, low, high))) {
//        (cache((op, low, high)), cache)
//      } else {
//        val varNode = VarNode(op, low = low, high = high)
//        (varNode, cache + (fromTuple(op, low, high) -> varNode))
//      }
    }

  def orDefault(): ITEPathCondition = new ITEPathCondition(FalseNode, Map.empty)
  def apply(): ITEPathCondition = new ITEPathCondition(TrueNode, Map.empty)
  def fromCondition(condition: Condition, cache: Cache)(
      ordering: Ordering[OP]): (Node, Cache) = condition match {
    case o: OP => create(cache, o, low = FalseNode, high = TrueNode)
    case FAND(conditions) =>
      conditions.foldLeft((TrueNode, cache): (Node, Cache))((acc, x) => {
        val r = fromCondition(x, acc._2)(ordering)
        and(acc._1, r._1, r._2)(ordering)
      })
    case FOR(conditions) =>
      conditions.foldLeft((FalseNode, cache): (Node, Cache))((acc, x) => {
        val r = fromCondition(x, acc._2)(ordering)
        or(acc._1, r._1, r._2)(ordering)
      })
    case FNOT(c) =>
      val fc = fromCondition(c, cache)(ordering)
      not(fc._1, fc._2)(ordering)
    case TRUE  => (TrueNode, cache)
    case FALSE => (FalseNode, cache)
    case _     => ???
  }
}
