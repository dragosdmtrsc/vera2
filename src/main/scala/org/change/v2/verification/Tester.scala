package org.change.v2.verification

import java.io.{BufferedOutputStream, File, FileOutputStream, PrintStream}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.parser.p4.{ControlFlowInterpreter, P4ExecutionContext}
import org.change.utils.prettifier.JsonUtil
import org.change.v2.abstractnet.optimized.macswitch.OptimizedSwitch
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.executor.{AbstractInstructionExecutor, CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.expression.concrete.{ConstantStringValue, ConstantValue}
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.executor.clickabstractnetwork.AggregatedBuilder._
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.util.canonicalnames._
import org.change.v2.verification.Formula.Formula
import org.change.v2.verification.Policy._
import org.change.v2.verification.Tester._

// import parser.p4.test.{executeAndPrintStats, printResults}

//import org.change.v2.verification.Formula._


/**
 * Created by matei on 12/01/17.
 */



trait Test {
  def execute : Boolean
}

/*
case class TopoTest (msg:String, policy:Formula, start: LocationId, topo:Topo, expected:Boolean) extends Test
{
  def execute : Boolean = {
    println("Running "+msg);
    (verify(policy,start,code(topo),links(topo)),expected) match {
      case (true,true) | (false,false) => println("Test passed "); true
      case (x,y) => println("Test failed, expected "+y+" received "+x); false
    }
  }
}*/

/*
class SeflTest (msg:String, policy:Formula, prog:Instruction, expected:Boolean)
{
  def execute : Boolean = {
    println("Running "+msg);
    (verify(policy,prog),expected) match {
      case (true,true) | (false,false) => println("Test passed "); true
      case (x,y) => println("Test failed, expected "+y+" received "+x); false
    }
  }
}*/


object Tester {
  type Topo = (Map[String,Instruction],Map[LocationId,LocationId])
  def code (t:Topo) = t._1
  def links (t:Topo) = t._2


  def time[T](block : => T) : T = {
    val t0 = System.nanoTime()
    val result = block
    val t1 = System.nanoTime()
    println("Elapsed time: "+ (t1 - t0)/1000000 + "ms")
    result
  }

  def thousandip : Instruction = {
    InstructionBlock ((1 to 1000).toList.map((i : Int) => Constrain(IPSrc,:~:(:==:(ConstantValue(i))))))
  }

  def verifyP4NAT[T<:ISwitchInstance] (param : String, f: Formula, start: LocationId, ib : Instruction, res : ControlFlowInterpreter[T]) : List[PolicyLogger] = {
    param match {
      // run verification on all non-cpu-encapsulated packets
      case "default" =>
        var exe:AbstractInstructionExecutor = CodeAwareInstructionExecutor(res.instructions(),res.links(),new Z3BVSolver)

        var (l, _) = exe.execute(ib,State.clean,true);
        l = l.slice(3,6)

        l.map ((s:State) => {
          var (x,y) = check(f,res.instructions()(start),new MapState(res.instructions(),res.links(),s,exe),new PolicyLogger(start))
          println("\n =================\n Formula is ",x.status)
          y
        })

    }
  }

  def copytocpu_port_reached = {

    val dir = "inputs/copy-to-cpu-star/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands_star.txt"
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"),
      Map[Int, Int](250 -> 3), Switch.fromFile(p4), dataplane)
    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue("router.input.1")),
      Forward("router.input.1")
    )

    //Printer.vizPrinter((res.instructions(),res.links),"p4copy_to_cpu_original.html");

    var f : Formula = EF(Constrain("CurrentPort",:==:(ConstantStringValue("router.deparser.out"))))

    var log_list = verifyP4(f,"router.input.1",ib,res)

    var log = log_list(0);
    Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_0.html");

    /*

    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_"+i+".html");
      i += 1
    }


*/

  }

  def copytocpu_headerfield_unchanged = {
    val dir = "inputs/copy-to-cpu-star/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands_star.txt"
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"),
      Map[Int, Int](250 -> 3), Switch.fromFile(p4), dataplane)
    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)

    val etherDst = (Tag("START")+0)

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue("router.input.1")),
      Allocate("StoredEtherDst",48),
      Assign("StoredEtherDst",:@(etherDst)) //dest ether
      //Forward("router.input.1")
    )

    //Printer.vizPrinter((res.instructions(),res.links),"p4copy_to_cpu_original.html");

    // A => B
    var f : Formula = AG(Fork(
      Constrain("CurrentPort",:~:(:==:(ConstantStringValue("router.output.3")))),
      ConstrainRaw(etherDst,:==:(:@("StoredEtherDst")))
    ))

    //EF(Constrain("CurrentPort",:==:(ConstantStringValue("router.deparser.out"))))

    var log_list = verifyP4(f,"router.input.1",ib,res)

    //var log = log_list(0);
    //Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_0.html");

    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_"+i+".html");
      i += 1
    }
  }

  def copytocpu_cpu_packets_dropped = {

    val dir = "inputs/copy-to-cpu-star/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands_star.txt"
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"),
      Map[Int, Int](250 -> 3), Switch.fromFile(p4), dataplane)
    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue("router.input.1"))
      //Forward("router.input.1")
    )

    //Printer.vizPrinter((res.instructions(),res.links),"p4copy_to_cpu_original.html");

    // A => B
    var f : Formula = AG(Constrain("CurrentPort",:~:(:==:(ConstantStringValue("router.output.3")))))

    //EF(Constrain("CurrentPort",:==:(ConstantStringValue("router.deparser.out"))))

    var log_list = verifyP4(f,"router.input.1",ib,res)

    //var log = log_list(1);
    //Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_1.html");


    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_"+i+".html");
      i += 1
    }

  }

  def copytocpu_cpu_encapsulation = {

    val dir = "inputs/copy-to-cpu-star/"
    val p4 = s"$dir/copy_to_cpu-ppc.p4"
    val dataplane = s"$dir/commands_star.txt"
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 3 -> "cpu"),
      Map[Int, Int](250 -> 3), Switch.fromFile(p4), dataplane)
    val res = new ControlFlowInterpreter(switchInstance, switchInstance.switch)

    val ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue("router.input.1"))
      //Forward("router.input.1")
    )

    //Printer.vizPrinter((res.instructions(),res.links),"p4copy_to_cpu_original.html");

    // A => B
    var f : Formula = AG(Fork(
      Constrain("CurrentPort",:~:(:==:(ConstantStringValue("router.output.3")))),
      InstructionBlock(Exists(Tag("START")+0),Exists(Tag("START")+48),Exists(Tag("START")+56))
      //Exists(Tag("START")+56)
      //assert cpu encapsulation
    ))

    //EF(Constrain("CurrentPort",:==:(ConstantStringValue("router.deparser.out"))))

    var log_list = verifyP4(f,"router.input.1",ib,res)

    //var log = log_list(1);
    //Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_1.html");


    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"p4copy_to_cpu_"+i+".html");
      i += 1
    }

  }

  def nat_test = {
    val dir = "inputs/simple-nat-testing/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
    val port = 1

    var ib = InstructionBlock(
      res.allParserStatesInstruction(),
      //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
      Assign("CurrentPort",ConstantStringValue("router.input.1"))//,
      //Forward("router.input.1")
    )

    var log_list = verifyP4(EF(Formula.Fail),"router.input.1",ib,res)

    //var log = log_list(1);
    //Printer.vizPrinter((res.instructions(),res.links),"p4nat_original.html");

    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"p4nat_"+i+".html");
      i += 1
    }



  }

  /*
  if no 'miss' rules are inserted, packets are dropped
   */
def nat_default_drop = {
  val dir = "inputs/simple-nat-testing/"
  val p4 = s"$dir/simple_nat-ppc.p4"
  val dataplane = s"$dir/commands.txt"
  val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")
  val port = 1

  var ib = InstructionBlock(
    res.allParserStatesInstruction(),
    //      Constrain(Tag("START") + 0, :~:(:==:(ConstantValue(0)))),
    Assign("CurrentPort",ConstantStringValue("router.input.1"))//,
    //Forward("router.input.1")
  )

  var log_list = verifyP4NAT("default",EF(Formula.Fail),"router.input.1",ib,res)

  //var log = log_list(1);
  //Printer.vizPrinter((res.instructions(),res.links),"p4nat_original.html");

  var i = 0
  for (log <- log_list) {
    Printer.vizPrinter((log.code,log.links),"t0_nat_default_drop_"+i+".html");
    i += 1
  }
}

def nat_star_to_cpu = {
  val dir = "inputs/simple-nat-testing/"
  val p4 = s"$dir/simple_nat-ppc.p4"
  val dataplane = s"$dir/commands.txt"
  val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"),
    Map[Int, Int](250 -> 11), Switch.fromFile(p4), dataplane)

  val res = ControlFlowInterpreter.buildSymbolicInterpreter(switchInstance, switchInstance.switch)

  var ib = InstructionBlock(
    res.allParserStatesInstruction(),
    res.initFactory(switchInstance)
  )

  var log_list = verifyP4NAT("default",EF(Formula.Fail),"router.input.1",ib,res)

  //var log = log_list(1);
  //Printer.vizPrinter((res.instructions(),res.links),"p4nat_original.html");

  var i = 0
  for (log <- log_list) {
    Printer.vizPrinter((log.code,log.links),"t0_nat_to_cpu_"+i+".html");
    i += 1
  }
}

def nat_drop_from_external_when_no_mapping = {
  val dir = "inputs/simple-nat-testing/"
  val p4 = s"$dir/simple_nat-ppc.p4"
  val dataplane = s"$dir/commands.txt"
  val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"),
    Map[Int, Int](250 -> 11), Switch.fromFile(p4), dataplane)

  val res = ControlFlowInterpreter.buildSymbolicInterpreter(switchInstance, switchInstance.switch)

  var ib = InstructionBlock(
    res.allParserStatesInstruction(),
    res.initFactory(switchInstance)
  )

  var log_list = verifyP4NAT("default",EF(Formula.Fail),"router.input.2",ib,res)

  //var log = log_list(1);
  //Printer.vizPrinter((res.instructions(),res.links),"p4nat_original.html");

  var i = 0
  for (log <- log_list) {
    Printer.vizPrinter((log.code,log.links),"t0_drop_from_external_when_no_mapping_"+i+".html");
    i += 1
  }
}

  def nat_star_actions_from_int = {
    val dir = "inputs/simple-nat-testing/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val dataplane = s"$dir/commands.txt"
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("router", Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"),
      Map[Int, Int](250 -> 11), Switch.fromFile(p4), dataplane)

    val res = ControlFlowInterpreter.buildSymbolicInterpreter(switchInstance, switchInstance.switch)

    //println("Instruction ",res.instructions().find((r) => r._1.startsWith("router.table.nat.in.")).get)

    var ib = InstructionBlock(
      res.allParserStatesInstruction(),
      res.initFactory(switchInstance)
    )

    var log_list = verifyP4NAT("default",EF(Formula.Fail),"router.input.1",ib,res)

    //var log = log_list(1);
    //Printer.vizPrinter((res.instructions(),res.links),"p4nat_original.html");

    /*
    var i = 0
    for (log <- log_list) {
      Printer.vizPrinter((log.code,log.links),"t1_star_actions_from_int_"+i+".html");
      i += 1
    }
    */

  }


  def dragos = {

    // ----------------------------
    // copy to cpu policies
    // ----------------------------

    //copytocpu_headerfield_unchanged
    //copytocpu_cpu_packets_dropped
    //copytocpu_cpu_encapsulation

    // ----------------------------
    // nat policies
    // ----------------------------

    //nat_default_drop
    //nat_star_to_cpu
    //nat_drop_from_external_when_no_mapping
    nat_star_actions_from_int


    /*
       var topo = Map (
         "A" -> Fork(Forward("X"),Forward("B"),Forward("C")),
         "X" -> Fork(InstructionBlock(NoOp,NoOp,Fail("X"))),
         "B" -> InstructionBlock(Fork(NoOp,NoOp),Fail("Fin")),
         "Fin" -> NoOp,
         "Syn" -> NoOp,
         "C" -> InstructionBlock(Fork(NoOp,Fork(InstructionBlock(NoOp,Fail("1")),InstructionBlock(NoOp,Fail("2")))),Forward("D")),
         "D" -> Fork(InstructionBlock(NoOp,Forward("None1")),InstructionBlock(NoOp,Forward("None2")))
       )

       var log = verify(EF(Constrain("CurrentPort",:==:(ConstantStringValue("D")))),"A",topo,Map[LocationId,LocationId]())
       Printer.vizPrinter((log.code,log.links),"test.html");
    */








  }

  def main(args: Array[String]): Unit = {


    var tests = List (
      //TopoTest("Existential test 1 on hop-based impl",EF(Constrain(IPDst,:==:(ConstantValue(100)))),"0",sample_topo(model_1),false)
      //TopoTest("Check-sequence test",EF(Constrain(IPDst,:==:(ConstantValue(100)))),"0",linear_topo,false)
      //TopoTest("Existential test 2 on hop-based impl",EF(Constrain(TcpSrc,:==:(ConstantValue(5)))),"0",sample_topo(model_1),true),
      //TopoTest("Universal test 3 on hop-based impl",AF(Constrain(TcpSrc,:==:(ConstantValue(5)))),"0",sample_topo(model_1),false)

      // this test currently fails
      //TopoTest("Universal test 4 on hop-based impl",AF(Constrain(TcpSrc,:>=:(ConstantValue(5)))),"0",sample_topo(model_1),false)
      //TopoTest("Simple history test",EF(Constrain(IPDst,:>=:(ConstantValue(11)))),"0",tiny_topo_2,true)
    )


  /*
    for (t <- tests){
      t.execute
    }
*/



                     // asa
    dragos

    //var (v,logger) = verify(EF(Formula.Fail), "0", code(tiny_topo),links(tiny_topo))
    //Printer.vizPrinter((logger.code,logger.links),"policy.html")

    //println(logger.code)
    //println(logger.links)


    //println(tiny_topo)
  }


  /*
   {IPSrc >= 5 ; TCPSrc >= 5} ||
   {{IPSrc = 3 || IPSrc = 4 || IPSrc = 5} ; TCPSrc = 5} ||
   {IPSrc == 5 ; {TCPSrc >= 3 || TCPSrc >= 4 || TCPSrc >=5}}


   which is flattened to:
    {IPSrc >= 5 ; TCPSrc >= 5} ||
    {IPSrc = 3 ; TCPSrc = 5} ||
    {IPSrc = 4; TCPSrc = 5} ||
    {IPSrc = 5; TCPSrc = 5}  ||
    {IPSrc == 5 ;TCPSrc >= 3} ||
    {IPSrc == 5 ; TCPSrc >= 4} ||
    {IPSrc == 5 ; TCPSrc >=5}

    */


  def model_1 = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
    ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
    ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
  )
  /*
   {IPSrc >= 5 ; TCPSrc >= 5} ||
   {{IPSrc = 3 || IPSrc = 4 || IPSrc = 5} ; TCPSrc = 5} ||
   {IPSrc == 5 ; {TCPSrc >= 3 || TCPSrc >= 4 || TCPSrc >=5}}
    */
  def model_2 = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
    ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
    ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
  )

  def sample_topo(model:Instruction) : Topo = (Map("0" -> InstructionBlock(model, Forward("1")),
                                "11" -> InstructionBlock(Constrain(TcpDst, :>=:(ConstantValue(10))), Forward("2")),
                                "22" -> InstructionBlock(Assign(IPDst,ConstantValue(100)), Assign(TcpDst,ConstantValue(0)))), Map("1" -> "11", "2" -> "22"):Map[LocationId,LocationId])

  def linear_topo : Topo =
    ( Map("0" -> InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(11))),Forward("1")),
          "11" -> InstructionBlock(Constrain(IPDst,:==:(ConstantValue(12))),Forward("2")),
          "22" -> InstructionBlock(NoOp)
    )
      ,
      Map("1" -> "11", "2" -> "22"))


  def tiny_topo : Topo =

    (Map("0" -> InstructionBlock(Assign(IPSrc,ConstantValue(10)),Fork(Forward("1"),Forward("2"))),
        "11" -> InstructionBlock(NoOp,Forward("3")),
        "33" -> InstructionBlock(Fork(InstructionBlock(Assign(IPDst,ConstantValue(10)),Forward("5")),Forward("6"))),
        "22" -> InstructionBlock(Constrain(IPSrc,:>=:(ConstantValue(4))), Forward("4")),
        "44" -> InstructionBlock(NoOp),
        "55" -> InstructionBlock(Assign(IPDst,ConstantValue(11)),Forward("6")),
        "66" -> NoOp
    ),Map("1" -> "11", "2" -> "22", "3" -> "33", "4" -> "44", "5" -> "55", "6" -> "66"):Map[LocationId,LocationId])

  def tiny_topo_2 : Topo =
    (Map("0" -> Fork(Forward("1"),Forward("2"),Forward("3")),
         "1" -> InstructionBlock(NoOp,Assign(IPDst,ConstantValue(11)),Forward("11")),
         "2" -> InstructionBlock(NoOp,Assign(IPDst,ConstantValue(22)),Forward("22")),
         "3" -> InstructionBlock(NoOp,Assign(IPDst,ConstantValue(3)),Forward("33")),
         "4" -> InstructionBlock(NoOp),
         "11" -> InstructionBlock(Forward("4")),
         "22" -> InstructionBlock(Assign(IPDst,ConstantValue(11)),Forward("4")),
         "33" -> InstructionBlock(Assign(IPDst,ConstantValue(5)),Forward("11"))
    ),Map():Map[LocationId,LocationId])






  def asa = {
    val dataPlaneFolder = "src/main/resources/asa"
    //var policy = EF(Constrain(IPSrc,:==:(ConstantValue(RepresentationConversion.ipToNumber("10.0.0.0")))))
    //var policy = EF(Formula.Fail)
    var policy = EF(Constrain(VLANTag,:==:(ConstantValue(999))))
    //var policy = EF(Assign(VLANTag,ConstantValue(999))))
    //var policy = EF(Constrain(EtherDst,:==:(ConstantValue(RepresentationConversion.macToNumberCiscoFormat("0023.ebbb.f14d")))))


    var exe = executorFromFolder(new File(dataPlaneFolder), Map(
      "switch" -> OptimizedSwitch.trivialSwitchNetworkConfig _,
      "click" -> {f => ClickToAbstractNetwork.buildConfig(f, prefixedElements = true)},
      "router" -> OptimizedRouter.trivialRouterNetwrokConfig _
    )).setLogger(JsonLogger)


    var log = verify(EF(Formula.Fail),"packet-in-0-in",exe.instructions,exe.links)
    Printer.vizPrinter((log.code,log.links),"asa.html");

    //val start = System.currentTimeMillis()
    //exe = time{exe.untilDone(true)}

/*
    println("=== Successful states ===")
    println(exe.okStates.length)
    println("=== Stuck states ===")
    println(exe.stuckStates.length)
    println("=== Failed states ===")
    println(exe.failedStates)
*/

    //    println(System.currentTimeMillis() - start)
    //println("It took me:" + (System.currentTimeMillis() - start))

    //in -> EtherDecap -> EtherEncap(2048, 00:23:eb:bb:f1:4c, 00:23:eb:bb:f1:4d) -> VLANEncap(225) -> out;

    /*
    var r = false;
    time{(r,_) = verify(policy,"packet-in-0-in",exe.instructions,exe.links)}
    println("Formula is "+r)
    */

    /*
    // for checking the update on Constrain which checks allocation first
    var s0 = state("", exe.instructions,exe.links).state
    //println(s0.memory)
    println("Final:");
    VLANTag(s0)
    println("Execute:");
    var res = ConstrainRaw(VLANTag,:==:(ConstantValue(0))).apply(s0)
    println("Res:"+res)
    */




    //packet:out:0 -> asa:main_input:0
    //packet:out:0 -> dump:in:0
    //in -> EtherEncap(2048, 00:23:eb:bb:f1:4c, 00:23:eb:bb:f1:4d) -> VLANEncap(225) -> out;



  }


  def topology = {

    /*

    {IPSrc >= 5 ; TCPSrc >= 5} ||
    {{IPSrc = 3 || IPSrc = 4 || IPSrc = 5} ; TCPSrc = 5} ||
    {IPSrc == 5 ; {TCPSrc >= 3 || TCPSrc >= 4 || TCPSrc >=5}}

    which is flattened to:
    {IPSrc >= 5 ; TCPSrc >= 5} ||
    {IPSrc = 3 ; TCPSrc = 5} ||
    {IPSrc = 4; TCPSrc = 5} ||
    {IPSrc = 5; TCPSrc = 5}  ||
    {IPSrc == 5 ;TCPSrc >= 3} ||
    {IPSrc == 5 ; TCPSrc >= 4} ||
    {IPSrc == 5 ; TCPSrc >=5}

     */

    var model = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
      ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
      ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
    )


    var t = Map("0" -> InstructionBlock(model, Forward("1")),
                "1" -> InstructionBlock(Constrain(TcpDst, :>=:(ConstantValue(10))), Forward("2")),
                "2" -> InstructionBlock(Assign(IPDst,ConstantValue(100)), Assign(TcpDst,ConstantValue(0)))
    )
    var links : Map[LocationId,LocationId] = Map()
    var policy = EF(Constrain(IPDst,:==:(ConstantValue(100))))
    verify(policy,"0",t,links)


  }



  def costin_example = {

    var model = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
      ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
      ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
    )



    var policy = And(AG (Or(Constrain(IPSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(3))))),
                 Or(EF(Constrain(IPSrc,:>=:(ConstantValue(5)))),EF(Constrain(TcpSrc,:>=:(ConstantValue(5))))))




  }

  def fork_checker = {

    // {IPSrc == 1 ; { {IPSrc >= 21 || IPSrc >= 22 } || IPSrc >= 9 } ; IPSrc >= 3 } || {IPSrc >= 3 || IPSrc >= 4}

    var model = Fork(InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Fork(Fork(Constrain(IPSrc, :>=:(ConstantValue(21))),Constrain(IPSrc, :>=:(ConstantValue(22)))),Constrain(IPSrc, :>=:(ConstantValue(9)))),
      Constrain(IPSrc, :>=:(ConstantValue(3)))
    ),
      Fork(Constrain(IPSrc, :>=:(ConstantValue(4))),Constrain(IPSrc, :>=:(ConstantValue(5))))
    )

    var policy = AG(EF(Constrain(IPSrc,:>=:(ConstantValue(2)))))

  }


}

