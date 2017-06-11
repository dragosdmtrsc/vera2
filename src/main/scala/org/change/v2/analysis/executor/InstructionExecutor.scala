package org.change.v2.analysis.executor

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.memory.TagExp
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.DeallocateNamedSymbol
import org.change.v2.analysis.processingmodels.instructions.ConstrainNamedSymbol
import org.change.v2.analysis.processingmodels.instructions.ConstrainRaw
import org.change.v2.analysis.processingmodels.instructions.DestroyTag
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.analysis.processingmodels.instructions.AssignRaw
import org.change.v2.analysis.processingmodels.instructions.AssignNamedSymbol
import org.change.v2.analysis.processingmodels.instructions.AllocateSymbol
import org.change.v2.analysis.memory.TagExp
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.processingmodels.instructions.AllocateRaw
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.DeallocateRaw
import org.change.v2.analysis.processingmodels.instructions.Fork
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.optionToStatePair
import org.change.v2.analysis.processingmodels.instructions.stateToError
import org.change.v2.analysis.processingmodels.instructions.:~:
import System.out._
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.executor.solvers.Solver 
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.analysis.executor.solvers.Z3SolverEnhanced
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.NoOp

trait IExecutor[T] {
  def execute(instruction : Instruction, 
      state : State, verbose : Boolean) : T;
}

abstract class Executor[T] extends IExecutor[T] {
  override def execute(instruction : Instruction, 
      state : State, verbose : Boolean) : T = {
    val s = if (verbose && 
          !instruction.isInstanceOf[If] && 
          !instruction.isInstanceOf[Fork] &&
          !instruction.isInstanceOf[InstructionBlock])
        state.addInstructionToHistory(instruction)
      else
        state  
    val as = instruction match {
      case v @ NoOp => executeNoOp(s, verbose)
      case v: AllocateRaw => {
        executeAllocateRaw(v, s, verbose)
      }
      case v : AllocateSymbol => {
        executeAllocateSymbol(v, s, verbose)
      }
      case v : AssignNamedSymbol => {
        executeAssignNamedSymbol(v, s, verbose)
      }
      case v : AssignRaw => {
        executeAssignRaw(v, s, verbose)
      }
      case v : ConstrainRaw => {
        executeConstrainRaw(v, s, verbose)
      }
      case v : ConstrainNamedSymbol => {
        executeConstrainNamedSymbol(v, s, verbose)
      }
      case v : CreateTag => {
        executeCreateTag(v, s, verbose)       
      }
      case v : DeallocateNamedSymbol => {
        executeDeallocateNamedSymbol(v, s, verbose)       
      }
      case v : DeallocateRaw => {
        executeDeallocateRaw(v, s, verbose)
      }
      case v : DestroyTag => {
        executeDestroyTag(v, s, verbose)
      }
      case v : Fail => {
        executeFail(v, s, verbose)        

      }
      case v : Fork => {
        executeFork(v, s, verbose)        
      }
      case v : Forward => {
        executeForward(v, s, verbose)
      }
      case v : If => {
        executeIf(v, s, verbose)        
      }
      case v : InstructionBlock => {
        executeInstructionBlock(v, s, verbose)
      }
      case _ => {
        executeExoticInstruction(instruction, s, verbose)
      }
    }
    as
  }
  
  def executeExoticInstruction(instruction : Instruction, 
      s : State,
      verbose : Boolean) : T;
  
  def executeNoOp(s : State, v : Boolean  = false) : T;
  
  def executeInstructionBlock(instruction : InstructionBlock, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeIf(instruction : If, 
      s : State, 
      v : Boolean = false) : T;
  
  def executeForward(instruction : Forward, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeFork(instruction : Fork, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeFail(instruction : Fail, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeConstrainRaw(instruction : ConstrainRaw, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeConstrainNamedSymbol(instruction : ConstrainNamedSymbol, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeAssignNamedSymbol(instruction : AssignNamedSymbol, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeAllocateRaw(instruction : AllocateRaw, 
      state : State, 
      verbose : Boolean = false) : 
    T;
  
  def executeAssignRaw(instruction : AssignRaw, 
      s : State, 
      v: Boolean = false) : 
    T;
  
  def executeAllocateSymbol(instruction : AllocateSymbol, 
      s : State, 
      v : Boolean = false) : 
    T;
  
  def executeDestroyTag(instruction : DestroyTag,
      s : State,
      v : Boolean = false) :
   T;
  
  def executeDeallocateNamedSymbol(instruction : DeallocateNamedSymbol, 
      s : State,
      v : Boolean = false) : 
    T;
  
  def executeCreateTag(instruction : CreateTag, s : State, v : Boolean = false) : 
    T;
  
  
  def executeDeallocateRaw(instruction : DeallocateRaw, 
      s : State, 
      v : Boolean = false) : 
    T;
}


object InstructionExecutor {
  def apply() : InstructionExecutor = {
    new DecoratedInstructionExecutor(new Z3SolverEnhanced())
  }
}

// thread unsafe thing. Beware! All 
// implementing classes should and must be bound to 1 thread
// and 1 thread only
abstract class InstructionExecutor extends Executor[(List[State], List[State])] {
  def executeInstructionBlock(instruction : InstructionBlock, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeIf(instruction : If, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeForward(instruction : Forward, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeFork(instruction : Fork, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeFail(instruction : Fail, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeConstrainRaw(instruction : ConstrainRaw, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeConstrainNamedSymbol(instruction : ConstrainNamedSymbol, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeAssignNamedSymbol(instruction : AssignNamedSymbol, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeAllocateRaw(instruction : AllocateRaw, 
      state : State, 
      verbose : Boolean = false) : 
    (List[State], List[State]);
  
  def executeAssignRaw(instruction : AssignRaw, 
      s : State, 
      v: Boolean = false) : 
    (List[State], List[State]);
  
  def executeAllocateSymbol(instruction : AllocateSymbol, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeDestroyTag(instruction : DestroyTag,
      s : State,
      v : Boolean = false) :
   (List[State], List[State]);
  
  def executeDeallocateNamedSymbol(instruction : DeallocateNamedSymbol, 
      s : State,
      v : Boolean = false) : 
    (List[State], List[State]);
  
  def executeCreateTag(instruction : CreateTag, s : State, v : Boolean = false) : 
    (List[State], List[State]);
  
  
  def executeDeallocateRaw(instruction : DeallocateRaw, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]);
  
}

abstract class AbstractInstructionExecutor extends InstructionExecutor {
  override def executeInstructionBlock(instruction : InstructionBlock, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]) = {
    val InstructionBlock(instructions) = instruction
    instructions.foldLeft((List(s), Nil: List[State])) { (acc, i) => {
        val (valid: List[State], failed: List[State]) = acc
        val (nextValid, nextFailed) = valid.map(execute(i, _, v)).unzip
        val allValid = nextValid.foldLeft(Nil: List[State])(_ ++ _)
        val allFailed = nextFailed.foldLeft(failed: List[State])(_ ++ _)
        (allValid, allFailed)
      }
    }
  }
  
  
  override def executeNoOp(s : State, v : Boolean = false) : (List[State], List[State]) = {
    NoOp(s, v)
  }
  
  override def executeIf(instruction : If, s : State, v : Boolean = false) : 
    (List[State], List[State]) = { 
    val If(testInstr, thenWhat, elseWhat) = instruction
    
    testInstr match {
      case ConstrainNamedSymbol(what, withWhat, _) => {
        withWhat instantiate s match {
          case Left(c) if s.memory.symbolIsAssigned(what) => {
            val (sa, fa) = execute(InstructionBlock(ConstrainNamedSymbol(what, withWhat, Some(c)), thenWhat), s, v)
            val (sb, fb) = execute(InstructionBlock(ConstrainNamedSymbol(what, :~:(withWhat), Some(NOT(c))), elseWhat), s, v)
            (sa ++ sb, fa ++ fb)
          }
          case _ => elseWhat(s, v)
        }
      }
      case ConstrainRaw(what, withWhat, _) => what(s) match {
        case Some(i) => withWhat instantiate s match {
          case Left(c) if s.memory.canRead(i) => {
            val (sa, fa) = execute(InstructionBlock(ConstrainRaw(what, withWhat, Some(c)), thenWhat), s, v)
            val (sb, fb) = execute(InstructionBlock(ConstrainRaw(what, :~:(withWhat), Some(NOT(c))), elseWhat), s, v)
            (sa ++ sb, fa ++ fb)
          }
          case _ => execute(elseWhat, s, v)
        }
        case None => execute(elseWhat, s,v)
      }
      case _ => stateToError(s, "Bad test instruction")
    }
    
  }
      
  override def executeForward(instruction : Forward, s : State, v : Boolean = false) : 
    (List[State], List[State]) = { 
    val Forward(place) = instruction
    (List(s.forwardTo(place)), Nil)
  }
    
  override def executeFork(instruction : Fork, s : State, v : Boolean = false) : 
    (List[State], List[State]) = { 
    val Fork(forkBlocks) = instruction
    val (success, fail) = forkBlocks.map(i => execute(i, s, v)).unzip
    (success.flatten.toList, fail.flatten.toList)
  }
  
  
  override def executeFail(instruction : Fail, s : State, v : Boolean = false) : 
    (List[State], List[State]) = {
    val Fail(errMsg) = instruction
    stateToError(s, errMsg)
  }
  
  override def executeExoticInstruction(instruction : Instruction, s : State, v : Boolean) : 
    (List[State], List[State]) = {
    instruction(s, v)
  }
  
  protected def isSat(memory : MemorySpace) : Boolean;
  
  override def executeConstrainRaw(instruction : ConstrainRaw, s : State, v : Boolean = false): 
    (List[State], List[State]) = { 
    val ConstrainRaw(a, dc, c) = instruction
    a(s) match {
      case Some(int) => c match {
          case None => dc instantiate s match {
            case Left(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc") (s => {
              val maybeNewMem = s.memory.addConstraint(int, c)
              getNewMemory(maybeNewMem)
            })
            case Right(err) => Fail(err)(s, v)
          }
          case Some(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc") (s => {
            val maybeNewMem = s.memory.addConstraint(int, c)
            getNewMemory(maybeNewMem)
          })
        }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  private def getNewMemory(maybeNewMem: Option[org.change.v2.analysis.memory.MemorySpace]) = {
    maybeNewMem match {
      case None => None
      case Some(m) => {
        if (isSat(m))
        {
          Some(m)
        }
        else
        {
          None
        }
      }
    }
  }
  
  override def executeConstrainNamedSymbol(instruction : ConstrainNamedSymbol, s : State, v : Boolean = false) : 
    (List[State], List[State]) = {
    val ConstrainNamedSymbol(id, dc, c) = instruction
    c match {
      case None => dc instantiate s match {
        case Left(c) => optionToStatePair(s, s"Symbol $id cannot $dc") (s => {
          val maybeNewMem = s.memory.addConstraint(id, c)
          getNewMemory(maybeNewMem)
        })
        case Right(err) => Fail(err)(s, v)
      }
      case Some(c) => optionToStatePair(s, s"Symbol $id cannot $dc") (s => {
          val maybeNewMem = s.memory.addConstraint(id, c)
          getNewMemory(maybeNewMem)
      })
    }
  }
  
  
  override def executeAssignNamedSymbol(instruction : AssignNamedSymbol, 
      s : State, v : Boolean = false) : 
    (List[State], List[State]) = { 
    val AssignNamedSymbol(id, exp, t) = instruction
    exp instantiate  s match {
      case Left(e) => optionToStatePair(s, s"Error during assignment of $id") (s => {
        s.memory.Assign(id, e, t)
      })
      case Right(err) => execute(Fail(err), s, v)
    }
  }
  
  
  override def executeAllocateRaw(instruction : AllocateRaw, 
      state : State, verbose : Boolean = false) : 
    (List[State], List[State]) = { 
    val AllocateRaw(a, size) = instruction
    a(state) match {
      case Some(int) => optionToStatePair(
          state, 
          s"Cannot allocate at $a size $size")(s => {
        s.memory.Allocate(int, size)
      })
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), state, verbose)
    }
  }
  
  override def executeAssignRaw(instruction : AssignRaw, 
      s : State, v: Boolean = false) : 
    (List[State], List[State]) = { 
    val AssignRaw(a, exp, t) = instruction
    a(s) match {
      case Some(int) => { exp instantiate  s match {
        case Left(e) => optionToStatePair(s, s"Error during assignment at $a") (s => {
          s.memory.Assign(int, e)
        })
        case Right(err) => execute(Fail(err), s, v)
      }}
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s,v)
    }
  }
  
  override def executeAllocateSymbol(instruction : AllocateSymbol, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]) = { 
    val AllocateSymbol(id) = instruction
    optionToStatePair(s, s"Cannot allocate $id") (s => {
      s.memory.Allocate(id)
    })
  }
  
  override def executeDestroyTag(instruction : DestroyTag,
      s : State,
      v : Boolean = false) :
   (List[State], List[State]) = {
   val DestroyTag(name) = instruction
   optionToStatePair(s, s"Error during untagging of $name") (s => {
        s.memory.UnTag(name)
    })
  }
  
  override def executeDeallocateNamedSymbol(instruction : DeallocateNamedSymbol, 
      s : State,
      v : Boolean = false) : 
    (List[State], List[State]) = { 
    val DeallocateNamedSymbol(id) = instruction
    optionToStatePair(s, s"Cannot deallocate $id") (s => {
      s.memory.Deallocate(id)
    })
  }
  
  
  override def executeCreateTag(instruction : CreateTag, s : State, v : Boolean = false) : 
    (List[State], List[State]) = { 
    val CreateTag(name: String, value: Intable) = instruction
    value(s) match {
      case Some(int) => optionToStatePair(s, s"Error during tagging of $name") (s => {
        s.memory.Tag(name, int)
      })
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }
  
  
  override def executeDeallocateRaw(instruction : DeallocateRaw, 
      s : State, 
      v : Boolean = false) : 
    (List[State], List[State]) = { 
    val DeallocateRaw(a: Intable, size: Int) = instruction
    a(s) match {
      case Some(int) => optionToStatePair(s, s"Cannot deallocate @ $a of size $size") (s => {
        s.memory.Deallocate(int, size)
      })
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }
}


class DecoratedInstructionExecutor(solver : Solver) extends AbstractInstructionExecutor {  
  override protected def isSat(memory : MemorySpace) : Boolean = {
    return solver.solve(memory)
  }
}

