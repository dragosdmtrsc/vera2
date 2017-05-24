package org.change.v2.abstractnet.mat.conflictforest

/**
  * A small gift from radu to symnetic.
  */
class Forest[T] {

  def add(condition: T): Forest[T] = this

  def nodes(witHint: T): Seq[Node[T]] = nodes
  def nodes: Seq[Node[T]] = Nil

}

object Forest {
  /**
    *
    * @param conditions sored in descending order by generality.
    * @return
    */
  def build[T](conditions: Seq[T]): Forest[T] = conditions.foldLeft(empty[T])(_ add _)

  def empty[T]: Forest[T] = new Forest[T]()
}