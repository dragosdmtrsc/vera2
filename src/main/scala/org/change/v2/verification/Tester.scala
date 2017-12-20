package org.change.v2.verification

import java.io.File

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.v2.abstractnet.optimized.macswitch.OptimizedSwitch
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.executor.clickabstractnetwork.AggregatedBuilder._
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.util.canonicalnames._
import org.change.v2.verification.Formula.Formula
import org.change.v2.verification.Policy._
import org.change.v2.verification.Tester._

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


    //costin_example
    //topology
    //future_test
    //println(State.bigBang)

                      asa
    //iftest
    //ib test
    //ibtest

    /*
    var (v,logger) = verify(EF(Formula.Fail), "0", code(tiny_topo),links(tiny_topo))
    Printer.vizPrinter((logger.code,logger.links),"policy.html")

    println(logger.code)
    println(logger.links)
  */

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


    var (v,log) = verify(EF(Formula.Fail),"packet-in-0-in",exe.instructions,exe.links)
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

  def ibtest = {
    var model = InstructionBlock(Assign(IPSrc,ConstantValue(5)),
      Assign(IPDst,ConstantValue(6)),
      Assign(TcpSrc,ConstantValue(7)),
      Assign(TcpDst,ConstantValue(8)))

    var policy = AF(Constrain(IPSrc,:>:(ConstantValue(9))))
    verify(policy,model)

    /*
    var s = Policy.state
    var sp = s.execute(Assign(IPSrc,ConstantValue(666)))
    println("History "+sp.state.instructionHistory)
    */


  }

  def iftest = {
    var model = InstructionBlock(Assign(IPSrc,ConstantValue(5)),
                                If(Constrain(IPDst,:==:(ConstantValue(10))),
                                        Assign(IPSrc,ConstantValue(10)),
                                        Assign(IPSrc, ConstantValue(9))))
    var policy = AF(Constrain(IPSrc,:>:(ConstantValue(4))))
    verify(policy,model)
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

  def future_test = {
    var model = InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
    verify (EF(Constrain(IPDst,:==:(ConstantValue(100)))),model)
  }

  def costin_example = {

    var model = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
      ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
      ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
    )



    var policy = And(AG (Or(Constrain(IPSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(3))))),
                 Or(EF(Constrain(IPSrc,:>=:(ConstantValue(5)))),EF(Constrain(TcpSrc,:>=:(ConstantValue(5))))))


    verify(policy,model)


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
    verify(policy,model)


  }


}

