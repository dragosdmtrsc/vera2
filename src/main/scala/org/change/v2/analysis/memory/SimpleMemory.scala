package org.change.v2.analysis.memory

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.util.UUID

import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.Mapper
import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.SimpleMemory.NaturalKey
import org.change.v2.analysis.processingmodels.{Instruction, SuperFork}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.types.NumericType
import org.change.v2.interval.IntervalOps
import org.change.v2.analysis.memory.TagExp._
import z3.scala.{Z3AST, Z3Context, Z3Solver}

import scala.annotation.tailrec
import scala.collection.{immutable, mutable}
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.collection.mutable.ListBuffer
import scala.util.Random

case class SimpleMemoryObject(expression: Expression = ConstantValue(0),
                              size: Int = 64)

trait BranchC {
  val id: Long
}
case class IfB(override val id: Long = Random.nextLong()) extends BranchC
case class ForkB(override val id: Long = Random.nextLong()) extends BranchC

case class SimpleMemory(
    errorCause: Option[String] = None,
    history: List[String] = Nil,
    symbols: SortedMap[String, SimpleMemoryObject] = SortedMap.empty,
    rawObjects: SortedMap[Int, SimpleMemoryObject] = SortedMap.empty,
    memTags: SortedMap[String, Int] = SortedMap.empty,
    pathCondition: SimplePathCondition = SimplePathCondition.apply(),
    branchHistory: List[BranchC] = Nil) {
  def createTag(name: String, value: Int): SimpleMemory =
    copy(memTags = memTags + (name -> value))

  def addBranch(b: BranchC): SimpleMemory =
    copy(branchHistory = b :: branchHistory)
  def rmBranch(): SimpleMemory = copy(branchHistory = branchHistory.tail)

  def eval(tag: Intable): Option[Int] = tag match {
    case v: IntImprovements => Some(v.value)
    case Tag(name)          => memTags.get(name)
    case TagExp(plusTags, minusTags, rest) =>
      plusTags
        .foldLeft(Some(rest): Option[Int])((acc, r) => {
          acc.flatMap(f => eval(r).map(_ + f))
        })
        .flatMap(f => {
          minusTags.foldLeft(Some(f): Option[Int])((acc, r) => {
            acc.flatMap(f => eval(r).map(_ - f))
          })
        })
    case _ => ???
  }

  def canRead(s: String): Boolean = symbols.contains(s)
  def canRead(t: Int): Boolean = rawObjects.contains(t)

  def fail(because: String): SimpleMemory = copy(errorCause = Some(because))
  def location: String = history.headOption.getOrElse("")
  def forwardTo(place: String): SimpleMemory = copy(history = place :: history)
  def error: String = errorCause.getOrElse("")
  def addCondition(condition: Condition): SimpleMemory =
    copy(pathCondition = pathCondition && condition)
  def addCondition(condition: SimplePathCondition): SimpleMemory =
    copy(pathCondition = pathCondition && condition)

  def Tag(name: String, value: Int): Option[SimpleMemory] =
    Some(copy(memTags = memTags + (name -> value)))
  def UnTag(name: String): Option[SimpleMemory] =
    Some(copy(memTags = memTags - name))
  def canModifyExisting(a: Int, size: Int): Boolean =
    rawObjects.contains(a) && rawObjects(a).size == size

  def doesNotOverlap(a: Int, size: Int): Boolean = {
    (!rawObjects.contains(a)) &&
    rawObjects.forall(kv =>
      !IntervalOps
        .intervalIntersectionIsInterval(a, a + size, kv._1, kv._1 + kv._2.size))
  }
  def canModify(a: Int, size: Int): Boolean =
    doesNotOverlap(a, size) ||
      (rawObjects.contains(a) && rawObjects(a).size == size)
  def Allocate(a: Int, size: Int): Option[SimpleMemory] =
    if (canModifyExisting(a, size))
      Some(
        copy(
          rawObjects = rawObjects + (a -> rawObjects(a).copy(size = size))
        ))
    else if (canModify(a, size))
      Some(
        copy(
          rawObjects = rawObjects + (a -> SimpleMemoryObject(size = size))
        ))
    else None
  def Allocate(id: String, sz: Int): SimpleMemory =
    copy(symbols = symbols + (id -> SimpleMemoryObject(size = sz)))

  def assignNewValue(a: Int, exp: Expression): Option[SimpleMemory] =
    if (canRead(a)) {
      Some(
        copy(
          rawObjects = rawObjects + (a -> rawObjects(a).copy(expression = exp))
        ))
    } else {
      None
    }

  def declareSymbol(name: String, size: Int) = SymbolicValue(name)

  def assignNewValue(id: String, exp: Expression): Option[SimpleMemory] =
    Some(
      symbols
        .get(id)
        .map(h => copy(symbols = symbols + (id -> h.copy(expression = exp))))
        .getOrElse(
          copy(symbols = symbols + (id -> SimpleMemoryObject(exp)))
        ))
  def deallocate(s: String): Option[SimpleMemory] =
    if (canRead(s))
      Some(copy(symbols = symbols - s))
    else
      None
  def deallocate(a: Int, size: Int): Option[SimpleMemory] =
    if (canModifyExisting(a, size))
      Some(copy(rawObjects = rawObjects - a))
    else
      None
}

class SimpleMemoryInterpreter(
    quickTrimStrategy: (Condition, Condition) => Boolean = (_, _) => true)
    extends Mapper[SimpleMemory, Triple[SimpleMemory]] {
  def instantiate(fexp: FloatingExpression,
                  simpleMemory: SimpleMemory): Option[Expression] = fexp match {
    case Havoc(prefix) =>
      Some(SymbolicValue(prefix + s"${UUID.randomUUID().toString}"))
    case :<<:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => LShift(Value(r1), Value(r2)))
      })
    case :!:(left)  => instantiate(left, simpleMemory).map(e => LNot(Value(e)))
    case Symbol(id) => simpleMemory.symbols.get(id).map(_.expression)
    case :||:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => Lor(Value(r1), Value(r2)))
      })
    case :^:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => LXor(Value(r1), Value(r2)))
      })
    case :&&:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => LAnd(Value(r1), Value(r2)))
      })
    case :+:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => Plus(Value(r1), Value(r2)))
      })
    case Address(a) =>
      simpleMemory.eval(a) match {
        case Some(x) => simpleMemory.rawObjects.get(x).map(_.expression)
        case None    => None
      }

    case :-:(left, right) =>
      instantiate(left, simpleMemory).flatMap(r1 => {
        instantiate(right, simpleMemory).map(r2 => Minus(Value(r1), Value(r2)))
      })
    case cv: ConstantValue        => Some(cv)
    case cbv: ConstantBValue      => Some(cbv)
    case csv: ConstantStringValue => Some(csv)
    case sv: SymbolicValue        => Some(sv)
  }

  def instantiate(fc: FloatingConstraint,
                  simpleMemory: SimpleMemory): Option[Constraint] = fc match {
    case :|:(a, b) =>
      instantiate(a, simpleMemory).flatMap(ca => {
        instantiate(b, simpleMemory).map(cb => {
          OR(ca :: cb :: Nil)
        })
      })
    case :&:(a, b) =>
      instantiate(a, simpleMemory).flatMap(ca => {
        instantiate(b, simpleMemory).map(cb => {
          AND(ca :: cb :: Nil)
        })
      })
    case Yes()     => Some(Truth())
    case :~:(c)    => instantiate(c, simpleMemory).map(NOT)
    case :==:(exp) => instantiate(exp, simpleMemory).map(EQ_E)
    case :<:(exp)  => instantiate(exp, simpleMemory).map(LT_E)
    case :<=:(exp) => instantiate(exp, simpleMemory).map(LTE_E)
    case :>:(exp)  => instantiate(exp, simpleMemory).map(GT_E)
    case :>=:(exp) => instantiate(exp, simpleMemory).map(GTE_E)
    case or: OR    => Some(or)
    case an: AND   => Some(an)
  }

  def inferWidth(fexp: FloatingExpression,
                 simpleMemory: SimpleMemory,
                 width: Int = 0): Int = fexp match {
    case :<<:(left, right) => inferWidth(left, simpleMemory, width)
    case :!:(left)         => inferWidth(left, simpleMemory, width)
    case Symbol(id)        => Math.max(simpleMemory.symbols(id).size, width)
    case :^:(left, right) =>
      inferWidth(right, simpleMemory, inferWidth(left, simpleMemory, width))
    case :&&:(left, right) =>
      inferWidth(right, simpleMemory, inferWidth(left, simpleMemory, width))
    case :+:(left, right) =>
      inferWidth(right, simpleMemory, inferWidth(left, simpleMemory, width))
    case Address(a) =>
      Math.max(simpleMemory.rawObjects(simpleMemory.eval(a).get).size, width)
    case :-:(left, right) =>
      inferWidth(right, simpleMemory, inferWidth(left, simpleMemory, width))
    case cv: ConstantValue =>
      val minWidth = Math.ceil(Math.log(cv.value) / Math.log(2)).intValue()
      Math.max(minWidth, width)
    case cbv: ConstantBValue => Math.max(width, cbv.size)
    case csv: ConstantStringValue =>
      val minWidth =
        Math.ceil(Math.log(csv.value.hashCode) / Math.log(2)).intValue()
      Math.max(minWidth, width)
      Math.max(width, 64)
    case sv: SymbolicValue => width
    case _                 => ???
  }

  def instantiate(fexp: FloatingExpression,
                  fc: FloatingConstraint,
                  simpleMemory: SimpleMemory): Option[Condition] =
    instantiate(fexp, simpleMemory).flatMap(exp => {
      instantiate(fc, simpleMemory).map(c => {
        val width = inferWidth(fexp, simpleMemory)
        OP(exp, c, width)
      })
    })

  def tryEval(v: Long, constraint: Constraint): Option[Boolean] =
    constraint match {
      case LT_E(e)       => tryEval(e).map(v < _)
      case LTE_E(e)      => tryEval(e).map(v <= _)
      case GTE_E(e)      => tryEval(e).map(v >= _)
      case GT_E(e)       => tryEval(e).map(v > _)
      case EQ_E(e)       => tryEval(e).map(v == _)
      case No            => Some(false)
      case LT(v2)        => Some(v < v2)
      case LTE(v2)       => Some(v <= v2)
      case GT(v2)        => Some(v > v2)
      case GTE(v2)       => Some(v >= v2)
      case E(v2)         => Some(v == v2)
      case Truth()       => Some(true)
      case Range(v1, v2) => Some(v >= v1 && v <= v2)
      case OR(cts) =>
        val evald = cts.map(tryEval(v, _))
        if (evald.exists(h => h.nonEmpty && h.get))
          Some(true)
        else if (evald.forall(h => h.nonEmpty && !h.get))
          Some(false)
        else
          None
      case AND(cts) =>
        val evald = cts.map(tryEval(v, _))
        if (evald.exists(h => h.nonEmpty && !h.get))
          Some(false)
        else if (evald.forall(h => h.nonEmpty && !h.get))
          Some(true)
        else
          None
      case NOT(c) => tryEval(v, c).map(!_)
      case _      => None
    }

  def tryEval(expression: Expression): Option[Long] = expression match {
    case LShift(a, b) =>
      tryEval(a.e).flatMap(l => tryEval(b.e).map(r => l << r))
    case Plus(a, b)                        => tryEval(a.e).flatMap(l => tryEval(b.e).map(r => r + l))
    case LNot(a)                           => tryEval(a.e).map(~_)
    case LAnd(a, b)                        => tryEval(a.e).flatMap(l => tryEval(b.e).map(r => l & r))
    case Lor(a, b)                         => tryEval(a.e).flatMap(l => tryEval(b.e).map(r => l | r))
    case LXor(a, b)                        => tryEval(a.e).flatMap(l => tryEval(b.e).map(r => r ^ l))
    case Minus(a, b)                       => tryEval(a.e).flatMap(l => tryEval(b.e).map(r => l - r))
    case ConstantValue(value, isIp, isMac) => Some(value)
    case ConstantStringValue(value)        => Some(value.hashCode)
    case _                                 => None
  }

  def tryEval(condition: Condition): Option[Boolean] = condition match {
    case FAND(cts) =>
      val evald = cts.map(tryEval)
      if (evald.exists(h => h.nonEmpty && !h.get))
        Some(false)
      else if (evald.forall(h => h.nonEmpty && h.get))
        Some(true)
      else
        None
    case FOR(cts) =>
      val evald = cts.map(tryEval)
      if (evald.exists(h => h.nonEmpty && h.get))
        Some(true)
      else if (evald.forall(h => h.nonEmpty && !h.get))
        Some(false)
      else
        None
    case FNOT(c) => tryEval(c).map(!_)
    case OP(e, ct, sz) =>
      tryEval(e).flatMap(v => tryEval(v, ct))
    case _ => None
  }

  var nrCallsGenerate = 0
  var totalGenerate = 0l

  def generateConditions(
      instr: Instruction,
      state: SimpleMemory): (Condition, Condition, Condition) = {
    val c = instr match {
      case InstructionBlock(is) =>
        is.map(generateConditions(_, state)).foldLeft(
          (TRUE, FALSE, FALSE): (Condition, Condition, Condition)
        )(
          (rest, ch) => {
            if (ch._3 == FALSE) {
              (FAND.makeFAND(ch._1 :: rest._1 :: Nil),
               FOR.makeFOR(
                 ch._2 :: rest._2 :: Nil
               ), if (rest._3 == FALSE) {
                  FALSE
                } else {
                  FAND.makeFAND(ch._1 :: rest._3 :: Nil)
                })
            } else {
              (FAND.makeFAND(ch._1 :: rest._1 :: Nil),
               FOR.makeFOR(
                 ch._2 :: FAND.makeFAND(ch._1 :: rest._2 :: Nil) :: Nil
               ),
               if (rest._3 == FALSE) {
                 ch._3
               } else {
                 FOR.makeFOR(
                   ch._3 :: FAND.makeFAND(ch._1 :: rest._3 :: Nil) :: Nil
                 )
               })
            }
          })
      case Fork(is) =>
        is.map(generateConditions(_, state)).foldLeft(
          (FALSE, TRUE, FALSE): (Condition, Condition, Condition)
        )(
          (rest, ch) => {
            if (ch._3 == FALSE) {
              (FOR.makeFOR(
                ch._1 :: rest._1 :: Nil
              ),
                FAND.makeFAND(
                  ch._2 :: rest._2 :: Nil
                ),
                if (rest._3 == FALSE) {
                  FALSE
                } else {
                  FAND.makeFAND(ch._2 :: rest._3 :: Nil)
                })
            } else {
              (FOR.makeFOR(
                ch._1 :: FAND.makeFAND(ch._2 :: rest._1 :: Nil) :: Nil
              ),
                FAND.makeFAND(
                  ch._2 :: rest._2 :: Nil
                ),
                if (rest._3 == FALSE) {
                  ch._3
                } else {
                  FOR.makeFOR(
                    ch._3 :: FAND.makeFAND(ch._2 :: rest._3 :: Nil) :: Nil
                  )
                })
            }
          })
      case ConstrainFloatingExpression(fe, dc) =>
        instantiate(fe, dc, state)
          .map(r => {
            tryEval(r)
              .map(u => {
                if (u) (TRUE, FALSE, FALSE) else (FALSE, TRUE, FALSE: Condition)
              })
              .getOrElse((r, FNOT.makeFNOT(r), FALSE))
          })
          .getOrElse((FALSE, FALSE, TRUE))
      case ConstrainNamedSymbol(id, fc, _) =>
        generateConditions(ConstrainFloatingExpression(:@(id), fc), state)
      case ConstrainRaw(a, fc, _) =>
        generateConditions(ConstrainFloatingExpression(:@(a), fc), state)
    }
    c
  }

  override def execute(instruction: Instruction,
                       state: SimpleMemory,
                       verbose: Boolean): Triple[SimpleMemory] =
    instruction match {
      case Fork(forkBlocks) =>
        val fb = ForkB()
        val withH = state.addBranch(fb)
        forkBlocks
          .map(execute(_, withH, verbose))
          .foldLeft(new Triple[SimpleMemory]())((acc, r) => {
            acc + r
          })
      case SuperFork(forkBlocks) =>
        val fb = ForkB()
        val withH = state.addBranch(fb)
        forkBlocks
          .map(execute(_, withH, verbose))
          .foldLeft(new Triple[SimpleMemory]())((acc, r) => {
            acc + r
          })
      case InstructionBlock(instructions) =>
        instructions.foldLeft(Triple.startFrom[SimpleMemory](state))(
          (acc, i) => {
            acc.continue.foldLeft(acc.finals())((res, crt) => {
              res + execute(i, crt, verbose)
            })
          })
      case Fail(errMsg) =>
        new Triple[SimpleMemory](Nil, state.fail(errMsg) :: Nil, Nil)
      case NoOp => Triple.startFrom[SimpleMemory](state)
      case Forward(place) =>
        new Triple[SimpleMemory](state.forwardTo(place) :: Nil, Nil, Nil)
      case ExistsRaw(a) => Triple.startFrom[SimpleMemory](state)
      case AllocateSymbol(id, size) =>
        Triple.startFrom[SimpleMemory](state.Allocate(id, size))
      case CreateTag(name, value) =>
        state
          .eval(value)
          .map(v => {
            Triple.startFrom[SimpleMemory](state.createTag(name, v))
          })
          .getOrElse(
            new Triple[SimpleMemory](
              success = Nil,
              continue = Nil,
              failed = state.fail(TagExp.brokenTagExpErrorMessage) :: Nil))
      case DeallocateNamedSymbol(id) =>
        state
          .deallocate(id)
          .map(Triple.startFrom[SimpleMemory])
          .getOrElse(
            Triple[SimpleMemory](Nil,
                                 state.fail(s"can't deallocate $id") :: Nil,
                                 Nil))
      case If(testInstr, thenWhat, elseWhat) =>
        val ifb = IfB()
        def validate(cds: (Condition, Condition, Condition)) = {
          val takeB =
            if (cds._1 != FALSE && quickTrimStrategy(state.pathCondition.cd,
                                                     cds._1)) {
              cds._1
            } else {
              FALSE
            }
          val takeC =
            if (cds._2 != FALSE && quickTrimStrategy(state.pathCondition.cd,
                                                     cds._2)) {
              cds._2
            } else {
              FALSE
            }
          val takeF =
            if (cds._3 != FALSE && quickTrimStrategy(state.pathCondition.cd,
                                                     cds._3)) {
              cds._3
            } else {
              FALSE
            }
          (takeB, takeC, takeF)
        }
        val start = System.currentTimeMillis()
        val c = generateConditions(testInstr, state)
        val end = System.currentTimeMillis()
        totalGenerate += (end - start)
        nrCallsGenerate += 1
        val cds = validate(c)
        if (cds._3 != FALSE) {
          System.err.println(
            s"found segfault condition $testInstr == ${cds._3}")
          val takeF = new Triple[SimpleMemory](
            continue = Nil,
            success = Nil,
            failed = state
              .addCondition(cds._3)
              .fail(s"segfault at $testInstr") :: Nil)
          val n3 = state.addCondition(FNOT.makeFNOT(cds._3))
          val takeB = if (cds._1 != FALSE) {
            if (cds._2 == FALSE) {
              execute(thenWhat, n3.addBranch(ifb), verbose)
            } else {
              execute(thenWhat,
                      state.addBranch(ifb).addCondition(cds._1),
                      verbose)
            }
          } else {
            new Triple[SimpleMemory]()
          }
          val takeC = if (cds._2 != FALSE) {
            if (cds._1 == FALSE) {
              execute(elseWhat, n3.addBranch(ifb), verbose)
            } else {
              execute(elseWhat,
                      state.addBranch(ifb).addCondition(cds._2),
                      verbose)
            }
          } else {
            new Triple[SimpleMemory]()
          }
          takeB + takeC + takeF
        } else {
          val takeB = if (cds._1 != FALSE) {
            if (cds._2 == FALSE) {
              execute(thenWhat, state.addBranch(ifb), verbose)
            } else {
              execute(thenWhat,
                      state.addBranch(ifb).addCondition(cds._1),
                      verbose)
            }
          } else {
            new Triple[SimpleMemory]()
          }
          val takeC = if (cds._2 != FALSE) {
            if (cds._1 == FALSE) {
              execute(elseWhat, state.addBranch(ifb), verbose)
            } else {
              execute(elseWhat,
                      state.addBranch(ifb).addCondition(cds._2),
                      verbose)
            }
          } else {
            new Triple[SimpleMemory]()
          }
          takeB + takeC
        }
      case Assume(i) =>
        val start = System.currentTimeMillis()
        val cds = generateConditions(i, state)
        val end = System.currentTimeMillis()
        totalGenerate += (end - start)
        nrCallsGenerate += 1
        val fld =
          if (cds._3 != FALSE && quickTrimStrategy(state.pathCondition.cd,
                                                   cds._3)) {
            state.addCondition(cds._3).fail(s"segfault in $i") :: Nil
          } else {
            Nil
          }
        val cont =
          if (cds._1 != FALSE && quickTrimStrategy(state.pathCondition.cd,
                                                   cds._1)) {
            state.addCondition(cds._1) :: Nil
          } else {
            Nil
          }
        new Triple[SimpleMemory](continue = cont, success = Nil, failed = fld)

      case AssignNamedSymbol(id, exp, t) =>
        instantiate(exp, state)
          .flatMap(e => {
            tryEval(e)
              .map(f => {
                state
                  .assignNewValue(id, ConstantValue(f))
                  .map(Triple.startFrom[SimpleMemory])
              })
              .getOrElse(
                state.assignNewValue(id, e).map(Triple.startFrom[SimpleMemory])
              )
          })
          .getOrElse(
            Triple[SimpleMemory](Nil,
                                 state.fail(s"cannot Assign $id $exp") :: Nil,
                                 Nil))
      case ExistsNamedSymbol(symbol) =>
        if (state.canRead(symbol))
          Triple.startFrom[SimpleMemory](state)
        else
          Triple[SimpleMemory](Nil,
                               state.fail(s"$symbol doesn't exist") :: Nil,
                               Nil)
      case ConstrainFloatingExpression(floatingExpression, dc) =>
        instantiate(floatingExpression, dc, state)
          .map(r => {
            if (quickTrimStrategy(state.pathCondition.cd, r))
              Triple.startFrom[SimpleMemory](state.addCondition(r))
            else
              new Triple[SimpleMemory]()
          })
          .getOrElse(Triple[SimpleMemory](
            Nil,
            state.fail(
              s"cannot ConstrainFloatingExpression $floatingExpression $dc") :: Nil,
            Nil))
      case DeallocateRaw(a, size) =>
        state.eval(a) match {
          case Some(i) =>
            state
              .deallocate(i, size)
              .map(Triple.startFrom[SimpleMemory])
              .getOrElse(
                Triple[SimpleMemory](Nil,
                                     state.fail(s"can't deallocate $a") :: Nil,
                                     Nil))
          case None =>
            Triple[SimpleMemory](
              Nil,
              state
                .fail(TagExp.brokenTagExpErrorMessage + s" in deallocate $a") :: Nil,
              Nil)
        }

      case AllocateRaw(a, size) =>
        state.eval(a) match {
          case Some(i) =>
            state
              .Allocate(i, size)
              .map(Triple.startFrom[SimpleMemory])
              .getOrElse(
                Triple[SimpleMemory](Nil,
                                     state.fail(s"can't allocate $a") :: Nil,
                                     Nil))
          case None =>
            Triple[SimpleMemory](
              Nil,
              state
                .fail(TagExp.brokenTagExpErrorMessage + s"in allocate $a") :: Nil,
              Nil)
        }
      case AssignRaw(a, exp, t) =>
        state.eval(a) match {
          case Some(i) =>
            instantiate(exp, state)
              .map(e => {
                tryEval(e)
                  .map(v => {
                    ConstantValue(v)
                  })
                  .map(state.assignNewValue(i, _))
                  .getOrElse(state.assignNewValue(i, e))
                  .map(Triple.startFrom[SimpleMemory])
                  .getOrElse(
                    Triple[SimpleMemory](
                      Nil,
                      state.fail(s"can't assign $a <- exp") :: Nil,
                      Nil))
              })
              .getOrElse(Triple[SimpleMemory](
                Nil,
                state.fail(s"can't assign $a <- $exp") :: Nil,
                Nil))
          case None =>
            Triple[SimpleMemory](
              Nil,
              state
                .fail(TagExp.brokenTagExpErrorMessage + s" in assign $a") :: Nil,
              Nil)
        }
      case ConstrainRaw(a, dc, c) =>
        execute(ConstrainFloatingExpression(:@(a), dc), state, verbose)
      case ConstrainNamedSymbol(id, dc, c) =>
        execute(ConstrainFloatingExpression(:@(id), dc), state, verbose)
      case mp: Mapper[SimpleMemory, Triple[SimpleMemory]] =>
        mp.execute(instruction, state, verbose)
    }
}
object TrivialSimpleMemoryInterpreter extends SimpleMemoryInterpreter

object SimpleMemory {
  var statsPrinter = new PrintStream("stats.csv")

  def mergeConditions(states: Iterable[SimpleMemory],
                      base: SimpleMemory): Option[SimpleMemory] = {
    if (states.isEmpty)
      None
    else {
      if (states.size == 1) {
        Some(
          base.addCondition(states.head.pathCondition)
        )
      } else {
        Some(
          base.addCondition(
            states.foldLeft(SimplePathCondition.orDefault())((acc, x) => {
              acc || x.pathCondition
            }))
        )
      }
    }
  }
  type NaturalKey = (SortedMap[String, Int], SortedSet[Int], SortedSet[String])

  val rnd = new Random(13)

  def naturalGroup(h: SimpleMemory): NaturalKey =
    (h.memTags, h.rawObjects.keySet, h.symbols.keySet)
  def naturalMerge(k: NaturalKey, chi: Iterable[SimpleMemory]): SimpleMemory = {
    assert(chi.nonEmpty)
    if (chi.size == 1)
      chi.head
    else {
      val v = chi
//        .groupBy(r => {
//          k._3
//            .map(f => r.symbols(f))
//            .toList ++ k._2.map(f => r.rawObjects(f)).toList
//        })
//        .values
//        .map(vv => {
//          vv.head.copy(pathCondition = SimplePathCondition.apply(FOR.makeFOR(vv.map(r => r.pathCondition.cd).toList)))
//        })
      val hd = v.head
      val raws = k._2.map(r => {
        val repr = hd.rawObjects(r)
        r -> (if (v.tail.forall(h => h.rawObjects(r) == repr))
                Some(repr)
              else
                None)
      })
      val syms = k._3.map(r => {
        val repr = hd.symbols(r)
        r -> (if (v.tail.forall(h => h.symbols(r) == repr))
                Some(repr)
              else
                None)
      })

      val id = rnd.nextLong().toString
      val withSyms = syms
        .collect {
          case (sym, None) => sym
        }
        .foldLeft(chi.map(h => (h, h.pathCondition)))((acc, st) => {
          val sbname = SymbolicValue(s"meta$st$id")
          acc.map(
            r =>
              r._1 -> (r._2 && OP(r._1.symbols(st).expression,
                                  EQ_E(sbname),
                                  r._1.symbols(st).size)))
        })

      val withRaws = raws
        .collect {
          case (raw, None) => raw
        }
        .foldLeft(withSyms)((acc, raw) => {
          val sbname = SymbolicValue(s"header$raw$id")
          acc.map(
            r =>
              r._1 -> (r._2 && OP(r._1.rawObjects(raw).expression,
                                  EQ_E(sbname),
                                  r._1.rawObjects(raw).size)))
        })

      SimpleMemory(
        history = hd.history.head :: Nil,
        memTags = k._1,
        rawObjects = SortedMap.empty[Int, SimpleMemoryObject] ++ raws.collect {
          case (raw, Some(t)) => raw -> t
          case (raw, None) =>
            raw -> SimpleMemoryObject(SymbolicValue(s"header$raw$id"),
                                      hd.rawObjects(raw).size)
        }.toMap,
        symbols = SortedMap.empty[String, SimpleMemoryObject] ++ syms.collect {
          case (sym, Some(t)) => sym -> t
          case (sym, None) =>
            sym -> SimpleMemoryObject(SymbolicValue(s"meta$sym$id"),
                                      hd.symbols(sym).size)
        }
      ).addCondition(
        withRaws.foldLeft(SimplePathCondition.orDefault())((acc, h) => {
          acc || h._2
        }))
    }
  }

  def hitMe(states: Iterable[SimpleMemory]): Iterable[SimpleMemory] = {
    merge(states)(naturalGroup)(naturalMerge)
  }

  def group[T](states: Iterable[SimpleMemory])(
      fun: SimpleMemory => T): Map[T, Iterable[SimpleMemory]] =
    states.groupBy(fun)

  def merge[T](states: Iterable[SimpleMemory])(fun: SimpleMemory => T)(
      merge: (T, Iterable[SimpleMemory]) => SimpleMemory)
    : Iterable[SimpleMemory] =
    group(states)(fun).map(f => merge(f._1, f._2))

  class DumbTranslator {

    def visit(expression: Expression): Unit = expression match {
      case LShift(a, b)    => visit(a.e); visit(b.e)
      case Plus(a, b)      => visit(a.e); visit(b.e)
      case LNot(a)         => visit(a.e)
      case LAnd(a, b)      => visit(a.e); visit(b.e)
      case Lor(a, b)       => visit(a.e); visit(b.e)
      case LXor(a, b)      => visit(a.e); visit(b.e)
      case Minus(a, b)     => visit(a.e); visit(b.e)
      case PlusE(a, b)     => visit(a); visit(b)
      case MinusE(a, b)    => visit(a); visit(b)
      case LogicalOr(a, b) => visit(a.e); visit(b.e)
      case _               => ;
    }
    def visit(constraint: Constraint): Unit = constraint match {
      case LT_E(e)          => visit(e)
      case LTE_E(e)         => visit(e)
      case GTE_E(e)         => visit(e)
      case GT_E(e)          => visit(e)
      case EQ_E(e)          => visit(e)
      case OR(constraints)  => constraints.foreach(visit)
      case AND(constraints) => constraints.foreach(visit)
      case NOT(constraint)  => visit(constraint)
      case _                => ;
    }
    def visit(condition: Condition, level: Int = 0): Unit = {
      val st = new mutable.Stack[Condition]()
      st.push(condition)
      var mm = st.size
      var crt = 1
      while (st.nonEmpty) {

        val condition = st.pop()
        condition match {
//          case op : OP => visit(op.expression); visit(op.constraint)
          case a: FAND => a.conditions.foreach(st.push)
          case o: FOR  => o.conditions.foreach(st.push)
          case n: FNOT => st.push(n.condition)
          case _       => ;
        }
        mm = Math.max(st.size, mm)
      }
    }

  }
  import z3.scala.dsl._

  class Translator(z3: Z3Context, slv: Z3Solver, checkParam: Int = -1) {
    def createAST(condition: Condition): Z3AST = condition match {
      case OP(expression, constraint, size) => //z3.mkFreshBoolConst("abc")
        translateC(translateE(size, expression), size, constraint)
      case FAND(conditions)   => z3.mkAnd(conditions.map(createAST): _*)
      case FOR(conditions)    => z3.mkOr(conditions.map(createAST): _*)
      case FNOT(condition)    => z3.mkNot(createAST(condition))
      case Placeholder(_, id) => z3.mkBoolConst(id)
      case TRUE               => z3.mkTrue()
      case FALSE              => z3.mkFalse()
    }

    def solve(condition: Condition): Boolean = {
      val normal = createAST(condition)
//      System.err.println(s"normal ${normal.toString}")
//      System.err.println(s"simplified ${simplified.toString}")
      slv.assertCnstr(normal)
      slv.check().getOrElse(false)
    }

    def translateC(ast: Z3AST, sz: Int, constr: Constraint): Z3AST =
      constr match {
        case AND(constrs) => z3.mkAnd(constrs.map(translateC(ast, sz, _)): _*)
        case OR(constrs)  => z3.mkOr(constrs.map(translateC(ast, sz, _)): _*)
        case NOT(c)       => z3.mkNot(translateC(ast, sz, c))
        case GT_E(e)      => z3.mkBVUgt(ast, translateE(sz, e))
        case LT_E(e)      => z3.mkBVUlt(ast, translateE(sz, e))
        case LTE_E(e)     => z3.mkBVUle(ast, translateE(sz, e))
        case GTE_E(e) =>
          z3.mkBVUge(ast, translateE(sz, e))
        case EQ_E(e) =>
          z3.mkEq(ast, translateE(sz, e))
      }

    def translateE(sz: Int, expression: Expression): Z3AST =
      expression match {
        case LShift(a, b) =>
          z3.mkBVLshr(translateE(sz, a.e), translateE(sz, b.e))
        case sv: SymbolicValue =>
          z3.mkConst(sv.canonicalName(), z3.mkBVSort(sz))
        case Plus(a, b) => z3.mkBVAdd(translateE(sz, a.e), translateE(sz, b.e))
        case LNot(a)    => z3.mkNot(translateE(sz, a.e))
        case LAnd(a, b) =>
          z3.mkBVAnd(translateE(sz, a.e), translateE(sz, b.e))
        case Lor(a, b)   => z3.mkBVOr(translateE(sz, a.e), translateE(sz, b.e))
        case LXor(a, b)  => z3.mkBVXor(translateE(sz, a.e), translateE(sz, b.e))
        case Minus(a, b) => z3.mkBVSub(translateE(sz, a.e), translateE(sz, b.e))
        case ConstantValue(value, isIp, isMac) =>
          z3.mkNumeral(value.toString, z3.mkBVSort(sz))
        case PlusE(a, b)  => z3.mkAdd(translateE(sz, a), translateE(sz, b))
        case MinusE(a, b) => z3.mkSub(translateE(sz, a), translateE(sz, b))
        case LogicalOr(a, b) =>
          z3.mkBVOr(translateE(sz, a.e), translateE(sz, b.e))
        case ConstantBValue(v, size) =>
          z3.mkNumeral(BigInt(v.substring(2), 16).toString, z3.mkBVSort(size))
        case ConstantStringValue(v) =>
          z3.mkNumeral(v.hashCode.toString, z3.mkBVSort(sz))
        case _ => ???
      }

    def filter(lst: ContextPackage): ContextPackage =
      if (lst.canBe && solve(lst))
        lst.copy(sureSat = true, canBe = true)
      else
        lst.copy(canBe = false)

    def solve(lst: ContextPackage): Boolean = {
      if (lst.sureSat)
        true
      else {
        slv.push()
        val mmap = mutable.Map[String, (Condition, Z3AST)]()
        lst.asserted.foreach(h => {
          val ast =
            translateC(translateE(h.size, h.expression), h.size, h.constraint)
          val uuid = UUID.randomUUID().toString
          val bla = z3.mkBoolConst(s"bla$uuid")
          slv.assertCnstr(z3.mkImplies(bla, ast))
          mmap.put(s"bla$uuid", (h, bla))
        })

        lst.negd.foreach(h => {
          val ast =
            z3.mkNot(
              translateC(translateE(h.size, h.expression),
                         h.size,
                         h.constraint))
          val uuid = UUID.randomUUID().toString
          val bla = z3.mkBoolConst(s"bla$uuid")
          slv.assertCnstr(z3.mkImplies(bla, ast))
          mmap.put(s"bla$uuid", (FNOT(h), bla))
        })
        val start = System.currentTimeMillis()
        val q = slv.checkAssumptions(mmap.map(h => h._2._2).toList: _*).get
        val full = System.currentTimeMillis() - start
        if (!q) {
          val unsatCore = slv.getUnsatCore().toList
          System.err.println(
            s"Path wrong because\n${mmap.filter(r => unsatCore.contains(r._2._2)).map(r => r._2._1).mkString("\n")}")
        }
        statsPrinter.println(s"solved,$q,$full,${lst.size}")
        slv.pop()
        q
      }
    }

    case class ContextPackage(asserted: Set[OP] = Set.empty,
                              negd: Set[OP] = Set.empty,
                              hardcode: Map[SymbolicValue, Long] = Map.empty,
                              canBe: Boolean = true,
                              sureSat: Boolean = false) {
      def size: Int = asserted.size + negd.size
      def &(op: OP): ContextPackage = {
        if (!canBe)
          this
        else
          op.constraint match {
            case NOT(r) =>
              val newop = OP(op.expression, r, op.size)
              this ~ newop
            case _ =>
              val ct = asserted.contains(op)
              if (ct)
                this
              else {
                op match {
                  case OP(sv: SymbolicValue, EQ_E(cv: ConstantValue), _) =>
                    val hct = hardcode.contains(sv)
                    if (hct && hardcode(sv) != cv.value)
                      copy(canBe = false, sureSat = false)
                    else if (hct) {
                      this
                    } else {
                      copy(asserted = asserted + op,
                           hardcode = hardcode + (sv -> cv.value),
                           canBe = !negd.contains(op),
                           sureSat = false)
                    }
                  case OP(cv: ConstantValue, EQ_E(sv: SymbolicValue), size) =>
                    val hct = hardcode.contains(sv)
                    if (hct && hardcode(sv) != cv.value)
                      copy(canBe = false, sureSat = false)
                    else if (hct) {
                      this
                    } else {
                      val nop = OP(sv, EQ_E(cv), size)
                      copy(asserted = asserted + nop,
                           hardcode = hardcode + (sv -> cv.value),
                           canBe = !negd.contains(nop),
                           sureSat = false)
                    }
                  case _ =>
                    copy(asserted = asserted + op,
                         canBe = !negd.contains(op),
                         sureSat = false)
                }
              }
          }
      }
      def ~(op: OP): ContextPackage = {
        if (!canBe)
          this
        else
          op.constraint match {
            case NOT(r) =>
              val newop = OP(op.expression, r, op.size)
              this & newop
            case _ =>
              op match {
                case OP(sv: SymbolicValue, EQ_E(cv: ConstantValue), _) =>
                  val hct = hardcode.contains(sv)
                  if (hct && hardcode(sv) == cv.value)
                    copy(canBe = false, sureSat = false)
                  else {
                    copy(negd = negd + op,
                         canBe = !asserted.contains(op),
                         sureSat = false)
                  }
                case OP(cv: ConstantValue, EQ_E(sv: SymbolicValue), _) =>
                  val hct = hardcode.contains(sv)
                  if (hct && hardcode(sv) == cv.value)
                    copy(canBe = false, sureSat = false)
                  else {
                    val nop = OP(sv, EQ_E(cv), size)
                    copy(negd = negd + nop,
                         canBe = !asserted.contains(nop),
                         sureSat = false)
                  }
                case _ =>
                  copy(negd = negd + op,
                       canBe = !asserted.contains(op),
                       sureSat = false)
              }
          }
      }
    }

    def hlSolve(ctx: ContextPackage): Boolean = {
      val v = solve(ctx)
//      System.err.println(s"high level reached $v")
      v
    }

    def go(cd: Condition): Boolean =
      go(cd, ContextPackage(), hlSolve)

    def go(cd: Condition,
           crt: ContextPackage,
           continueWith: ContextPackage => Boolean): Boolean = {
      if (!crt.canBe)
        false
      else {
        cd match {
          case op: OP =>
            val newCd = crt & op
            if (newCd.canBe) {
              if (checkParam > 0 && crt.size % checkParam == 0) {
                val ns = filter(newCd)
                if (!ns.canBe) {
                  false
                } else {
                  continueWith(ns)
                }
              } else {
                continueWith(newCd)
              }
            } else {
              false
            }
          case FAND(conditions) =>
            if (conditions.isEmpty)
              true
            else if (conditions.size == 1)
              go(conditions.head, crt, continueWith)
            else {
              val sorted = conditions.partition {
                case o: FOR => false
                case _      => true
              }
              val full = sorted._1 ++ sorted._2
              go(full.head, crt, lst => {
                go(FAND(full.tail), lst, continueWith)
              })
            }
          case FOR(conditions) =>
            if (conditions.isEmpty)
              false
            else {
              val ns = filter(crt)
              if (ns.canBe) {
                val sorted = conditions
                if (go(sorted.head, ns, continueWith))
                  true
                else {
                  go(FOR(sorted.tail), ns, continueWith)
                }
              } else false
            }
          case FNOT(o: OP) =>
            val newCd = crt ~ o
            if (newCd.canBe) {
              if (checkParam > 0 && crt.size % checkParam == 0) {
                val ns = filter(newCd)
                if (!ns.canBe) {
                  false
                } else {
                  continueWith(ns)
                }
              } else {
                continueWith(newCd)
              }
            } else {
              false
            }
          case TRUE  => true
          case FALSE => false
          case _     => ???
        }
      }
    }

    final def apply(cd: Condition, continueWith: () => Boolean): Boolean =
      cd match {
        case OP(expression, constraint, size) =>
          val ast = translateC(translateE(size, expression), size, constraint)
          slv.assertCnstr(ast)
          continueWith()
        case FAND(conditions) =>
          if (conditions.isEmpty)
            false
          else if (conditions.size == 1)
            apply(conditions.head, continueWith)
          else {
            if (continueWith()) {
              apply(conditions.head, () => {
                apply(FAND(conditions.tail), continueWith)
              })
            } else {
              false
            }
          }
        case FOR(conditions) =>
          if (conditions.isEmpty)
            false
          else if (conditions.size == 1) {
            apply(conditions.head, continueWith)
          } else {
            slv.push()
            if (apply(conditions.head, continueWith))
              true
            else {
              slv.pop()
              if (continueWith()) {
                apply(FOR(conditions.tail), continueWith)
              } else {
                false
              }
            }
          }
        case FNOT(OP(expression, constraint, size)) =>
          val ast =
            z3.mkNot(translateC(translateE(size, expression), size, constraint))
          slv.assertCnstr(ast)
          continueWith()
        case TRUE  => true
        case FALSE => false
        case _     => ???
      }
  }

  def isSat(simpleMemory: SimpleMemory, full: Boolean): Boolean = {
    val z3Context = new Z3Context("MODEL" -> true)
    val slv = z3Context.mkSolver()
    val trans = new Translator(z3Context, slv)
    val start = System.currentTimeMillis()
    val b = trans.go(simpleMemory.pathCondition.cd)
    val total = System.currentTimeMillis() - start
    b
  }

  def getExample(simpleMemory: SimpleMemory): Option[SimpleMemory] = {
    val cd = simpleMemory.pathCondition.cd
    val z3Context = new Z3Context("MODEL" -> true)
    val slv = z3Context.mkSolver()
    val trans = new Translator(z3Context, slv)

    cd match {
      case FAND(r) =>
        r.foreach(u => {
          val ast = trans.createAST(u)
          slv.assertCnstr(ast)
        })
      case _ =>
        slv.assertCnstr(trans.createAST(cd))
    }
    val b = slv.check().get
    if (b) {
      val model = slv.getModel()
      val raws = simpleMemory.rawObjects.mapValues(r =>
        r.expression match {
          case cv: ConstantValue       => r
          case cv: ConstantStringValue => r
          case cv: ConstantBValue      => r
          case _ =>
            val evald = model.eval(trans.translateE(r.size, r.expression))
            try {
              evald
                .map(ev => {
                  val dec = java.lang.Long.decode(ev.toString.replace('#', '0'))
                  r.copy(expression = ConstantValue(dec))
                })
                .getOrElse(r)
            } catch {
              case e: Exception =>
                System.err.println(s"at ${r.expression} $e")
                r
            }
      })
      val syms = simpleMemory.symbols.mapValues(r =>
        r.expression match {
          case cv: ConstantValue       => r
          case cv: ConstantStringValue => r
          case cv: ConstantBValue      => r
          case _ =>
            val evald = model.eval(trans.translateE(r.size, r.expression))
            try {
              evald
                .map(ev => {
                  val dec = java.lang.Long.decode(ev.toString.replace('#', '0'))
                  r.copy(expression = ConstantValue(dec))
                })
                .getOrElse(r)
            } catch {
              case e: Exception =>
                System.err.println(s"at ${r.expression} $e")
                r
            }
      })
      val sm = Some(simpleMemory.copy(symbols = syms, rawObjects = raws))
      sm
    } else {
      None
    }
  }

  def isSatS(cd: Condition): Boolean = {
    val z3Context = new Z3Context("MODEL" -> true)
    val slv = z3Context.mkSolver()
    val trans = new Translator(z3Context, slv)

    cd match {
      case FAND(r) =>
        r.foreach(u => {
          val ast = trans.createAST(u)
          slv.assertCnstr(ast)
        })
      case _ =>
        slv.assertCnstr(trans.createAST(cd))
    }
    val b = slv.check().get
    z3Context.delete()
    b
  }
  def isSatS(simpleMemory: SimpleMemory): Boolean =
    isSatS(simpleMemory.pathCondition.cd)

  def isSat(simpleMemory: SimpleMemory): Boolean = {
    isSat(simpleMemory, false)
  }

  def apply(state: State): SimpleMemory = {
    new SimpleMemory(
      errorCause = state.errorCause,
      history = state.history,
      symbols = SortedMap[String, SimpleMemoryObject]() ++ state.memory.symbols
        .filter(h => {
          h._2.value.nonEmpty
        })
        .mapValues(h => {
          SimpleMemoryObject(h.value.get.e, h.size)
        }),
      rawObjects = SortedMap[Int, SimpleMemoryObject]() ++ state.memory.rawObjects
        .filter(h => {
          h._2.value.nonEmpty
        })
        .mapValues(h => {
          SimpleMemoryObject(h.value.get.e, h.size)
        }),
      memTags = SortedMap[String, Int]() ++ state.memory.memTags,
      pathCondition = SimplePathCondition.apply()
    ).addCondition(FAND.makeFAND(state.memory.pathConditions))
  }
}

class ToTheEndExecutor(val tripleExecutor: SimpleMemoryInterpreter,
                       program: Map[String, Instruction]) {
  private val q: mutable.Queue[SimpleMemory] = mutable.Queue[SimpleMemory]()

  def executeFrom(start: String,
                  simpleMemory: SimpleMemory,
                  someSolver: Option[Solver] = None): Triple[SimpleMemory] = {
    var crtResult = new Triple[SimpleMemory]()
    q.enqueue(simpleMemory.forwardTo(start))
    while (q.nonEmpty) {
      val crt = q.dequeue()
      if (program.contains(crt.location)) {
        val prog = program(crt.location)
        val trip = tripleExecutor.execute(prog, crt, true)
        val filtered = if (someSolver.nonEmpty) {
          trip.filter(SimpleMemory.isSatS)
        } else {
          trip
        }
        crtResult = crtResult + filtered.copy(success = filtered.continue,
                                              continue = Nil)
        q.enqueue(filtered.success: _*)
      } else {
        crtResult = crtResult + new Triple[SimpleMemory](success = crt :: Nil,
                                                         failed = Nil,
                                                         continue = Nil)
      }
    }
    assert(crtResult.continue.isEmpty)
    crtResult
  }

  def gcd(bh1: List[BranchC], bh2: List[BranchC]): Option[BranchC] = {
    if (bh1.isEmpty)
      None
    else {

      if (bh2.contains(bh1.head))
        Some(bh1.head)
      else {
        gcd(bh1.tail, bh2)
      }
    }
  }

  def gcd(state1: SimpleMemory, state2: SimpleMemory): Option[BranchC] = {
    val bh1 = state1.branchHistory
    val bh2 = state2.branchHistory
    gcd(bh1, bh2)
  }

}

object ToTheEndExecutor {

  case class PartitionHolder(iterable: List[(Condition, List[SimpleMemory])]) {

    @tailrec
    private def add(cd: (Condition, List[SimpleMemory]),
                    crt: List[(Condition, List[SimpleMemory])],
                    remaining: List[(Condition, List[SimpleMemory])])
    : List[(Condition, List[SimpleMemory])] = {
      if (remaining.isEmpty) {
        crt :+ cd
      } else {
        val (hd :: tail) = remaining
        val ac = FAND.makeFAND(hd._1 :: cd._1 :: Nil)
        if (SimpleMemory.isSatS(ac)) {
          val newCrt = crt :+ (ac, hd._2 ++ cd._2)
          val ncrtHd = FAND.makeFAND(FNOT.makeFNOT(cd._1) :: hd._1 :: Nil)
          val rem = if (SimpleMemory.isSatS(ncrtHd)) {
            newCrt :+ (ncrtHd, hd._2)
          } else {
            newCrt
          }
          val nhdCrt = FAND.makeFAND(FNOT.makeFNOT(hd._1) :: cd._1 :: Nil)
          if (SimpleMemory.isSatS(nhdCrt)) {
            add((nhdCrt, cd._2), rem, tail)
          } else {
            if (rem.eq(newCrt)) {
              (crt :+ (hd._1, hd._2 ++ cd._2)) ++ tail
            } else {
              rem ++ tail
            }
          }
        } else {
          add(cd, hd :: crt, tail)
        }
      }
    }

    def add(cd: (Condition, List[SimpleMemory])): PartitionHolder = {
      copy(add(cd, Nil, iterable))
    }
  }

  final def refine(candidates: Iterable[(Condition, Iterable[SimpleMemory])])
  : Iterable[(Condition, Iterable[SimpleMemory])] = {
    val partitionHolder = new PartitionHolder(Nil)
    candidates
      .foldLeft(partitionHolder)((acc, x) => {
        acc.add((x._1, x._2.toList))
      })
      .iterable
  }

  def sieve(states: List[SimpleMemory])
  : Iterable[(Condition, Iterable[SimpleMemory])] = {
    refine(states.map(r => (r.pathCondition.cd, r :: Nil)))
  }
  def noSieve(states: List[SimpleMemory])
  : Iterable[(Condition, Iterable[SimpleMemory])] = {
    states.map(r => (r.pathCondition.cd, r :: Nil))
  }
}
