package org.change.v2.analysis.constraint

import java.io.{File, PrintStream}
import java.util.UUID

import org.change.v2.analysis.constraint.FOR.makeFOR
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.util.ToDot
import org.change.v2.util.conversion.RepresentationConversion.{numberToIp, numberToMac}

import scala.collection.mutable

trait Condition
case class OP(expression: Expression, constraint: Constraint, size: Int)
    extends Condition {
  OP.add(this)
  def stringify(constraint: Constraint) : String = constraint match {
    case LT_E(e) => s"<${stringify(e)}"
    case LTE_E(e) => s"<=${stringify(e)}"
    case GTE_E(e) => s">=${stringify(e)}"
    case GT_E(e) => s">${stringify(e)}"
    case EQ_E(e) => s"==${stringify(e)}"
    case No => s"0"
    case LT(v) => s"<$v"
    case LTE(v) => s"<=$v"
    case GT(v) =>  s">$v"
    case GTE(v) => s">=$v"
    case E(v) => s"===$v"
    case Truth() => s"1"
    case Range(v1, v2) => s"<=$v2 && >=$v1"
    case OR(constraints) =>  constraints.map(stringify).mkString("||")
    case AND(constraints) => constraints.map(stringify).mkString("&&")
    case NOT(constraint) => "!" + stringify(constraint)
    case _ => ???
  }

  def stringify(expression: Expression) : String = expression match {
    case LShift(a, b) => s"${stringify(a.e)}<<${stringify(b.e)}"
    case SymbolicValue(name) => if (name != "")
      name
    else "sym" + expression.id
    case Plus(a, b) => s"${stringify(a.e)}+${stringify(b.e)}"
    case LNot(a) => s"!${stringify(a.e)}"
    case LAnd(a, b) => s"${stringify(a.e)}&${stringify(b.e)}"
    case Lor(a, b) => s"${stringify(a.e)}|${stringify(b.e)}"
    case LXor(a, b) => s"${stringify(a.e)}^${stringify(b.e)}"
    case Minus(a, b) => s"${stringify(a.e)}-${stringify(b.e)}"
    case cv : ConstantValue => if (cv.isMac)
      numberToMac(cv.value)
    else if (!cv.isIp)
      s"${cv.value}"
    else
      numberToIp(cv.value)
    case PlusE(a, b) => s"${stringify(a)}-${stringify(b)}"
    case MinusE(a, b) => s"${stringify(a)}-${stringify(b)}"
    case LogicalOr(a, b) => s"${stringify(a.e)}-${stringify(b.e)}"
    case ConstantBValue(value, size) => s"0${size}b$value"
    case ConstantStringValue(value) => value
    case _ => ???
  }

  def stringify(op : OP): String =
    "(" + stringify(op.expression) + stringify(op.constraint) + s",$size)"

  private lazy val stringRepr = stringify(this)
  override def toString: String = stringRepr
  override lazy val hashCode: Int = stringRepr.hashCode

  override def equals(o: scala.Any): Boolean = o match {
    case op : OP => op.stringRepr == this.stringRepr
    case _ => false
  }

}

case class Placeholder(condition: Condition, id : String) extends Condition

object OP {
  val st = mutable.Set.empty[OP]
  def add(o : OP): Unit = {} /*st += o*/
}

case class FAND(conditions: List[Condition]) extends Condition {
  override def toString: String = {
    "(" + conditions.map(_.toString).mkString(" && ") + ")"
  }
}
case class FOR(conditions: List[Condition]) extends Condition {
  override def toString: String = {
    "(" + conditions.map(_.toString).mkString(" || ") + ")"
  }
}
case class FNOT(condition: Condition) extends Condition {
  override def toString = "(!" + condition.toString + ")"
}
object TRUE extends Condition
object FALSE extends Condition

trait PathCondition[T <: PathCondition[_]] {
  def &&(condition: Condition): T
  def ||(pathCondition: T): T
  def &&(pathCondition: T): T
}

case class SimplePathCondition(cd: Condition, tracker: Set[OP], size: Long)
    extends PathCondition[SimplePathCondition] {
  import SimplePathCondition._


  val print = false
  def toDot(condition: Condition, parent : String, sb : mutable.StringBuilder) : Unit = {
    val id = ToDot.normalize(UUID.randomUUID().toString)
    condition match {
      case op : OP => sb.append(s"${ToDot.normalize(op.toString)} [shape=box];\n")
        sb.append(s"$parent -> ${ToDot.normalize(op.toString)};\n")
      case FAND(conditions) =>
        sb.append(s"$id [label=AND];\n")
        sb.append(s"$parent -> $id;\n")
        for (x <- conditions)
          toDot(x, id, sb)
      case FOR(conditions) => sb.append(s"$id [label=OR];\n")
        sb.append(s"$parent -> $id;\n")
        for (x <- conditions)
          toDot(x, id, sb)
      case FNOT(op : OP) => sb.append(s"${ToDot.normalize("!" + op.toString)} [shape=box];\n")
        sb.append(s"$parent -> ${ToDot.normalize("!" + op.toString)};\n")
      case FNOT(c) => sb.append(s"$id [label=NOT];\n")
        sb.append(s"$parent -> $id;\n")
        toDot(c, id, sb)
      case TRUE =>  sb.append(s"1 [shape=box];\n")
        sb.append(s"$parent -> 1;\n")
      case FALSE => sb.append(s"0 [shape=box];\n")
        sb.append(s"$parent -> 0;\n")
      case _ => ???
    }
  }

  def toDot : String = {
    val sb = StringBuilder.newBuilder
    val id = ToDot.normalize(UUID.randomUUID().toString)
    sb.append("digraph Condition {\n")
    cd match {
      case op : OP => sb.append(s"$id [shape=box,label=${ToDot.normalize(op.toString)}];\n")
      case FAND(conditions) =>
        sb.append(s"$id [label=AND];\n")
        for (x <- conditions)
          toDot(x, id, sb)
      case FOR(conditions) => sb.append(s"$id [label=OR];\n")
        for (x <- conditions)
          toDot(x, id, sb)
      case FNOT(c) => sb.append(s"$id [label=NOT];\n")
        toDot(c, id, sb)
      case TRUE =>  sb.append(s"$id [shape=box,label=1];\n")
      case FALSE => sb.append(s"$id [shape=box,label=0];\n")
      case _ => ???
    }
    sb.append("}\n")
    sb.toString()
  }

  override def &&(c: Condition): SimplePathCondition = {
    val pc = copy(cd = FAND.makeFAND(c :: cd :: Nil)
//      ,
//         size = size + sz(c),
//         tracker = tracker ++ track(c)
    )
//    if (print && SimplePathCondition.updateMax(pc.size)) {
//      val rnd = "%06d".format(cachedefs.next())
//      val f = new File(s"dots/${rnd}_andc/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_andc/full.dot")
//      ps.println(pc.toDot)
//      ps.close()
//    }
    pc
  }

  override def ||(pathCondition: SimplePathCondition): SimplePathCondition = {
    val pc = copy(cd = FOR.makeFOR(pathCondition.cd :: cd :: Nil)/*,
         tracker = tracker ++ pathCondition.tracker,
         size = size + pathCondition.size*/)
//    if (print && SimplePathCondition.updateMax(pc.size)) {
//      val rnd = "%06d".format(cachedefs.next())
//      val f = new File(s"dots/${rnd}_or/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_or/full.dot")
//      ps.println(pc.toDot)
//      ps.close()
//    }
    pc
  }

  override def &&(pathCondition: SimplePathCondition): SimplePathCondition = {
    val pc = copy(cd = FAND.makeFAND(pathCondition.cd :: cd :: Nil)/*,
         tracker = tracker ++ pathCondition.tracker,
         size = size + pathCondition.size*/)
//    if (print && SimplePathCondition.updateMax(pc.size)) {
//      val rnd = "%06d".format(cachedefs.next())
//      val f = new File(s"dots/${rnd}_and/")
//      f.mkdir()
//      val ps = new PrintStream(s"dots/${rnd}_and/full.dot")
//      ps.println(pc.toDot)
//      ps.close()
//    }
    pc
  }
}

object SimplePathCondition {
  var max = 0l
  def updateMax(sz : Long) : Boolean = {
    if (max < sz) {
      max = sz
      true
    } else {
      false
    }
  }

  def track(cd: Condition): Set[OP] = cd match {
    case o: OP            => Set(o)
    case FAND(conditions) => conditions.flatMap(c => track(c)).toSet
    case FOR(conditions)  => conditions.flatMap(c => track(c)).toSet
    case FNOT(condition)  => track(condition)
    case _                => Set.empty
  }

  def sz(cd: Condition): Long = cd match {
    case o: OP            => 1
    case FAND(conditions) => conditions.map(c => sz(c)).sum
    case FOR(conditions)  => conditions.map(c => sz(c)).sum
    case FNOT(condition)  => sz(condition)
    case _                => 0
  }

  def apply(cd: Condition): SimplePathCondition =
    new SimplePathCondition(cd, tracker = track(cd), size = sz(cd))
  def apply(): SimplePathCondition = new SimplePathCondition(TRUE, Set.empty, 0)
  def orDefault(): SimplePathCondition = new SimplePathCondition(FALSE, Set.empty, 0)
}

object FOR {
  def makeFOR(cds: List[Condition]): Condition =
    if (cds.isEmpty)
      FALSE
    else if (cds.contains(TRUE))
      TRUE
    else if (cds.size == 1)
      cds.head
    else {
      val (ors, others) = cds.filter(_ != FALSE).partition(_.isInstanceOf[FOR])
      if (ors.isEmpty && others.size == cds.size) {
        new FOR(others)
      } else {
        makeFOR(others ++ ors.flatMap(_.asInstanceOf[FOR].conditions))
      }
    }
}

object FAND {
  def makeFAND(cds: List[Condition]): Condition =
    if (cds.isEmpty)
      TRUE
    else if (cds.contains(FALSE))
      FALSE
    else if (cds.size == 1)
      cds.head
    else {
      val (ors, others) = cds.filter(_ != TRUE).partition(_.isInstanceOf[FAND])
      if (ors.isEmpty && others.size == cds.size) {
        new FAND(others)
      } else {
        makeFAND(others ++ ors.flatMap(_.asInstanceOf[FAND].conditions))
      }
    }
}

object FNOT {

  def makeFNOT(condition: Condition): Condition = condition match {
    case OP(e, NOT(constraint), sz) => OP(e, constraint, sz)
//    case and : FAND           => new FNOT(and)
    case FAND(conditions)           => FOR.apply(conditions.map(makeFNOT))
//    case or : FOR            => new FNOT(or)
    case FOR(conditions)            => FAND.apply(conditions.map(makeFNOT))
    case FNOT(condition)            => condition
    case TRUE                       => FALSE
    case FALSE                      => TRUE
    case _                          => new FNOT(condition)
  }
}
