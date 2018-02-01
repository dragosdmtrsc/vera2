package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.{ParserGenerator, SkipParserAndDeparser, SwitchBasedParserGenerator, TrivialDeparserGenerator}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, CodeAwareInstructionExecutorWithListeners}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.scalatest.FunSuite
import org.change.v2.analysis.memory.TagExp.IntImprovements

class SwitchPerformanceTesting  extends FunSuite {

  test("SWITCH - L3Ipv4 with trivial deparser & deterministic parser") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L3Ipv4Test.txt"
    val port = 1
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, ethernetIp4TcpPacket,
      "parse_ethernet.parse_ipv4.parse_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, ethernetIp4TcpPacket,
      "parse_ethernet.parse_ipv4.parse_tcp"))
    println(cruntime + "," + sruntime + "," + "L3Ipv4")
  }

  test("SWITCH - L2VxlanTunnelTest with trivial deparser & deterministic parser encap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L2VxlanTunnelTest.txt"
    val port = 1
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port,
      (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_ipv4.parse_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port,
      (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_ipv4.parse_tcp"))
    println(cruntime +","+ sruntime + "," + "L2VxlanTunnelTest,encap")

  }

  test("SWITCH - L2VxlanTunnelTest with trivial deparser & deterministic parser decap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L2VxlanTunnelTest.txt"
    val port = 2
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpTcp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpTcp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_tcp"))
    println(cruntime +","+ sruntime + "," + "L2VxlanTunnelTest,decap")
  }

  test("SWITCH - L3VxlanTunnelTest with trivial deparser & deterministic parser decap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L3VxlanTunnelTest.txt"
    val port = 1
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpTcp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpTcp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_tcp"))
    println(cruntime +","+ sruntime + "," + "L3VxlanTunnelTest,decap")

  }

  test("SWITCH - L3VxlanTunnelTest with trivial deparser & deterministic parser encap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L3VxlanTunnelTest.txt"
    val port = 2
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_ipv4.parse_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
          "parse_ethernet.parse_ipv4.parse_tcp"))
    println(cruntime +","+ sruntime + "," + "L3VxlanTunnelTest,encap")
  }

  test("SWITCH - L3VxlanTunnelTest with trivial deparser & deterministic parser double encap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L3VxlanTunnelTest.txt"
    val port = 2
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpUdp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_udp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, ethernetIp4UdpVxlanIpUdp,
      "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_udp"))
    println(cruntime +","+ sruntime + "," + "L3VxlanTunnelTest,2encap")
  }

  test("SWITCH - L2QinQTest with trivial deparser & deterministic parser encap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L2QinQTest.txt"
    val port = 2
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_ipv4.parse_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_ipv4.parse_tcp"))
    println(cruntime +","+ sruntime + "," + "L2QinQTest,encap")
  }

  test("SWITCH - L2QinQTest with trivial deparser & deterministic parser decap") {
    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val dataplane = s"$dir/pd-L2QinQTest.txt"
    val port = 1
    val (_, cruntime) = runAndLog(() => setupAndRunSwitchWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_qinq.parse_qinq_vlan.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_qinq.parse_qinq_vlan.parse_ipv4.parse_tcp"))
    val (_, sruntime) = runAndLog(() => setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, (x) => x == "parse_ethernet.parse_qinq.parse_qinq_vlan.parse_ipv4.parse_tcp",
      "parse_ethernet.parse_qinq.parse_qinq_vlan.parse_ipv4.parse_tcp"))
    println(cruntime +","+ sruntime + "," + "L2QinQTest,encap")
  }

  def ethernetIp4UdpVxlanIpTcp(x : String) : Boolean =
    x == "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_tcp"
  def ethernetIp4UdpVxlanIpUdp(x : String) : Boolean =
    x == "parse_ethernet.parse_ipv4.parse_udp.parse_vxlan.parse_inner_ethernet.parse_inner_ipv4.parse_inner_udp"

  def ethernetIp4TcpPacket(x : String): Boolean =  x.contains("parse_ethernet") &&
    x.contains("parse_ipv4") &&
    x.contains("parse_tcp")


  def setupAndRunSwitchWithSimpleParser(dir : String, p4 : String, dataplane : String,
                                  port : Int,
                                  stringFilter : (String) => Boolean,
                                  layout : String,
                                  postParser : Instruction = NoOp): Unit = {
    this.setupAndRunSwitchWithParser(dir, p4, dataplane, port, postParser, layout, (sw, switchInstance) => new SkipParserAndDeparser(switch = sw,
      switchInstance = switchInstance,
      codeFilter = Some(stringFilter)
    ))
  }


  def setupAndRunSwitchWithParser(dir : String, p4 : String, dataplane : String,
                                  port : Int,
                                  postParser : Instruction = NoOp,
                                  packetLayout : String,
                                  factory : Function2[Switch, ISwitchInstance, ParserGenerator]): Unit = {
    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2",
      2 -> "veth4", 3 -> "veth6",
      4 -> "veth8", 5 -> "veth10",
      6 -> "veth12", 7 -> "veth14",
      8 -> "veth16", 64 -> "veth250"
    )
    setupAndRun(dir, p4, dataplane, postParser, ifaces, dir, packetLayout, port, factory)
  }


  def setupAndRunSwitchSymWithSimpleParser(dir : String, p4 : String, dataplane : String,
                                        port : Int,
                                        stringFilter : (String) => Boolean,
                                        layout : String,
                                        postParser : Instruction = NoOp): Unit = {
    this.setupAndRunSwitchSymWithSimpleParser(dir, p4, dataplane, port, postParser, layout, (sw, switchInstance) => new SkipParserAndDeparser(switch = sw,
      switchInstance = switchInstance,
      codeFilter = Some(stringFilter)
    ))
  }


  def setupAndRunSwitchSymWithSimpleParser(dir : String, p4 : String, dataplane : String,
                                  port : Int,
                                  postParser : Instruction,
                                  packetLayout : String,
                                  factory : Function2[Switch, ISwitchInstance, ParserGenerator]): Unit = {
    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2",
      2 -> "veth4", 3 -> "veth6",
      4 -> "veth8", 5 -> "veth10",
      6 -> "veth12", 7 -> "veth14",
      8 -> "veth16", 64 -> "veth250"
    )
    setupAndRun(dir, p4, dataplane, postParser, ifaces, dir, packetLayout, port, factory, useSyms = true, forceSyms = true)
  }


}
