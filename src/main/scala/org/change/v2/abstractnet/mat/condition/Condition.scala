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


}
