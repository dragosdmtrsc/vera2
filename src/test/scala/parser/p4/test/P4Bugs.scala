package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.factories.FullTableFactory
import org.change.parser.p4.tables.InstanceBasedFullTable
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.SwitchInstance
import org.scalatest.FunSuite

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

    import spray.json._
    import JsonWriter._
    import org.change.v2.analysis.memory.jsonformatters.StateToJson._
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


  private def printResults(dir: String, port: Int, ok: List[State], failed: List[State], okBase: String): Unit = {
    val psok = new BufferedOutputStream(new FileOutputStream(s"$dir/ok-port$port-$okBase.json"))
    JsonUtil.toJson(ok, psok)
    psok.close()

    val relevant = failed

    val psko = new BufferedOutputStream(new FileOutputStream(s"$dir/fail-port$port-$okBase.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()

    import spray.json._
    import JsonWriter._
    import org.change.v2.analysis.memory.jsonformatters.StateToJson._
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

  private def executeAndPrintStats(ib: InstructionBlock, initial: List[State], codeAwareInstructionExecutor : CodeAwareInstructionExecutor) = {
    val init = System.currentTimeMillis()
    val (ok, failed) = initial.foldLeft((Nil, Nil): (List[State], List[State]))((acc, init) => {
      val (o, f) = codeAwareInstructionExecutor.execute(ib, init, true)
      (acc._1 ++ o, acc._2 ++ f)
    })
    println(s"Failed # ${failed.size}, Ok # ${ok.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")
    (ok, failed)
  }
}
