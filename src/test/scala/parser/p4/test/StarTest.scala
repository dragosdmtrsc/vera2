package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

class StarTest extends FunSuite {

  test("INTEGRATION - copy-to-cpu star entries") {
    val dir = "inputs/copy-to-cpu-star/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands_star.txt"

    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"),
      Map[Int, Int](250 -> 3), Switch.fromFile(p4), dataplane)
    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)

    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "good")
  }
}
