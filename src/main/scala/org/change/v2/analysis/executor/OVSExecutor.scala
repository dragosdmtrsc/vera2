package org.change.v2.analysis.executor

import java.util.concurrent.{ExecutorService, Executors}

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.{ConstraintLogger, LogSolver, Solver}
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.nonprimitive.{Minus, Plus, Reference}
import org.change.v2.analysis.expression.concrete.{ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{ConstrainNamedSymbol, ConstrainRaw, CreateTag, Fail, Fork, Forward, If, InstructionBlock, _}

import scala.collection.mutable.ArrayBuffer


object OVSExecutor {

  def normalize(c: Constraint): Constraint = {
    c match {
      case NOT(EQ_E(expr)) => NOT(EQ_E(normalize(expr)))
      case NOT(GT_E(expr)) => LTE_E(normalize(expr))
      case NOT(GTE_E(expr)) => LT_E(normalize(expr))
      case NOT(LT_E(expr)) => GTE_E(normalize(expr))
      case NOT(LTE_E(expr)) => GT_E(normalize(expr))
      case (EQ_E(expr)) => (EQ_E(normalize(expr)))
      case (GT_E(expr)) => GT_E(normalize(expr))
      case (GTE_E(expr)) => GTE_E(normalize(expr))
      case (LT_E(expr)) => LT_E(normalize(expr))
      case (LTE_E(expr)) => LTE_E(normalize(expr))
      case NOT(OR(instrs)) => AND(instrs.map { x => normalize(NOT(x)) })
      case NOT(AND(instrs)) => OR(instrs.map { x => normalize(NOT(x)) })
      case NOT(NOT(z)) => normalize(z)
      case _ => c
    }
  }

  def normalize(expr: Expression): Expression = expr match {
    case ConstantStringValue(x) => ConstantValue(x.hashCode())
    case Plus(Value(e1, _, _), Value(e2, _, _)) => Plus(Value(normalize(e1)), Value(normalize(e2)))
    case Minus(Value(e1, _, _), Value(e2, _, _)) => Minus(Value(normalize(e1)), Value(normalize(e2)))
    case Reference(v, _) => {
      v.e match {
        case j: ConstantValue => j
        case j: ConstantStringValue => normalize(j)
        case j: Reference => normalize(j)
        case _ => Reference(v)
      }
    }
    case _ => expr
  }
}


class OVSExecutor(solver: Solver) extends DecoratedInstructionExecutor(solver) {

  import OVSExecutor.normalize


  override def executeAssignRaw(instruction: AssignRaw,
                                s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val AssignRaw(a, exp, t) = instruction
    a(s) match {
      case Some(int) => {
        instantiate(s, exp) match {
          case Left(e) => optionToStatePair(s, s"Error during assignment at $a")(s => {
            s.memory.Assign(int, e)
          })
          case Right(err) => execute(Fail(err), s, v)
        }
      }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }


  override def executeExoticInstruction(instruction: Instruction, s: State, v: Boolean):
  (List[State], List[State]) = {
    instruction match {
      case translatable: Translatable => this.execute(translatable.generateInstruction(), s, v)
      case destroy : DestroyPacket => destroy(s, verbose = true)
      case AssignNamedSymbolWithLength(id, exp, width) => AssignNamedSymbolWithLength(id, exp, width)(s, v)
      case Call(i) => executeForward(Forward(i), s, v)
      case er : ExistsRaw => er(s, v)
      case er : ExistsNamedSymbol => er(s, v)
      case er : NotExistsNamedSymbol => er(s, v)
      case er : NotExistsRaw => er(s, v)
      case _ if instruction.isTool => instruction(s, v)
      case _ => throw new UnsupportedOperationException("Cannot handle this kind of instruction. Make it Translatable " + instruction)
    }
  }


  override def executeConstrainRaw(instruction: ConstrainRaw, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val ConstrainRaw(a, dc, c) = instruction
    a(s) match {
      case Some(int) => c match {
        case None => instantiate(s, dc) match {
          case Left(c) => s.memory.eval(int) match {
            case Some(_) => optionToStatePair(s, s"Memory object @ $a cannot $dc")(s => {
              getNewMemory(Some(s.memory), Left(int), c)
            })
            case None => Fail(s"No object found for $int")(s, v)
          }
          case Right(err) => Fail(err)(s, v)
        }
        case Some(c) => s.memory.eval(int) match {
          case Some(_) => optionToStatePair(s, s"Memory object @ $a cannot $dc")(s => {
            getNewMemory(Some(s.memory), Left(int), c)
          })
          case None => Fail(s"No object found for $int")(s, v)
        }
      }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }

  def mapMemory(mem: MemorySpace, id: Long, value: Long): MemorySpace = {
    mem.copy(rawObjects = mem.rawObjects.map(x => {
      (x._1, mapMemoryObject(x._2, id, value))
    }).toMap,
      symbols = mem.symbols.map(x => {
        (x._1, mapMemoryObject(x._2, id, value))
      }).toMap
    )
  }

  def mapExpression(expr: Expression, id: Long, value: Long): Expression = {
    expr match {
      case y: SymbolicValue if y.id == id => ConstantValue(value)
      case Plus(a, b) => Plus(mapValue(a, id, value), mapValue(b, id, value))
      case Minus(a, b) => Minus(mapValue(a, id, value), mapValue(b, id, value))
      case _ => expr
    }
  }

  def mapConstraint(ct: Constraint, id: Long, value: Long): Constraint = {
    ct match {
      case OR(cs) => OR(cs.map(mapConstraint(_, id, value)))
      case AND(cs) => AND(cs.map(mapConstraint(_, id, value)))
      case LTE_E(expr) => LTE_E(mapExpression(expr, id, value))
      case GTE_E(expr) => GTE_E(mapExpression(expr, id, value))
      case GT_E(expr) => GT_E(mapExpression(expr, id, value))
      case LT_E(expr) => LT_E(mapExpression(expr, id, value))
      case EQ_E(expr) => EQ_E(mapExpression(expr, id, value))
      case NOT(c) => NOT(mapConstraint(c, id, value))
      case _ => ct
    }
  }

  def mapValue(v: Value, id: Long, value: Long): Value = {
    v.copy(e = mapExpression(v.e, id, value), cts = v.cts.map(mapConstraint(_, id, value)))
  }

  def mapValueStack(vs: ValueStack, id: Long, value: Long): ValueStack = {
    vs.vs match {
      case head :: tail => vs.copy(vs = mapValue(head, id, value) :: tail)
      case Nil => vs
    }
  }

  def mapMemoryObject(mo: MemoryObject, id: Long, value: Long): MemoryObject = {
    mo.valueStack match {
      case head :: tail => {
        mo.copy(valueStack = mapValueStack(head, id, value) :: tail)
      }
      case Nil => mo
    }
  }

  protected def log(a: Either[Int, String], value: Value, c: Constraint, sat: Boolean) {
  }


  protected def getNewMemory(maybeNewMem: Option[MemorySpace],
                             a: Either[Int, String],
                             nc: Constraint): Option[MemorySpace] = {
    val c = normalize(nc)
    maybeNewMem match {
      case None => None
      case Some(m) => {
        val maybeValue = a match {
          case Right(s) => m.eval(s)
          case Left(s) => m.eval(s)
        }

        if (maybeValue.isEmpty) {
          System.err.println("NOT found!!!!!" + a)
        }

        maybeValue.flatMap { value => {
          val negd = normalize(NOT(c))
          if (value.cts.contains(c))
            maybeNewMem
          else if (value.cts.contains(negd))
            None
          else {
            val mm = normalize(value.e) match {
              case ConstantValue(x, _, _) =>
                def constrainSimple(c: Constraint, maybeNewMem: Option[MemorySpace]):
                (Option[MemorySpace], Boolean) = {
                  c match {
                    case EQ_E(ConstantValue(y, _, _)) if y == x => (maybeNewMem, true)
                    case EQ_E(ConstantValue(y, _, _)) if y != x => (None, true)
                    case NOT(EQ_E(ConstantValue(y, _, _))) if y == x => (None, true)
                    case NOT(EQ_E(ConstantValue(y, _, _))) if y != x => (maybeNewMem, true)
                    case LTE_E(ConstantValue(y, _, _)) if x > y => (None, true)
                    case GTE_E(ConstantValue(y, _, _)) if x < y => (None, true)
                    case LT_E(ConstantValue(y, _, _)) if x >= y => (None, true)
                    case GT_E(ConstantValue(y, _, _)) if x <= y => (None, true)
                    case LTE_E(ConstantValue(y, _, _)) if x <= y => (maybeNewMem, true)
                    case GTE_E(ConstantValue(y, _, _)) if x >= y => (maybeNewMem, true)
                    case LT_E(ConstantValue(y, _, _)) if x < y => (maybeNewMem, true)
                    case GT_E(ConstantValue(y, _, _)) if x > y => (maybeNewMem, true)
                    case AND(cts) => {
                      cts.foldLeft((maybeNewMem, true))((acc, x) => {
                        if (acc._1.isEmpty)
                          (acc._1, true)
                        else {
                          val mmmm = constrainSimple(x, acc._1)
                          if (mmmm._2) {
                            if (mmmm._1.isEmpty)
                              (None, true)
                            else
                              mmmm
                          }
                          else {
                            (mmmm._1, false)
                          }
                        }
                      })
                    }
                    case _ => (m.addConstraint(a, c, true), false)
                  }
                }

                constrainSimple(c, maybeNewMem)
              case _ => (m.addConstraint(a, c, true), false)
            }
            if (!mm._2) {
              mm._1.flatMap(m2 => {
                val sat = isSat(m2)
                log(a, value, c, sat)
                if (sat) {
                  c match {
                    case EQ_E(ConstantValue(y, _, _)) => {
                      value.e match {
                        //                          case z : SymbolicValue => Some(mapMemory(m2, z.id, y))
                        case _ => Some(m2)
                      }
                    }
                    case _ => Some(m2)
                  }
                }
                else {
                  None
                }
              })
            }
            else
              mm._1
          }
        }
        }
      }
    }
  }

  override def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol, s: State, v: Boolean = false):
  (List[State], List[State]) = {
    val ConstrainNamedSymbol(id, dc, c) = instruction
    c match {
      case None => instantiate(s, dc) match {
        case Left(c) => s.memory.eval(id) match {
          case Some(_) => optionToStatePair(s, s"Symbol $id cannot $dc")(s => {
            //          val maybeNewMem = s.memory.addConstraint(id, c, true)
            getNewMemory(Some(s.memory), Right(id), c)
          })
          case None => Fail(s"No object found at $id")(s, v)
        }
        case Right(err) => Fail(err)(s, v)
      }
      case Some(c) => s.memory.eval(id) match {
        case Some(_) => optionToStatePair(s, s"Symbol $id cannot $dc")(s => {
          //          val maybeNewMem = s.memory.addConstraint(id, c, true)
          getNewMemory(Some(s.memory), Right(id), c)
        })
        case None => Fail(s"No object found at $id")(s, v)
      }
    }
  }
}


class OVSLogExecutor(solver: Solver,
                     val logger: ConstraintLogger) extends OVSExecutor(solver) {

  override def log(a: Either[Int, String], value: Value, c: Constraint, sat: Boolean) {
    val (tt, st) = if (this.solver.isInstanceOf[LogSolver[_]])
      this.solver.asInstanceOf[LogSolver[_]].stats
    else
      (0l, 0l)
    logger.log(a, value, c, tt, st, sat)
  }
}


class OVSAsyncExecutor(syncExec: InstructionExecutor,
                       nThreads: Int = 4)
  extends Executor[Unit] {

  import OVSExecutor.normalize

  var exec: ExecutorService = Executors.newFixedThreadPool(nThreads)

  private var pending: Int = 0
  //  private var running : AtomicInteger = new AtomicInteger(0)
  private var syncObject: Object = new Object()

  def isOver = syncObject.synchronized(this.pending == 0)


  def waitOver(): OVSAsyncExecutor = {
    syncObject.synchronized(
      while (pending != 0)
        syncObject.wait()
    )
    this
  }

  def clearResults() {
    this.oks.clear()
    this.fails.clear()
  }

  def pushExec(instruction: Instruction, state: State, v: Boolean): Unit = {
    syncObject.synchronized {
      pending += 1
    }
    exec.submit(new Runnable() {
      def run() {
        try {
          execute(instruction, state, v)
        }
        catch {
          case e: Exception => e.printStackTrace()
        }
        syncObject.synchronized {
          pending -= 1
          if (pending == 0) {
            syncObject.notifyAll()
          }
        }
      }
    })
  }

  def close(): Unit = {
    this.exec.shutdown()
  }

  def putOk(state: State): Unit = {
    this.synchronized {
      oks += state
    }
  }

  def putFail(state: State): Unit = {
    this.synchronized {
      fails += state
    }
  }

  private var oks: ArrayBuffer[State] = ArrayBuffer[State]()
  private var fails: ArrayBuffer[State] = ArrayBuffer[State]()

  def getOks = this.synchronized {
    oks.toList
  }

  def getFails = this.synchronized {
    fails.toList
  }

  override def executeInstructionBlock(instruction: InstructionBlock,
                                       s: State,
                                       v: Boolean = false): Unit = {
    val InstructionBlock(unflat) = instruction
    val instructions = unflat.flatMap { x => {
      x match {
        case y: InstructionBlock => y.instructions
        case _ => List[Instruction](flattenInstruction(x, s))
      }
    }
    }.toList
    instructions match {
      case Nil => {
        putOk(s)
      }
      case If(a, b, c) :: tail => {
        val lst = If(a, b, c).branch(s)
        for (outcome <- lst)
          this.pushExec(InstructionBlock(outcome :: tail), s.copy(), v)
      }
      case Fork(instrs) :: tail => {
        for (inst <- instrs) {
          this.pushExec(InstructionBlock(inst :: tail), s.copy(), v)
        }
      }
      case InstructionBlock(iiis) :: tail => {
        this.executeInstructionBlock(InstructionBlock(iiis.toList ++ tail), s, v)
      }
      case head :: tail => {
        if (head.isInstanceOf[Translatable])
          this.executeInstructionBlock(InstructionBlock(
            head.asInstanceOf[Translatable].generateInstruction :: tail
          ), s, v)
        else {
          val (ok, fail) = syncExec.execute(head, s, v)
          if (!ok.isEmpty && !fail.isEmpty)
            throw new Exception("No comprende")
          for (f <- fail)
            this.putFail(f)
          for (o <- ok)
            this.executeInstructionBlock(InstructionBlock(tail), o.copy(), v)
        }
      }
    }
  }


  def flattenInstruction(instr: Instruction, s: State): Instruction = instr match {
    case ConstrainRaw(a, dc, c) => {
      val ct: Either[Constraint, String] = c match {
        case None => instantiate(s, dc) match {
          case Left(c) => Left(c)
          case Right(err) => Right(err)
        }
        case Some(c) => Left(c)
      }
      if (ct.isRight) {
        Fail(ct.right.get)
      }
      else {
        val c = normalize(ct.left.get)
        c match {
          case AND(cts) => {
            InstructionBlock(
              cts.map { x => flattenInstruction(ConstrainRaw(a, null, Some(x)), s) }
            )
          }
          case OR(cts) => {
            Fork(
              cts.map { x => flattenInstruction(ConstrainRaw(a, null, Some(x)), s) }
            )
          }
          case _ => instr
        }
      }
    }
    case ConstrainNamedSymbol(a, dc, c) => {
      val ct: Either[Constraint, String] = c match {
        case None => instantiate(s, dc) match {
          case Left(c) => Left(c)
          case Right(err) => Right(err)
        }
        case Some(c) => Left(c)
      }
      if (ct.isRight) {
        Fail(ct.right.get)
      }
      else {
        val c = normalize(ct.left.get)
        c match {
          case AND(cts) => {
            InstructionBlock(
              cts.map { x => flattenInstruction(ConstrainNamedSymbol(a, null, Some(x)), s) }
            )
          }
          case OR(cts) => {
            Fork(
              cts.map { x => flattenInstruction(ConstrainNamedSymbol(a, null, Some(x)), s) }
            )
          }
          case _ => instr
        }
      }
    }
    case _ => instr
  }


  def executeIf(instruction: If,
                s: State,
                v: Boolean = false): Unit = {
    this.executeInstructionBlock(InstructionBlock(instruction), s, v)
  }

  def executeForward(instruction: Forward,
                     s: State,
                     v: Boolean = false):
  Unit = {
    this.executeInstructionBlock(InstructionBlock(instruction), s, v)
  }

  def executeFork(instruction: Fork,
                  s: State,
                  v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeFail(instruction: Fail,
                  s: State,
                  v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeConstrainRaw(instruction: ConstrainRaw,
                          s: State,
                          v: Boolean = false):
  Unit = {

    executeInstructionBlock(InstructionBlock(instruction), s, v)
  }

  def executeConstrainNamedSymbol(instruction: ConstrainNamedSymbol,
                                  s: State,
                                  v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeAssignNamedSymbol(instruction: AssignNamedSymbol,
                               s: State,
                               v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeAllocateRaw(instruction: AllocateRaw,
                         s: State,
                         v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeAssignRaw(instruction: AssignRaw,
                       s: State,
                       v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeAllocateSymbol(instruction: AllocateSymbol,
                            s: State,
                            v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeDestroyTag(instruction: DestroyTag,
                        s: State,
                        v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeDeallocateNamedSymbol(instruction: DeallocateNamedSymbol,
                                   s: State,
                                   v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)

  def executeCreateTag(instruction: CreateTag, s: State, v: Boolean = false):
  Unit = executeInstructionBlock(InstructionBlock(instruction), s, v)


  def executeDeallocateRaw(instruction: DeallocateRaw,
                           s: State,
                           v: Boolean = false):
  Unit = {
    executeInstructionBlock(InstructionBlock(instruction), s, v)
  }

  def executeExoticInstruction(instruction: Instruction,
                               s: State,
                               v: Boolean = false): Unit = {
    instruction match {
      case i: Translatable => this.executeInstructionBlock(InstructionBlock(i.generateInstruction()), s, v)
      case _ => throw new UnsupportedOperationException("Cannot handle non-translatable instructions")
    }
  }

  override def executeNoOp(s: State, v: Boolean = false): Unit = this.putOk(s)
}


