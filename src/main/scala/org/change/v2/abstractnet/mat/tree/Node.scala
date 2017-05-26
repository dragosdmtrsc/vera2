package org.change.v2.abstractnet.mat.tree

import org.change.v2.abstractnet.mat.condition.{Condition, Relation}
import org.change.v2.abstractnet.mat.tree.Node.Forest

/**
  * A small gift from radu to symnetic.
  */
case class Node[T <: Condition](condition: T) {

 def children: Forest[T] = ???

}

object Node {

  type Forest[T <: Condition] = Seq[Node[T]]

  /**
    * Create a new node corresponding to a given condition.
    * @param condition
    * @param forest
    * @tparam T
    * @return The new forest, including the newly created node.
    */
  def add[T <: Condition](condition: T, forest: Forest[T]): Forest[T] = ???

  def partitionByCondition[T <: Condition](p: T => Boolean, forest: Forest[T]): (Forest[T],Forest[T]) =
    forest.partition(n => p(n.condition))

  def groupByRelation[T <: Condition](condition: T, forest: Forest[T]): Map[Relation, Forest[T]] =
    forest.groupBy(_.condition.compare(condition))
}