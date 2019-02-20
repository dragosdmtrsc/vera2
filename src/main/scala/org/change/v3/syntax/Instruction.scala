package org.change.v3.syntax

trait Instruction

object NoOp extends Instruction
case class Forward(to : String) extends Instruction
case class Fail(message : String) extends Instruction
case class Drop(message : String) extends Instruction
case class Assign(left: LVExpr, right: BVExpr) extends Instruction
case class Allocate(left : LVExpr, sz : Int) extends Instruction
case class Deallocate(left : LVExpr) extends Instruction

case class CreateTag(name : String, right : Intable) extends Instruction
case class DestroyTag(name : String) extends Instruction
case class Assume(bExpr: BExpr) extends Instruction

case class If(bExpr: BExpr, thn : Instruction, els : Instruction) extends Instruction
case class InstructionBlock(is : Iterable[Instruction]) extends Instruction
case class Clone(is : Iterable[Instruction]) extends Instruction
case class Fork(is : Iterable[Instruction]) extends Instruction