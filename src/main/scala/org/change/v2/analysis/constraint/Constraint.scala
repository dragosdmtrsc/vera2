package org.change.v2.analysis.constraint

import org.change.v2.analysis.types._
import org.change.v2.interval._
import org.change.v2.interval.IntervalOps._

trait Constraint {
  /**
   * Materialize a constraint as the set of possible values.
   * @param valueType For bound checks.
   */
  def asSet(valueType: NumericType = LongType): List[Interval] = Nil
}

object No extends Constraint {
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((valueType.min, valueType.max))
  }
}
case class LT(v: Long) extends Constraint {
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((valueType.min, Math.min(v-1, valueType.max)))
  }
}
case class LTE(v: Long) extends Constraint {
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((valueType.min, Math.min(v, valueType.max)))
  }
}
case class GT(v: Long) extends Constraint {
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((Math.max(v+1, valueType.min), valueType.max))
  }
}
case class GTE(v: Long) extends Constraint{
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((Math.max(v, valueType.min), valueType.max))
  }
}
case class E(v: Long) extends Constraint{
  override def asSet(valueType: NumericType): List[(Long, Long)] =
    if (valueType.min <= v && v <= valueType.max) List((v,v))
    else Nil
}
case class Range(v1: Long, v2: Long) extends Constraint {
  override def asSet(valueType: NumericType): List[(Long, Long)] = {
    List((Math.max(v1, valueType.min), Math.min(v2, valueType.max)))
  }
}

object Range {
  def apply(range: Tuple2[Long, Long]): Range = Range(range._1, range._2)
}

case class OR(constraints: List[Constraint]) extends Constraint {
  override def asSet(valueType: NumericType = LongType): List[Interval] =
    union(constraints.map(_.asSet(valueType)))
}

case class AND(constraints: List[Constraint]) extends Constraint {
  override def asSet(valueType: NumericType = LongType): List[Interval] =
    intersect(constraints.map(_.asSet(valueType)))
}

case class NOT(constraint: Constraint) extends Constraint {
  override def asSet(valueType: NumericType = LongType): List[Interval] =
    complement(constraint.asSet(valueType))
}

object Constraint {
  def applyConstraint(s: ValueSet, c: Constraint,
                      t: NumericType = LongType): ValueSet =
    intersect(List(s, c.asSet(t)))

  def applyConstraints(s: ValueSet, cts: List[Constraint],
                       t: NumericType = LongType): ValueSet =
    intersect(s :: cts.map(_.asSet()))
}