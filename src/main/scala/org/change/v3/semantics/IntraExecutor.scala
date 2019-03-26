package org.change.v3.semantics

import java.util.logging.Logger

import org.change.v2.analysis.memory.Triple
import org.change.v3.syntax._
import z3.scala.{Z3AST, Z3Solver}

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

object BranchingEstimator {
  def estimate(instruction : Instruction, start : Long = 1) : (Long, Long) = instruction match {
    case NoOp => (start, 0)
    case Forward(to) => (0, start)
    case Fail(message) => (0, start)
    case Drop(message) => (0, start)
    case Assign(left, right) => (start, 0)
    case Allocate(left, sz) => (start, 0)
    case Deallocate(left) => (start, 0)
    case CreateTag(name, right) => (start, 0)
    case DestroyTag(name) => (start, 0)
    case Assume(bExpr) => (start, 0)
    case If(bExpr, thn, els) => val estimate1 = estimate(thn, start)
      val estimate2 = estimate(els, start)
      (estimate1._1 + estimate2._1, estimate1._2 + estimate2._2)
    case InstructionBlock(is) =>
      is.foldLeft((start, 0l))((acc, x) => {
        val est = estimate(x, acc._1)
        (est._1, est._2 + acc._2)
      })
    case Clone(is) =>
      is.map(instr => estimate(instr, start)).foldLeft((0l, 0l))((acc, x) => (acc._1 + x._1, acc._2 + acc._2))
    case Fork(is) => is.map(instr => estimate(instr, start)).foldLeft((0l, 0l))((acc, x) => (acc._1 + x._1, acc._2 + acc._2))
  }
}

class IntraExecutor extends AbsMapper[SimpleMemory, Triple[SimpleMemory]] {

  private var optionalSolver : Option[Z3Solver] = None
  private var level: Int = 0
  private var nrSolverCalls = 0l
  private var totalSolverTime = 0l
  private var nrTrue = 0l
  private var nrFalse = 0l

  def reset(): IntraExecutor = {
    optionalSolver = None
    level = 0
    this
  }
  def push() : IntraExecutor = {
    level = level + 1
    optionalSolver.foreach(_.push())
    this
  }
  def pop(n : Int = 1) : IntraExecutor = if (n == 0) {
    this
  } else {
    optionalSolver.foreach(_.pop(n))
    level = level - n
    this
  }
  def dumpStats(): Unit = {
    Logger.getLogger(this.getClass.getName).info(s"solver time ${totalSolverTime}ms / " +
      s"$nrSolverCalls - $nrTrue sat vs $nrFalse unsat")
  }
  private def check(simpleMemory: SimpleMemory, newCondition : Condition): (Option[SimpleMemory], Int, Int) = {
    val simpd = context.simplifyAst(newCondition)
    val lev = level
    if (simpd == context.mkFalse()) {
      (None, lev, lev)
    } else if (simpd == context.mkTrue()) {
      (Some(simpleMemory), lev, lev)
    } else {
      val slv = if (optionalSolver.isEmpty) {
        val s = context.mkSolver()
        optionalSolver = Some(s)
        simpleMemory.pathCondition.foreach(s.assertCnstr)
        s
      } else {
        optionalSolver.get
      }
      push()
      val newLevel = level
      slv.assertCnstr(simpd)
      nrSolverCalls = nrSolverCalls + 1
      val start =System.currentTimeMillis()
      val res = true//slv.check().get
      val end = System.currentTimeMillis()
      if (end - start > 300) {
        Logger.getLogger(this.getClass.getName).info(s"heavy hitter ${end - start}ms")
      }
      totalSolverTime += (end - start)
      (if (res) {
        nrTrue = nrTrue + 1
        Some(simpleMemory.addCondition(simpd))
      } else {
        nrFalse = nrFalse + 1
        None
      }, lev, newLevel)
    }
  }

  private def undo(oldLevel : Int, newLevel : Int): IntraExecutor = {
    assert(newLevel == level)
    pop(newLevel - oldLevel)
  }

  private def checkAndRun(state: SimpleMemory, cd : Condition,
                          m : SimpleMemory => Triple[SimpleMemory]) = {
    val (ra, ola, nla) = check(state, cd)
    val resa = ra.map(m).getOrElse(Triple.empty)
    undo(ola, nla)
    resa
  }

  override def execute(instruction: Instruction, state: SimpleMemory, verbose: Boolean): Triple[SimpleMemory] = {
    val init = level
    val res = super.execute(instruction, state, verbose)
    val end = level
    assert(end == init)
    res
  }
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
          case Left(v2) =>
            Left(context.mkEq(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case LT(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkBVSlt(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case GT(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkBVSgt(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case LTE(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkBVSle(v, v2))
          case Right(m) => Right(m)
        }
        case Right(m) => Right(m)
      }
      case GTE(a, b) => tinstantiate(a, state) match {
        case Left(v) => tinstantiate(b, state) match {
          case Left(v2) => Left(context.mkBVSge(v, v2))
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
        checkAndRun(state, takea, newstate => execute(iff.thn, newstate, verbose)) +
        checkAndRun(state, takeb, newstate => execute(iff.els, newstate, verbose)) +
        checkAndRun(state, takef, newstate => Triple.fail(newstate.fail(s"segfault found in ${iff.bExpr}")))
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
      case Left(tinfo) =>
        tinfo.get(bvexp).filter(w => w > 0).map(src => {
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
        checkAndRun(state, takea, Triple.startFrom) +
        checkAndRun(state, takef, x => Triple.fail(x.fail(s"segfault found in ${assume.bExpr}")))
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