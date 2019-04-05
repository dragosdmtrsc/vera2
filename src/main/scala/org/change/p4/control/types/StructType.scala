package org.change.p4.control.types

case class StructType(members: Map[String, P4Type]) extends P4Type
