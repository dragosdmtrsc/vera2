package org.change.v3.syntax

trait Type
// will be enabled on major re-design of SEFL
//object IntType extends Type
//object BoolType extends Type
//object PacketType extends Type

case class BVType(sz : Int) extends Type
case class FixedArrayType(inner : Type, sz : Int) extends Type
