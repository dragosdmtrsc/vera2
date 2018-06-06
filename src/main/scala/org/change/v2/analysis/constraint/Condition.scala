package org.change.v2.analysis.constraint

import org.change.v2.analysis.memory.MemoryObject

trait Condition
case class OP(memoryObject: MemoryObject, constraint: Constraint) extends Condition
case class FAND(conditions : List[Condition]) extends Condition
case class FOR(conditions : List[Condition]) extends Condition
case class FNOT(condition : Condition) extends Condition
case class TRUE() extends Condition
case class FALSE() extends Condition