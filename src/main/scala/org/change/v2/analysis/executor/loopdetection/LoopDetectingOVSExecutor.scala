package org.change.v2.analysis.executor.loopdetection

import java.util.concurrent.ConcurrentLinkedQueue

import org.change.v2.analysis.executor.OVSExecutor
import org.change.v2.analysis.executor.solvers.{Solver, Z3BVSolver, Z3Solver}
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.LocationId
import org.change.v2.analysis.processingmodels.instructions.{Fail, Forward}

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}

class LoopDetectingOVSExecutor(checkedPorts: Set[LocationId], solver: Solver) extends OVSExecutor(solver) {

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
}


class BVLoopDetectingExecutor(checkedPorts: Set[LocationId])
  extends LoopDetectingOVSExecutor(checkedPorts, new Z3BVSolver())

class VanillaLoopDetectingExecutor(checkedPorts: Set[LocationId])
  extends LoopDetectingOVSExecutor(checkedPorts, new Z3Solver())