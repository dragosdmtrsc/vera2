package org.change.v2.analysis.equivalence

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.expression.concrete.nonprimitive.{:@ => :@}
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.:==:
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import z3.scala.Z3Context
import org.change.v2.analysis.z3.Z3Util
import z3.scala.Z3Pattern
import z3.scala.Z3Sort
import z3.scala.Z3Symbol
import org.change.v2.analysis.constraint.LT_E
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.nonprimitive.Plus
import org.change.v2.analysis.expression.concrete.nonprimitive.Minus
import org.change.v2.analysis.expression.concrete.nonprimitive.Reference
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.constraint.Constraint
import org.change.v2.analysis.constraint.AND
import org.change.v2.analysis.constraint.OR
import org.change.v2.analysis.constraint.LTE_E
import org.change.v2.analysis.constraint.EQ_E
import org.change.v2.analysis.constraint.GT_E
import org.change.v2.analysis.constraint.GTE_E
import org.change.v2.analysis.memory.Value
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.analysis.expression.concrete.nonprimitive.PlusE
import org.change.v2.analysis.expression.concrete.nonprimitive.MinusE
import scala.collection.mutable.Stack
import javax.swing.tree.DefaultMutableTreeNode
import org.change.v2.stategraph.StateGraph
import org.change.v2.analysis.constraint.NOT
import java.io.FileInputStream
import org.change.utils.prettifier.JsonUtil
import java.io.PrintWriter
import org.change.v2.stategraph.Gauss
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.{:<=:, :>=:, :&:}
import org.change.v2.analysis.expression.concrete.nonprimitive.{:-:, :+:}
import org.change.v2.analysis.processingmodels.instructions.Forward

object SuperExpression {
  def fromConstant(x: Long): SuperExpression = {
    SuperExpression(Map("" -> x))
  }

  def apply(v: Long): SuperExpression = {
    fromConstant(v)
  }

  def apply(): SuperExpression = {
    fromConstant(0)
  }

  def fromSymbol(y: String): SuperExpression = {
    SuperExpression(Map(y -> 1L))
  }

  def apply(v: SymbolicValue): SuperExpression = {
    fromSymbol("sym" + v.id.toString())
  }

  def apply(v: ConstantValue): SuperExpression = {
    fromConstant(v.value)
  }

  def children(v: DefaultMutableTreeNode) = {
    val enumer = v.children()
    var lst = List[DefaultMutableTreeNode]()
    while (enumer.hasMoreElements())
      lst = enumer.nextElement().asInstanceOf[DefaultMutableTreeNode] :: lst
    lst
  }

  def allChildren(v: DefaultMutableTreeNode)(q: DefaultMutableTreeNode => Boolean): List[DefaultMutableTreeNode] = {
    if (q(v))
      v :: children(v).flatMap { x => allChildren(x)(q) }
    else
      children(v).flatMap { x => allChildren(x)(q) }
  }

  private def exprSibling(v: DefaultMutableTreeNode) = {
    children(v.getParent.asInstanceOf[DefaultMutableTreeNode]).find { x =>
      x.getUserObject.isInstanceOf[StateGraph.NodeTag] &&
        x.getUserObject.asInstanceOf[StateGraph.NodeTag].isExpression()
    }.get
  }

  def apply(v: DefaultMutableTreeNode): SuperExpression = {
    if (v.getUserObject.isInstanceOf[StateGraph.NodeTag]) {
      val nt = v.getUserObject.asInstanceOf[StateGraph.NodeTag]
      nt match {
        case StateGraph.NodeTag.SYMBOL => fromSymbol(children(v).head.getUserObject.asInstanceOf[String])
        case StateGraph.NodeTag.CONSTANT => apply(children(v).head.getUserObject.asInstanceOf[Long])
        case StateGraph.NodeTag.PLUS => children(v).foldLeft(SuperExpression(0))((acc, x) => {
          acc + SuperExpression(x)
        })
        case StateGraph.NodeTag.MINUS => {
          val chlds = children(v)
          val first = chlds.head
          val tail = chlds.tail
          apply(first) - tail.foldLeft(SuperExpression(0))((acc, x) => {
            acc + SuperExpression(x)
          })
        }
        case StateGraph.NodeTag.LT => {
          val chlds = children(v)
          val first = chlds.head
          apply(exprSibling(v)) + SuperExpression(1) + SuperExpression(SymbolicValue()) - apply(first)
        }
        case StateGraph.NodeTag.LTE => {
          val chlds = children(v)
          val first = chlds.head
          apply(first) + SuperExpression(SymbolicValue()) - apply(exprSibling(v))
        }
        case StateGraph.NodeTag.EQ => {
          val chlds = children(v)
          val first = chlds.head
          -apply(first) + apply(exprSibling(v))
        }
        case StateGraph.NodeTag.GT => {
          val chlds = children(v)
          val first = chlds.head
          apply(first) + SuperExpression(SymbolicValue()) + SuperExpression(1) - apply(exprSibling(v))
        }
        case StateGraph.NodeTag.GTE => {
          val chlds = children(v)
          val first = chlds.head
          apply(first) + SuperExpression(SymbolicValue()) - apply(exprSibling(v))
        }
        case _ => ???
      }
    } else {
      throw new UnsupportedOperationException("Can't handle the truth")
    }
  }

}

case class SuperExpression(coefs: Map[String, Long]) {
  def -(right: SuperExpression) = {
    this + (-right)
  }

  override def toString = {
    coefs.map(x => x._2 + {
      if (x._1 != "")
        "*" + x._1
      else
        ""
    }
    ).mkString("+")
  }

  def isPresent(sym: String) = coefs.contains(sym)

  def apply(sym: String) = coefs(sym)

  def +(right: SuperExpression) = {
    SuperExpression(coefs.map(kv => {
      if (right.isPresent(kv._1)) {
        (kv._1 -> (kv._2 + right(kv._1)))
      }
      else {
        kv
      }
    }) ++ right.coefs.filter { x => !this.isPresent(x._1) })
  }

  def unary_-() = {
    SuperExpression(coefs.map(kv => (kv._1, -kv._2)).toMap)
  }
}

case class System(list: List[SuperExpression]) {
  override def toString = {
    list.map { x => x.toString() }.mkString(";\n")
  }

  lazy val orderedVars = variables._1.toSeq.sortWith { (y, v) => y._2 < v._2 }.map {
    _._1
  }

  lazy val vars = variables._1
  lazy val nVars = variables._2

  private lazy val variables = {
    list.foldLeft((Map[String, Int](), 0)) { (acc, x) => {
      x.coefs.foldLeft(acc)((acc2, y) => {
        if (y._1 != "" && !acc2._1.contains(y._1))
          (acc2._1 + (y._1 -> acc2._2), acc2._2 + 1)
        else
          acc2
      })
    }
    }
  }

  private lazy val eqMap = list.map { x =>
    x.coefs.map(kv =>
      if (kv._1 != "")
        vars(kv._1) -> kv._2
      else
        -1 -> kv._2
    )
  }

  lazy val systemMatrix = {
    Array.tabulate(list.size, nVars)((x, y) => {
      if (eqMap(x).contains(y))
        eqMap(x)(y)
      else 0L
    })
  }

  lazy val freeCoefs = Array.tabulate(list.size, 1)((x, y) => {
    if (eqMap(x).contains(-1))
      -eqMap(x)(-1)
    else 0L
  })

  def dumpToDir(prefix: String) {
    val tmpX = new PrintWriter(s"${prefix}_mat")
    val tmpXb = new PrintWriter(s"${prefix}_b")
    val tmpXsyms = new PrintWriter(s"${prefix}_syms")
    tmpX.println("[" + systemMatrix.map { x => "[" + x.mkString(",") + "]" }.mkString(",") + "]")
    tmpXb.println("[" + freeCoefs.map { x => "[" + x.mkString(",") + "]" }.mkString(",") + "]")
    tmpXsyms.println("[" + orderedVars.map {
      "\"" + _ + "\""
    }.mkString(",") + "]")
    tmpXsyms.close()
    tmpXb.close()
    tmpX.close()
  }

  def removeTrivials(): System = {
    this.copy(list = list.filter { x => x.coefs.keySet.size != 1 })
  }

}

object System {

  def apply(v: DefaultMutableTreeNode) = {
    val states = SuperExpression.children(v).filter { x => x.getUserObject.equals(StateGraph.NodeTag.STATE) }
    states.map { x => {
      new System(SuperExpression.allChildren(x)(q => q.getUserObject.equals(StateGraph.NodeTag.EQ) ||
        q.getUserObject.equals(StateGraph.NodeTag.LT) ||
        q.getUserObject.equals(StateGraph.NodeTag.LTE) ||
        q.getUserObject.equals(StateGraph.NodeTag.GT) ||
        q.getUserObject.equals(StateGraph.NodeTag.GTE)
      ).map { t => SuperExpression(t) }).removeTrivials
    }
    }
  }
}

object EquivalenceProver {

  def toSystems(state: State) = {
    import Equation._
    val rooted = convertToTree(state)
    StateGraph.removeReferences(rooted)
    while (StateGraph.removeAND(rooted) || StateGraph.removeNOT(rooted) || StateGraph.removeOR(rooted)) {}
    val syspw = new PrintWriter("sys.out")
    val rootpw = new PrintWriter("root.out")
    System(rooted)
  }

  def existsIn(state: State,
               rightStates: List[State],
               tags: List[Either[Int, String]])(portMapping: (String, String) => Boolean): Boolean = {
    import Equation._
    val filtered = rightStates.filter { x => portMapping(x.history.head, state.history.head) }
    val exprs = tags.flatMap { x => {
      val vOpt = x match {
        case Left(a) => state.memory.eval(a)
        case Right(id) => state.memory.eval(id)
      }
      vOpt.map { y => y.e }
    }
    }

    val asStates = filtered.map { y => {
      tags.zip(exprs).foldLeft(Some(y): Option[State]) { (acc, x) => {
        if (!acc.isEmpty) {
          val vOpt = x._1 match {
            case Left(a) => y.memory.addConstraint(a, EQ_E(x._2))
            case Right(id) => y.memory.addConstraint(id, EQ_E(x._2))
          }
          vOpt.map { z => acc.get.copy(memory = z) }
        }
        else {
          acc
        }
      }
      }
    }
    }.filter { x => x.isDefined }.map { x => x.get }

    val systems = toSystems(state)
    val allOthers = asStates.flatMap(toSystems)

    systems.forall { sys => {
      allOthers.exists { x => forallSolsExistsSolPrime(sys, x) }
    }
    }
  }

  def main(x: Array[String]) {
    import org.change.v2.util.canonicalnames._
    val fis = new FileInputStream(x(0))
    //    val states = JsonUtil.fromJson[List[State]](fis)
    //    val st = states(0)

    val init = State.clean
    val cCommon = InstructionBlock(
      Allocate("y"),
      Assign("y", SymbolicValue()),
      Constrain("y", :&:(:<=:(ConstantValue(100)), :>=:(ConstantValue(0))))
    )
    //    val cLeft = InstructionBlock(
    //                  Assign("y", :-:(ConstantValue(100), :@("y"))),
    //                  Forward("out")
    //                )


    val cLeft = InstructionBlock(
      Allocate("x"),
      Assign("x", :-:(:@("y"), ConstantValue(50))),
      Assign("y", :+:(:@("x"), ConstantValue(50))),
      Forward("out")
    )
    val cRight = InstructionBlock(
      Assign("y", :@("y")),
      Forward("out")
    )

    val st = InstructionBlock(cCommon, cLeft)(init)._1(0)
    val states = InstructionBlock(cCommon, cRight)(init)._1

    if (existsIn(st, states, List[Either[Int, String]](Right("y")))((x, y) => x == y)) {
      println("Are equivalent")
    }
    else {
      println("Not equivalent under " + Right("y"))
    }
  }

  def forallSolsExistsSolPrime(x: System, y: System): Boolean = {

    x.dumpToDir("tmpX")
    y.dumpToDir("tmpY")
    import sys.process._
    val result: String = "python solve_system.py tmpX tmpY" !!
    val cp = result.trim().equals("True")
    cp
  }
}


object Equation {

  def main(argv: Array[String]) {
    val fis = new FileInputStream(argv(0))
    val states = JsonUtil.fromJson[List[State]](fis)

    val st = states(0)
    var root = convertToTree(st)
    StateGraph.removeReferences(root)
    while (StateGraph.removeAND(root) || StateGraph.removeNOT(root) || StateGraph.removeOR(root)) {}
    val syspw = new PrintWriter("sys.out")
    val rootpw = new PrintWriter("root.out")
    val systems = System(root)
    syspw.println(systems)
    rootpw.println(toString(root))

    syspw.println(systems.map { x => (x.vars, x.nVars) })
    syspw.println(systems.map { x => x.systemMatrix.map { x => x.mkString(",") }.mkString(",") })
    syspw.println(systems.map { x => x.freeCoefs.map { x => x.mkString(",") }.mkString(",") })

    syspw.close
    rootpw.close
  }

  def toString(treeNode: DefaultMutableTreeNode, level: Int = 0): String = {
    var str = ""
    for (i <- 0 until level)
      str += "  "
    str += treeNode.toString()
    str += "\n"
    val e = treeNode.children()
    while (e.hasMoreElements())
      str += toString(e.nextElement().asInstanceOf[DefaultMutableTreeNode], level + 1)
    str
  }

  def toCoefs(expr: Expression): SuperExpression = {
    expr match {
      case Plus(a, b) => toCoefs(a.e) + toCoefs(b.e)
      case Minus(a, b) => toCoefs(a.e) - toCoefs(b.e)
      case Reference(v, _) => toCoefs(v.e)
      case ConstantValue(v, _, _) => SuperExpression.fromConstant(v)
      case u: SymbolicValue => SuperExpression.fromSymbol(s"sym${u.id.toString}")
    }
  }

  def isNormal(expr: Expression): Boolean = {
    expr match {
      case Plus(a, b) => false
      case Minus(a, b) => false
      case PlusE(a, b) => isNormal(a) && isNormal(b)
      case MinusE(a, b) => isNormal(a) && isNormal(b)
      case Reference(v, _) => false
      case ConstantValue(v, _, _) => true
      case ConstantStringValue(v) => true
      case u: SymbolicValue => true
      case _ => false
    }
  }

  def isNormal(c: Constraint): Boolean = {
    c match {
      case AND(block) => false
      case LTE_E(e) => isNormal(e)
      case LT_E(e) => isNormal(e)
      case GT_E(e) => isNormal(e)
      case GTE_E(e) => isNormal(e)
      case EQ_E(e) => isNormal(e)
      case _ => false
    }
  }

  def isNormal(value: Value): Boolean = {
    isNormal(value.e) && value.cts.forall { x => isNormal(x) }
  }

  def isNormal(v: Any): Boolean = {
    v match {
      case y: Value => isNormal(y)
      case y: Expression => isNormal(y)
      case y: Constraint => isNormal(y)
      case y: State => values(y).forall { x => isNormal(x) }
      case y: Tree => y.nodes.forall(isNormal)
      case _ => false
    }
  }

  def convertToTree(state: State): DefaultMutableTreeNode = {
    val root = new DefaultMutableTreeNode()
    root.setUserObject(StateGraph.NodeTag.ROOT)
    val stateNode = new DefaultMutableTreeNode()
    stateNode.setUserObject(StateGraph.NodeTag.STATE)
    root.add(stateNode)

    val vals = values(state)
    for (v <- vals) {
      stateNode.add(convertToTree(v))
    }
    root
  }

  def convertToTree(value: Value): DefaultMutableTreeNode = {
    val root = new DefaultMutableTreeNode()
    root.setUserObject(StateGraph.NodeTag.VALUE)
    root.add(convertToTree(value.e))
    for (c <- value.cts)
      root.add(convertToTree(c))
    root
  }

  def convertToTree(ct: Constraint): DefaultMutableTreeNode = {
    val root = new DefaultMutableTreeNode()
    ct match {
      case NOT(ct) => {
        root.setUserObject(StateGraph.NodeTag.NOT)
        root.add(convertToTree(ct))
      }
      case AND(block) => {
        root.setUserObject(StateGraph.NodeTag.AND)
        for (b <- block)
          root.add(convertToTree(b))
      }
      case OR(block) => {
        root.setUserObject(StateGraph.NodeTag.OR)
        for (b <- block)
          root.add(convertToTree(b))
      }
      case LTE_E(e) => {
        root.setUserObject(StateGraph.NodeTag.LTE)
        root.add(convertToTree(e))
      }
      case LT_E(e) => {
        root.setUserObject(StateGraph.NodeTag.LT)
        root.add(convertToTree(e))
      }
      case GT_E(e) => {
        root.setUserObject(StateGraph.NodeTag.GT)
        root.add(convertToTree(e))
      }
      case GTE_E(e) => {
        root.setUserObject(StateGraph.NodeTag.GTE)
        root.add(convertToTree(e))
      }
      case EQ_E(e) => {
        root.setUserObject(StateGraph.NodeTag.EQ)
        root.add(convertToTree(e))
      }
    }
    root
  }

  def convertToTree(expr: Expression): DefaultMutableTreeNode = {
    val root = new DefaultMutableTreeNode()
    expr match {
      case ConstantValue(x, _, _) => {
        root.setUserObject(StateGraph.NodeTag.CONSTANT)
        root.add(new DefaultMutableTreeNode(x))
      }
      case ConstantStringValue(x) => {
        root.setUserObject(StateGraph.NodeTag.CONSTANT)
        root.add(new DefaultMutableTreeNode(x.hashCode()))
      }
      case r: SymbolicValue => {
        root.setUserObject(StateGraph.NodeTag.SYMBOL)
        root.add(new DefaultMutableTreeNode(s"sym${r.id}"))
      }
      case Plus(a, b) => {
        root.setUserObject(StateGraph.NodeTag.PLUS)
        val aref = convertToTree(Reference(a))
        val bref = convertToTree(Reference(b))
        root.add(aref)
        root.add(bref)
      }
      case Minus(a, b) => {
        root.setUserObject(StateGraph.NodeTag.MINUS)
        val aref = convertToTree(Reference(a))
        val bref = convertToTree(Reference(b))
        root.add(aref)
        root.add(bref)
      }
      case Reference(v, _) => {
        root.setUserObject(StateGraph.NodeTag.REFERENCE)
        root.add(convertToTree(v))
      }
    }
    root
  }

  def values(state: State): List[Value] = {
    (state.memory.rawObjects.values.
      flatMap { x => x.value }.toList ++
      state.memory.symbols.values.flatMap { x => x.value }.toList)
  }

  case class Tree(nodes: List[List[Value]]) {
    def addState(state: State) = copy(nodes = values(state) :: nodes)

    def addValues(vals: List[Value]) = copy(nodes = vals :: nodes)

    def children(node: Any): List[Any] = {
      node match {
        case y: Tree => nodes
        case y: SymbolicValue => Nil
        case y: ConstantValue => Nil
        case y: ConstantStringValue => Nil
        case head :: tail if head.isInstanceOf[Value] => head :: tail
        case y: Value => y.e :: y.cts.asInstanceOf[List[Any]]
        case Plus(a, b) => List[Any](a, b)
        case Minus(a, b) => List[Any](a, b)
        case PlusE(a, b) => List[Any](a, b)
        case MinusE(a, b) => List[Any](a, b)
        case Reference(v, _) => v :: Nil
        case AND(block) => block.toList
        case OR(block) => block.toList
        case LTE_E(e) => e :: Nil
        case LT_E(e) => e :: Nil
        case GT_E(e) => e :: Nil
        case GTE_E(e) => e :: Nil
        case EQ_E(e) => e :: Nil
        case _ => throw new UnsupportedOperationException("Unknown")
      }
    }
  }

  def flatten(initial: State) = {

  }

  def toCoefs(expr: Expression, constraint: Constraint): List[SuperExpression] = {
    constraint match {
      case AND(block) => {
        block.flatMap(x => toCoefs(expr, x))
      }
      case LTE_E(e) => {
        List[SuperExpression](toCoefs(expr) +
          SuperExpression(SymbolicValue()) -
          toCoefs(e)
        )
      }
      case LT_E(e) => {
        List[SuperExpression](toCoefs(expr) +
          SuperExpression(SymbolicValue()) +
          SuperExpression(1) -
          toCoefs(e)
        )
      }
      case GT_E(e) => {
        List[SuperExpression](toCoefs(e) - toCoefs(expr) +
          SuperExpression(SymbolicValue()) +
          SuperExpression(1)
        )
      }
      case GTE_E(e) => {
        List[SuperExpression](toCoefs(e) - toCoefs(expr) +
          SuperExpression(SymbolicValue())
        )
      }
      case EQ_E(e) => {
        List[SuperExpression](toCoefs(e) - toCoefs(expr))
      }
      case _ => throw new UnsupportedOperationException("Flatten first, then come to me")
    }
  }

}

object Z3Test {
  def main(argv: Array[String]) {
    val z3 = new Z3Context
    val symbol = z3.mkIntSymbol(1)
    val constant = z3.mkIntConst("a")
    val slv = z3.mkSolver()

  }

  def equivalent(left: State, right: State): Boolean = {
    // Step 1: Normalize states => i.e. push NOT Inside, flatten AND, pop OR
    true
  }

}
