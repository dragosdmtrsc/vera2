package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

import org.change.parser.p4.{ControlFlowInterpreter, P4ExecutionContext}
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.OVSExecutor
import org.change.v2.analysis.executor.loopdetection.BVLoopDetectingExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}
import org.scalatest.FunSuite

class P4LoopDetectorTests extends FunSuite {

  test("copy-to-cpu with loop detector") {
    val dir = "inputs/copy-to-cpu/"
    val p4 = s"$dir/copy_to_cpu-ppc-loop.p4"
    val dataplane = s"$dir/commands_loop.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"), "router")
    val ib = InstructionBlock(
      res.allParserStatesInline(),
      Forward("router.input.1")
    )
    val bvExec = new BVLoopDetectingExecutor(Set("router.parser"), res.instructions())

    var clickExecutionContext = P4ExecutionContext(
      res.instructions(), res.links(), bvExec.execute(ib, State.clean, true)._1, bvExec
    )

    var init = System.currentTimeMillis()
    var runs  = 0
    while (!clickExecutionContext.isDone && runs < 10000) {
      clickExecutionContext = clickExecutionContext.execute(true)
      runs = runs + 1
    }
    println(s"Failed # ${clickExecutionContext.failedStates.size}, Ok # ${clickExecutionContext.stuckStates.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")

    val psok = new BufferedOutputStream(new FileOutputStream(s"$dir/click-exec-ok-port0.json"))
    JsonUtil.toJson(clickExecutionContext.stuckStates, psok)
    psok.close()
    val relevant = clickExecutionContext.failedStates
    printResults(dir, 0, clickExecutionContext.stuckStates,  clickExecutionContext.failedStates, "nasty")
  }

}
