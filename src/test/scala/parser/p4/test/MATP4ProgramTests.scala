package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

class MATP4ProgramTests extends FunSuite {

  test("MAT parsing") {

    val dir = "inputs/mat-p4/"
    val p4 = s"$dir/mat-p4.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0"), "filter")

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      Forward("filter.input.1")
    )

//    val ps = new PrintStream(s"$dir/ctrl1-instrs.json")
//    ps.println(JsonUtil.toJson(res.instructions()))
//    ps.println(JsonUtil.toJson(res.links()))
//    ps.close()
//
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    var init = System.currentTimeMillis()
    val (ok, failed) = codeAwareInstructionExecutor.execute(ib, State.clean, verbose = true)
    println(s"Failed # ${failed.size}, Ok # ${ok.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")
  }

  test("MAT with syms") {
    val dir = "inputs/mat-p4/"
    val p4 = s"$dir/mat-p4.p4"
    val dataplane = s"$dir/commands-sym.txt"

    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("filter", Map[Int, String](1 -> "veth0"),
      Map.empty, Switch.fromFile(p4), dataplane)

    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)
    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      Forward("filter.input.1")
    )

    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    var init = System.currentTimeMillis()
    val (ok, failed) = codeAwareInstructionExecutor.execute(ib, State.clean, verbose = true)
    println(s"Failed # ${failed.size}, Ok # ${ok.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")
  }

}
