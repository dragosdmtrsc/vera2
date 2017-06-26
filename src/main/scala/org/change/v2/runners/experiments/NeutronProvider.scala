package org.change.v2.runners.experiments

import java.io.PrintStream

import scala.collection.JavaConversions.asScalaBuffer

import org.change.v2.abstractnet.linux.iptables.IPTablesConstants
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions.varString
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.linux.ovs.PacketMirror
import org.change.v2.abstractnet.neutron.CSVBackedNetworking
import org.change.v2.analysis.executor.solvers.ConstraintLogger
import org.change.v2.analysis.executor.solvers.LogSolver
import org.change.v2.analysis.executor.OVSExecutor
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.z3.Z3Util
import org.change.v2.model.WorldModel
import org.change.v2.util.canonicalnames.EtherDst
import org.change.v2.util.canonicalnames.EtherSrc
import org.change.v2.util.canonicalnames.IPDst
import org.change.v2.util.canonicalnames.IPSrc
import org.change.v2.util.conversion.RepresentationConversion.ipToNumber
import org.change.v2.util.conversion.RepresentationConversion.macToNumber
import org.change.v2.analysis.executor.OVSAsyncExecutor
import org.change.v2.analysis.executor.OVSLogExecutor
import org.change.utils.prettifier.JsonUtil

object IngressToMachine
{
  
  def main(argv : Array[String]) {
    val ipsrc = "8.8.8.8"
    val enterIface = "eth2"
    val pcName = "controller"
    val ipDst = "203.0.113.104"
    // ensure that we aim for qg...
    val ethDst = "fa:16:3e:9c:99:77"
    
    val expName = "fip"
    val dir = "stack-inputs/generated2"
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    val world = WorldModel.fromFolder(dir)
    psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
    init = System.currentTimeMillis()
    val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
          x.getNamespaces.map { y => InstructionBlock(
              Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
              Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
              Assign("IsTracked".scopeTo(y), ConstantValue(0))
            )
          }
        }
      } ++ world.getPcs.flatMap { x => {
          x.getCtZones.map { y => {
              val prefix = x.getName + "." + y.intValue
              InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
              )
            }
          }
        }
      }
    )
    
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true)),
        Assign(EtherDst, ConstantValue(macToNumber(ethDst), isMac = true))
      )(State.bigBang, true)._1
    init =System.currentTimeMillis()
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(enterIface)
    val ns = pc.getNamespaceForNic(enterIface)
    
    //ingress example
    val iib = new EnterIface(pcName, enterIface, world).generateInstruction
    val worldModel = world
    val iip = InstructionBlock(
      Assign("InputInterface", ConstantStringValue(enterIface)),
      Allocate("OutputInterface"),
      iib
    )
    val psCode = new PrintStream(s"$dir/generated-code-$expName.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    val psout = new PrintStream(s"$dir/generated-$expName.out")
    val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
    val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
    
    init = System.currentTimeMillis()
    val okProvider = initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
      System.out.println(System.currentTimeMillis() - init)
      psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Forward number of states: " + ok.size + "," + fail.size)
      psout.println(JsonUtil.toJson(ok))
      psFailed.println(JsonUtil.toJson(fail))
      val success = ok.filter { x => x.history.head.startsWith("DeliveredLocallyVM") }
      init = System.currentTimeMillis()
      val ib = 
          success.foldLeft((List[State](), List[State]())){ (acc, x) => 
            "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
                findAllIn(x.history.head).matchData.foldRight(acc) {
                  (r, acc2) => {
                    val tap = r.group(1)
                    val machine = r.group(2)
                    println(s"Entering $tap at $machine")
                    val ii = InstructionBlock(
                        PacketMirror(),
                        Assign("InputInterface", ConstantStringValue(tap)),
                        new EnterIface(machine, tap, world).generateInstruction()
                    )(x, true)
                    (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                }
            }
          }
      psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
      val (okRev, failedRev) = ib
      psOutRev.println(JsonUtil.toJson(okRev))
      psFailedRev.println(JsonUtil.toJson(failedRev))
      ok
    }
    psOutRev.close()
    psFailedRev.close()
    psStats.close()
    psout.close()
    psFailed.close()
//    
//    
//    val okTenant = runInbound(dir)
//    EquivalenceProver((l1, l2) => {
//      
//    }).areEquivalent(okTenant, )
//    
//    
  }
}



object L2Connectivity
{
  
  def experiment(enterIface : String, 
      pcName : String,
      dir : String,
      codeFirst : Instruction = NoOp,
      exp : String = "eastwest") = {
    val expName = s"$exp-$enterIface-$pcName"
    try
    {
      var init = System.currentTimeMillis()
      val psStats = new PrintStream(s"$dir/stats-$expName.txt")
      val world = WorldModel.fromFolder(dir)
      psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
      init = System.currentTimeMillis()
      val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
            x.getNamespaces.map { y => InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
                Assign("IsTracked".scopeTo(y), ConstantValue(0)),
                Assign("SNAT.IsSNAT".scopeTo(y), ConstantValue(0)),
                Assign("DNAT.IsDNAT".scopeTo(y), ConstantValue(0))
              )
            }
          }
        } ++ world.getPcs.flatMap { x => {
            x.getCtZones.map { y => {
                val prefix = x.getName + "." + y.intValue
                InstructionBlock(
                  Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                  Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                  Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
                )
              }
            }
          }
        }
      )
      val clogger = new ConstraintLogger(s"$dir/$expName-profiling.csv")
      def decorated = new OVSLogExecutor(new LogSolver(new Z3Solver()), clogger)
      val initials = 
        InstructionBlock(
          State.eher,
          State.tunnel,
          initcode,
          codeFirst,
          Assign("IsUnicast", ConstantValue(1))
        )(State.bigBang, true)._1
      init =System.currentTimeMillis()
      val pc = world.getComputer(pcName)
      val nic = pc.getNic(enterIface)
      val ns = pc.getNamespaceForNic(enterIface)
      
      //ingress example
      val iib = new EnterIface(pcName, enterIface, world)
      val worldModel = world
  //    val iib = new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel).generateInstruction()
      val iip = InstructionBlock(
        Assign("InputInterface", ConstantStringValue(enterIface)),
        Allocate("OutputInterface"),
        iib
      )
      val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
      val psout = new PrintStream(s"$dir/generated-$expName.out")
      val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
      val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
      
      init = System.currentTimeMillis()
      initials.map { initial => 
        val (ok, fail) =  decorated.execute(iip, initial, true)
        System.out.println(System.currentTimeMillis() - init)
        psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Forward number of states: " + ok.size + "," + fail.size)
        psout.println(JsonUtil.toJson(ok))
        psFailed.println(JsonUtil.toJson(fail))
        val success = ok.filter { x => x.history.head.startsWith("DeliveredLocallyVM") }
        init = System.currentTimeMillis()
        val ib = 
            success.foldLeft((List[State](), List[State]())){ (acc, x) => 
              "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
                  findAllIn(x.history.head).matchData.foldRight(acc) {
                    (r, acc2) => {
                      val tap = r.group(1)
                      val machine = r.group(2)
                      println(s"Entering $tap at $machine")
                      val ii = decorated.execute(InstructionBlock(
                          PacketMirror(),
                          Assign("InputInterface", ConstantStringValue(tap)),
                          new EnterIface(machine, tap, world)
                      ), x, true)
                      (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                  }
              }
            }
        psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
        val (okRev, failedRev) = ib
        psOutRev.println(JsonUtil.toJson(okRev))
        psFailedRev.println(JsonUtil.toJson(failedRev))
      }
      psOutRev.close()
      psFailedRev.close()
      psStats.close()
      psout.close()
      psFailed.close()
      clogger.close()
    }
    catch {
      case e : Exception => e.printStackTrace()
    }
    
  }
  
  
  def experimentAsync(enterIface : String, 
      pcName : String,
      dir : String,
      codeFirst : Instruction = NoOp,
      exp : String = "eastwest-async",
      threads : Int = 4) : Unit = {
    
    val expName = s"$exp-$enterIface-$pcName-$threads"
    try
    {
      var init = System.currentTimeMillis()
      val psStats = new PrintStream(s"$dir/stats-$expName.txt")
      psStats.println(s"Experiment:$expName\nWith $threads threads")
      val world = WorldModel.fromFolder(dir)
      psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
      init = System.currentTimeMillis()
      val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
            x.getNamespaces.map { y => InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
                Assign("IsTracked".scopeTo(y), ConstantValue(0)),
                Assign("SNAT.IsSNAT".scopeTo(y), ConstantValue(0)),
                Assign("DNAT.IsDNAT".scopeTo(y), ConstantValue(0))
              )
            }
          }
        } ++ world.getPcs.flatMap { x => {
            x.getCtZones.map { y => {
                val prefix = x.getName + "." + y.intValue
                InstructionBlock(
                  Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                  Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                  Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
                )
              }
            }
          }
        }
      )
      val clogger = new ConstraintLogger(s"$dir/$expName-profiling.csv")
      val decorated = new OVSAsyncExecutor(new OVSExecutor(new Z3Solver()), threads)
      val initials = 
        InstructionBlock(
          State.eher,
          State.tunnel,
          initcode,
          codeFirst,
          Assign("IsUnicast", ConstantValue(1))
        )(State.bigBang, true)._1
      init =System.currentTimeMillis()
      val pc = world.getComputer(pcName)
      val nic = pc.getNic(enterIface)
      val ns = pc.getNamespaceForNic(enterIface)
      
      //ingress example
      val iib = new EnterIface(pcName, enterIface, world)
      val worldModel = world
  //    val iib = new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel).generateInstruction()
      val iip = InstructionBlock(
        Assign("InputInterface", ConstantStringValue(enterIface)),
        Allocate("OutputInterface"),
        iib
      )
      val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
      val psout = new PrintStream(s"$dir/generated-$expName.out")
      val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
      val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
      
      init = System.currentTimeMillis()
      initials.map { initial => 
        decorated.pushExec(iip, initial, true)
        decorated.waitOver()
        val (ok, fail) =  (decorated.getOks, decorated.getFails)
        decorated.clearResults
        System.out.println(System.currentTimeMillis() - init)
        psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Forward number of states: " + ok.size + "," + fail.size)
        psout.println(JsonUtil.toJson(ok))
        psFailed.println(JsonUtil.toJson(fail))
        val success = ok.filter { x => x.history.head.startsWith("DeliveredLocallyVM") }
        init = System.currentTimeMillis()
        val ib = 
            success.foldLeft((List[State](), List[State]())){ (acc, x) => 
              "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
                  findAllIn(x.history.head).matchData.foldRight(acc) {
                    (r, acc2) => {
                      val tap = r.group(1)
                      val machine = r.group(2)
                      println(s"Entering $tap at $machine")
                      decorated.pushExec(InstructionBlock(
                          PacketMirror(),
                          Assign("InputInterface", ConstantStringValue(tap)),
                          new EnterIface(machine, tap, world)
                      ), x, true)
                      decorated.waitOver()
                      val ii = (decorated.getOks, decorated.getFails)
                      decorated.clearResults
                      (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                  }
              }
            }
        psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
        val (okRev, failedRev) = ib
        psOutRev.println(JsonUtil.toJson(okRev))
        psFailedRev.println(JsonUtil.toJson(failedRev))
      }
      
      decorated.close()
      
      psOutRev.close()
      psFailedRev.close()
      psStats.close()
      psout.close()
      psFailed.close()
      clogger.close()
    }
    catch {
      case e : Exception => e.printStackTrace()
    }
  }
  
  def main(argv : Array[String]) {
    var ipsrc = "192.168.13.3"
    var enterIface = "tap72d5355f-c6"
    var pcName = "compute1"
    var dir = "stack-inputs/generated2"
    var neutroncfg = CSVBackedNetworking.loadFromFolder(dir)

    var ports = neutroncfg.getPorts.filter { x => x.getDeviceOwner == "compute:None" && x.getId.startsWith("72d5355f-c6") }
    
    ports.foreach { x => {
        ipsrc = x.getFixedIps.iterator().next().getIpAddress
        enterIface = "tap" + x.getId.take("72d5355f-c6".length())
        pcName = "compute1"
        val codeFirst = InstructionBlock(
          Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
//          Assign(IPDst, ConstantValue(ipToNumber("8.8.8.8"), isIp = true)),
//          Assign(EtherDst, ConstantValue(macToNumber("fa:16:3e:f8:03:e4"), isMac = true)),
          Assign(EtherSrc, ConstantValue(macToNumber(x.getMacAddress), isMac = true))
        )
//        experiment(enterIface, pcName, dir, codeFirst = codeFirst)
        experiment(enterIface, pcName, dir, codeFirst = codeFirst)
        Z3Util.z3Context.finalize()
        Z3Util.refreshCache
      }
    }
//    
//    
//    dir = "stack-inputs/generated4"
//    neutroncfg = CSVBackedNetworking.loadFromFolder(dir)
//
//    ports = neutroncfg.getPorts.filter { x => x.getDeviceOwner == "compute:None" }
//    ports.foreach { x => {
//        ipsrc = x.getFixedIps.iterator().next().getIpAddress
//        enterIface = "tap" + x.getId.take("72d5355f-c6".length())
//        pcName = "compute1"
//        val codeFirst = InstructionBlock(
//          Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
////          Assign(IPDst, ConstantValue(ipToNumber("8.8.8.8"), isIp = true)),
////          Assign(EtherDst, ConstantValue(macToNumber("fa:16:3e:f8:03:e4"), isMac = true)),
//          Assign(EtherSrc, ConstantValue(macToNumber(x.getMacAddress), isMac = true))
//        )
//        experiment(enterIface, pcName, dir, codeFirst = codeFirst, threads = 4)
//        Z3Util.z3Context.finalize()
//        Z3Util.refreshCache
//      }
//    }
//    val ipDst = "192.168.13.4"
//    experiment(ipsrc, enterIface, pcName, dir)
  }
}






object EgressFromMachine
{
  def main(argv : Array[String]) {
    val ipsrc = "192.168.13.5"
    val enterIface = "tap72d5355f-c6"
    val pcName = "compute1"
    val ipDst = "8.8.8.8"
    
    val expName = "northbound"
    val dir = "stack-inputs/generated2"
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    val world = WorldModel.fromFolder(dir)
    psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
    init = System.currentTimeMillis()
    val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
          x.getNamespaces.map { y => InstructionBlock(
              Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
              Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
              Assign("IsTracked".scopeTo(y), ConstantValue(0))
            )
          }
        }
      } ++ world.getPcs.flatMap { x => {
          x.getCtZones.map { y => {
              val prefix = x.getName + "." + y.intValue
              InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
              )
            }
          }
        }
      }
    )
    
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true))
      )(State.bigBang, true)._1
    init =System.currentTimeMillis()
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(enterIface)
    val ns = pc.getNamespaceForNic(enterIface)
    
    //ingress example
    val iib = new EnterIface(pcName, enterIface, world).generateInstruction
    val worldModel = world
//    val iib = new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel).generateInstruction()
    val iip = InstructionBlock(
      Assign("InputInterface", ConstantStringValue(enterIface)),
      Allocate("OutputInterface"),
      iib
    )
    val psCode = new PrintStream(s"$dir/generated-code-$expName.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    val psout = new PrintStream(s"$dir/generated-$expName.out")
    val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
    val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
    
    init = System.currentTimeMillis()
    initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
      System.out.println(System.currentTimeMillis() - init)
      psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Forward number of states: " + ok.size + "," + fail.size)
      psout.println("[" + ok.mkString(",") + "]")
      psFailed.println("[" + fail.mkString(",") + "]")
      val success = ok.filter { x => x.history.head.startsWith("MaybeOutbound") }
      init = System.currentTimeMillis()
      val ib = 
          success.foldLeft((List[State](), List[State]())){ (acc, x) => 
            "MaybeOutbound\\((.*),(.*),(.*)\\)".r.
                findAllIn(x.history.head).matchData.foldRight(acc) {
                  (r, acc2) => {
                    val tap = r.group(1)
                    val br = r.group(2)
                    val machine = r.group(3)
                    println(s"Entering $tap at $machine")
                    val ii = InstructionBlock(
                        PacketMirror(),
                        Assign("InputInterface", ConstantStringValue(tap)),
                        new EnterIface(machine, tap, world).generateInstruction()
                    )(x, true)
                    (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                }
            }
          }
      psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
      val (okRev, failedRev) = ib
      psOutRev.println("[" + okRev.mkString(",") + "]")
      psFailedRev.println("[" + failedRev.mkString(",") + "]")
    }
    psOutRev.close()
    psFailedRev.close()
    psStats.close()
    psout.close()
    psFailed.close()
  }
}