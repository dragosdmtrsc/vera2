package org.change.v2.analysis.executor

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.{Solver, Z3Solver}
import org.change.v2.analysis.memory.{MemorySpace, State, TagExp}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._


object SpeculativeExecutor {
  def apply(instruction: Instruction, state: State, verbose: Boolean = false) = {
    new SpeculativeExecutor().execute(instruction, state, verbose)
  }
}

class SpeculativeExecutor(solver: Solver = new Z3Solver)
  extends DecoratedInstructionExecutor(solver) {


  def constrainMemory(memory: MemorySpace, id: Int, c: Constraint) = {
    val MemorySpace(symbols, rawObjects, memTags) = memory
    memory.eval(id).flatMap(smb => {
      val newSmb = smb.constrain(c)
      val newMem = memory.replaceValue(id, newSmb).get

      val subject = newMem.eval(id).get

      c match {
        case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
        case GT_E(someE) if someE.id == subject.e.id => None
        case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
        case LT_E(someE) if someE.id == subject.e.id => None
        case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
        case _ => Some(newMem)
      }
    })
  }


  def constrainMemory(memory: MemorySpace, id: String, c: Constraint) = {
    val MemorySpace(symbols, rawObjects, memTags) = memory
    memory.eval(id).flatMap(smb => {
      val newSmb = smb.constrain(c)
      val newMem = memory.replaceValue(id, newSmb).get
      val subject = newMem.eval(id).get
      c match {
        case EQ_E(someE) if someE.id == subject.e.id => Some(newMem)
        case GT_E(someE) if someE.id == subject.e.id => None
        case GTE_E(someE) if someE.id == subject.e.id => Some(newMem)
        case LT_E(someE) if someE.id == subject.e.id => None
        case LTE_E(someE) if someE.id == subject.e.id => Some(newMem)
        case _ => Some(newMem)
      }
    })
  }


  override def executeForward(instruction: Forward, s: State, v: Boolean = false)
  : (List[State], List[State]) = {
    optionToStatePair(s.forwardTo(instruction.place), "Failed dramatically")(s => {
      if (this.isSat(s.memory))
        Some(s.memory)
      else
        None
    })
  }


  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val ConstrainRaw(a, dc, c) = instruction
    a(s) match {
      case Some(int) => c match {
        case None => instantiate(s, dc) match {
          case Left(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc")(s => {
            //              s.memory.Constrain(int, c)
            this.constrainMemory(s.memory, int, c)
          })
          case Right(err) => Fail(err)(s, v)
        }
        case Some(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc")(s => {
          //            s.memory.Constrain(int, c)
          this.constrainMemory(s.memory, int, c)

        })
      }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  override def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val ConstrainNamedSymbol(id, dc, c) = instruction
    c match {
      case None => instantiate(s, dc) match {
        case Left(c) => optionToStatePair(s, s"Symbol $id cannot $dc")(s => {
          //          s.memory.Constrain(id, c)
          this.constrainMemory(s.memory, id, c)
        })
        case Right(err) => Fail(err)(s, v)
      }
      case Some(c) => optionToStatePair(s, s"Symbol $id cannot $dc")(s => {
        //        s.memory.Constrain(id, c)
        this.constrainMemory(s.memory, id, c)

      })
    }
  }
}