package org.change.p4.control.types

case class FixedArrayType(inner: P4Type, sz: Int) extends P4Type
