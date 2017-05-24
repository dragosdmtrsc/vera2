package org.change.v2.abstractnet.mat.conflictforest

/**
  * A small gift from radu to symnetic.
  */
case class Node[T](condition: T) {
  def children: Set[Node[T]] = ???
  def children(withHint: T): Set[Node[T]] = ???
  def lateral: Set[Node[T]] = ???
  def conflictual: Set[Node[T]] = children union lateral
  def hasNoConflicts: Boolean = conflictual.isEmpty
}
