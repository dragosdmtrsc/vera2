package org.change.v2.analysis.executor.loopdetection

import java.util.concurrent.ConcurrentLinkedQueue

import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.executor.solvers.{Solver, Z3BVSolver, Z3Solver}
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.analysis.processingmodels.instructions.{Call, Fail, Forward}

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}

class LoopDetectingOVSExecutor(
                                checkedPorts: Set[LocationId],
                                instructions: Map[LocationId, Instruction],
                                solver: Solver) extends CodeAwareInstructionExecutor(instructions, solver) {

  lazy val stateHistory: ConcurrentMap[LocationId, ConcurrentLinkedQueue[State]] = {
    val history = new TrieMap[LocationId, ConcurrentLinkedQueue[State]]()

    for {
      port <- checkedPorts
    } history.put(port, new ConcurrentLinkedQueue[State]())

    history
  }

  override def executeForward(instruction: Forward, s: memory.State, v: Boolean) = {
    var toExecuteNext = () => super.executeForward(instruction, s, v)

    if (s.history.nonEmpty) {
      val port = s.location

      if (checkedPorts contains port)
        if (stateHistory(port).isEmpty)
          stateHistory(port).add(s)
        else {
          import scala.collection.JavaConverters._
          if (stateHistory(port).asScala.exists(BVLoopDetector.loop(s, _)))
          // Stop, loop detected
            toExecuteNext = () => super.executeFail(Fail("Loop detected"), s, v)
          else {
            // Add state to history
            stateHistory(port).add(s)
          }
        }
    }

    toExecuteNext()
  }

  override def executeExoticInstruction(instruction: Instruction, s: State, v: Boolean) = instruction match {
    // Map a Call instruction to Forward
    case Call(i) => executeForward(Forward(i), s, v)
    case _ => super.executeExoticInstruction(instruction, s, v)
  }
}


class BVLoopDetectingExecutor(checkedPorts: Set[LocationId], instructions: Map[LocationId, Instruction])
  extends LoopDetectingOVSExecutor(checkedPorts, instructions, new Z3BVSolver())

class VanillaLoopDetectingExecutor(checkedPorts: Set[LocationId], instructions: Map[LocationId, Instruction])
  extends LoopDetectingOVSExecutor(checkedPorts, instructions, new Z3Solver())