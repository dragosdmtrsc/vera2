package org.change.v2.analysis.executor

import java.util.concurrent.{ExecutorService, Executors}
import java.util.concurrent.atomic.AtomicInteger

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.{Instruction, _}
import org.change.v2.analysis.processingmodels.instructions._

import scala.collection.mutable.ArrayBuffer


class AsyncExecutor(syncExec: InstructionExecutor,
                    instructions: Map[LocationId, Instruction],
                    maxLevel: Int = 1000,
                    var exec: ExecutorService = Executors.newFixedThreadPool(4))
  extends Executor[Unit] {

  private var pending: AtomicInteger = new AtomicInteger(0)
  private var running: AtomicInteger = new AtomicInteger(0)
  private var syncObject: Object = new Object()

  def shouldStop(state: State): Boolean = {
    state.history.size >= maxLevel
  }

  def pushExec(instruction: Instruction, state: State, v: Boolean): Unit = {
    syncObject.synchronized {
      pending.incrementAndGet()
    }
    exec.submit(new Runnable() {
      def run() {
        syncObject.synchronized {
          pending.decrementAndGet()
          running.incrementAndGet()
        }
        execute(instruction, state, v)
        syncObject.synchronized {
          running.decrementAndGet()
        }
      }
    })
  }


  def pushForward(s: State, v: Boolean): Unit = {
    val loc = s.location
    if (!shouldStop(s)) {
      val instr = getInstruction(loc)
      if (!instr.isEmpty) {
        this.pushExec(instr.get, s, v)
      }
      else {
        putOk(s)
      }
    }
    else {
      // time's up => it's cool. Mark it as ok state
      putOk(s)
    }
  }

  def close(): Unit = {
    this.exec.shutdown()
  }

  def isOver = syncObject.synchronized(this.running.get() == 0 &&
    this.pending.get == 0)

  protected def getInstruction(loc: LocationId): Option[Instruction] = {
    this.synchronized({
      this.instructions.get(loc)
    })
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


  override def executeNoOp(s: State, v: Boolean = false): Unit = {
    putOk(s)
  }

  override def executeInstructionBlock(instruction: InstructionBlock,
                                       s: State,
                                       v: Boolean = false): Unit = {
    val InstructionBlock(instructions) = instruction
    instructions match {
      case Nil => {
        putOk(s)
      }
      case If(a, b, c) :: tail => {
        val outcomes = If(a, b, c).branch(s)
        for (o <- outcomes)
          this.pushExec(InstructionBlock(o :: tail), s, v)
      }
      case Fork(instrs) :: tail => {
        for (inst <- instrs) {
          this.pushExec(InstructionBlock(inst :: tail), s, v)
        }
      }
      case Forward(x) :: tail => {
        // in case forward comes up, simply neglect what comes after
        // is this the right thing to do?
        val ll = syncExec.executeForward(Forward(x), s, v)._1
        val st = ll(0)
        this.pushForward(st, v)
        //        if (tail != Nil)
        //          this.pushExec(InstructionBlock(tail), s, v)
      }
      case InstructionBlock(instructions) :: tail => {
        val instrs = instructions.toList ++ tail
        this.executeInstructionBlock(InstructionBlock(instrs), s, v)
      }
      case head :: tail => {
        val (ok, fail) = syncExec.execute(head, s, v)
        if (!ok.isEmpty && !fail.isEmpty)
          throw new Exception("No comprende")
        if (ok.isEmpty)
          putFail(fail(0))
        else
          this.executeInstructionBlock(InstructionBlock(tail), ok(0), v)
      }
    }
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

}