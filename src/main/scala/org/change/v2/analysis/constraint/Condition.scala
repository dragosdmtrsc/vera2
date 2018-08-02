package org.change.v2.analysis.constraint

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.MemoryObject

import scala.collection.mutable

trait Condition
case class OP(expression: Expression, constraint: Constraint, size: Int)
    extends Condition {
  OP.add(this)
}

object OP {
  val st = mutable.Set.empty[OP]
  def add(o : OP): Unit = st += o
}

case class FAND(conditions: List[Condition]) extends Condition
case class FOR(conditions: List[Condition]) extends Condition
case class FNOT(condition: Condition) extends Condition
object TRUE extends Condition
object FALSE extends Condition

trait PathCondition[T <: PathCondition[_]] {
  def &&(condition: Condition): T
  def ||(pathCondition: T): T
  def &&(pathCondition: T): T
}

case class SimplePathCondition(cd: Condition, tracker: Set[OP], size: Int)
    extends PathCondition[SimplePathCondition] {
  import SimplePathCondition._

  override def &&(c: Condition): SimplePathCondition = {
    copy(cd = FAND.makeFAND(c :: cd :: Nil),
         size = size + sz(c),
         tracker = tracker ++ track(c))
  }

  override def ||(pathCondition: SimplePathCondition): SimplePathCondition = {
    copy(cd = FOR.makeFOR(pathCondition.cd :: cd :: Nil),
         tracker = tracker ++ pathCondition.tracker,
         size = size + pathCondition.size)
  }

  override def &&(pathCondition: SimplePathCondition): SimplePathCondition = {
    copy(cd = FAND.makeFAND(pathCondition.cd :: cd :: Nil),
         tracker = tracker ++ pathCondition.tracker,
         size = size + pathCondition.size)
  }
}

case class NaivePathCondition(fAND: FAND)
    extends PathCondition[NaivePathCondition] {
  override def &&(condition: Condition): NaivePathCondition = {
    copy(fAND = fAND.copy(conditions = condition :: fAND.conditions))
  }

  override def ||(pathCondition: NaivePathCondition): NaivePathCondition = {
    copy(fAND = FAND.apply(pathCondition.fAND.conditions.flatMap(r => {
      pathCondition.fAND.conditions.map(c => {
        FOR.apply(r :: c :: Nil)
      })
    })))
  }

  override def &&(pathCondition: NaivePathCondition): NaivePathCondition = {
    copy(
      fAND = fAND.copy(
        conditions = pathCondition.fAND.conditions ++ fAND.conditions))
  }
}

object NaivePathCondition {
  def apply(): NaivePathCondition = new NaivePathCondition(FAND(Nil))
}
object SimplePathCondition {


  def track(cd: Condition): Set[OP] = cd match {
    case o: OP            => Set(o)
    case FAND(conditions) => conditions.flatMap(c => track(c)).toSet
    case FOR(conditions)  => conditions.flatMap(c => track(c)).toSet
    case FNOT(condition)  => track(condition)
    case _                => Set.empty
  }

  def sz(cd: Condition): Int = cd match {
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
  def makeFOR(cds: Iterable[Condition]): Condition =
    if (cds.isEmpty)
      FALSE
    else if (cds.exists(_ == TRUE))
      TRUE
    else if (cds.size == 1)
      cds.head
    else {
      val flatm = cds.collect {
        case FOR(cs)      => cs
        case FALSE        => Nil
        case v: Condition => v :: Nil
      }.flatten
      if (cds.size == flatm.size) {
        new FOR(flatm.toList)
      } else {
        FOR.makeFOR(flatm.toList)
      }
    }
}

object FAND {
  def makeFAND(cds: Iterable[Condition]): Condition =
    if (cds.isEmpty)
      TRUE
    else if (cds.exists(_ == FALSE))
      FALSE
    else if (cds.size == 1)
      cds.head
    else {
      val flatm = cds.collect {
        case TRUE         => Nil
        case FAND(cs)     => cs
        case v: Condition => v :: Nil
      }.flatten
      if (cds.size == flatm.size) {
        new FAND(flatm.toList)
      } else {
        FAND.makeFAND(flatm.toList)
      }
    }
}

object FNOT {

  def makeFNOT(condition: Condition): Condition = condition match {
    case OP(e, NOT(constraint), sz) => OP(e, constraint, sz)
    case o: OP                      => new FNOT(o)
    case FAND(conditions)           => FOR.apply(conditions.map(makeFNOT))
    case FOR(conditions)            => FAND.apply(conditions.map(makeFNOT))
    case FNOT(condition)            => condition
    case TRUE                       => FALSE
    case FALSE                      => TRUE
    case _                          => ???
  }
}
