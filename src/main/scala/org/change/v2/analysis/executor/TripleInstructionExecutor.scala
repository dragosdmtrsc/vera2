package org.change.v2.analysis.executor

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.{AlwaysTrue, Solver}
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{Intable, MemorySpace, State, TagExp}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

import scala.annotation.tailrec

class TripleInstructionExecutor(solver: Solver) extends Executor[(List[State], List[State], List[State])] {
  override def executeExoticInstruction(instruction: Instruction, s: State, verbose: Boolean):
  (List[State], List[State], List[State]) = instruction match {
    case t : Translatable => execute(t.generateInstruction(), s, verbose)
    case _ if instruction.isTool => val (ss, sf) = instruction(s, verbose)
      (Nil, sf, ss)
  }

  override def executeNoOp(s: State, v: Boolean): (List[State], List[State], List[State]) = (Nil, Nil, s :: Nil)

  override def executeInstructionBlock(instruction: InstructionBlock, s: State, v: Boolean):
  (List[State], List[State], List[State]) = {
    instruction.instructions.foldLeft((Nil, Nil, s :: Nil) : (List[State], List[State], List[State]))((acc, x) => {
      acc._3.foldLeft((acc._1, acc._2, Nil : List[State]))((acc2, s) => {
        val y = execute(x, s, v)
        (acc2._1 ++ y._1, acc2._2 ++ y._2, acc2._3 ++ y._3)
      })
    })
  }

  def isSat(memorySpace : MemorySpace) : Boolean = {
    solver.solve(memorySpace)
  }

  def evaluateE(expression: Expression)  : Option[BigInt] = expression match {
    case Reference(a, _) => evaluateE(a.e)
    case ConstantStringValue(x) => Some(BigInt(x.hashCode.toLong))
    case ConstantValue(y, _, _) => Some(BigInt(y))
    case ConstantBValue(h, sz) => if (h.startsWith("#x"))
      Some(BigInt.apply(h.replace("#x", ""), 16))
    else
      Some(BigInt.apply(h, 16))
    case Plus(a, b) => evaluateE(a.e).flatMap(x => evaluateE(b.e).map(x + _))
    case LAnd(a, b) => evaluateE(a.e).flatMap(x => evaluateE(b.e).map(x & _))
    case Lor(a, b) => evaluateE(a.e).flatMap(x => evaluateE(b.e).map(x | _))
    case LNot(a) => evaluateE(a.e).map(~_)
    case _ => None
  }

  def tryEval(ev : BigInt, constraint: Constraint) : Option[Boolean] = constraint match {
    case LT_E(e) => evaluateE(e).map(ev < _)
    case LTE_E(e) => evaluateE(e).map(ev <= _)
    case GTE_E(e) => evaluateE(e).map(ev >= _)
    case GT_E(e) => evaluateE(e).map(ev > _)
    case EQ_E(e) => evaluateE(e).map(ev == _)
    case No => Some(false)
    case LT(v) => Some(ev < v)
    case LTE(v) => Some(ev <= v)
    case GT(v) => Some(ev > v)
    case GTE(v) => Some(ev >= v)
    case E(v) => Some(ev == v)
    case Truth() => Some(true)
    case Range(v1, v2) => Some(ev >= v1 && ev <= v2)
    case OR(constraints) => constraints.foldLeft(Some(false) : Option[Boolean])((acc, v) => acc match {
      case Some(true) => Some(true)
      case Some(false) => tryEval(ev, v)
      case None => tryEval(ev, v) match {
        case Some(true) => Some(true)
        case _ => None
      }
    })
    case AND(constraints) => constraints.foldLeft(Some(false) : Option[Boolean])((acc, v) => acc match {
      case Some(false) => Some(false)
      case Some(true) => tryEval(ev, v)
      case None => tryEval(ev, v) match {
        case Some(false) => Some(false)
        case _ => None
      }
    })
    case NOT(constraint) => tryEval(ev, constraint) match {
      case Some(t) => Some(!t)
      case None => None
    }
    case _ => ???
  }

  def tryEval(expression: Expression, constraint: Constraint): Option[Boolean] = {
    evaluateE(expression).flatMap(ev => tryEval(ev, constraint))
  }

  def tryEval(condition: Condition) : Option[Boolean] = condition match {
    case OP(expression, constraint, sz) => tryEval(expression, constraint)
    case FAND(conditions) => conditions.foldLeft(Some(true) : Option[Boolean])((acc, v) => acc match {
      case Some(false) => Some(false)
      case Some(true) => tryEval(v)
      case None => tryEval(v) match {
        case Some(false) => Some(false)
        case _ => None
      }
    })
    case FOR(conditions) => conditions.foldLeft(Some(false) : Option[Boolean])((acc, v) => acc match {
      case Some(true) => Some(true)
      case Some(false) => tryEval(v)
      case None => tryEval(v) match {
        case Some(true) => Some(true)
        case _ => None
      }
    })
    case FNOT(condition) => tryEval(condition) match {
      case None => None
      case Some(t) => Some(!t)
    }
    case TRUE() => Some(true)
    case FALSE() => Some(false)
    case _ => ???
  }

  protected def executeIf(testInstr : Instruction,
                          thenWhat : Instruction,
                          elseWhat : Instruction,
                          s : State,
                          v : Boolean) : (List[State], List[State], List[State]) = {
    @tailrec
    def findKiller(instruction : Iterable[Instruction],
                   current : List[Condition],
                   left : Iterable[Instruction]) :
    (Option[Instruction], List[Condition], Iterable[Instruction]) = {
      if (instruction.isEmpty)
        (None, current, Nil)
      else
        buildCondition(s, instruction.head) match {
          case Left(c) => findKiller(instruction.tail, current :+ c, instruction.tail)
          case Right(m) => (Some(instruction.head), current, instruction.tail)
        }
    }
    val cd = buildCondition(s, testInstr)
    cd match {
      case Left(c) =>
        executeIfCond(c, thenWhat, elseWhat, s, v)
      // pretty uncool stuff... example use case:
      // void f(int *x) {
      //  if (x && *x == 10) ...
      //}
      // if x is null then the whole if condition will fail, even though
      // explicit protection in this case has been provided => there is no bug
      // in this case
      // TODO: need a clean way of doing this!!! too much recursion will eventually kill you
      case Right(m) =>
        testInstr match {
          case InstructionBlock(instrs) =>
            val (offender, cds, left) = findKiller(instrs, Nil, instrs)
//            System.err.println(s"Nasty case $m, $testInstr $left")
            offender match {
              case None => executeIfCond(FAND(cds), thenWhat, elseWhat, s, v)
              case Some(Fork(xs)) => if (cds.isEmpty) {
                executeIf(Fork(xs),
                  if (left.nonEmpty) {
                    If (InstructionBlock(left),
                      thenWhat,
                      elseWhat
                    )
                  } else {
                    thenWhat
                  }, elseWhat, s, v)
              } else {
                executeIfCond(FAND(cds),
                  If(Fork(xs),
                    if (left.isEmpty)
                      thenWhat
                    else
                      If (InstructionBlock(left),
                        thenWhat,
                        elseWhat
                      ),
                    elseWhat
                  ),
                  elseWhat, s, v
                )
              }
              case _ => if (cds.isEmpty) {
                execute(Fail(m), s, v)
              } else {
                executeIfCond(FAND(cds),
                  Fail(m),
                  elseWhat, s, v)
              }
            }
          //TODO: do the same as above for Fork
          case Fork(fb) => execute(fb.foldRight(elseWhat)((i, acc) => {
            If (i, thenWhat, acc)
          }), s, v)
          case _ => this.execute(Fail(m), s, v)
        }
    }
  }


  protected def executeIfCond(c: Condition,
                            thenWhat: Instruction,
                            elseWhat: Instruction, s: State, v: Boolean) :
  (List[State], List[State], List[State]) = {
    tryEval(c) match {
      case Some(true) =>
        this.execute(thenWhat, s, v)
      case Some(false) =>
        this.execute(elseWhat, s, v)
      case None =>
        val crtSet = s.memory.getSymbols()
        val syms = s.memory.crawlCondition(c)
        val mustCheck = syms.intersect(crtSet).nonEmpty
        val cnstrd = s.memory.addCondition(c)
        val bt = if (!mustCheck || isSat(cnstrd)) {
          this.execute(thenWhat, s.copy(memory = cnstrd), v)
        } else {
          (Nil, Nil, Nil)
        }
        val cnstrdf = s.memory.addCondition(FNOT(c))
        val bf = if (!mustCheck || isSat(cnstrdf))
          this.execute(elseWhat, s.copy(memory = cnstrdf), v)
        else
          (Nil, Nil, Nil)
        (bt._1 ++ bf._1, bt._2 ++ bf._2, bt._3 ++ bf._3)
    }
  }

  @tailrec
  final def buildOr(s : State, lst : List[Instruction], or : FOR): Either[Condition, String] = lst match {
    case Nil => Left(or)
    case h :: tail => buildCondition(s, h) match {
      case Right(err) => Right(err)
      case Left(c) => buildOr(s, tail, or.copy(c :: or.conditions))
    }
  }

  @tailrec
  final def buildAnd(s : State, lst : List[Instruction], fand : FAND): Either[Condition, String] = lst match {
    case Nil => Left(fand)
    case h :: tail => buildCondition(s, h) match {
      case Right(err) => Right(err)
      case Left(c) => buildAnd(s, tail, fand.copy(c :: fand.conditions))
    }
  }

  def buildCondition(s : State, testInstr : Instruction) : Either[Condition, String] = testInstr match {
    case Fork(fb) if fb.isEmpty => Left(FALSE())
    case Fork(fb) => buildCondition(s, fb.head) match {
      case Right(err) => Right(err)
      case Left(c) => buildOr(s, fb.tail.toList, FOR(c :: Nil))
    }
    case InstructionBlock(i) if i.isEmpty => Left(TRUE())
    case InstructionBlock(lst) => buildCondition(s, lst.head) match {
      case Right(err) => Right(err)
      case Left(c) => buildAnd(s, lst.tail.toList, FAND(c :: Nil))
    }
    case ConstrainNamedSymbol(what, withWhat, _) => instantiate(s, withWhat) match {
      case Left(c) if s.memory.symbolIsAssigned(what) =>
        val mo = s.memory.evalToObject(what).get
        Left(OP(mo.value.get.e, c, mo.size))
      case Left(c) => Right(s"Symbol $what does not exist")
      case Right(msg) => Right(msg)
    }
    case ConstrainRaw(what, withWhat, _) => what(s) match {
      case Some(i) => instantiate(s, withWhat) match {
        case Left(c) if s.memory.canRead(i) =>
          val mo = s.memory.evalToObject(i).get
          Left(OP(mo.value.get.e, c, mo.size))
        case Left(c) => Right(s"Symbol $what does not exist")
        case Right(msg) => Right(msg)
      }
      case None => Left(FALSE())
    }
    case ConstrainFloatingExpression(what, withWhat) => instantiate(s, what) match {
      case Left(e) =>
        instantiate(s, withWhat) match {
          case Left(c) => Left(OP(e._1, c, e._2))
          case Right(m) => Right(m)
        }
      case Right(m) => Right(m)
    }
  }


  override def executeIf(instruction: If, s: State, v: Boolean): (List[State], List[State], List[State]) =
    executeIf(instruction.testInstr, instruction.thenWhat, instruction.elseWhat, s, v)


  override def executeForward(instruction: Forward, s: State, v: Boolean): (List[State], List[State], List[State]) = {
    (s.forwardTo(instruction.place) :: Nil, Nil, Nil)
  }

  override def executeFork(instruction: Fork, s: State, v: Boolean): (List[State], List[State], List[State]) = {
    val (success, fail, outstanding) = instruction.forkBlocks.map(i => execute(i, s, v)).unzip3
    (success.flatten.toList, fail.flatten.toList, outstanding.flatten.toList)
  }

  override def executeFail(instruction: Fail, s: State, v: Boolean): (List[State], List[State], List[State]) =
    (Nil, s.copy(errorCause = Some(instruction.errMsg)) :: Nil, Nil)

  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean):
  (List[State], List[State], List[State]) = {
    buildCondition(s, instruction) match {
      case Left(c) =>
        tryEval(c) match {
          case Some(b) if b => (Nil, Nil, s :: Nil)
          case Some(b) if !b => (Nil, Nil, Nil)
          case None => val mem = s.memory.addCondition(c)
            if (isSat(mem))
              (Nil, Nil, s :: Nil)
            else
              (Nil, Nil, Nil)
        }
      case Right(m) => execute(Fail(m), s, v)
    }
  }

  override def executeConstrainFloatingExpression(i: ConstrainFloatingExpression, s: State, v: Boolean):
    (List[State], List[State], List[State]) = buildCondition(s, i) match {
    case Left(c) =>
      tryEval(c) match {
        case Some(b) if b => (Nil, Nil, s :: Nil)
        case Some(b) if !b => (Nil, Nil, Nil)
        case None => val mem = s.memory.addCondition(c)
          if (isSat(mem))
            (Nil, Nil, s :: Nil)
          else
            (Nil, Nil, Nil)
      }
    case Right(m) => execute(Fail(m), s, v)
  }

  override def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol, s: State, v: Boolean):
  (List[State], List[State], List[State]) = buildCondition(s, instruction) match {
    case Left(c) =>
      tryEval(c) match {
        case Some(b) if b => (Nil, Nil, s :: Nil)
        case Some(b) if !b => (Nil, Nil, Nil)
        case None => val mem = s.memory.addCondition(c)
          if (isSat(mem))
            (Nil, Nil, s :: Nil)
          else
            (Nil, Nil, Nil)
      }
    case Right(m) => execute(Fail(m), s, v)
  }

  override def executeAssignNamedSymbol(instruction: AssignNamedSymbol, s: State, v: Boolean):
    (List[State], List[State], List[State]) = {
    val AssignNamedSymbol(id, exp, t) = instruction
    assert(exp != null, s"Something terribly wrong for instruction $instruction")
    instantiate(s, exp) match {
      case Left(e) =>
        val sss = if (v && !s.memory.symbolIsDefined(id)) {
          val h = s.instructionHistory.head
          s.copy(instructionHistory = h :: AllocateSymbol(id, 64) :: s.instructionHistory.tail)
        } else {
          s
        }
        val (ss, sf) = optionToStatePair(sss, s"Error during assignment of $id")(s => {
          s.memory.Assign(id, e._1, t)
        })
      (Nil, sf, ss)
      case Right(err) => execute(Fail(err), s, v)
    }
  }

  override def executeAllocateRaw(instruction: AllocateRaw, state: State, verbose: Boolean):
    (List[State], List[State], List[State]) = {
    val AllocateRaw(a, size) = instruction
    a(state) match {
      case Some(int) => val (ss, sf) = optionToStatePair(
          state,
          s"Cannot allocate at $a size $size")(s => {
          s.memory.Allocate(int, size)
        })
        (Nil, sf, ss)
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), state, verbose)
    }
  }

  override def executeAssignRaw(instruction: AssignRaw, s: State, v: Boolean):
  (List[State], List[State], List[State]) = {
    val AssignRaw(a, exp, t) = instruction
    a(s) match {
      case Some(int) => instantiate(s, exp) match {
        case Left(e) => val (ss, sf) = optionToStatePair(s, s"Error during assignment at $a")(s => {
            s.memory.Assign(int, e._1)
          })
          (Nil, sf, ss)
        case Right(err) => execute(Fail(err), s, v)
      }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  override def executeAllocateSymbol(instruction: AllocateSymbol, s: State, v: Boolean):
  (List[State], List[State], List[State]) = {
    val AllocateSymbol(id, size) = instruction
    val (ss, sf) = optionToStatePair(s, s"Cannot allocate $id")(s => {
      s.memory.Allocate(id, size)
    })
    (Nil, sf, ss)
  }

  override def executeDestroyTag(instruction: DestroyTag, s: State, v: Boolean):
  (List[State], List[State], List[State]) = {
    val DestroyTag(name) = instruction
    val (ss, sf) = optionToStatePair(s, s"Error during untagging of $name")(s => {
      s.memory.UnTag(name)
    })
    (Nil, sf, ss)
  }

  override def executeDeallocateNamedSymbol(instruction: DeallocateNamedSymbol, s: State, v: Boolean):
    (List[State], List[State], List[State]) = ???

  override def executeCreateTag(instruction: CreateTag, s: State, v: Boolean):
    (List[State], List[State], List[State]) = {
    val CreateTag(name: String, value: Intable) = instruction
    value(s) match {
      case Some(int) => val (ss, sf) = optionToStatePair(s, s"Error during tagging of $name")(s => {
          s.memory.Tag(name, int)
        })
        (Nil, sf, ss)
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  override def executeDeallocateRaw(instruction: DeallocateRaw, s: State, v: Boolean):
    (List[State], List[State], List[State]) = ???
}

class ReallySimpleInstructionExecutor extends TripleInstructionExecutor(AlwaysTrue) {

  protected override def executeIfCond(c: Condition,
                              thenWhat: Instruction,
                              elseWhat: Instruction, s: State, v: Boolean) :
  (List[State], List[State], List[State]) = {
    tryEval(c) match {
      case Some(true) =>
        this.execute(thenWhat, s, v)
      case Some(false) =>
        this.execute(elseWhat, s, v)
      case None =>
        val cnstrd = s.memory.addCondition(c)
        val bt = this.execute(thenWhat, s.copy(memory = cnstrd), v)
        val cnstrdf = s.memory.addCondition(FNOT(c))
        val bf = this.execute(elseWhat, s.copy(memory = cnstrdf), v)
        (bt._1 ++ bf._1, bt._2 ++ bf._2, bt._3 ++ bf._3)
    }
  }

}

object TrivialTripleInstructionExecutor extends ReallySimpleInstructionExecutor
