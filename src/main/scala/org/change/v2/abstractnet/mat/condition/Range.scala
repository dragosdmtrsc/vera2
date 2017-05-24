package org.change.v2.abstractnet.mat.condition

/**
  * A small gift from radu to symnetic.
  */
case class Range(lower: Long, upper: Long) extends Condition {
  override def generality: Long = upper - lower
}

object Range {
  def ordering: Ordering[Range] = Ordering.by(_.generality)
}
