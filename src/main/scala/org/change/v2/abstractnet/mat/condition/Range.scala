package org.change.v2.abstractnet.mat.condition

import org.change.v2.abstractnet.mat.condition.Range._

/**
  * A small gift from radu to symnetic.
  */
case class Range(lower: Long, upper: Long) extends Condition {
  override def generality: Long = upper - lower

  override def compare(other: Condition): Relation = other match {
    case Range(otherLower, otherUpper) => {
      (CompareResult(lower, otherLower), CompareResult(upper, otherUpper)) match {
        case (GT, LT) => Sub
        case (LT, GT) => Super
        case (EQ, EQ) => Same
        case _ => (CompareResult(lower, otherUpper), CompareResult(upper, otherLower)) match {
          case (GT, _) | (_, LT) => Disjoint
          case _ => Intersect
        }
      }

    }
    case _ => No
  }
}

object Range {
  def ordering: Ordering[Range] = Ordering.by(_.generality)

  trait CompareResult
  object LT extends CompareResult
  object GT extends CompareResult
  object EQ extends CompareResult

  object CompareResult {
    def apply(res: Int): CompareResult =
      if (res < 0) LT
      else if (res > 0) GT
      else EQ
    def apply(a: Long, b: Long): CompareResult =
      if (a == b) EQ
      else if (a <= b) LT
      else GT

  }
}
