//package parser.p4.test.updated
//
//import org.change.v2.p4.model.updated.program.P4Program
//import org.scalatest.FunSuite
//
//class P4NATHeaderDeclarationTest extends FunSuite {
//  val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
//  val p4Program = P4Program.fromP4File(p4).right.get
//
//
//  test("The number of declared headers is correct.") {
//    assert(p4Program.headerDeclarations.keySet.size == (6 + 1)) //standard_metadata_t is the extra one
//  }
//
//  val headerTypes = Set("ethernet_t", "ipv4_t", "cpu_header_t", "tcp_t", "meta_t", "intrinsic_metadata_t")
//  test("All the header types are present.") {
//    for {
//      headerType <- headerTypes
//    } assert(p4Program.headerDeclarations.keySet.contains(headerType))
//  }
//
//  val headerInstances = Set("ethernet", "ipv4", "cpu_header", "tcp", "meta", "intrinsic_metadata")
//  test("All header and metadata instances are present.") {
//    assert(p4Program.headerOrMetadataInstances.size == 7)
//    for {
//      headerInstance <- headerInstances
//    } assert(p4Program.headerOrMetadataInstances.keySet.contains(headerInstance))
//  }
//
//  val complexActions = Set(
//    "_drop",
//    "set_if_info",
//    "nat_miss_ext_to_int",
//    "nat_miss_int_to_ext",
//    "nat_hit_int_to_ext",
//    "nat_hit_ext_to_int",
//    "nat_no_nat",
//    "set_nhop",
//    "set_dmac",
//    "do_rewrites",
//    "do_cpu_encap"
//  )
//  test("All complex actions are present.") {
//    for {
//      action <- complexActions
//    } assert(p4Program.complexActions.keySet contains action)
//  }
//
//}
