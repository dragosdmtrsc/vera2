package org.change.v2.analysis.memory

import org.change.v2.analysis.executor.Mapper
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{
  ConstantBValue,
  ConstantStringValue,
  ConstantValue,
  SymbolicValue
}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import scodec.bits.BitVector

case class AbsState(allocated: BitVector,
                    valid: BitVector,
                    dontCares: Set[String] = Set.empty,
                    mapping: Map[String, Int] = Map.empty,
                    valMapping: Map[String, Int] = Map.empty,
                    history: List[String] = Nil,
                    errorMessage: Option[String] = None) {
  def location: String = history.head
  def fail(why: String): AbsState = copy(errorMessage = Some(why))
  def forward(to: String): AbsState = copy(history = to :: history)
  def allocate(s: String): AbsState =
    if (!dontCares.contains(s))
      copy(allocated = allocated.set(mapping(s)))
    else
      this
  def deallocate(s: String): AbsState =
    copy(allocated = allocated.clear(mapping(s)))
  def invalidate(s: String): AbsState =
    copy(valid = valid.clear(valMapping(s)))
  def validate(s: String): AbsState =
    if (valMapping.contains(s))
      copy(valid = valid.set(valMapping(s)))
    else
      this
  def valid(s: String): Boolean = valid(valMapping(s))
  def allocd(s: String): Boolean =
    (mapping.contains(s) && allocated(mapping(s))) || dontCares.contains(s)

  def canEval(floatingExpression: FloatingExpression): Boolean =
    floatingExpression match {
      case SymbolicValue(name)               => true
      case ConstantValue(value, isIp, isMac) => true
      case :<<:(left, right)                 => canEval(left) && canEval(right)
      case :!:(left)                         => canEval(left)
      case Symbol(id)                        => allocd(id)
      case :||:(left, right)                 => canEval(left) && canEval(right)
      case :^:(left, right)                  => canEval(left) && canEval(right)
      case :&&:(left, right)                 => canEval(left) && canEval(right)
      case :+:(left, right)                  => canEval(left) && canEval(right)
      case Address(a)                        => true
      case :-:(left, right)                  => canEval(left) && canEval(right)
      case ConstantBValue(value, size)       => true
      case ConstantStringValue(value)        => true
    }

  def eval(fe: FloatingExpression): Option[Long] = fe match {
    case SymbolicValue(name)               => None
    case ConstantValue(value, isIp, isMac) => Some(value)
    case :<<:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v << v2))
    case :!:(left) => eval(left).map(v => ~v)
    case Symbol(id) =>
      if (valMapping.contains(id))
        Some(if (valid(valMapping(id))) 0 else 1)
      else
        None
    case :||:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v | v2))
    case :^:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v ^ v2))
    case :&&:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v & v2))
    case :+:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v + v2))
    case Address(a) => None
    case :-:(left, right) =>
      eval(left).flatMap(v => eval(right).map(v2 => v - v2))
    case ConstantBValue(value, size) => None
    case ConstantStringValue(value)  => None
  }

  def eval(id: String, fc: FloatingConstraint): Option[Boolean] = {
    if (!valMapping.contains(id))
      None
    else {
      val e = if (valid(valMapping(id))) 0 else 1
      eval(e, fc)
    }
  }

  private def eval(e: Long, fc: FloatingConstraint): Option[Boolean] = {
    fc match {
      case :&:(a, b) => eval(e, a).flatMap(v => eval(e, b).map(v2 => v && v2))
      case Yes()     => Some(true)
      case :~:(c)    => eval(e, c).map(!_)
      case :==:(exp) => eval(exp).map(_ == e)
      case :<:(exp)  => eval(exp).map(_ < e)
      case :<=:(exp) => eval(exp).map(_ <= e)
      case :>:(exp)  => eval(exp).map(_ > e)
      case :>=:(exp) => eval(exp).map(_ >= e)
    }
  }

  def eval(fe: FloatingExpression, fc: FloatingConstraint): Option[Boolean] = {
    eval(fe).flatMap(r => {
      eval(r, fc)
    })
  }

  def canEval(id: String, fc: FloatingConstraint): Boolean =
    allocated(mapping(id)) && canEval(fc)
  def canEval(fc: FloatingConstraint): Boolean = {
    fc match {
      case :|:(a, b) => canEval(a) && canEval(b)
      case :&:(a, b) => canEval(a) && canEval(b)
      case Yes()     => true
      case :~:(c)    => canEval(c)
      case :==:(exp) => canEval(exp)
      case :<:(exp)  => canEval(exp)
      case :<=:(exp) => canEval(exp)
      case :>:(exp)  => canEval(exp)
      case :>=:(exp) => canEval(exp)
      case _         => ???
    }
  }

}

object AbsState {
  def fromState(state: State,
                rw: Set[String],
                rnw: Set[String],
                wnr: Set[String]): AbsState = {
    val allocd = state.memory.symbols.keySet
    val valids = allocd
      .filter(s => s.endsWith("IsValid"))
      .map(
        r =>
          r -> (state.memory
            .symbols(r)
            .expression
            .asInstanceOf[ConstantValue]
            .value == 1))
      .toMap
    val unsafe = rnw.diff(allocd)
    if (unsafe.nonEmpty)
      System.err.println(
        s"There are ${unsafe.size} unallocated vars read-only but initially not allocated ${unsafe
          .mkString(",")}")
    val allSymbols = (rw ++ unsafe).toList
    val bvAlloc = BitVector.bits(allSymbols.map(allocd.contains))
    val bvValid = BitVector.bits(
      allSymbols
        .filter(x => x.endsWith("IsValid"))
        .map(r => valids.contains(r) && valids(r)))
    new AbsState(
      allocated = bvAlloc,
      valid = bvValid,
      dontCares = wnr,
      mapping = allSymbols.zipWithIndex.toMap,
      valMapping = allSymbols
        .filter(x => x.endsWith("IsValid"))
        .zipWithIndex
        .toMap,
      history = state.history
    )
  }
}

case class AbsTriple(success: List[AbsState],
                     failed: List[AbsState],
                     continue: List[AbsState]) {
  def this() = this(Nil, Nil, Nil)
  def +(n: AbsTriple): AbsTriple =
    copy(success ++ n.success,
         failed = failed ++ n.failed,
         continue = continue ++ n.continue)
  def finals() = AbsTriple(success, failed, Nil)
}

object AbsTriple {
  def startFrom(a: AbsState) =
    AbsTriple(success = Nil, failed = Nil, continue = a :: Nil)
}

case class Triple[T](success: List[T], failed: List[T], continue: List[T]) {
  def this() = this(Nil, Nil, Nil)
  def +(n: Triple[T]): Triple[T] =
    new Triple[T](success ++ n.success,
                  failed = failed ++ n.failed,
                  continue = continue ++ n.continue)
  def finals() = new Triple[T](success, failed, Nil)
}

object Triple {
  def startFrom[T](a: T) =
    new Triple[T](success = Nil, failed = Nil, continue = a :: Nil)
}

class AbsInterpreter extends Mapper[AbsState, AbsTriple] {
  override def execute(instruction: Instruction,
                       state: AbsState,
                       verbose: Boolean): AbsTriple = instruction match {
    case Fork(forkBlocks) =>
      forkBlocks
        .map(r => {
          execute(r, state, verbose)
        })
        .foldLeft(new AbsTriple())((acc, r) => {
          acc + r
        })

    case InstructionBlock(instructions) =>
      instructions.foldLeft(AbsTriple.startFrom(state))((acc, i) => {
        acc.continue.foldLeft(acc.finals())((res, crt) => {
          res + execute(i, crt, verbose)
        })
      })
    case Fail(errMsg)             => AbsTriple(Nil, state.fail(errMsg) :: Nil, Nil)
    case NoOp                     => AbsTriple.startFrom(state)
    case Forward(place)           => AbsTriple(state.forward(place) :: Nil, Nil, Nil)
    case ExistsRaw(a)             => AbsTriple.startFrom(state)
    case AllocateSymbol(id, size) => AbsTriple.startFrom(state.allocate(id))
    case CreateTag(name, value)   => AbsTriple.startFrom(state)
    case DeallocateNamedSymbol(id) =>
      if (state.allocd(id))
        AbsTriple.startFrom(state.deallocate(id))
      else
        AbsTriple(Nil, state.fail(s"can't deallocate $id") :: Nil, Nil)
    case If(testInstr, thenWhat, elseWhat) =>
      testInstr match {
        case InstructionBlock(is) =>
          val at = is.foldLeft(AbsTriple.startFrom(state))((acc, hd) => {
            val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                             acc.continue.head,
                             verbose)
            val (gtt, gff) = a1.success
              .map(r => r.copy(history = r.history.tail))
              .partition(_.location == "tt")
            acc + a1.copy(success = gff, continue = gtt)
          })
          (at.success.headOption
            .map(h => execute(elseWhat, h, verbose))
            .getOrElse(new AbsTriple()) + execute(thenWhat,
                                                  at.continue.head,
                                                  verbose))
            .copy(failed = at.failed)
        case Fork(fb) =>
          val at = fb.foldLeft(AbsTriple.startFrom(state))((acc, hd) => {
            val a1 = execute(If(hd, Forward("tt"), Forward("ff")),
                             acc.continue.head,
                             verbose)
            val (gff, gtt) = a1.success
              .map(r => r.copy(history = r.history.tail))
              .partition(_.location == "ff")
            acc + a1.copy(success = gtt, continue = gff)
          })
          (execute(thenWhat,
                   at.success.head.copy(history = at.success.head.history.tail),
                   verbose) + execute(elseWhat, at.continue.head, verbose))
            .copy(failed = at.failed)
        case ConstrainNamedSymbol(id, dc, _) =>
          if (state.canEval(id, dc)) {
            state
              .eval(id, dc)
              .map(h => {
                if (h)
                  execute(thenWhat, state, verbose)
                else
                  execute(elseWhat, state, verbose)
              })
              .getOrElse(
                execute(thenWhat, state, verbose) + execute(elseWhat,
                                                            state,
                                                            verbose))
          } else {
            AbsTriple(failed = state.fail(s"cannot evaluate $testInstr") :: Nil,
                      success = Nil,
                      continue = Nil)
          }
        case ConstrainFloatingExpression(floatingExpression,
                                         floatingConstraint) =>
          if (state.canEval(floatingConstraint) && state.canEval(
                floatingExpression)) {
            state
              .eval(floatingExpression, floatingConstraint)
              .map(h => {
                if (h)
                  execute(thenWhat, state, verbose)
                else
                  execute(elseWhat, state, verbose)
              })
              .getOrElse(
                execute(thenWhat, state, verbose) + execute(elseWhat,
                                                            state,
                                                            verbose))
          } else {
            AbsTriple(failed = state.fail(s"cannot evaluate $testInstr") :: Nil,
                      success = Nil,
                      continue = Nil)
          }
        case ConstrainRaw(a, dc, _) =>
          execute(thenWhat, state, verbose) + execute(elseWhat, state, verbose)
      }
    case ConstrainNamedSymbol(id, dc, c) =>
      if (!state.canEval(id, dc))
        AbsTriple(failed = state.fail(s"cannot evaluate $instruction") :: Nil,
                  success = Nil,
                  continue = Nil)
      else
        state
          .eval(id, dc)
          .flatMap(r => if (r) Some(AbsTriple.startFrom(state)) else None)
          .getOrElse(AbsTriple.startFrom(state))
    case AssignNamedSymbol(id, exp, t) =>
      val newstate = state.allocate(id)
      AbsTriple.startFrom(
        if (id.endsWith("IsValid"))
          exp match {
            case ConstantValue(v, _, _) if v == 1 => newstate.validate(id)
            case ConstantValue(v, _, _) if v == 0 => newstate.invalidate(id)
            case _                                => newstate
          } else
          newstate
      )
    case ExistsNamedSymbol(symbol) =>
      if (state.valid(symbol))
        AbsTriple.startFrom(state)
      else
        AbsTriple(success = Nil,
                  failed = state.fail(s"$symbol doesn't exist") :: Nil,
                  Nil)
    case ConstrainFloatingExpression(floatingExpression, dc) =>
      if (state.canEval(floatingExpression) && state.canEval(dc))
        state
          .eval(floatingExpression, dc)
          .map(h => {
            if (h) AbsTriple.startFrom(state) else new AbsTriple()
          })
          .getOrElse(AbsTriple.startFrom(state))
      else
        AbsTriple(failed = state.fail(s"cannot evaluate $instruction") :: Nil,
                  success = Nil,
                  continue = Nil)
    case DeallocateRaw(a, size) => AbsTriple.startFrom(state)
    case AllocateRaw(a, size)   => AbsTriple.startFrom(state)
    case AssignRaw(a, exp, t)   => AbsTriple.startFrom(state)
    case ConstrainRaw(a, dc, c) => AbsTriple.startFrom(state)
    case AssignNamedSymbolWithLength(id, exp, width) =>
      AbsTriple.startFrom(state)
    case NotExistsRaw(a)              => AbsTriple.startFrom(state)
    case NotExistsNamedSymbol(symbol) => AbsTriple.startFrom(state)
    case _                            => ???
  }
}

object TrivialAbsInterpreter extends AbsInterpreter