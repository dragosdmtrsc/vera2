package org.change.v3.semantics

import org.change.v2.analysis.memory.Triple
import org.change.v3.syntax._
import z3.scala.Z3AST

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
      case ib : InstructionBlock => executeInstructionBlock(ib) _
      case f : Fork => executeFork(f) _
      case clone: Clone => executeClone(clone) _
      case iff : If => executeIf(iff) _

      case ct : CreateTag => executeCreateTag(ct) _
      case destroyTag: DestroyTag => executeDestroyTag(destroyTag) _
      case alloc : Allocate => executeAllocate(alloc) _
      case deallocate: Deallocate => executeDeallocate(deallocate) _
      case assign: Assign => executeAssign(assign) _
      case assume: Assume => executeAssume(assume) _

      case drop: Drop => executeDrop(drop) _
      case fail: Fail => executeFail(fail) _
      case forward: Forward => executeForward(forward) _
      case NoOp => executeNoOp() _
      case _ => executeExoticInstruction(instruction) _
    })(s, verbose)
  }
  def executeExoticInstruction(instruction: Instruction)(state : S, verbose : Boolean) : T

  def executeInstructionBlock(ib : InstructionBlock)(state : S, verbose : Boolean) : T
  def executeFork(fork : Fork)(state : S, verbose : Boolean) : T
  def executeClone(clone : Clone)(state : S, verbose : Boolean) : T
  def executeIf(iff : If)(state : S, verbose : Boolean) : T

  def executeCreateTag(createTag: CreateTag)(state : S, verbose : Boolean) : T
  def executeDestroyTag(destroyTag: DestroyTag)(state : S, verbose : Boolean) : T
  def executeAllocate(allocate: Allocate)(state : S, verbose : Boolean) : T
  def executeDeallocate(deallocate: Deallocate)(state : S, verbose : Boolean) : T
  def executeAssign(assign: Assign)(state : S, verbose : Boolean) : T
  def executeAssume(assume: Assume)(state : S, verbose : Boolean) : T

  def executeDrop(drop: Drop)(state : S, verbose : Boolean) : T
  def executeFail(fail: Fail)(state : S, verbose : Boolean) : T
  def executeForward(forward: Forward)(state : S, verbose : Boolean) : T
  def executeNoOp()(state : S, verbose : Boolean) : T
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

  def instantiate(tinfo : Map[BVExpr, Int])
                 (bexpr : BExpr, state : SimpleMemory) : Either[Condition, String] = {
    val tinstantiate = instantiateBVWithTypes(tinfo) _
    bexpr match {
      case BoolLiteral(v) => Left(if (v) context.mkTrue() else context.mkFalse())
      case EQ(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkEq(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case LT(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkLT(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case GT(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkGT(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case LTE(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkLE(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case GTE(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkGE(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
    }
  }

  def generateConditions(tinfo : Map[BVExpr, Int])(instr: BExpr,
                         state: SimpleMemory): (Condition, Condition, Condition) = {
    val gc = generateConditions(tinfo)(_ : BExpr, state)
    val c = instr match {
      case LAnd(a, b) =>
        val (ta, fa, ca) = gc(a)
        val (tb, fb, cb) = gc(b)
        (context.mkAnd(ta, tb), context.mkOr(fa, fb), context.mkOr(ca, context.mkAnd(ta, cb)))
      case LOr(a, b) =>
        val (ta, fa, ca) = gc(a)
        val (tb, fb, cb) = gc(b)
        (context.mkOr(ta, tb), context.mkAnd(fa, fb), context.mkOr(ca, context.mkAnd(fa, cb)))
      case LNot(a) => val (ta, fa, ca) = gc(a)
        (context.mkNot(ta), context.mkNot(fa), ca)
      case _ => instantiate(tinfo)(instr, state) match {
        case Left(x) => (x, context.mkNot(x), context.mkFalse())
        case Right(m) => {
          (context.mkFalse(), context.mkFalse(), context.mkTrue())
        }
      }
    }
    c
  }
  def generateConditions(instr : BExpr, state : SimpleMemory): Either[(Condition, Condition, Condition), String] = {
    TypeInference.solveTypes(instr, state, Nil) match {
      case Left(tinfo) => Left(generateConditions(tinfo)(instr, state))
      case Right(m) => Right(m)
    }
  }

  override def executeIf(iff: If)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = {
    generateConditions(iff.bExpr, state) match {
      case Right(m) => Triple.fail(state.fail(m))
      case Left((takea, takeb, takef)) =>
        val first = if (quickTrimStrategy(state.pathCondition, takea)) {
          execute(iff.thn, state.addCondition(takea), verbose)
        } else {
          Triple.empty[SimpleMemory]
        }
        val second = if (quickTrimStrategy(state.pathCondition, takeb)) {
          execute(iff.els, state.addCondition(takeb), verbose)
        } else {
          Triple.empty[SimpleMemory]
        }
        val third = if (context.simplifyAst(takef) != context.mkFalse() &&
          quickTrimStrategy(state.pathCondition, takef)) {
          Triple.fail(state.fail(s"segfault found in ${iff.bExpr}"))
        } else {
          Triple.empty[SimpleMemory]
        }
        first + second + third
    }
  }

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

  def instantiateBVWithTypes(tinfo : Map[BVExpr, Int])
                 (bvexp : BVExpr, state : SimpleMemory) : Either[Z3AST, String] = {
    val inst = instantiateBVWithTypes(tinfo)(_ : BVExpr, state)
    bvexp match {
      case BVAdd(a, b) => inst(a) match {
        case Left(v1) => inst(b) match {
          case Left(v2) => Left(context.mkBVAdd(v1, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case BVSub(a, b) => inst(a) match {
        case Left(v1) => inst(b) match {
          case Left(v2) => Left(context.mkBVSub(v1, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case BVShl(a, b) => inst(a) match {
        case Left(v1) => inst(b) match {
          case Left(v2) => Left(context.mkBVShl(v1, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case BVAnd(a, b) => inst(a) match {
        case Left(v1) => inst(b) match {
          case Left(v2) => Left(context.mkBVAnd(v1, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case BVOr(a, b) => inst(a) match {
        case Left(v1) => inst(b) match {
          case Left(v2) => Left(context.mkBVOr(v1, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case BVNot(b) => inst(b) match {
        case Left(v) => Left(context.mkBVNot(v))
        case Right(m) => Right(m)
      }
      case BVNeg(a) => inst(a) match {
        case Left(v) => Left(context.mkBVNeg(v))
        case Right(m) => Right(m)
      }
      case hp : Havoc => tinfo.get(hp).map(r => {
        Left(context.mkFreshConst(hp.prefix, context.mkBVSort(r)))
      }).getOrElse(Right(s"no type information found for havoc(${hp.prefix})"))
      case lit : Literal => tinfo.get(lit).map(r => {
        Left(context.mkNumeral(lit.v.toString(), context.mkBVSort(r)))
      }).getOrElse(Right(s"no type information found for literal ${lit.v}"))
      case Symbol(path) => state.symbols.get(path).map(Left(_)).getOrElse(Right(s"symbol not found $path")).left
        .map(_.ast).left
        .flatMap(x => x.map(Left(_)).getOrElse(Right(s"read before assigning $path")))
      case Reference(intable) =>
        state.eval(intable) match {
          case Some(off) => state.rawObjects.get(off).flatMap(r => {
            r.ast
          }).map(Left(_)).getOrElse(Right(s"cannot resolve $off"))
          case None => Right(TagExp.brokenTagExpErrorMessage)
        }
    }
  }

  def instantiate(bvexp : BVExpr,
                  state : SimpleMemory,
                  target : Int) : Either[Z3AST, String] = {
    val newtypes = TypeInference.solveTypes(bvexp, state, Nil)
    newtypes match {
      case Left(tinfo) => {
        tinfo.get(bvexp).map(src => {
          val cast = if (src == target) {
            x : Z3AST => x
          } else if (src < target) {
            x : Z3AST => context.mkZeroExt(target - src, x)
          } else {
            x : Z3AST => context.mkExtract(target - 1, 0, x)
          }
          instantiateBVWithTypes(tinfo)(bvexp, state).left.map(cast)
        }).getOrElse({
          TypeInference.solveTypes(bvexp, state, FixedSize(bvexp, target) :: Nil).left.flatMap(tinfo => {
            instantiateBVWithTypes(tinfo)(bvexp, state)
          })
        })
      }
      case Right(m) => Right(m)
    }
  }

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
      case Left(l) => instantiate(assign.right, state, l.size) match {
        case Left(expr) => state.assignNewValue(assign.left, expr)
          .map(Triple.startFrom)
          .getOrElse(Triple.fail(state.fail(s"can't assign ${assign.left} = ${assign.right}")))
        case Right(msg) => Triple.fail(state.fail(msg))
      }
      case Right(m) => Triple.fail(state.fail(m))
    }
  }

  override def executeAssume(assume: Assume)(state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = {
    generateConditions(assume.bExpr, state) match {
      case Right(m) => Triple.fail(state.fail(m))
      case Left((takea, takeb, takef)) =>
        val first = if (quickTrimStrategy(state.pathCondition, takea)) {
          Triple.startFrom(state.addCondition(takea))
        } else {
          Triple.empty[SimpleMemory]
        }
        val third = if (context.simplifyAst(takef) != context.mkFalse() &&
          quickTrimStrategy(state.pathCondition, takef)) {
          Triple.fail(state.fail(s"segfault found in ${assume.bExpr}"))
        } else {
          Triple.empty[SimpleMemory]
        }
        first + third
    }
  }

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