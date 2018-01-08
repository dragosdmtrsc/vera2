package parser.p4.test

import java.util

import org.change.parser.p4.tables.{FireAction, FireDefaultAction}
import org.change.utils.prettifier.JsonUtil
import org.change.v2.p4.model.SwitchInstance
import org.scalatest.FunSuite

class P4NATParsingTests extends FunSuite {

  test("NAT spec can be parsed - actions, reg defs and field lists are there") {
    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = SwitchInstance.fromP4AndDataplane(p4, dataplane, "nat", util.Arrays.asList("veth0", "veth1"))
    assert(res.getSwitchSpec.getActionRegistrar.getAction("_drop") != null)
    assert(res.getSwitchSpec.getActionRegistrar.getAction("set_dmac").getParameterList.size() == 1)
    assert(res.getSwitchSpec.getRegisterSpecificationMap != null)
    assert(res.flowInstanceIterator("ipv4_lpm").size() > 0)

    import scala.collection.JavaConversions._

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

}
