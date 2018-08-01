package org.change.v2.analysis.memory

import java.io.{FileOutputStream, PrintStream}
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
  def addCondition(condition: Condition): SimpleMemory = condition match {
    case FAND(Nil)   => this
    case FOR(Nil)    => copy(pathConditions = FALSE :: Nil)
    case FNOT(TRUE)  => copy(pathConditions = FALSE :: Nil)
    case FNOT(FALSE) => this
    case TRUE        => this
    case FALSE       => copy(pathConditions = FALSE :: Nil)
    case _           => copy(pathConditions = condition :: pathConditions)
  }

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
                  state.copy(pathConditions = Nil)))((acc, hd) => {
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

  def mergeConditions(states: Iterable[SimpleMemory],
                      base: SimpleMemory): Option[SimpleMemory] = {
    if (states.isEmpty)
      None
    else {
      if (states.size == 1) {
        Some(
          states.head.pathConditions.foldLeft(base)((acc, c) => {
            acc.addCondition(c)
          })
        )
      } else {
        Some(
          base.addCondition(
            FOR.makeFOR(
              states
                .map(r => {
                  FAND.makeFAND(r.pathConditions)
                })
                .toList)))
      }
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
      ).addCondition(FOR.makeFOR(withRaws.map(h => FAND.makeFAND(h._2)).toList))
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

  class Translator(z3: Z3Context, slv : Z3Solver) {

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
        case Lor(a, b)   => z3.mkBVAnd(translateE(sz, a.e), translateE(sz, b.e))
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

    def apply(cd: Condition): Boolean = cd match {
      case OP(expression, constraint, size) =>
        val ast = translateC(translateE(size, expression), size, constraint)
        slv.assertCnstr(ast)
        slv.check().get
      case FAND(conditions) =>
        if (conditions.isEmpty)
          true
        else
          conditions.forall(apply)
      case FOR(conditions) =>
        if (conditions.isEmpty)
          false
        else
          conditions.exists(h => {
            slv.push()
            val b = apply(h)
            if (!b)
             slv.pop()
            b
          })
      case FNOT(OP(expression, constraint, size)) =>
        val ast = z3.mkNot(translateC(translateE(size, expression), size, constraint))
        slv.assertCnstr(ast)
        slv.check().get
      case TRUE            => true
      case FALSE           => false
      case _               => ???
    }
  }

  def isSat(simpleMemory: SimpleMemory, full: Boolean): Boolean = {
    val visitStart = System.currentTimeMillis()
    val startBuildup = System.currentTimeMillis()
    val z3Context = new Z3Context(new Z3Config("MODEL" -> true))
    val slv = z3Context.mkSolver()
    val trans = new Translator(z3Context, slv)
    val endBuildup = System.currentTimeMillis()
    System.out.println(s"build-up done in ${endBuildup - startBuildup}ms")
    val b = simpleMemory.pathConditions.forall(trans.apply)
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
