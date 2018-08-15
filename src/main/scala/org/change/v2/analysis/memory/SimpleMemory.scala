package org.change.v2.analysis.memory

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.util.UUID

import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.Mapper
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.{
  ConstantBValue,
  ConstantStringValue,
  ConstantValue,
  SymbolicValue
}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.types.NumericType
import org.change.v2.interval.IntervalOps
import org.change.v2.analysis.memory.TagExp._
import z3.scala.{Z3AST, Z3Config, Z3Context, Z3Solver}

import scala.annotation.tailrec
import scala.collection.{immutable, mutable}
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.collection.mutable.ListBuffer
import scala.util.Random

case class SimpleMemoryObject(expression: Expression = ConstantValue(0),
                              size: Int = 64)

case class SimpleMemory(
    errorCause: Option[String] = None,
    history: List[String] = Nil,
    symbols: SortedMap[String, SimpleMemoryObject] = SortedMap.empty,
    rawObjects: SortedMap[Int, SimpleMemoryObject] = SortedMap.empty,
    memTags: SortedMap[String, Int] = SortedMap.empty,
    intersections: List[State] = Nil,
    differences: List[State] = Nil,
    pathCondition: SimplePathCondition = SimplePathCondition.apply(),
    myId: Long = 0,
    parentId: Long = -1) {
  def setId(l: Long): SimpleMemory = copy(parentId = myId, myId = l)
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
  def location: String = history.head
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

class SimpleMemoryInterpreter
    extends Mapper[SimpleMemory, Triple[SimpleMemory]] {
  def instantiate(fexp: FloatingExpression,
                  simpleMemory: SimpleMemory): Option[Expression] = fexp match {
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
    case _                        => ???
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
    case _         => ???
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

  override def execute(instruction: Instruction,
                       state: SimpleMemory,
                       verbose: Boolean): Triple[SimpleMemory] =
    instruction match {
      case Fork(forkBlocks) =>
        forkBlocks.zipWithIndex
          .map(p => {
            val (r, idx) = p
            if (idx != 0) {
              val myId = Random.nextLong()
              execute(r, state.setId(myId), verbose)
            } else {
              execute(r, state, verbose)
            }
          })
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
      case CreateTag(name, value) => Triple.startFrom[SimpleMemory](state)
      case DeallocateNamedSymbol(id) =>
        state
          .deallocate(id)
          .map(Triple.startFrom[SimpleMemory])
          .getOrElse(
            Triple[SimpleMemory](Nil,
                                 state.fail(s"can't deallocate $id") :: Nil,
                                 Nil))
      case If(testInstr, thenWhat, elseWhat) =>
        testInstr match {
          case InstructionBlock(is) =>
            val at =
              is.foldLeft(
                Triple.startFrom[SimpleMemory](
                  state.copy(pathCondition = SimplePathCondition.apply())))(
                (acc, hd) => {
                  val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                                   acc.continue.head,
                                   verbose)
                  val (gt, gf) = a1.success.partition(_.location == "tt")
                  val (gtt, gff) =
                    (gt.map(r => r.copy(history = r.history.tail)),
                     gf.map(r => r.copy(history = r.history.tail)))
                  acc + a1.copy(success = gff, continue = gtt)
                })
            val distinctFailures = at.failed
              .groupBy(_.error)
              .map(h => {
                SimpleMemory.mergeConditions(h._2, state).map(r => r.fail(h._1))
              })
              .collect {
                case Some(s) => s
              }
              .toList
            SimpleMemory
              .mergeConditions(at.success, state)
              .map(
                execute(elseWhat, _, verbose)
              )
              .getOrElse(new Triple[SimpleMemory]()) + SimpleMemory
              .mergeConditions(at.continue, state)
              .map(
                execute(thenWhat, _, verbose)
              )
              .getOrElse(new Triple[SimpleMemory]()) +
              new Triple[SimpleMemory](Nil, distinctFailures, Nil)
          case Fork(fb) =>
            val at =
              fb.foldLeft(
                Triple.startFrom[SimpleMemory](
                  state.copy(pathCondition = SimplePathCondition.apply())))(
                (acc, hd) => {
                  val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                                   acc.continue.head,
                                   verbose)
                  val (gf, gt) = a1.success.partition(_.location == "ff")
                  val (gff, gtt) =
                    (gf.map(r => r.copy(history = r.history.tail)),
                     gt.map(r => r.copy(history = r.history.tail)))
                  acc + a1.copy(success = gtt, continue = gff)
                })
            val distinctFailures = at.failed
              .groupBy(_.error)
              .map(h => {
                SimpleMemory.mergeConditions(h._2, state).map(r => r.fail(h._1))
              })
              .collect {
                case Some(s) => s
              }
              .toList
            SimpleMemory
              .mergeConditions(at.success, state)
              .map(
                execute(thenWhat, _, verbose)
              )
              .getOrElse(new Triple[SimpleMemory]()) +
              SimpleMemory
                .mergeConditions(at.continue, state)
                .map(
                  execute(elseWhat, _, verbose)
                )
                .getOrElse(new Triple[SimpleMemory]()) +
              new Triple[SimpleMemory](Nil, distinctFailures, Nil)
          case ConstrainNamedSymbol(id, dc, _) =>
            execute(
              If(ConstrainFloatingExpression(:@(id), dc), thenWhat, elseWhat),
              state,
              verbose)
          case ConstrainFloatingExpression(floatingExpression,
                                           floatingConstraint) =>
            instantiate(floatingExpression, floatingConstraint, state)
              .map(r => {
                tryEval(r)
                  .map(h => {
                    if (h) {
                      execute(thenWhat, state, verbose)
                    } else {
                      execute(elseWhat, state, verbose)
                    }
                  })
                  .getOrElse(
                    execute(thenWhat, state.addCondition(r), verbose) + execute(
                      elseWhat,
                      state.addCondition(FNOT.makeFNOT(r)),
                      verbose))
              })
              .getOrElse(Triple[SimpleMemory](
                Nil,
                state.fail(
                  s"cannot instantiate $floatingExpression $floatingConstraint") :: Nil,
                Nil))
          case ConstrainRaw(a, dc, _) =>
            execute(
              If(ConstrainFloatingExpression(:@(a), dc), thenWhat, elseWhat),
              state,
              verbose)
        }
      case AssignNamedSymbol(id, exp, t) =>
        instantiate(exp, state)
          .flatMap(e => {
            state.assignNewValue(id, e).map(Triple.startFrom[SimpleMemory])
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
            Triple.startFrom[SimpleMemory](state.addCondition(r))
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
              state.fail(TagExp.brokenTagExpErrorMessage) :: Nil,
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
              state.fail(TagExp.brokenTagExpErrorMessage) :: Nil,
              Nil)
        }
      case AssignRaw(a, exp, t) =>
        state.eval(a) match {
          case Some(i) =>
            instantiate(exp, state)
              .map(e => {
                state
                  .assignNewValue(i, e)
                  .map(Triple.startFrom[SimpleMemory])
                  .getOrElse(
                    Triple[SimpleMemory](
                      Nil,
                      state.fail(s"can't assign $a <- exp") :: Nil,
                      Nil))
              })
              .getOrElse(
                Triple[SimpleMemory](
                  Nil,
                  state.fail(s"can't assign $a <- $exp") :: Nil,
                  Nil))
          case None =>
            Triple[SimpleMemory](
              Nil,
              state.fail(TagExp.brokenTagExpErrorMessage) :: Nil,
              Nil)
        }
      case ConstrainRaw(a, dc, c) =>
        execute(ConstrainFloatingExpression(:@(a), dc), state, verbose)
      case ConstrainNamedSymbol(id, dc, c) =>
        execute(ConstrainFloatingExpression(:@(id), dc), state, verbose)
      case _ => ???
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
      val v = chi.groupBy(r => {
        k._3.map(f => r.symbols(f)).toList ++ k._2.map(f => r.rawObjects(f)).toList
      }).values.map(_.head)

      val uq = k._3.map(r => {
        r -> v.groupBy(_.symbols(r)).size
      }).filter(_._2 > 1).toMap
      System.err.println(s"distinct symbols $uq")

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
        .foldLeft(v.map(h => (h, h.pathCondition)))((acc, st) => {
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

  class Translator(z3: Z3Context, slv: Z3Solver) {

    def translateC(ast: Z3AST, sz: Int, constr: Constraint): Z3AST =
      constr match {
        case AND(constrs) => z3.mkAnd(constrs.map(translateC(ast, sz, _)): _*)
        case OR(constrs)  => z3.mkOr(constrs.map(translateC(ast, sz, _)): _*)
        case NOT(c)       => z3.mkNot(translateC(ast, sz, c))
        case GT_E(e)      => z3.mkBVUgt(ast, translateE(sz, e))
        case LT_E(e)      => z3.mkBVUlt(ast, translateE(sz, e))
        case LTE_E(e)     => z3.mkBVUle(ast, translateE(sz, e))
        case GTE_E(e) =>
          z3.mkOr(z3.mkBVUgt(ast, translateE(sz, e)),
                  z3.mkEq(ast, translateE(sz, e)))
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
      System.err.println(s"high level reached $v")
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
              if (crt.size % 20 == 0) {
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
                case o : FOR => false
                case _ => true
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
              if (crt.size % 20 == 0) {
                System.err.println("ordered by tick")
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
    val z3Context = new Z3Context(new Z3Config("MODEL" -> true))
    val slv = z3Context.mkSolver()
    val trans = new Translator(z3Context, slv)

    slv.push()
    val ast = trans.translateC(
      trans.translateE(16,
                       LAnd(Value(ConstantValue(0)), Value(ConstantValue(8)))),
      16,
      EQ_E(ConstantValue(0)))
    slv.assertCnstr(ast)
    assert(slv.check().get)
    slv.pop()

    System.err.println(
      s"@${simpleMemory.location}/${simpleMemory.error} all ops = " + (OP.st.size, simpleMemory.pathCondition.size, simpleMemory.pathCondition.tracker.size))
    val start = System.currentTimeMillis()
    val b = trans.go(simpleMemory.pathCondition.cd)
    val total = System.currentTimeMillis() - start

    statsPrinter.println(
      s"${simpleMemory.location},${simpleMemory.error},${simpleMemory.pathCondition.size},${simpleMemory.pathCondition.tracker.size},$total")
    statsPrinter.flush()
    System.err.println(
      s"solving done in ${System.currentTimeMillis() - start}ms")
    if (!b)
      System.err.println(
        "Hypothesis false for the moment, need to check myself other paths")
    b
  }

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
