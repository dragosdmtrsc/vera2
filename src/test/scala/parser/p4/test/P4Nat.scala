package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
import org.scalatest.FunSuite

class P4Nat extends FunSuite {
  test("INTEGRATION - simple-nat test no entries") {
    val dir = "inputs/simple-nat-testing/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        res.allParserStatesInstruction()
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "soso")
    assert(ok.forall(r => r.history.head == "router.output.11"))
  }

  test("INTEGRATION - simple-nat egress all drop no entries") {
    val dir = "inputs/simple-nat-testing/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
    val port = 2
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        res.allParserStatesInstruction()
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "egress")
    assert(ok.isEmpty)
  }

  test("simple-nat - entries ingress") {
    val dir = "inputs/simple-nat-populated/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val parserout = codeAwareInstructionExecutor.program("router.control.ingress")
    val newparserout = InstructionBlock(
      Constrain("ethernet.IsValid", :==:(ConstantValue(1))),
      Constrain("ethernet.srcAddr", :~:(:==:(:@("ethernet.dstAddr")))),
//      Constrain("ethernet.dstAddr", :==:(ConstantValue(0xaabb000004l))),
      Constrain("ipv4.IsValid", :==:(ConstantValue(1))),
      Constrain("ipv4.srcAddr", :~:(:==:(:@("ipv4.dstAddr")))),
//      Constrain("ipv4.srcAddr", :==:(ConstantValue(0x0a00000al))),
//      Constrain("tcp.IsValid", :==:(ConstantValue(1))),
//      Constrain("tcp.srcPort", :==:(ConstantValue(0x86b6l))),
      Forward("router.control.ingress.new")
    )
    import org.change.v2.analysis.memory.TagExp.IntImprovements
    val newinstrs = (codeAwareInstructionExecutor.program + ("router.control.ingress" -> newparserout)) + ("router.control.ingress.new" -> parserout)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        InstructionBlock(
          CreateTag("START", 0),
          Call("router.generator.parse_ethernet.parse_ipv4.parse_tcp")
        )
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial,
      CodeAwareInstructionExecutor(newinstrs, Map.empty, solver = new Z3BVSolver))
    printResults(dir, port, ok, failed, "egress")
    assert(ok.nonEmpty && ok.exists(_.history.head == "router.output.2"))
  }

}
