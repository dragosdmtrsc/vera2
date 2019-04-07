package org.change.p4.control.types

trait IntType extends P4Type
object UnboundedInt extends IntType
case class BoundedInt() extends IntType