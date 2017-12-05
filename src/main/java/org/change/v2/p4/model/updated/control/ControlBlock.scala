package org.change.v2.p4.model.updated.control

case class ControlBlock(
                       statements: Iterable[ControlStatement]
                       ) extends ControlStatement {

}
