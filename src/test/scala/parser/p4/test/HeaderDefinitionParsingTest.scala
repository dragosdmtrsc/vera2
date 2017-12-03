package parser.p4.test

import java.io.{BufferedInputStream, BufferedOutputStream, FileOutputStream, PrintStream}
import java.util

import org.change.parser.p4._
import org.change.parser.p4.control.P4ToAbstractNetwork
import org.change.parser.p4.parser.{DFSState, StateExpander}
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.{DecoratedInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.p4.model.{Switch, SwitchInstance}
import org.scalatest.FunSuite

import scala.collection.JavaConversions._

class HeaderDefinitionParsingTest extends FunSuite {

  test("vlan_t example is parsed correctly") {
    val p4 = "src/main/resources/p4s/tests/vlan_t.p4"
    val res = P4ParserRunner.parse(p4)

    assert(res.declaredHeaders.size == 1)

    val vlantHeader = res.declaredHeaders.head._2
    assert(vlantHeader.length == 32)
    assert(
      Seq("pcp", "cfi", "vid", "ethertype").forall(f => vlantHeader.fields.values.exists(f == _._1))
    )
  }

  test("local_metadata example is parsed correctly") {
    val p4 = "src/main/resources/p4s/tests/local_metadata.p4"
    val res = P4ParserRunner.parse(p4)

    assert(res.declaredHeaders.size == 1)

    val vlantHeader = res.declaredHeaders.head._2
    assert(vlantHeader.length == 24)
    assert(
      Seq("bad_packet", "cpu_code").forall(f => vlantHeader.fields.values.exists(f == _._1))
    )
  }

  test("metadata and header instances can be parsed - vlan and local_metadata example") {
    val p4 = "src/main/resources/p4s/tests/header_and_metadata_instance.p4"
    val res = P4ParserRunner.parse(p4)

    assert(res.declaredHeaders.size == 2)
    assert(res.headerInstances.size == 2)
    assert(res.headerInstances("inner_vlan_tag").layout.headerName == "vlan_t")
    assert(res.headerInstances("local_metadata").asInstanceOf[MetadataInstance].values("bad_packet") == 1)
  }

  test("actions can be parsed - registrar is not empty") {
    val p4 = "inputs/simple-router/simple_router.p4"
    val res = P4ParserRunner.parse(p4)

    assert(res.actionRegistrar.getDeclaredActions.iterator().hasNext)
    assert(res.actionRegistrar.getAction("_drop") != null)
    for (x <- res.actionRegistrar.getDeclaredActions) {
      println(x)
    }
  }


  test("switch spec can be parsed - actions, reg defs and field lists are there") {
    val p4 = "inputs/simple-router/simple_router.p4"
    val dataplane = "inputs/simple-router/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, util.Arrays.asList("veth0", "veth1"))
    assert(res.getSwitchSpec.getActionRegistrar.getAction("_drop") != null)
    assert(res.getSwitchSpec.getActionRegistrar.getAction("set_dmac").getParameterList.size() == 1)
    assert(res.getSwitchSpec.getRegisterSpecificationMap != null)
    assert(res.flowInstanceIterator("ipv4_lpm").size() > 0)

    for (x <- res.getDeclaredTables) {
//      println(x.getTable + " " + x.getFireAction + " - " + x.getMatchParams + " - " + x.getActionParams)
      var i = 0
      for (y <- res.flowInstanceIterator(x)) {
        val fireAction = new FireAction(x, i, res).symnetCode()
        println(s"$i@$x")
        println(fireAction)
        i = i + 1
      }
      assert(res.getDefaultAction(x) != null)
      val p4ActionCall = new FireDefaultAction(x, res)
      println(s"default@$x")
      println(p4ActionCall.symnetCode())
    }
  }

  test("NAT parsing without data plane config") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"

    val sw = Switch.fromFile(p4)

    println("hello")
  }

  test("NAT spec can be parsed - actions, reg defs and field lists are there") {
    val p4 = "inputs/simple-nat/simple_nat.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))
    assert(res.getSwitchSpec.getActionRegistrar.getAction("_drop") != null)
    assert(res.getSwitchSpec.getActionRegistrar.getAction("set_dmac").getParameterList.size() == 1)
    assert(res.getSwitchSpec.getRegisterSpecificationMap != null)
    assert(res.flowInstanceIterator("ipv4_lpm").size() > 0)

    for (x <- res.getDeclaredTables) {
      //      println(x.getTable + " " + x.getFireAction + " - " + x.getMatchParams + " - " + x.getActionParams)
      var i = 0
      for (y <- res.flowInstanceIterator(x)) {
        val fireAction = new FireAction(x, i, res).symnetCode()
        println(s"$i@$x")
        println(JsonUtil.toJson(fireAction))
        i = i + 1
      }
      assert(res.getDefaultAction(x) != null)
      val p4ActionCall = new FireDefaultAction(x, res)
      println(s"default@$x")
      println(JsonUtil.toJson(p4ActionCall.symnetCode()))
    }
  }

  test("SWITCH - new packet initialzier") {
    val p4 = "inputs/simple-nat/simple_nat.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))

    val initializeCode = new InitializeCode(res)
    println(JsonUtil.toJson(initializeCode.switchInitializePacketEnter(0)))
  }
  test("SWITCH - global initialzier") {
    val p4 = "inputs/register/register.p4"
    val dataplane = "inputs/register/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))

    val initializeCode = new InitializeCode(res)
    println(JsonUtil.toJson(initializeCode.switchInitializeGlobally()))
  }

  test("SWITCH - reg actions") {
    val p4 = "inputs/register/register.p4"
    val dataplane = "inputs/register/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))

    for (x <- res.getDeclaredTables) {
      //      println(x.getTable + " " + x.getFireAction + " - " + x.getMatchParams + " - " + x.getActionParams)
      var i = 0
      for (y <- res.flowInstanceIterator(x)) {
        val fireAction = new FireAction(x, i, res).symnetCode()
        println(s"$i@$x")
        println(JsonUtil.toJson(fireAction))
        i = i + 1
      }
    }
  }


  test("SWITCH - full table flow run #1") {
    val p4 = "inputs/simple-router/simple_router.p4"
    val dataplane = "inputs/simple-router/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))

    for (tab <- res.getDeclaredTables) {
      println(JsonUtil.toJson(new FullTable(tab, res).fullAction()))
    }
  }

  test("CONTROL - flow is parsed OK") {
    val p4 = "inputs/simple-router/simple_router.p4"
    P4ToAbstractNetwork.buildConfig(p4, "1")
  }

  test("CONTROL - flow is parsed OK run #2") {
    val p4 = "inputs/simple-router/simple_router.p4"
    val parser = P4ToAbstractNetwork.getParser(p4)
    println(JsonUtil.toJson(parser.instructions))
    println(JsonUtil.toJson(parser.links))
  }

  test("CONTROL - flow is parsed OK run NAT #3") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))
    println(JsonUtil.toJson(res.getSwitchSpec.getCtx.instructions))
    println(JsonUtil.toJson(res.getSwitchSpec.getCtx.links))
  }


  test("CONTROL flow and table integration") {
    val p4 = "inputs/simple-router/simple_router.p4"
    val dataplane = "inputs/simple-router/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"),"router")
    println(JsonUtil.toJson(res.instructions()))
    println(JsonUtil.toJson(res.links))
  }

  test("CONTROL flow and table integration for simple nat") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"),"router")
    val ps = new PrintStream("inputs/simple-nat/ctrl1-instrs.json")
    ps.println(JsonUtil.toJson(res.instructions()))
    ps.println(JsonUtil.toJson(res.links))
    ps.close()
  }


  test("CONTROL flow and table integration for mtag-edge") {
    val p4 = "inputs/mTag/mtag-edge-ppc.p4"
    val dataplane = "inputs/mTag/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"),"router")
    val ps = new PrintStream("inputs/simple-nat/ctrl1-instrs.json")
    ps.println(JsonUtil.toJson(res.instructions()))
    ps.println(JsonUtil.toJson(res.links))
    ps.close()
  }


  test("PARSER - generating all packet layouts") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"),"router")
    val bvExec = new DecoratedInstructionExecutor(new Z3BVSolver)

    val fork = StateExpander.generateAllPossiblePackets(
      new StateExpander(res.switch, "start").doDFS(DFSState(0)), res.switch
    )
    val ps = new PrintStream("inputs/simple-nat/initial-possibilities.json")
    val (ok, fail) = bvExec.execute(fork, State.clean, true)
    ps.println(JsonUtil.toJson(ok))
    ps.close()
    assert(fail.isEmpty)
  }

  test("PARSER - run #1") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"),"router")
    val bvExec = new DecoratedInstructionExecutor(new Z3BVSolver)
    val fork = res.allParserStatesInstruction()
    val initCode = res.initialize(0)

    val tests = InstructionBlock(
      initCode,
      res.parserCode()
    )
    var ps = new PrintStream("inputs/simple-nat/initial-possibilities.json")
    val (ok, fail) = bvExec.execute(fork, State.clean, true)
    ps.println(JsonUtil.toJson(ok))
    ps.close()

    ps = new PrintStream("inputs/simple-nat/parser-run-ok.json")
    val (newok, newf) = ok.foldLeft((Nil : List[State], Nil : List[State]))((acc, x) => {
      val (okk, faill) = bvExec.execute(tests, x, true)
      (acc._1 ++ okk, acc._2 ++ faill)
    })
    ps.println(JsonUtil.toJson(newok))
    ps.close()
    ps = new PrintStream("inputs/simple-nat/parser-run-fail.json")
    ps.println(JsonUtil.toJson(newf))
    ps.close()
    assert(newok.size == ok.size)
  }

  test("INTEGRATION - graph") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"), "router")
    val fin = "inputs/simple-nat/graph.dot"
    val ps = new PrintStream(fin)
    ps.println(res.toDot())
    ps.close()
    import sys.process._
    s"dot -Tpng $fin -O" !
  }

  test("INTEGRATION - vis") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](0 -> "veth0", 1 -> "veth1", 11 -> "cpu"), "router")
    val fin = "inputs/simple-nat/graph.html"
    val ps = new PrintStream(fin)
    ps.println(res.toVis())
    ps.close()
  }

  test("INTEGRATION - run #1") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
//    val fin = "inputs/simple-nat/graph.dot"
//    val ps = new PrintStream(fin)
//    ps.println(res.toDot())
//    ps.close()
//    import sys.process._
//    s"dot -Tpng $fin -O" !
    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
//      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Forward("router.input.1")
    )


    val ps = new PrintStream("inputs/simple-nat/ctrl1-instrs.json")

    for ((tag,instr)<-res.instructions()){
      ps.println(tag);
      ps.println(instr + "\n")
    }
    //ps.println(JsonUtil.toJson(res.instructions()))
    //ps.println(JsonUtil.toJson(res.links))
    ps.close()

    val bvExec = new OVSExecutor(new Z3BVSolver)

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

    val psok = new BufferedOutputStream(new FileOutputStream("inputs/simple-nat/click-exec-ok-port0.json"))
    JsonUtil.toJson(clickExecutionContext.stuckStates, psok)
    psok.close()

    val relevant = clickExecutionContext.failedStates.filter(x => {
      !x.history.head.startsWith("router.parser")
//      true
    })

    val psko = new BufferedOutputStream(new FileOutputStream("inputs/simple-nat/click-exec-fail-port0.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()


    val psokpretty = new PrintStream("inputs/simple-nat/click-exec-ok-port0-pretty.json")
    psokpretty.println(clickExecutionContext.stuckStates)
    psokpretty.close()

    val pskopretty = new PrintStream("inputs/simple-nat/click-exec-fail-port0-pretty.json")
    pskopretty.println(relevant)
    pskopretty.close()

  }


  test("INTEGRATION - simple-router run #1") {
    val dir = "inputs/simple-router/"
    val p4 = s"$dir/simple_router.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
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

    val bvExec = new OVSExecutor(new Z3BVSolver)

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

  test("INTEGRATION - copy-to-cpu run #1") {
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

    val bvExec = new OVSExecutor(new Z3BVSolver)

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
