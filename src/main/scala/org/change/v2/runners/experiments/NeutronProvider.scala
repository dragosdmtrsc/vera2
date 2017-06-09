package org.change.v2.runners.experiments

import scala.collection.JavaConversions.asScalaBuffer

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{ :@ => :@ }
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.NIC
import org.change.v2.model.OVSBridge
import org.change.v2.model.OpenFlowNIC
import org.change.v2.model.OpenFlowTable
import org.change.v2.model.WorldModel
import org.change.v2.model.openflow.Action
import org.change.v2.model.openflow.ActionType
import org.change.v2.model.openflow.FlowEntry
import org.change.v2.model.openflow.actions.ApplyActionsAction
import org.change.v2.model.openflow.actions.DropAction
import org.change.v2.model.openflow.actions.LoadAction
import org.change.v2.model.openflow.actions.ModDlDstAction
import org.change.v2.model.openflow.actions.ModDlSrcAction
import org.change.v2.model.openflow.actions.ModNwDstAction
import org.change.v2.model.openflow.actions.ModNwSrcAction
import org.change.v2.model.openflow.actions.ModTpDstAction
import org.change.v2.model.openflow.actions.ModTpSrcAction
import org.change.v2.model.openflow.actions.ModVlanVidAction
import org.change.v2.model.openflow.actions.MoveAction
import org.change.v2.model.openflow.actions.NormalAction
import org.change.v2.model.openflow.actions.OutputAction
import org.change.v2.model.openflow.actions.PushVlanAction
import org.change.v2.model.openflow.actions.ResubmitAction
import org.change.v2.model.openflow.actions.SetFieldAction
import org.change.v2.model.openflow.actions.SetTunnelAction

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.change.v2.model.openflow.actions.StripVlanAction
import org.change.v2.analysis.processingmodels.instructions.Deallocate
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.{:==:, :<=:, :>=:, :&:}
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fork
import org.change.v2.analysis.memory.Value
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.Intable
import java.io.PrintStream
import org.change.v2.model.openflow.actions.LearnAction
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.abstractnet.linux.iptables.EnterIPInterface
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.abstractnet.linux.iptables.EnterIPTablesChain
import org.change.v2.abstractnet.linux.iptables.IPTablesConstants
import org.change.v2.abstractnet.linux.iptables.ConntrackTrack
import org.change.v2.abstractnet.linux.iptables.ConntrackUnDnat
import org.change.v2.abstractnet.linux.iptables.ConntrackUnSnat
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions._
import scala.util.matching.Regex._
import org.change.v2.model.openflow.actions.CTAction
import org.change.v2.abstractnet.linux.iptables.ConntrackZone
import org.change.v2.abstractnet.linux.iptables.ConntrackCommitZone
import org.change.v2.abstractnet.linux.iptables.ConntrackTrackZone
import org.change.v2.abstractnet.linux.ovs.PacketMirror
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.linux.ovs.EnterIface



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
    initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
      System.out.println(System.currentTimeMillis() - init)
      psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Forward number of states: " + ok.size + "," + fail.size)
      psout.println("[" + ok.mkString(",") + "]")
      psFailed.println("[" + fail.mkString(",") + "]")
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




object L2Connectivity
{
  def main(argv : Array[String]) {
    val ipsrc = "192.168.13.3"
    val enterIface = "tap72d5355f-c6"
    val pcName = "compute1"
//    val ipDst = "192.168.13.4"
    
    val expName = "eastwest"
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
        Assign(IPDst, SymbolicValue()),
        Constrain(IPDst, :&:(:<=:(ConstantValue(ipToNumber("192.168.13.254"))), 
            :>=:(ConstantValue(ipToNumber("192.168.13.1")))))
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






object EgressFromMachine
{
  def main(argv : Array[String]) {
    val ipsrc = "192.168.13.3"
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