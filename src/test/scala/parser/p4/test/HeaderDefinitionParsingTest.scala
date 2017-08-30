package parser.p4.test

import org.change.parser.p4.P4ParserRunner
import org.scalatest.FunSuite

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

}
