package org.change.v3.semantics

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v3.syntax._
import org.change.v2.analysis.memory.{SimpleMemory, Triple}
import z3.scala.Z3AST

import scala.collection.TraversableOnce.MonadOps

trait Mapper[S, T] {
  def execute(instruction : Instruction, state: S, verbose : Boolean) : T
}
abstract class AbsMapper[S, T] extends Mapper[S, T] {
  def addToHistory(state : S, instruction : Instruction) : S = state
  override def execute(instruction: Instruction,
                       state: S, verbose: Boolean): T = {
    val s = if (verbose &&
      !instruction.isInstanceOf[If] &&
      !instruction.isInstanceOf[Fork] &&
      !instruction.isInstanceOf[InstructionBlock])
      addToHistory(state, instruction)
    else
      state
    (instruction match {
      case ib : InstructionBlock => executeInstructionBlock(ib)
      case f : Fork => executeFork(f)
      case clone: Clone => executeClone(clone)
      case iff : If => executeIf(iff)

      case ct : CreateTag => executeCreateTag(ct)
      case destroyTag: DestroyTag => executeDestroyTag(destroyTag)
      case alloc : Allocate => executeAllocate(alloc)
      case deallocate: Deallocate => executeDeallocate(deallocate)
      case assign: Assign => executeAssign(assign)
      case assume: Assume => executeAssume(assume)

      case drop: Drop => executeDrop(drop)
      case fail: Fail => executeFail(fail)
      case forward: Forward => executeForward(forward)
      case NoOp => executeNoOp()
      case _ => executeExoticInstruction(instruction)
    })(s, verbose)
  }
  abstract def executeExoticInstruction(instruction: Instruction)(state : S, verbose : Boolean) : T

  abstract def executeInstructionBlock(ib : InstructionBlock)(state : S, verbose : Boolean) : T
  abstract def executeFork(fork : Fork)(state : S, verbose : Boolean) : T
  abstract def executeClone(clone : Clone)(state : S, verbose : Boolean) : T
  abstract def executeIf(iff : If)(state : S, verbose : Boolean) : T

  abstract def executeCreateTag(createTag: CreateTag)(state : S, verbose : Boolean) : T
  abstract def executeDestroyTag(destroyTag: DestroyTag)(state : S, verbose : Boolean) : T
  abstract def executeAllocate(allocate: Allocate)(state : S, verbose : Boolean) : T
  abstract def executeDeallocate(deallocate: Deallocate)(state : S, verbose : Boolean) : T
  abstract def executeAssign(assign: Assign)(state : S, verbose : Boolean) : T
  abstract def executeAssume(assume: Assume)(state : S, verbose : Boolean) : T

  abstract def executeDrop(drop: Drop)(state : S, verbose : Boolean) : T
  abstract def executeFail(fail: Fail)(state : S, verbose : Boolean) : T
  abstract def executeForward(forward: Forward)(state : S, verbose : Boolean) : T
  abstract def executeNoOp()(state : S, verbose : Boolean) : T
}

class IntraExecutor(
                     quickTrimStrategy: (SimplePathCondition, Condition) => Boolean = (_, _) => true) extends
  AbsMapper[SimpleMemory, Triple[SimpleMemory]] {
  override def executeExoticInstruction(instruction: Instruction)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = ???

  override def executeInstructionBlock(ib: InstructionBlock)
                                      (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = ib.is.
    foldLeft(Triple.startFrom[SimpleMemory](state))(
    (acc, i) => {
      acc.continue.foldLeft(acc.finals())((res, crt) => {
        res + execute(i, crt, verbose)
      })
    })

  override def executeFork(fork: Fork)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = fork.is
    .map(execute(_, state, verbose))
    .foldLeft(new Triple[SimpleMemory]())((acc, r) => {
      acc + r
    })

  override def executeClone(clone: Clone)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = clone.is
    .map(execute(_, state, verbose))
    .foldLeft(new Triple[SimpleMemory]())((acc, r) => {
      acc + r
    })

  override def executeIf(iff: If)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = ???

  override def executeCreateTag(createTag: CreateTag)
                             (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = state
    .eval(createTag.right)
    .map(v => {
      Triple.startFrom[SimpleMemory](state.createTag(createTag.name, v))
    }).getOrElse(Triple.fail(state.fail(TagExp.brokenTagExpErrorMessage)))

  override def executeDestroyTag(destroyTag: DestroyTag)
                                (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] =
    state
      .destroyTag(destroyTag.name)
      .map(Triple.startFrom)
      .getOrElse(Triple.fail(state.fail(TagExp.brokenTagExpErrorMessage)))

  override def executeAllocate(allocate: Allocate)
                              (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = allocate.left match {
    case Reference(intable) =>
      state.eval(intable) match {
        case None => Triple.fail(state.fail(TagExp.brokenTagExpErrorMessage))
        case Some(x) => state
          .Allocate(x, allocate.sz)
          .map(Triple.startFrom)
          .getOrElse(Triple.fail(state.fail(s"cannot allocate at $x")))
      }
    case Symbol(id) => Triple.startFrom(state.Allocate(id, allocate.sz))
  }

  override def executeDeallocate(deallocate: Deallocate)
                                (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = deallocate.left match {
    case Symbol(id) => state
      .deallocate(id)
      .map(Triple.startFrom[SimpleMemory])
      .getOrElse(Triple.fail(state.fail(s"cannot deallocate because symbol $id not found")))
    case Reference(a) => state.eval(a) match {
      case None => Triple.fail(state.fail(TagExp.brokenTagExpErrorMessage))
      case Some(loc) => state.deallocate(loc).map(Triple.startFrom[SimpleMemory])
        .getOrElse(Triple.fail(state.fail(s"cannot deallocate because ref $loc not found")))
    }
  }

  def instantiate(bvexp : BVExpr,
                  state : SimpleMemory,
                  constraints : List[TypeConstraint]) : Either[Z3AST, String] = ???

  def getMemoryRef(lvExpr: LVExpr, mem : SimpleMemory): Either[SimpleMemoryObject, String] = lvExpr match {
    case Symbol(id) => mem.symbols.get(id).map(obj => Left(obj)).getOrElse(Right(s"$id is not in memory"))
    case Reference(a) => mem.eval(a) match {
      case None => Right(TagExp.brokenTagExpErrorMessage)
      case Some(v) => mem.rawObjects.get(v).map(obj => Left(obj)).getOrElse(Right(s"$v is not in memory"))
    }
  }

  override def executeAssign(assign: Assign)
                            (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = {
    val r = getMemoryRef(assign.left, state)
    r match {
      case Left(l) => instantiate(assign.right,
        state,
        List(FixedSize(assign.right, l.size))) match {
        case Left(expr) => state.assignNewValue(assign.left, expr)
          .map(Triple.startFrom)
          .getOrElse(Triple.fail(state.fail(s"can't assign ${assign.left} = ${assign.right}")))
        case Right(msg) => Triple.fail(state.fail(msg))
      }
      case Right(m) => Triple.fail(state.fail(m))
    }
  }

  override def executeAssume(assume: Assume)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = ???

  override def executeDrop(drop: Drop)
                          (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] =
    Triple.fail(state.fail("dropped:" + drop.message))

  override def executeFail(fail: Fail)
                          (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] =
    Triple.fail(state.fail(fail.message))

  override def executeForward(forward: Forward)
                             (state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] =
    Triple.success(state.forwardTo(forward.to))

  override def executeNoOp()(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] =
    Triple.startFrom[SimpleMemory](state)
}