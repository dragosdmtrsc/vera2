package parser.p4.test

import java.util

import org.change.parser.p4._
import org.change.parser.p4.control.P4ToAbstractNetwork
import org.change.utils.prettifier.JsonUtil
import org.change.v2.p4.model.SwitchInstance
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
    val res = ControlFlowInterpreter(p4, dataplane, List[String]("veth0", "veth1"),"router")
    println(JsonUtil.toJson(res.instructions()))
    println(JsonUtil.toJson(res.links))
  }

}
