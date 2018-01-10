package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.factories.SymbolicRegistersInitFactory
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Call, CreateTag, Forward, InstructionBlock}
import org.scalatest.FunSuite
import org.change.v2.analysis.memory.TagExp.IntImprovements

class AxonTests extends FunSuite {
  test("axon") {
    val dir = "inputs/axon"
    val p4 = s"$dir/axon-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router", optAdditionalInitCode = Some((x, y) => {
      new SymbolicRegistersInitFactory(x).initCode()
    }))
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(res.allParserStatesInstruction(), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)

    printResults(dir, port, ok, failed.filter(r => {
      !(r.history.head.contains("router.parser.") && r.errorCause.exists(r => r.contains("Cannot resolve reference to")))
    }), "soso")
  }

}
