package org.change.v2.analysis.executor

import org.apache.commons.math3.stat.descriptive.rank.Max
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.{Solver, Z3SolverEnhanced}
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

import scala.annotation.tailrec

trait IExecutor[T] {
  def execute(instruction: Instruction,
              state: State, verbose: Boolean): T
}


abstract class Executor[T] extends IExecutor[T] {


  override def execute(instruction: Instruction,
                       state: State, verbose: Boolean): T = {
    val s = if (verbose &&
      !instruction.isInstanceOf[If] &&
      !instruction.isInstanceOf[Fork] &&
      !instruction.isInstanceOf[InstructionBlock] &&
      !instruction.isTool &&
      !instruction.isInstanceOf[Translatable])
      state.addInstructionToHistory(instruction)
    else
      state
    val as = instruction match {
      case v@NoOp => executeNoOp(s, verbose)
      case v: AllocateRaw =>
        executeAllocateRaw(v, s, verbose)
      case v: AllocateSymbol =>
        executeAllocateSymbol(v, s, verbose)
      case v: AssignNamedSymbol =>
        executeAssignNamedSymbol(v, s, verbose)
      case v: AssignRaw =>
        executeAssignRaw(v, s, verbose)
      case v: ConstrainRaw =>
        executeConstrainRaw(v, s, verbose)
      case v: ConstrainNamedSymbol =>
        executeConstrainNamedSymbol(v, s, verbose)
      case v: CreateTag =>
        executeCreateTag(v, s, verbose)
      case v: DeallocateNamedSymbol =>
        executeDeallocateNamedSymbol(v, s, verbose)
      case v: DeallocateRaw =>
        executeDeallocateRaw(v, s, verbose)
      case v: DestroyTag =>
        executeDestroyTag(v, s, verbose)
      case v: Fail =>
        executeFail(v, s, verbose)
      case v: Fork =>
        executeFork(v, s, verbose)
      case v: Forward =>
        executeForward(v, s, verbose)
      case v: If =>
        executeIf(v, s, verbose)
      case v: InstructionBlock =>
        executeInstructionBlock(v, s, verbose)
      case v : ConstrainFloatingExpression =>
        executeConstrainFloatingExpression(v, s, verbose)
      case _ =>
        executeExoticInstruction(instruction, s, verbose)
    }
    as
  }

  def executeExoticInstruction(instruction: Instruction,
                               s: State,
                               verbose: Boolean): T

  def executeNoOp(s: State, v: Boolean = false): T

  def executeInstructionBlock(instruction: InstructionBlock,
                              s: State,
                              v: Boolean = false):
  T

  def executeIf(instruction: If,
                s: State,
                v: Boolean = false): T

  def executeForward(instruction: Forward,
                     s: State,
                     v: Boolean = false):
  T

  def executeFork(instruction: Fork,
                  s: State,
                  v: Boolean = false):
  T

  def executeFail(instruction: Fail,
                  s: State,
                  v: Boolean = false):
  T

  def executeConstrainRaw(instruction: ConstrainRaw,
                          s: State,
                          v: Boolean = false):
  T
  def executeConstrainFloatingExpression(i: ConstrainFloatingExpression,
                                         s: State,
                                         v: Boolean) : T

  def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol,
                                  s: State,
                                  v: Boolean = false):
  T

  def executeAssignNamedSymbol(instruction: AssignNamedSymbol,
                               s: State,
                               v: Boolean = false):
  T

  def executeAllocateRaw(instruction: AllocateRaw,
                         state: State,
                         verbose: Boolean = false):
  T

  def executeAssignRaw(instruction: AssignRaw,
                       s: State,
                       v: Boolean = false):
  T

  def executeAllocateSymbol(instruction: AllocateSymbol,
                            s: State,
                            v: Boolean = false):
  T

  def executeDestroyTag(instruction: DestroyTag,
                        s: State,
                        v: Boolean = false):
  T

  def executeDeallocateNamedSymbol(instruction: DeallocateNamedSymbol,
                                   s: State,
                                   v: Boolean = false):
  T

  def executeCreateTag(instruction: CreateTag, s: State, v: Boolean = false):
  T


  def executeDeallocateRaw(instruction: DeallocateRaw,
                           s: State,
                           v: Boolean = false):
  T

  protected def instantiate(s: State, constraint: FloatingConstraint): Either[Constraint, String] = {
    constraint.instantiate(s)
  }

  protected def instantiate(s : State, expression: FloatingExpression, crt : Int):
  Either[(Expression, Int), String] = expression match {
    case sv : SymbolicValue => Left(sv, crt)
    case c : ConstantValue => Left((c, math.max(if (c.value != 0)
      math.ceil(math.log(c.value + 1) / math.log(2)).toInt
    else
      1, crt
    )))
    case :<<:(left, right) =>
      instantiate(s, left, crt) match {
        case Left(ee) => instantiate(s, right, ee._2)
        case Right(m) => Right(m)
      }
    case :!:(left) => instantiate(s, left, crt)
    case Symbol(id) => s.memory.evalToObject(id).flatMap(r => {
        r.value.map(v => Left((v.e, r.size)))
    }).getOrElse(Right(s"Cannot resolve reference to symbol: $id"))
    case :||:(left, right) => instantiate(s, left, crt) match {
      case Left(ee) => instantiate(s, right, ee._2)
      case Right(m) => Right(m)
    }
    case :^:(left, right) => instantiate(s, left, crt) match {
      case Left(ee) => instantiate(s, right, ee._2)
      case Right(m) => Right(m)
    }
    case :&&:(left, right) => instantiate(s, left, crt) match {
      case Left(ee) => instantiate(s, right, ee._2)
      case Right(m) => Right(m)
    }
    case :+:(left, right) => instantiate(s, left, crt) match {
      case Left(ee) => instantiate(s, right, ee._2)
      case Right(m) => Right(m)
    }
    case Address(a) => a(s) match {
      case Some(int) => s.memory.evalToObject(int).flatMap(r => {
        r.value.map(x => Left(x.e, r.size))
      }).getOrElse(Right(s"Cannot resolve reference to $int"))
      case None => Right(TagExp.brokenTagExpErrorMessage)
    }
    case :-:(left, right) => instantiate(s, left, crt) match {
      case Left(ee) => instantiate(s, right, ee._2)
      case Right(m) => Right(m)
    }
    case v : ConstantBValue => Left(v, math.max(crt, v.size))
    case v : ConstantStringValue => Left(v, math.max(crt, 64))
    case _ => ???
  }

  protected def instantiate(s: State, expression: FloatingExpression):
    Either[(Expression, Int), String] = instantiate(s, expression, 0)
}


object InstructionExecutor {
  def apply(): InstructionExecutor = {
    new DecoratedInstructionExecutor(new Z3SolverEnhanced())
  }
}

// thread unsafe thing. Beware! All 
// implementing classes should and must be bound to 1 thread
// and 1 thread only
abstract class InstructionExecutor extends Executor[(List[State], List[State])] {
  def executeInstructionBlock(instruction: InstructionBlock,
                              s: State,
                              v: Boolean = false):
  (List[State], List[State])

  def executeIf(instruction: If,
                s: State,
                v: Boolean = false):
  (List[State], List[State])

  def executeForward(instruction: Forward,
                     s: State,
                     v: Boolean = false):
  (List[State], List[State])

  def executeFork(instruction: Fork,
                  s: State,
                  v: Boolean = false):
  (List[State], List[State])

  def executeFail(instruction: Fail,
                  s: State,
                  v: Boolean = false):
  (List[State], List[State])

  def executeConstrainRaw(instruction: ConstrainRaw,
                          s: State,
                          v: Boolean = false):
  (List[State], List[State])

  def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol,
                                  s: State,
                                  v: Boolean = false):
  (List[State], List[State])

  def executeAssignNamedSymbol(instruction: AssignNamedSymbol,
                               s: State,
                               v: Boolean = false):
  (List[State], List[State])

  def executeAllocateRaw(instruction: AllocateRaw,
                         state: State,
                         verbose: Boolean = false):
  (List[State], List[State])

  def executeAssignRaw(instruction: AssignRaw,
                       s: State,
                       v: Boolean = false):
  (List[State], List[State])

  def executeAllocateSymbol(instruction: AllocateSymbol,
                            s: State,
                            v: Boolean = false):
  (List[State], List[State])

  def executeDestroyTag(instruction: DestroyTag,
                        s: State,
                        v: Boolean = false):
  (List[State], List[State])

  def executeDeallocateNamedSymbol(instruction: DeallocateNamedSymbol,
                                   s: State,
                                   v: Boolean = false):
  (List[State], List[State])

  def executeCreateTag(instruction: CreateTag, s: State, v: Boolean = false):
  (List[State], List[State])


  def executeDeallocateRaw(instruction: DeallocateRaw,
                           s: State,
                           v: Boolean = false):
  (List[State], List[State])

  override def executeConstrainFloatingExpression(i: ConstrainFloatingExpression,
                                                  s: State,
                                                  v: Boolean):
    (List[State], List[State])

}

abstract class AbstractInstructionExecutor extends InstructionExecutor {
  override def executeInstructionBlock(instruction: InstructionBlock,
                                       s: State,
                                       v: Boolean = false):
  (List[State], List[State]) = {
    instruction.instructions.foldLeft((List(s), Nil: List[State]))((acc, i) => {
      val (valid: List[State], failed: List[State]) = acc
      val (nextValid, nextFailed) = valid.map(execute(i, _, v)).unzip
      val allValid = nextValid.foldLeft(Nil: List[State])(_ ++ _)
      val allFailed = nextFailed.foldLeft(failed: List[State])(_ ++ _)
      (allValid, allFailed)
    })
  }

  override def executeConstrainFloatingExpression(instruction: ConstrainFloatingExpression,
                                                  s: State,
                                                  v: Boolean): (List[State], List[State]) =
    buildCondition(s, instruction) match {
      case Left(c) =>
        tryEval(c) match {
          case Some(b) if b => (s :: Nil, Nil)
          case Some(b) if !b => (Nil, Nil)
          case None => val mem = s.memory.addCondition(c)
            if (isSat(mem))
              (s :: Nil, Nil)
            else
              (Nil, Nil)
        }
      case Right(m) => execute(Fail(m), s, v)
    }

  override def executeNoOp(s: State, v: Boolean = false): (List[State], List[State]) = {
    (s :: Nil, Nil)
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
                          v : Boolean) : (List[State], List[State]) = {
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
          System.err.println(s"Nasty case $m, $testInstr $left")
          offender match {
            case None => executeIfCond(FAND(cds), thenWhat, elseWhat, s, v)
            case Some(Fork(xs)) => if (cds.isEmpty) {
              executeIf(Fork(xs),
                if (left.isEmpty)
                  thenWhat
                else
                  If (InstructionBlock(left),
                    thenWhat,
                    elseWhat
                  ),
                elseWhat, s, v)
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

  private def executeIfCond(c: Condition,
                            thenWhat: Instruction,
                            elseWhat: Instruction, s: State, v: Boolean) = {
    tryEval(c) match {
      case Some(true) =>
        val cnstrd = s.memory.addCondition(c)
        this.execute(thenWhat, s.copy(memory = cnstrd), v)
      case Some(false) =>
        val cnstrdf = s.memory.addCondition(FNOT(c))
        this.execute(elseWhat, s.copy(memory = cnstrdf), v)
      case None =>
        val crtSet = s.memory.getSymbols()
        val syms = s.memory.crawlCondition(c)
        val mustCheck = syms.intersect(crtSet).nonEmpty
        val cnstrd = s.memory.addCondition(c)
        val bt = if (!mustCheck || isSat(cnstrd)) {
          this.execute(thenWhat, s.copy(memory = cnstrd), v)
        } else {
          (Nil, Nil)
        }
        val cnstrdf = s.memory.addCondition(FNOT(c))
        val bf = if (!mustCheck || isSat(cnstrdf))
          this.execute(elseWhat, s.copy(memory = cnstrdf), v)
        else
          (Nil, Nil)
        (bt._1 ++ bf._1, bt._2 ++ bf._2)
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


  override def executeIf(instruction: If, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val If(testInstr, thenWhat, elseWhat) = instruction
    testInstr match {
      case in : ConstrainNamedSymbol => executeIf(in, thenWhat, elseWhat, s, v)
      case in : ConstrainRaw => executeIf(in, thenWhat, elseWhat, s, v)
      case in : InstructionBlock =>
        executeIf(in, thenWhat, elseWhat, s, v)
      case in : ConstrainFloatingExpression => executeIf(in, thenWhat, elseWhat, s, v)
      case in : Fork => executeIf(in, thenWhat, elseWhat, s, v)
      case _ => stateToError(s, s"Bad test instruction $testInstr")
    }
  }

  override def executeForward(instruction: Forward, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val Forward(place) = instruction
    val (asgOk, asgFail) = this.execute(Assign("CurrentPort", ConstantStringValue(place)), s, v)
    if (asgOk.isEmpty || asgFail.nonEmpty)
      throw new IllegalStateException("Something terribly wrong happened when assigning to port")
    (List(asgOk.head.forwardTo(place)), Nil)
  }

  override def executeFork(instruction: Fork, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val Fork(forkBlocks) = instruction
    val (success, fail) = forkBlocks.map(i => execute(i, s, v)).unzip
    (success.flatten.toList, fail.flatten.toList)
  }


  override def executeFail(instruction: Fail, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val Fail(errMsg) = instruction
    stateToError(s, errMsg)
  }

  override def executeExoticInstruction(instruction: Instruction, s: State, v: Boolean):
  (List[State], List[State]) = {
    instruction(s, v)
  }

  protected def isSat(memory: MemorySpace): Boolean

  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean = false):
  (List[State], List[State]) =
    buildCondition(s, instruction) match {
      case Left(c) =>
        tryEval(c) match {
          case Some(b) if b => (s :: Nil, Nil)
          case Some(b) if !b => (Nil, Nil)
          case None => val mem = s.memory.addCondition(c)
            if (isSat(mem))
              (s :: Nil, Nil)
            else
              (Nil, Nil)
        }
      case Right(m) => execute(Fail(m), s, v)
    }


  protected def getNewMemory(maybeNewMem: Option[org.change.v2.analysis.memory.MemorySpace]): Option[MemorySpace] = {
    maybeNewMem match {
      case None => None
      case Some(m) =>
        Some(m).filter(isSat)
    }
  }

  override def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol, s: State, v: Boolean = false):
  (List[State], List[State]) = buildCondition(s, instruction) match {
    case Left(c) =>
      tryEval(c) match {
        case Some(b) if b => (s :: Nil, Nil)
        case Some(b) if !b => (Nil, Nil)
        case None => val mem = s.memory.addCondition(c)
          if (isSat(mem))
            (s :: Nil, Nil)
          else
            (Nil, Nil)
      }
    case Right(m) => execute(Fail(m), s, v)
  }


  override def executeAssignNamedSymbol(instruction: AssignNamedSymbol,
                                        s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val AssignNamedSymbol(id, exp, t) = instruction
    assert(exp != null, s"Something terribly wrong for instruction $instruction")
    instantiate(s, exp) match {
      case Left(e) => optionToStatePair(s, s"Error during assignment of $id")(s => {
        s.memory.Assign(id, e._1, t)
      })
      case Right(err) => execute(Fail(err), s, v)
    }
  }


  override def executeAllocateRaw(instruction: AllocateRaw,
                                  state: State, verbose: Boolean = false):
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

  override def executeAssignRaw(instruction: AssignRaw,
                                s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val AssignRaw(a, exp, t) = instruction
    a(s) match {
      case Some(int) => instantiate(s, exp) match {
        case Left(e) => optionToStatePair(s, s"Error during assignment at $a")(s => {
          s.memory.Assign(int, e._1)
        })
        case Right(err) => execute(Fail(err), s, v)
      }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  override def executeAllocateSymbol(instruction: AllocateSymbol,
                                     s: State,
                                     v: Boolean = false):
  (List[State], List[State]) = {
    val AllocateSymbol(id, size) = instruction
    optionToStatePair(s, s"Cannot allocate $id")(s => {
      s.memory.Allocate(id, size)
    })
  }

  override def executeDestroyTag(instruction: DestroyTag,
                                 s: State,
                                 v: Boolean = false):
  (List[State], List[State]) = {
    val DestroyTag(name) = instruction
    optionToStatePair(s, s"Error during untagging of $name")(s => {
      s.memory.UnTag(name)
    })
  }

  override def executeDeallocateNamedSymbol(instruction: DeallocateNamedSymbol,
                                            s: State,
                                            v: Boolean = false):
  (List[State], List[State]) = {
    val DeallocateNamedSymbol(id) = instruction
    optionToStatePair(s, s"Cannot deallocate $id")(s => {
      s.memory.Deallocate(id)
    })
  }


  override def executeCreateTag(instruction: CreateTag, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val CreateTag(name: String, value: Intable) = instruction
    value(s) match {
      case Some(int) => optionToStatePair(s, s"Error during tagging of $name")(s => {
        s.memory.Tag(name, int)
      })
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  override def executeDeallocateRaw(instruction: DeallocateRaw,
                                    s: State,
                                    v: Boolean = false):
  (List[State], List[State]) = {
    val DeallocateRaw(a: Intable, size: Int) = instruction
    a(s) match {
      case Some(int) => optionToStatePair(s, s"Cannot deallocate @ $a of size $size")(s => {
        s.memory.Deallocate(int, size)
      })
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

}


class DecoratedInstructionExecutor(solver: Solver) extends AbstractInstructionExecutor {
  override protected def isSat(memory: MemorySpace): Boolean = {
    return solver.solve(memory)
  }
}

