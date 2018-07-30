package org.change.v2.analysis.memory

import java.io.{FileOutputStream, PrintStream}
import java.util.UUID

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
import z3.scala.{Z3AST, Z3Config, Z3Context}

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.immutable.{SortedMap, SortedSet}
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
    pathConditions: List[Condition] = Nil,
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
  def error: String = errorCause.get
  def addCondition(condition: Condition): SimpleMemory =
    copy(pathConditions = condition :: pathConditions)

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
    case OP(ConstantValue(x, _, _), ct, _) =>
      ct match {
        case LT_E(ConstantValue(y, _, _))  => Some(x < y)
        case LTE_E(ConstantValue(y, _, _)) => Some(x <= y)
        case GTE_E(ConstantValue(y, _, _)) => Some(x >= y)
        case GT_E(ConstantValue(y, _, _))  => Some(x > y)
        case EQ_E(ConstantValue(y, _, _))  => Some(x == y)
        case No                            => Some(false)
        case Truth()                       => Some(true)
        case _                             => None
      }
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
                  state.copy(pathConditions = Nil)))((acc, hd) => {
                val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                                 acc.continue.head,
                                 verbose)
                val (gt, gf) = a1.success.partition(_.location == "tt")
                val (gtt, gff) = (gt.map(r => r.copy(history = r.history.tail)),
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
              .getOrElse(new Triple[SimpleMemory]()) + at.continue.headOption
              .map(
                execute(thenWhat, _, verbose)
              )
              .getOrElse(new Triple[SimpleMemory]()) +
              new Triple[SimpleMemory](Nil, distinctFailures, Nil)
          case Fork(fb) =>
            val at =
              fb.foldLeft(Triple.startFrom[SimpleMemory](state))((acc, hd) => {
                val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                                 acc.continue.head,
                                 verbose)
                val (gf, gt) = a1.success.partition(_.location == "ff")
                val (gff, gtt) = (gf.map(r => r.copy(history = r.history.tail)),
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
              .getOrElse(new Triple[SimpleMemory]()) + at.continue.headOption
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
                      state.addCondition(FNOT(r)),
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

  def mergeConditions(states: Iterable[SimpleMemory],
                      base: SimpleMemory): Option[SimpleMemory] = {
    if (states.isEmpty)
      None
    else {
      Some(
        base.addCondition(
          FOR(
            states
              .map(r => {
                FAND(r.pathConditions)
              })
              .toList)))
    }
  }
  type NaturalKey = (SortedMap[String, Int], SortedSet[Int], SortedSet[String])

  def naturalGroup(h: SimpleMemory): NaturalKey =
    (h.memTags, h.rawObjects.keySet, h.symbols.keySet)
  def naturalMerge(k: NaturalKey, v: Iterable[SimpleMemory]): SimpleMemory = {
    assert(v.nonEmpty)
    if (v.size == 1)
      v.head
    else {
      val hd = v.head
      val raws = k._2.map(r => {
        val tentative = hd.rawObjects(r)
        if (v.tail.forall(p => p.rawObjects(r).equals(tentative))) {
          r -> Some(tentative)
        } else {
          r -> None
        }
      })
      val syms = k._3.map(r => {
        val tentative = hd.symbols(r)
        if (v.tail.forall(p => p.symbols(r).equals(tentative))) {
          r -> Some(tentative)
        } else {
          r -> None
        }
      })
      val id = UUID.randomUUID().toString
      val withSyms = syms
        .collect {
          case (sym, None) => sym
        }
        .foldLeft(v.map(h => (h, h.pathConditions)))((acc, st) => {
          val sbname = SymbolicValue(s"meta$st$id")
          acc.map(
            r =>
              r._1 -> (OP(r._1.symbols(st).expression,
                          EQ_E(sbname),
                          r._1.symbols(st).size) :: r._2))
        })

      val withRaws = raws
        .collect {
          case (raw, None) => raw
        }
        .foldLeft(withSyms)((acc, raw) => {
          val sbname = SymbolicValue(s"header$raw$id")
          acc.map(
            r =>
              r._1 -> (OP(r._1.rawObjects(raw).expression,
                          EQ_E(sbname),
                          r._1.rawObjects(raw).size) :: r._2))
        })

      val sm = SimpleMemory(
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
      )
      val byPid = v.groupBy(x => (x.parentId, x.myId))
      if (byPid.size == 1) {
        val cd = FOR(withRaws.map(h => FAND(h._2)).toList)
        sm.copy(myId = byPid.head._1._1, parentId = byPid.head._1._2)
          .addCondition(cd)
      } else {
        val cd = FOR(
          withRaws
            .map(
              h =>
                FAND(
                  if (h._1.parentId == -5 && h._1.myId == -5)
                    h._2
                  else
                    OP(SymbolicValue("my_id"),
                       EQ_E(ConstantValue(h._1.myId)),
                       64) ::
                      OP(SymbolicValue("parent_id"),
                         EQ_E(ConstantValue(h._1.parentId)),
                         64) ::
                      h._2
              ))
            .toList)
        sm.copy(myId = -5, parentId = -5).addCondition(cd)
      }
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

  def gatherSymbols(expression: Expression,
                    sz: Int,
                    crt: Map[String, Int]): Map[String, Int] =
    expression match {
      case LShift(a, b) => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case sym: SymbolicValue =>
        if (!crt.contains(sym.canonicalName()) || crt(sym.canonicalName()) == sz)
          crt + (sym.canonicalName() -> sz)
        else
          crt + (sym.canonicalName() -> Math.max(sz, crt(sym.canonicalName())))
      case Plus(a, b)   => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case LNot(a)      => gatherSymbols(a.e, sz, crt)
      case LAnd(a, b)   => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case Lor(a, b)    => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case LXor(a, b)   => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case Minus(a, b)  => gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case PlusE(a, b)  => gatherSymbols(b, sz, gatherSymbols(a, sz, crt))
      case MinusE(a, b) => gatherSymbols(b, sz, gatherSymbols(a, sz, crt))
      case LogicalOr(a, b) =>
        gatherSymbols(b.e, sz, gatherSymbols(a.e, sz, crt))
      case _ => crt
    }

  @tailrec
  def gatherSymbols(constraints: List[Constraint],
                    sz: Int,
                    crt: Map[String, Int]): Map[String, Int] = {
    if (constraints.isEmpty)
      crt
    else {
      val constraint = constraints.head
      constraint match {
        case LT_E(e)  => gatherSymbols(e, sz, crt)
        case LTE_E(e) => gatherSymbols(e, sz, crt)
        case GTE_E(e) => gatherSymbols(e, sz, crt)
        case GT_E(e)  => gatherSymbols(e, sz, crt)
        case EQ_E(e)  => gatherSymbols(e, sz, crt)
        case OR(cs)   => gatherSymbols(constraints.tail ++ cs, sz, crt)
        case AND(cs)  => gatherSymbols(constraints.tail ++ cs, sz, crt)
        case NOT(c)   => gatherSymbols(c :: constraints.tail, sz, crt)
        case _        => crt
      }
    }
  }

  def gatherSymbols(constraint: Constraint,
                    sz: Int,
                    crt: Map[String, Int]): Map[String, Int] =
    gatherSymbols(constraint :: Nil, sz, crt)
  @tailrec
  def gatherSymbols(conditions: List[Condition],
                    crt: Map[String, Int]): Map[String, Int] = {
    if (conditions.isEmpty)
      crt
    else {
      conditions.head match {
        case OP(expression, constraint, size) =>
          gatherSymbols(constraint, size, gatherSymbols(expression, size, crt))
        case FAND(cs) => gatherSymbols(conditions.tail ++ cs, crt)
        case FOR(cs)  => gatherSymbols(conditions.tail ++ cs, crt)
        case FNOT(c)  => gatherSymbols(c :: conditions.tail, crt)
        case _        => crt
      }
    }
  }

  def gatherSymbols(condition: Condition,
                    crt: Map[String, Int]): Map[String, Int] =
    gatherSymbols(condition :: Nil, crt)

  def translateC(context: Z3Context,
                 ast: Z3AST,
                 table: Map[String, Z3AST],
                 sz: Int)(constr: Constraint): Z3AST = {
    val fun = translateC(context, ast, table, sz) _
    val fune = translateE(context, table, sz) _
    constr match {
      case AND(constrs) => context.mkAnd(constrs.map(fun): _*)
      case OR(constrs)  => context.mkOr(constrs.map(fun): _*)
      case NOT(c)       => context.mkNot(fun(c))
      case GT_E(e)      => context.mkBVUgt(ast, fune(e))
      case LT_E(e)      => context.mkBVUlt(ast, fune(e))
      case LTE_E(e)     => context.mkBVUle(ast, fune(e))
      case GTE_E(e) =>
        context.mkOr(context.mkBVUgt(ast, fune(e)), context.mkEq(ast, fune(e)))
      case EQ_E(e) =>
        context.mkEq(ast, fune(e))
    }
  }

  def translateE(z3: Z3Context, table: Map[String, Z3AST], sz: Int)(
      expression: Expression): Z3AST = {
    val fun = translateE(z3, table, sz) _
    expression match {
      case LShift(a, b) => z3.mkBVLshr(fun(a.e), fun(b.e))
      case sv: SymbolicValue =>
        table.getOrElse(sv.canonicalName(),
                        z3.mkConst(sv.canonicalName(), z3.mkBVSort(sz)))
      case Plus(a, b) => z3.mkBVAdd(fun(a.e), fun(b.e))
      case LNot(a)    => z3.mkNot(fun(a.e))
      case LAnd(a, b) =>
        z3.mkBVAnd(fun(a.e), fun(b.e))
      case Lor(a, b)   => z3.mkBVAnd(fun(a.e), fun(b.e))
      case LXor(a, b)  => z3.mkBVXor(fun(a.e), fun(b.e))
      case Minus(a, b) => z3.mkBVSub(fun(a.e), fun(b.e))
      case ConstantValue(value, isIp, isMac) =>
        z3.mkNumeral(value.toString, z3.mkBVSort(sz))
      case PlusE(a, b)     => z3.mkAdd(fun(a), fun(b))
      case MinusE(a, b)    => z3.mkSub(fun(a), fun(b))
      case LogicalOr(a, b) => z3.mkBVOr(fun(a.e), fun(b.e))
      case ConstantBValue(v, size) =>
        z3.mkNumeral(BigInt(v.substring(2), 16).toString, z3.mkBVSort(size))
      case ConstantStringValue(v) =>
        z3.mkNumeral(v.hashCode.toString, z3.mkBVSort(sz))
      case _ => ???
    }
  }

  def translateCd(z3: Z3Context, table: Map[String, Z3AST])(
      cd: Condition): Z3AST = cd match {
    case OP(expression, constraint, size) =>
      translateC(z3, translateE(z3, table, size)(expression), table, size)(
        constraint)
    case FAND(conditions) =>
      if (conditions.isEmpty)
        z3.mkTrue()
      else if (conditions.size == 1)
        translateCd(z3, table)(conditions.head)
      else
        conditions.head match {
          case fand: FAND =>
            translateCd(z3, table)(FAND(fand.conditions ++ conditions.tail))
          case _ => z3.mkAnd(conditions.map(translateCd(z3, table)): _*)
        }
    case FOR(conditions) =>
      if (conditions.isEmpty)
        z3.mkFalse()
      else if (conditions.size == 1)
        translateCd(z3, table)(conditions.head)
      else
        conditions.head match {
          case value: FOR =>
            translateCd(z3, table)(FOR(value.conditions ++ conditions.tail))
          case _ =>
            val rnd = Random.nextInt(conditions.size)
            translateCd(z3, table)(conditions(rnd))
        }
    case FNOT(condition) => z3.mkNot(translateCd(z3, table)(condition))
    case TRUE()          => z3.mkTrue()
    case FALSE()         => z3.mkFalse()
    case _               => ???
  }

  def isSat(simpleMemory: SimpleMemory, full: Boolean): Boolean = {
    val startBuildup = System.currentTimeMillis()
    val syms = Map.empty[String, Int]
    val z3Context = new Z3Context(new Z3Config("MODEL" -> true))
    val s2ast = syms.map(h => {
      h._1 -> z3Context.mkConst(h._1, z3Context.mkBVSort(h._2))
    })
    val slv = z3Context.mkSolver()
    simpleMemory.pathConditions
      .foreach(pc => slv.assertCnstr(translateCd(z3Context, s2ast)(pc)))
    val endBuildup = System.currentTimeMillis()
    System.out.println(s"build-up done in ${endBuildup - startBuildup}ms")
    val b = slv.check().get
    System.out.println(
      s"solving done in ${System.currentTimeMillis() - endBuildup}ms")
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
      pathConditions = state.memory.pathConditions
    )
  }
}
