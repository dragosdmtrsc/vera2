package org.change.v2.abstractnet.mat.condition

/**
  * A small gift from radu to symnetic.
  */
trait Condition {
  /**
    * Computes a generality score (higher is more general).
    *
    * @return a generality ranking (how likely it is to be in conflict with others)
    */
  def generality: Long

  def compare(other: Condition): Relation
}

sealed trait Relation
object Disjoint extends Relation
object Sub extends Relation
object Super extends Relation
object Intersect extends Relation
object Same extends Relation
object No extends Relation