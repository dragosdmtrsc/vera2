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
   lateral: Forest[T],
   id: Long = Node.nextId) {
  def size: Int = 1 + Node.forestSize(children)
  def height: Int = 1 + Node.forestHeight(children)
  def avgHeight: Double = 1 + Node.avgForestHeight(children)
  def totalConstraintNumber: Long = children.length + lateral.length
}

object Node {

  private var id = Long.MinValue

  def nextId: Long = this.synchronized {
    id += 1
    id
  }

  type Forest[T <: Condition] = LinearSeq[Node[T]]

  def forestSize[T <: Condition](forest: Forest[T]): Int = forest.map(_.size).sum
  def forestHeight[T <: Condition](forest: Forest[T]): Int = if (forest.nonEmpty) forest.map(_.height).max else 0
  def avgForestHeight[T <: Condition](forest: Forest[T]): Double = forest.map(_.height).sum / forest.length.toDouble
  def totalConstraintNumber[T <: Condition](forest: Forest[T]): Long = forest.map(_.totalConstraintNumber).sum

  def makeForest[T <: Condition](conditions: Seq[T]): Forest[T] = {
    // TODO: Assert it is sorted properly
    conditions.foldLeft(Nil:Forest[T])(addIgnoringNewNode)
  }

  def addIgnoringNewNode[T <: Condition](forest: Forest[T], condition: T): Forest[T] =
    add(forest, condition)._1

  /**
    * Create a new node corresponding to a given condition.
    * @param forest
    * @param condition
    * @tparam T
    * @return The new forest, including the newly created node.
    */
  def add[T <: Condition](forest: Forest[T], condition: T): (Forest[T], Node[T]) = {
    /**
      * TODO: Partition the nodes into possible overlaps and the rest
      */
    val (possible, rest) = partitionByCondition({c: T => c.compare(condition) != Disjoint }, forest)

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

      // Propagate the new node in the oldest (most general) of the super nodes in order to avoid links between
      // trees that are not situated at the root.
      val superNode = grouped(Super).reduceLeft((old, current) => if (old.id > current.id) current else old)

      val (modifiedChildren, newNode) = add(superNode.children, condition)
      val nodeAfterPropagation = superNode.copy(children = modifiedChildren)

      // Rebuild the forest
      val result = grouped(Super).tail ++ (grouped.getOrElse(Intersect, Nil) :+ nodeAfterPropagation) ++ rest

      // The forest should be the same size
      assert(result.length == forest.length)

      (result, newNode)
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

      (result, newNode)
    } else if (grouped contains Same) {
      val sameNode = grouped(Same)

      // Never should two Same nodes be detected
      assert(sameNode.length == 1)

      (forest, sameNode.head)
    } else {
      // Construct a leaf node, with no links
      val newNode = Node(condition, Nil, Nil)
      (newNode +: rest, newNode)
    }
  }

  def partitionByCondition[T <: Condition](p: T => Boolean, forest: Forest[T]): (Forest[T],Forest[T]) =
    forest.partition(n => p(n.condition))

  def groupByRelation[T <: Condition](condition: T, forest: Forest[T]): Map[Relation, Forest[T]] =
    forest.groupBy(_.condition.compare(condition))
}