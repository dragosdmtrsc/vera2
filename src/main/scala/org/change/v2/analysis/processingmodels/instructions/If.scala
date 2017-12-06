package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.constraint.NOT
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
case class If(testInstr: Instruction, thenWhat: Instruction, elseWhat:Instruction = NoOp) extends Instruction {
  /**
   *
   * A state processing block produces a set of new states based on a previous one.
   *
   * @param s
   * @return
   */
  override def apply(s: State, v: Boolean): (List[State], List[State]) = testInstr match {
    // This is quite inappropriate
    case i @ ConstrainNamedSymbol(what, withWhat, _) => {
      withWhat instantiate s match {
        case Left(c) if s.memory.symbolIsAssigned(what) => {
          val (sa, fa) = InstructionBlock(ConstrainNamedSymbol(what, withWhat, Some(c)), thenWhat)(s, v)
          val (sb, fb) = InstructionBlock(ConstrainNamedSymbol(what, :~:(withWhat), Some(NOT(c))), elseWhat)(s, v)
          (sa ++ sb, fa ++ fb)
        }

        case _ => elseWhat(s, v)
      }
    }
    case rawi @ ConstrainRaw(what, withWhat, _) => what(s) match {
      case Some(i) => withWhat instantiate s match {
        case Left(c) if s.memory.canRead(i) => {
          val (sa, fa) = InstructionBlock(ConstrainRaw(what, withWhat, Some(c)), thenWhat)(s, v)
          val (sb, fb) = InstructionBlock(ConstrainRaw(what, :~:(withWhat), Some(NOT(c))), elseWhat)(s, v)
          (sa ++ sb, fa ++ fb)
        }

        case _ => {
          elseWhat(s, v)
        }
      }
      case None => elseWhat(s, v)
    }
    case i @ InstructionBlock(instrs) => {
      if (instrs.forall(x => x.isInstanceOf[ConstrainRaw] || x.isInstanceOf[ConstrainNamedSymbol])) {
        instrs.foldRight(thenWhat)((x, acc) => {
          If (x, acc, elseWhat)
        })(s,v)
      } else {
        throw new UnsupportedOperationException("Can't do it. All instructions in conditional instruction block MUST be Constrain")
      }
    }
    case _ => stateToError(s.addInstructionToHistory(this), "Bad test instruction")
  }

  def branch(s: State): List[Instruction] = testInstr match {
    // This is quite inappropriate
    case i@ConstrainNamedSymbol(what, withWhat, _) => {
      withWhat instantiate s match {
        case Left(c) if s.memory.symbolIsAssigned(what) => {
          val ifok = InstructionBlock(ConstrainNamedSymbol(what, withWhat, Some(c)), thenWhat)
          val ifko = InstructionBlock(ConstrainNamedSymbol(what, :~:(withWhat), Some(NOT(c))), elseWhat)
          List[Instruction](ifok, ifko)
        }
        case _ => List[Instruction](elseWhat)
      }
    }
    case rawi@ConstrainRaw(what, withWhat, _) => what(s) match {
      case Some(i) => withWhat instantiate s match {
        case Left(c) if s.memory.canRead(i) => {
          val ifok = InstructionBlock(ConstrainRaw(what, withWhat, Some(c)), thenWhat)
          val ifko = InstructionBlock(ConstrainRaw(what, :~:(withWhat), Some(NOT(c))), elseWhat)
          List[Instruction](ifok, ifko)
        }
        case _ => List[Instruction](elseWhat)
      }
      case None => List[Instruction](elseWhat)
    }
    case _ => List[Instruction](Fail("Some wrong stuff"))
  }


  override def toString = {
    "If(" + testInstr + ") {\n" + thenWhat.toString() + "\n}\nelse {\n" + elseWhat + "\n}\n"
  }

}
