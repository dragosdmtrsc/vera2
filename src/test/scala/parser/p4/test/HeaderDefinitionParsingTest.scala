package parser.p4.test

import org.change.parser.p4.{MetadataInstance, P4ParserRunner}
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
}
