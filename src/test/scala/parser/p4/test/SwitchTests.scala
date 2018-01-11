package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.factories.SymbolicRegistersInitFactory
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{:==:, Constrain, Forward, InstructionBlock}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

class SwitchTests extends FunSuite {

  test("SWITCH - check parsing ok") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc.p4"
    assert(Switch.fromFile(p4) != null)
  }


  test("SWITCH - first run") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc.p4"
    val dataplane = s"$dir/commands-switch.txt"
    //-i 0@veth0 -i 1@veth2 -i 2@veth4 -i 3@veth6 -i 4@veth8 -i 5@veth10 -i 6@veth12 -i 7@veth14 -i 8@veth16 -i 64@veth250
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](
      0 -> "veth0", 1 -> "veth2", 2 -> "veth4", 3 -> "veth6", 4 -> "veth8", 5 -> "veth10", 6 -> "veth12", 7 -> "veth14", 8 -> "veth16", 64 -> "veth250"
    ), "switch")
    val port = 0
    val ib = InstructionBlock(
      Forward(s"switch.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        res.allParserStatesInstruction()
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "soso")
  }
}
