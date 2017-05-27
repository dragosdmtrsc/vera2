package org.change.v2.abstractnet.mat.tree

import org.change.v2.abstractnet.mat.condition._
import org.change.v2.abstractnet.mat.tree.Node.Forest

import scala.collection.LinearSeq

/**
  * A small gift from radu to symnetic.
  */
case class Node[T <: Condition](
   condition: T,
   children: Forest[T],
   lateral: Forest[T]) {

  def size: Int = 1 + Node.forestSize(children)
}

object Node {

  type Forest[T <: Condition] = LinearSeq[Node[T]]

  def forestSize[T <: Condition](forest: Forest[T]): Int = forest.map(_.size).sum

  def makeForest[T <: Condition](conditions: Seq[T]): Forest[T] = {
    // TODO: Assert it is sorted properly
    conditions.foldLeft(Nil:Forest[T])(add)
  }

  /**
    * Create a new node corresponding to a given condition.
    * @param forest
    * @param condition
    * @tparam T
    * @return The new forest, including the newly created node.
    */
  def add[T <: Condition](forest: Forest[T], condition: T): Forest[T] = {
    /**
      * TODO: Partition the nodes into possible overlaps and the rest
      */
    val (possible, rest) = (forest, Nil)

    /**
      * The possible one is partitioned, only Same, Super or Intersection(if no Super) should found.
      *
      * Disjoint should be filtered at the previous step.
      * Sub should be avoided by the order of insertion.
      * No should not occur, all conditions should be "relate-able" in one forest.
      */
    val grouped = groupByRelation(condition, possible)

    /**
      * TODO: Imposing the previous invariants.
      */

    /**
      * If there is a node in Sub relation, then the condition gets propagated.
      */
    if (grouped contains Super) {
      // Only one Super node should exist.
      assert(grouped(Super).length == 1)

      // Propagate the node
      val superNode = grouped(Super).head
      val nodeAfterPropagation = superNode.copy(children = add(superNode.children, condition))

      // Rebuild the forest
      val result = (grouped.getOrElse(Intersect, Nil) :+ nodeAfterPropagation) ++ rest

      // The forest should be the same size
      assert(result.length == forest.length)

      result
    } else if (grouped contains Intersect) {
      // Construct a new node for this condition
      val newNode = Node(condition, Nil, grouped(Intersect))

      // Amend the lateral links with the newly created node
      val lateral = grouped.getOrElse(Intersect, Nil).map( lateralNode =>
        lateralNode.copy(lateral = lateralNode.lateral.+:(newNode))
      )

      val result = lateral.+:(newNode) ++ rest

      // One node should have been created at this step.
      assert(result.length == forest.length + 1)

      result
    } else if (grouped contains Same) {
      // TODO: Same is not yet considered
      forest
    } else {
      // Construct the root
      LinearSeq(Node(condition, Nil, Nil))
    }
  }

  def partitionByCondition[T <: Condition](p: T => Boolean, forest: Forest[T]): (Forest[T],Forest[T]) =
    forest.partition(n => p(n.condition))

  def groupByRelation[T <: Condition](condition: T, forest: Forest[T]): Map[Relation, Forest[T]] =
    forest.groupBy(_.condition.compare(condition))
}