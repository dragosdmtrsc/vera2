package parser.p4.test

import java.io.{File, PrintStream}
import java.util
import java.util.UUID
import javax.json.JsonValue

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.factories.SymbolicRegistersInitFactory
import org.change.parser.p4.parser.{ParserGenerator, SwitchBasedParserGenerator}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, CodeAwareInstructionExecutorWithListeners}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.{Switch, SwitchInstance}
import org.scalatest.FunSuite
import org.change.v2.analysis.memory.TagExp.IntImprovements

class SwitchTests extends FunSuite {

  test("SWITCH - check parsing ok") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val sw = Switch.fromFile(p4)
    assert( sw != null)
    assert(sw.getActionProfiles() != null && !sw.getActionProfiles.isEmpty)
    println(sw.getActionProfiles)
  }

  test("SWITCH - check parsing ok p4 & commands") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/table_dump_full.txt"
    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2", 2 -> "veth4", 3 -> "veth6", 4 -> "veth8", 5 -> "veth10", 6 -> "veth12", 7 -> "veth14", 8 -> "veth16", 64 -> "veth250"
    )
    val switchInstance = SwitchInstance.fromP4AndDataplane(p4, dataplane,
      "switch",
      ifaces.foldLeft(new util.HashMap[Integer, String]())((acc, x) => {
        acc.put(x._1, x._2)
        acc
      }))
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

  test("SWITCH - code dump") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/table_dump_full.txt"

    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2",
      2 -> "veth4", 3 -> "veth6",
      4 -> "veth8", 5 -> "veth10",
      6 -> "veth12", 7 -> "veth14",
      8 -> "veth16", 64 -> "veth250"
    )
    val sw = Switch.fromFile(p4)
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("switch",
      ifaces,
      Map.empty,
      sw,
      dataplane)
    val port = 0
    val ib = InstructionBlock(
      Forward(s"switch.input.$port")
    )
    //-i 0@veth0 -i 1@veth2 -i 2@veth4 -i 3@veth6 -i 4@veth8 -i 5@veth10
    // -i 6@veth12 -i 7@veth14 -i 8@veth16 -i 64@veth250
    val res = new ControlFlowInterpreter(switchInstance, switch = sw,
      optParserGenerator = Some(
        new SwitchBasedParserGenerator(switch = sw,
          switchInstance = switchInstance, codeFilter = Some((x : String) => {
            x.contains("parse_ethernet") &&
              x.contains("parse_ipv4") &&
              x.contains("parse_tcp")
          }))
      )
    )
    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver)
    import spray.json._
    val pscode = new PrintStream(s"$dir/code.json")

    println("big switch parsing done")
    pscode.println(codeAwareInstructionExecutor.program.toJson(JsonWriter.func2Writer(f => {
      JsObject(f.map(i => {
        try {
          i._1 -> JsString(i._2.toString)
        } catch {
          case ex : Exception => throw new Exception(s"Failed deserializing ${i._1}")
        }
      }))
    })).compactPrint)
  }

  test("SWITCH - tcp run") {
    import org.change.v2.analysis.executor.StateConsumer
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/table_dump_full.txt"


    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2",
      2 -> "veth4", 3 -> "veth6",
      4 -> "veth8", 5 -> "veth10",
      6 -> "veth12", 7 -> "veth14",
      8 -> "veth16", 64 -> "veth250"
    )
    val sw = Switch.fromFile(p4)
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("switch",
      ifaces,
      Map.empty,
      sw,
      dataplane)
    assert(switchInstance.tableDefinitions("system_acl").flowInstances.nonEmpty)
    println(switchInstance.tableDefinitions.get("system_acl"))
    val port = 0
    val ib = InstructionBlock(
      Forward(s"switch.input.$port")
    )
    //-i 0@veth0 -i 1@veth2 -i 2@veth4 -i 3@veth6 -i 4@veth8 -i 5@veth10 -i 6@veth12 -i 7@veth14 -i 8@veth16 -i 64@veth250
    val res = new ControlFlowInterpreter(switchInstance, switch = sw,
      optParserGenerator = Some(
        new SwitchBasedParserGenerator(switch = sw,
          switchInstance = switchInstance, codeFilter = Some((x : String) => {
            x.contains("parse_ethernet") &&
            x.contains("parse_ipv4") &&
            x.contains("parse_tcp")
          }))
      )
    )
    val (failIndex, successIndex, printer) = createConsumer(dir)

    val codeAwareInstructionExecutor = new CodeAwareInstructionExecutorWithListeners(
      CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver),
      successStateConsumers = printer :: Nil,
      failedStateConsumers = printer :: Nil
    )
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        CreateTag("START", 0),
        Call("switch.generator.parse_ethernet.parse_ipv4.parse_tcp")
      ), State.clean, verbose = true)
    println(s"initial states gathered ${initial.size}")
    val (ok: List[State], failed: List[State]) = executeAndPrintStats(ib, initial, codeAwareInstructionExecutor)
    printResults(dir, port, ok, failed.filter(r => {
      !(r.errorCause.exists(k => k.startsWith("Cannot resolve reference to")) &&
      r.history.head.contains("switch.parser."))
    }), "soso")
    successIndex.close()
    failIndex.close()
    if (ok.nonEmpty)
      println(ok)
  }


}
