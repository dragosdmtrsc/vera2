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

class P4LoopDetector extends FunSuite {

  test("copy-to-cpu with loop detector") {
    val dir = "inputs/copy-to-cpu/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"), "router")
    //    val fin = "inputs/simple-nat/graph.dot"
    //    val ps = new PrintStream(fin)
    //    ps.println(res.toDot())
    //    ps.close()
    //    import sys.process._
    //    s"dot -Tpng $fin -O" !
    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      Forward("router.input.1")
    )
    val ps = new PrintStream(s"$dir/ctrl1-instrs.json")
    ps.println(JsonUtil.toJson(res.instructions()))
    ps.println(JsonUtil.toJson(res.links))
    ps.close()

    val bvExec = new BVLoopDetectingExecutor(Set.empty)

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

    val relevant = clickExecutionContext.failedStates.filter(x => {
      !x.history.head.startsWith("router.parser")
      //      true
    })

    val psko = new BufferedOutputStream(new FileOutputStream(s"$dir/click-exec-fail-port0.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()


    val psokpretty = new PrintStream(s"$dir/click-exec-ok-port0-pretty.json")
    psokpretty.println(clickExecutionContext.stuckStates)
    psokpretty.close()

    val pskopretty = new PrintStream(s"$dir/click-exec-fail-port0-pretty.json")
    pskopretty.println(relevant)
    pskopretty.close()
  }

}
