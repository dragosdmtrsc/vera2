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
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"), "router")
    val ib = InstructionBlock(
      res.allParserStatesInline(),
      Forward("router.input.1")
    )
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
    val relevant = clickExecutionContext.failedStates
    printResults(dir, 0, clickExecutionContext.stuckStates,  clickExecutionContext.failedStates, "nasty")
  }

  private def printResults(dir: String, port: Int, ok: List[State], failed: List[State], okBase: String): Unit = {
    val psok = new BufferedOutputStream(new FileOutputStream(s"$dir/ok-port$port-$okBase.json"))
    JsonUtil.toJson(ok, psok)
    psok.close()
    val relevant = failed
    val psko = new BufferedOutputStream(new FileOutputStream(s"$dir/fail-port$port-$okBase.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()

    import org.change.v2.analysis.memory.jsonformatters.StateToJson._
    import spray.json._
    val psokpretty = new PrintStream(s"$dir/ok-port$port-pretty-$okBase.json")
    psokpretty.println(ok.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    psokpretty.close()

    val pskopretty = new PrintStream(s"$dir/fail-port$port-pretty-$okBase.json")
    pskopretty.println(relevant.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    pskopretty.close()
  }

}
