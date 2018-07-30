package org.change.v2.analysis.memory

import java.util.UUID

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.Mapper
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.types.NumericType
import org.change.v2.interval.IntervalOps
import org.change.v2.analysis.memory.TagExp._
import z3.scala.{Z3Config, Z3Context}

import scala.collection.immutable
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.util.Random

case class SimpleMemoryObject(expression: Expression = ConstantValue(0),
                              size: Int = 64)

case class SimpleMemory(errorCause: Option[String] = None,
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
  def instantiate(fexp: FloatingExpression,
                  fc: FloatingConstraint,
                  simpleMemory: SimpleMemory): Option[Condition] =
    instantiate(fexp, simpleMemory).flatMap(exp => {
      instantiate(fc, simpleMemory).map(c => {
        OP(exp, c, 64)
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
