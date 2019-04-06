package org.change.p4.control

package object queryimpl {
  type ChurnedMemPath = Iterable[(ScalarValue, MemPath)]
  type MemPath = List[Either[String, Int]]
  def emptyPath() : MemPath = List.empty[Either[String, Int]]
}
