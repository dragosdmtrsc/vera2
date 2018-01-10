package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.factories.{InstanceBasedInitFactory, SymbolicRegistersInitFactory}
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.SwitchInstance
import org.scalatest.FunSuite
import org.change.v2.analysis.memory.TagExp.IntImprovements

class P4Bugs extends FunSuite {
  test("INTEGRATION - copy-to-cpu parser bug") {
    val dir = "inputs/copy-to-cpu-bug-parser-failed/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 3 -> "cpu"), "router")
    val ib = InstructionBlock(
      Forward("router.input.1")
    )
    val ps = new PrintStream(s"$dir/ctrl1-instrs.json")
    ps.println(JsonUtil.toJson(res.instructions()))
    ps.println(JsonUtil.toJson(res.links()))
    ps.close()
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.execute(InstructionBlock(
      res.allParserStatesInstruction(),
      // this goes to say that we resolve to handling packets with cpu_header encapsulation + cpu_header.reason == 0, cpu_header.device == 0
      // but non-zero ether address. Expected behavior = only one failed state will spring out of this === the state where the parser
      // thinks this is an ethernet encapsulated packet, but it isn't => packet layout access violation
      Constrain(Tag("START") + 0, :==:(ConstantValue(0))),
      Constrain(Tag("START") + 8, :==:(ConstantValue(0))),
      Constrain(Tag("START") + 16, :~:(:==:(ConstantValue(0))))), State.clean, verbose = true
    )
    var init = System.currentTimeMillis()
    val (ok, failed) = initial.foldLeft((Nil, Nil) : (List[State], List[State]))((acc, init) => {
      val (o, f) = codeAwareInstructionExecutor.execute(ib, init, verbose = true)
      (acc._1 ++ o, acc._2 ++ f)
    })
    println(s"Failed # ${failed.size}, Ok # ${ok.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")

    val psok = new BufferedOutputStream(new FileOutputStream(s"$dir/click-exec-ok-port0.json"))
    JsonUtil.toJson(ok, psok)
    psok.close()

    val relevant = failed

    val psko = new BufferedOutputStream(new FileOutputStream(s"$dir/click-exec-fail-port0.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()

    import org.change.v2.analysis.memory.jsonformatters.StateToJson._
    import spray.json._
    val psokpretty = new PrintStream(s"$dir/click-exec-ok-port0-pretty.json")
    psokpretty.println(ok.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    psokpretty.close()

    val pskopretty = new PrintStream(s"$dir/click-exec-fail-port0-pretty.json")
    pskopretty.println(relevant.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    pskopretty.close()
  }

  test("INTEGRATION - copy-to-cpu remove_header good") {
    val dir = "inputs/copy-to-cpu-remove-header/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands-good.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 3 -> "cpu"), "router")
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

  test("INTEGRATION - copy-to-cpu remove_header bad") {
    val dir = "inputs/copy-to-cpu-remove-header/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands-bad.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 3 -> "cpu"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "bad")
  }

  test("INTEGRATION - copy-to-cpu invalid_access") {
    val dir = "inputs/copy-to-cpu-invalid-access/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands-bad.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 3 -> "cpu"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "bad")
    assert(ok.isEmpty)
  }

  test("INTEGRATION - copy-to-cpu remove_header ethernet") {
    val dir = "inputs/copy-to-cpu-remove-ethernet/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands-bad.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 3 -> "cpu"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "bad")
  }

  test("INTEGRATION - mplb no deparse") {
    val dir = "inputs/parser-deparser-bug/"
    val p4 = s"$dir/mplb_router-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1"), "router")
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, fld) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        CreateTag("START", 0),
        Call("router.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        Constrain(Tag("START") + 272 + 16, :==:(ConstantValue(1025)))
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    val relevant = failed
    printResults(dir, port, ok, relevant, "bad")
  }

  test("INTEGRATION - ndp_router reg access test") {
    val dir = "inputs/ndp-router-reg-access/"
    val p4 = s"$dir/ndp_router-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1"), "router", optAdditionalInitCode = Some((x, y) => {
      new SymbolicRegistersInitFactory(x).initCode()
    }))
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "soso")
  }
  test("INTEGRATION - ndp_router test") {
    val dir = "inputs/ndp-router/"
    val p4 = s"$dir/ndp_router-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1"), "router", optAdditionalInitCode = Some((x, y) => {
      new SymbolicRegistersInitFactory(x).initCode()
    }))
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "soso")
  }
  test("INTEGRATION - ndp_router readonly write") {
    val dir = "inputs/ndp-router-set-readonly/"
    val p4 = s"$dir/ndp_router-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1"), "router", optAdditionalInitCode = Some((x, y) => {
      new SymbolicRegistersInitFactory(x).initCode()
    }))
    val port = 1
    val ib = InstructionBlock(
      Forward(s"router.input.$port")
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(res.allParserStatesInstruction()), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed, "fail-readonly")
  }

  test("INTEGRATION - simple-router test") {
    val dir = "inputs/simple-router-testing/"
    val p4 = s"$dir/simple_router.p4"
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
  }

  test("p4xos/acceptor-ppc.p4") {
    val thrown = intercept[Exception] {
      val dir = "inputs/p4xos"
      val p4 = s"$dir/acceptor-ppc.p4"
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
    }
    assert(thrown.getMessage.toLowerCase().contains("no such action"))
  }

  test("p4xos/learner-ppc.p4") {
    val dir = "inputs/p4xos"
    val p4 = s"$dir/learner-ppc.p4"
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
      execute(InstructionBlock(
        CreateTag("START", 0),
        Call("router.generator.parse_ethernet.parse_ipv4.parse_udp.parse_paxos")
      ), State.clean, verbose = true)
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)

    printResults(dir, port, ok, failed.filter(r => {
      !(r.history.head.contains("router.parser.") && r.errorCause.exists(r => r.contains("Cannot resolve reference to")))
    }), "soso")
  }

  test("p4xos/coordinator-ppc.p4") {
    val thrown = intercept[Exception] {
      val dir = "inputs/p4xos"
      val p4 = s"$dir/coordinator-ppc.p4"
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
    }
    assert(thrown.getMessage.toLowerCase().contains("no such action") || thrown.getMessage.toLowerCase().contains("no such table"))
  }
}
