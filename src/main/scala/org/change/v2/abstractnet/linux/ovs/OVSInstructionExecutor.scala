package org.change.v2.abstractnet.linux.ovs

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
import org.change.v2.analysis.processingmodels.instructions.:==:
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



object PacketMirror {
  def apply() : Instruction = {
    InstructionBlock(
      Allocate("tmp"),
      Assign("tmp", :@(EtherSrc)),
      Assign(EtherSrc,:@(EtherDst)),
      Assign(EtherDst,:@("tmp")),
      Assign("tmp",:@(IPSrc)),
      Assign(IPSrc,:@(IPDst)),
      Assign(IPDst,:@("tmp")),
      If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
          InstructionBlock(
            Assign("tmp",:@(TcpSrc)),
            Assign(TcpSrc,:@(TcpDst)),
            Assign(TcpDst,:@("tmp"))
          ),
          If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
              InstructionBlock(
                Assign("tmp",:@(UDPSrc)),
                Assign(UDPSrc,:@(UDPDst)),
                Assign(UDPDst,:@("tmp"))
              )
          )
      ),
      Deallocate("tmp")
    )
  }
}

object VLANOps {
  def encap() : Instruction = InstructionBlock(
      Forward("VLANEncap()"),
      Allocate("s"),
      Assign("s",:@(EtherSrc)),
      Allocate("d"),
      Assign("d",:@(EtherDst)),
      Allocate("et"),
      Assign("et", :@(EtherType)),
      Deallocate(EtherSrc, 48),
      Deallocate(EtherDst, 48),
      CreateTag("L2",Tag("L2")-32),
      Allocate(EtherSrc,48),
      Assign(EtherSrc,:@("s")),
      Allocate(EtherDst,48),
      Assign(EtherDst,:@("d")),
      Allocate(EtherType,16),
      Allocate(VLANEtherType, 16),
      Assign(VLANEtherType, :@("et")),
      Assign(EtherType, ConstantValue(EtherProtoVLAN)),
      Allocate(PCP,3),
      Assign(PCP,ConstantValue(0)),
      Allocate(DEI,1),
      Assign(DEI,ConstantValue(0)),
      Allocate(VLANTag,12),
      Assign(VLANTag, SymbolicValue()),
      Deallocate("s"),
      Deallocate("d"),
      Deallocate("et")
  )
  
  def decap() : Instruction = InstructionBlock(
      Forward("VLANDecap()"),
      Constrain(EtherType,:==:(ConstantValue(EtherProtoVLAN))),
      Allocate("s"),
      Assign("s",:@(EtherSrc)),
      Allocate("d"),
      Assign("d",:@(EtherDst)),
      Deallocate(EtherSrc, 48),
      Deallocate(EtherDst, 48),
      Deallocate(EtherType, 16),
      Deallocate(PCP,3),
      Deallocate(DEI,1),
      Deallocate(VLANTag,12),
      CreateTag("L2",Tag("L2")+32),
      Allocate(EtherSrc,48),
      Assign(EtherSrc,:@("s")),
      Allocate(EtherDst,48),
      Assign(EtherDst,:@("d")),
      Deallocate("s"),
      Deallocate("d")
    )
}

object TestThatCrap
{
  
  def main(argv : Array[String]) {
    val ipsrc = "192.168.13.3"
    val enterIface = "tap72d5355f-c6"
    //ingress traffic example:
//    val enterIface = "qg-09d66f0a-46"
    
    //floating ip example:
//    val enterIface = "eth2"
    
    val pcName = "compute1"
//    val pcName = "controller"
//    val ipDst = "192.168.13.1"
    //floating ip example
    val ipDst = "8.8.8.8"
//    val ipDst = "192.168.13.3"
//    val macDst = "fa:16:3e:9c:99:77"
//    System.setErr(new PrintStream("generated.err"))
    var init = System.currentTimeMillis()
    val world = WorldModel.fromFolder("stack-inputs/generated")
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
//        , Assign(IPSrc, ConstantValue(ipToNumber("8.8.8.8"), isIp = true))
//        , Assign(EtherDst, ConstantValue(macToNumber(macDst), isMac = true))
      )(State.bigBang, true)._1
    System.out.println(System.currentTimeMillis() - init)
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
//      Assign("OutputInterface", SymbolicValue()),
//      Allocate("ShouldTrack"),
//      Assign("ShouldTrack", ConstantValue(1)),
      iib
    )
    val psCode = new PrintStream("generated-code.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream("generated-fail.out")
    val psout = new PrintStream("generated.out")
    val psCodeRev = new PrintStream("generated-code-rev.out")
    val psOutRev = new PrintStream("generated-reverse.out")
    val psFailedRev = new PrintStream("generated-reverse-fail.out")
    
    initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
//        new EnterIPInterface(pcName, enterIface, world)(initial, false)
      System.out.println(ok.size + " " + fail.size)
      System.out.println(System.currentTimeMillis() - init)

      psout.println("[" + ok.mkString(",") + "]")
      psFailed.println("[" + fail.mkString(",") + "]")
      val success = ok.filter { x => x.history.head.startsWith("MaybeOutbound") }
      val ib = 
          // floating ip => local vm acts like a responder
//          "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
          // external interface acts as a
          success.foldLeft((List[State](), List[State]())){ (acc, x) => 
            "MaybeOutbound\\((.*),(.*),(.*)\\)".r.
                findAllIn(x.history.head).matchData.foldRight(acc) {
                  (r, acc2) => {
  //                  floating example:
  //                  val tap = r.group(1)
  //                  val machine = r.group(2)
  //                  println(s"Entering $tap at $machine")
  //                  InstructionBlock(
  //                      PacketMirror(),
  //                      Assign("InputInterface", ConstantStringValue(tap)),
  //                      new EnterIface(machine, tap, world).generateInstruction()
  //                  )
  //                  northbound example
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
      psCodeRev.println(ib)
      val (okRev, failedRev) = ib
      println("Reverse path: " + okRev.length + "," + failedRev.length)
      psOutRev.println("[" + okRev.mkString(",") + "]")
      psFailedRev.println("[" + failedRev.mkString(",") + "]")
      
      val ps1 = new PrintStream("outcomes.out")
      
      ps1.println("[" + okRev.filter { x => x.history.head.startsWith("MaybeOutbound") }.
          map { x => x.toString() }.mkString(",") + "]")
      ps1.close
    }
    psOutRev.close()
    psFailedRev.close()
    psCodeRev.close()
    
    psout.close()
    psFailed.close()
  }
}

class EnterIface(pcName : String, iface : String, world : WorldModel) extends Instruction {
  override def toString : String = "EnterIface(" + iface + "," + pcName + ")" 
  
  def generateInstruction() : Instruction = {
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(iface)
    if (pc.isBridged(iface))
    {
      val br = pc.getBridge(iface)
      if (br != null && br.isInstanceOf[OVSBridge])
      {
        new EnterBridge(br.asInstanceOf[OVSBridge], nic, world).generateInstruction
      }
      else
        throw new UnsupportedOperationException("Not implemented for Linux Bridge")
    }
    else
    {
      val eii = new EnterIPInterface(pcName, iface, world).generateInstruction
      eii
    }
  }
  
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    generateInstruction()(state2, verbose)
  }
}


case class EnterBridge(br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
  
  def generateInstruction() : Instruction = {
    val ovsnic = br.getOvsNic(iface.getName)
    val ofPort = br.getOpenFlowPort(iface.getName)
    InstructionBlock(
      Forward(toString),
      new EnterOFPort(ofPort, br, ovsnic, world).generateInstruction()
    )
  }
  
  override def toString : String = "EnterBridge(" + br.getName + "," + iface.getName + "," + br.getComputer.getName + ")"
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    System.out.println(toString)
    generateInstruction()(state2, verbose)
  }
}


case class EnterBridgeNormal(br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction {
  override def toString : String = "EnterL2(" + br.getName + "," + iface.getName + "," + br.getComputer.getName + ")"
  
  def visit() : (Map[String, Instruction], Map[String, String]) = {
    (Map[String, Instruction](this.toString() -> this.generateInstruction()), Map[String, String]())
  }
  
  def generateInstruction() : Instruction = {
    val collected = br.getDistinctVlans()
    
    InstructionBlock(
      Forward(toString),
      if (iface.isAccess())
      {
        val vlan = iface.getVlans().get(0).intValue
        InstructionBlock(
          VLANOps.encap(),
          Assign(VLANTag, ConstantValue(vlan))
        )
      }
      else
        NoOp,
        { 
          val ib = InstructionBlock(
            if (!collected.isEmpty())
              collected.toArray().foldRight(Fail("VLAN unknown") : Instruction)((x, acc) =>  {
                val t  = x.asInstanceOf[Integer].intValue()
                val isvlans = br.getIfacesForVlan(t).filter { x => x != iface.getName && x != br.getName }.map { x => br.getOvsNic(x) }
                If (Constrain(VLANTag, :==:(ConstantValue(t))),
                  Fork(isvlans.map ( y => {
                    if (y.isAccess())
                    {
                       InstructionBlock(
                           VLANOps.decap(), 
                           new ExitBridge(br, y, world)
                       )
                    }
                    else
                      new ExitBridge(br, y, world)
                  })),
                  acc
                )
              })
            else
              // adicatelea: Nu am niciun VLAN declarat => pur si simplu fac fork (ma comport ca un LAN extender)
              Fork(br.getNICs.filter { x => x.getName != iface.getName && x.getName != br.getName }.
                  map { x => new ExitBridge(br, x, world) })
          )
          ib
        }
    )
  }
  
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    System.out.println(toString)
    val ib = generateInstruction()
    val ret = ib(state2, verbose)
    ret
  }
}

class ExitOFPort(ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
  
  override def toString = s"ExitOFPort(${ofPort.getPortNo}, ${br.getName}, ${br.getComputer.getName})"
  
  def generateInstruction() : Instruction = {
    val thePort = br.getOvsNic(ofPort)
    InstructionBlock(
      Forward(toString),
      new ExitBridge(br, thePort, world)
    )
  }
  
  override def apply(state : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    generateInstruction()(state, verbose)
  }
}


class ExitBridge(br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
  override def toString : String = "ExitBridge(" + br.getName + "," + iface.getName + ")"
  
  def generateInstruction() : Instruction = {
    val isInterbridge = iface.getType == "patch"
    val isVxlan = iface.getType == "vxlan"
    val isGre = iface.getType == "gre"
    val isInternal = iface.getType == "internal"
    val isSystem  = iface.getType == "system"
    val isVmAttached = iface.isVmConnected()
    
    InstructionBlock(
      Forward(toString),
      if (isVmAttached)
      {
        val ipIface = br.getComputer.getIpNic(iface.getName)
        if (ipIface == null || (ipIface.getIPAddresss.size == 0))
          If (Constrain(EtherDst, :==:(ConstantValue(iface.getVmMac, false, true))),
            Forward(s"DeliveredLocallyVM(${iface.getName}, ${br.getComputer.getName})"),
            Fail("Wrong EtherDst")
          )
        else
        {
          new EnterIPInterface(br.getComputer.getName, iface.getName, world)
        }
      }
      else if (isInterbridge)
      {
        val peer = iface.getOptions.get("peer")
        new EnterIface(br.getComputer.getName, peer, world)
      }
      else if (isVxlan || isGre)
      {
        val peer2 : String = iface.getOptions.get("remote_ip")
        val pc = world.getComputerByIp(peer2)
        val ifacePeer = pc.getNICListeningOnIP(peer2)
        new EnterIface(pc.getName, ifacePeer.getName, world)
      }
      else if (isInternal)
      {
        val ipIface = br.getComputer.getIpNic(iface.getName)
        if (ipIface == null || ipIface.getIPAddresss.size == 0)
          Fail("DeadEndInternalIface(" + iface.getName + "," + br.getName + "," + br.getComputer.getName + ")")
        else
          new EnterIPInterface(br.getComputer.getName, iface.getName, world)
//        Forward("ExitThroughInternal(" + iface.getName + ")")
      }
      else if (iface.getType == "")
      {
        val ipIface = br.getComputer.getIpNic(iface.getName)
        if (ipIface == null  || ipIface.getIPAddresss.size == 0)
          Forward("MaybeOutbound(" + iface.getName + "," + br.getName + "," + br.getComputer.getName + ")")
        else
          new EnterIPInterface(br.getComputer.getName, iface.getName, world)
      }
      else
      {
        throw new IllegalArgumentException("Unknown interface type " + iface.getType)
      }
    )
  }
  
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = 
  {
    System.out.println(toString)
    generateInstruction()(state2, verbose)
  }
}

object Scope {
  def scopeTo(br : OVSBridge, v : String) : String = {
    br.getComputer.getName + br.getName + v
  }
  
}

class EnterOFPort(ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
  override def toString : String = {
    "EnterOFPort(" + ofPort.getPortNo + "," + br.getName + ")"
  }
  
  def generateInstruction() : Instruction = {
    val table = br.getConfig.getTables.get(0)
    InstructionBlock(
        Forward(toString),
        Allocate("EtherType"),
        If (Constrain(EtherType, :==:(ConstantValue(EtherProtoVLAN))),
            Assign("EtherType", :@(VLANEtherType)),
            Assign("EtherType", :@(EtherType))
        ),
        Allocate("IsTracked"),
        Assign("IsTracked", ConstantValue(0)),
        Allocate("IsFirst"),
        Assign("IsFirst", ConstantValue(1)),
        Allocate("InPort"),
        Assign("ShouldTrack", ConstantValue(1)),
        Assign("InPort", ConstantValue(ofPort.getPortNo)),
        new EnterOFTable(Nil, table, ofPort, br, iface, world).generateInstruction,
        Deallocate("InPort"),
        Deallocate("IsTracked"),
        Deallocate("IsFirst"),
        Deallocate("EtherType")
    )
  }
  
    override def apply(state2 : State, verbose : Boolean) : 
      (List[State], List[State]) = {
      System.out.println(toString)
      generateInstruction()(state2, verbose)
    }
}

class EnterOFTable(actionSet : List[Action], table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
    extends Instruction {
  
  override def toString : String = {
    "EnterOFTable(" + table.getTableId + "," + ofPort.getPortNo + "," +br.getName + "," + br.getComputer.getName + ")"
  }
  
  def generateInstruction() : Instruction = {
    val flows = table.getEntries
    val doFlows = InstructionBlock(
       Forward(toString),
       InstructionBlock(
         Allocate("FFFMatched"),
         Assign("FFFMatched", ConstantValue(-1)),
         InstructionBlock(
           flows.zipWithIndex.map { 
            v => new EnterFlowEntry(v._1, v._2, table, ofPort, br, iface, world).generateInstruction
           }
         )
       ), 
       new EnterTableMiss(table, ofPort, br, iface, world).generateInstruction, 
       Deallocate("FFFMatched")
     )
     doFlows
  }
  
  override def apply(state2 : State, verbose : Boolean) : (List[State], List[State]) = {
      System.out.println(toString)
      generateInstruction()(state2, verbose)
    }
}

class ApplyAction(action : Action, table : OpenFlowTable, 
    ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
  
  def trackMe(u : Int, prefix : String) = {
    InstructionBlock(
        ConntrackTrackZone(br.getComputer, u, world).generateInstruction(),
        Assign("State", :@("State".scopeTo(prefix))),
        Assign(IPTablesConstants.CTMARK_BOTTOM, :@(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix))),
        Assign(IPTablesConstants.CTMARK_TOP, :@(IPTablesConstants.CTMARK_TOP.scopeTo(prefix))),
        Assign("IsBackward", :@("IsBackward".scopeTo(prefix))),
        Assign("IsForward", :@("IsForward".scopeTo(prefix))),
        Assign("IsFirst", :@("IsFirst".scopeTo(prefix))),
        Assign("IsTracked", :@("IsTracked".scopeTo(prefix))),
        Assign("CtZone", ConstantValue(u))
      )
  }
  
  def generateInstruction() : Instruction = {
    InstructionBlock(
      Forward(toString),
      action match {
        case action : ApplyActionsAction => InstructionBlock(action.getActions.map { x => 
          new ApplyAction(x, table, ofPort, br, iface, world)
          })
        case action : OutputAction => 
          if (action.isReg())
      		{
      		  throw new UnsupportedOperationException("Don't know how to handle registers... Yet")
      		}
      		else
      		{
      		  if (action.getPort == 0xfffffffa)
      		  {
      		    // NORMAL goes here
              new EnterBridgeNormal(br, iface, world).generateInstruction()
      		  }
      		  else
      		  {
        		  new ExitOFPort(br.getOpenFlowPort(action.getPort.intValue()), br, iface, world).generateInstruction()
      		  }
      		}
        case action : NormalAction => new EnterBridgeNormal(br, iface, world).generateInstruction()
        case action : DropAction => Fail("Drop action encountered")
        case action : LoadAction => {
           if (action.getTo.isPresent())
           {
             val x = FieldNameTranslator(action.getTo.get.getName)
             if (action.getTo.isPresent() && action.getFrom.isPresent())
          	  {
          	    Assign(x, :@(action.getFrom.get.getName))
          	  }
          	  else if (action.getTo.isPresent() && !action.getFrom.isPresent())
          	  {
          	    Assign(x, ConstantValue(action.getValue.get))
          	  }
          	  else 
          	  {
          	    throw new IllegalArgumentException("Cannot do that, load action must have at least one parameter")
          	  }
           }
           else
           {
         	    throw new IllegalArgumentException("Cannot do that, load action must have at least one parameter")
           }
        }
        case action : ModDlDstAction => Assign(EtherDst, ConstantValue(action.getMacAddr, isMac = true))
        case action : ModDlSrcAction => Assign(EtherSrc, ConstantValue(action.getMacAddr, isMac = true))
        case action : ModNwSrcAction => Assign(IPSrc, ConstantValue(action.getIpAddress, isIp = true))
        case action : ModNwDstAction => Assign(IPDst, ConstantValue(action.getIpAddress, isIp = true))
        case action : ModTpDstAction => If (Constrain(EtherType, :==:(ConstantValue(TCPProto))),
            Assign(TcpDst, ConstantValue(action.getIpAddress)),
            If (Constrain(EtherType, :==:(ConstantValue(ICMPProto))),
                Assign(ICMPType, ConstantValue(action.getIpAddress)),
                If (Constrain(EtherType, :==:(ConstantValue(UDPProto))),
                    Assign(UDPDst, ConstantValue(action.getIpAddress)),
                    Fail("No such protocol"))))
        case action : ModTpSrcAction => If (Constrain(EtherType, :==:(ConstantValue(TCPProto))),
            Assign(TcpSrc, ConstantValue(action.getIpAddress)),
            If (Constrain(EtherType, :==:(ConstantValue(ICMPProto))),
                Assign(ICMPType, ConstantValue(action.getIpAddress)),
                If (Constrain(EtherType, :==:(ConstantValue(UDPProto))),
                    Assign(UDPSrc, ConstantValue(action.getIpAddress)),
                    Fail("No such protocol"))))
        case action : ModVlanVidAction => 
          If(Constrain(EtherType, :==:(ConstantValue(EtherProtoVLAN))),
            Assign(VLANTag, ConstantValue(action.getVlanId)),
            InstructionBlock(
                VLANOps.encap(),
                Assign(VLANTag, ConstantValue(action.getVlanId))
            )
          )
        case action : MoveAction => Assign(action.getTo.getName, :@(action.getFrom.getName))
        case action : PushVlanAction => VLANOps.encap()
        case action : SetFieldAction => Assign(action.getField.getName, ConstantValue(action.getValue))
        case action : SetTunnelAction => Assign(TunId, ConstantValue(action.getTunId))
        case action : StripVlanAction => VLANOps.decap()
        case action : LearnAction => NoOp
        case action : CTAction => {
          val ib = 
          if (action.isCommit())
          {
            val zones = br.getComputer.getCtZones()
            val execs = action.getActions
            
            if (action.isZone() && !action.isRegZone())
            {
              InstructionBlock(
                ConntrackCommitZone(br.getComputer, action.getZoneVal, world),
                InstructionBlock(
                  execs.map { y => { 
                      val qf = y.getTo
                      if (qf.getName == "ct_label" || qf.getName == "NXM_NX_CT_LABEL")
                      {
                        Assign("CtLabel".scopeTo(br.getComputer.getName + "." + action.getZoneVal), ConstantValue(y.getValue))
                      }
                      else if (qf.getName == "ct_mark" || qf.getName == "NXM_NX_CT_MARK")
                      {
                        InstructionBlock(
                          Assign(IPTablesConstants.CTMARK_TOP.scopeTo(br.getComputer.getName + "." + action.getZoneVal), 
                            ConstantValue(y.getValue & 0xFFFF0000)),
                          Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(br.getComputer.getName + "." + action.getZoneVal), 
                            ConstantValue(y.getValue & 0x0000FFFF))
                        )
                      }
                      else 
                        NoOp
                    }
                  }
                )
              )
            }
            else
            {
              val ii = zones.foldRight(Fail("No zone matched") : Instruction)( (x, acc) => {
                val fff = FieldNameTranslator(action.getZoneReg.getName)
                val ct = fff match {
                  case Right(t) => Constrain(t, :==:(ConstantValue(x.intValue)))
                  case Left(t) => Constrain(t, :==:(ConstantValue(x.intValue)))
                }
                If (ct,
                    InstructionBlock(
                        ConntrackCommitZone(br.getComputer, x.intValue, world).generateInstruction(),
                        InstructionBlock(
                          execs.map { y => { 
                              val qf = y.getTo
                              if (qf.getName == "ct_label" || qf.getName == "NXM_NX_CT_LABEL")
                              {
                                Assign("CtLabel".scopeTo(br.getComputer.getName + "." + x.intValue), ConstantValue(y.getValue))
                              }
                              else if (qf.getName == "ct_mark" || qf.getName == "NXM_NX_CT_MARK")
                              {
                                InstructionBlock(
                                  Assign(IPTablesConstants.CTMARK_TOP.scopeTo(br.getComputer.getName + "." + x.intValue), 
                                    ConstantValue(y.getValue & 0xFFFF0000)),
                                  Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(br.getComputer.getName + "." + x.intValue), 
                                    ConstantValue(y.getValue & 0x0000FFFF))
                                )
                              }
                              else 
                                NoOp
                            }
                          }
                        )
                    ),
                    acc
                  )
              })
              ii
            }
          }
          else
          {
            val zones = br.getComputer.getCtZones()
            if (action.isZone() && !action.isRegZone())
            {
              val u = action.getZoneVal
              val prefix = br.getComputer.getName + "." + u
              trackMe(u, prefix)
            }
            else
            {
              val ii = zones.foldRight(Fail("No zone matched") : Instruction)( (x, acc) => {
                val fff = FieldNameTranslator(action.getZoneReg.getName)
                If (Constrain(fff, :==:(ConstantValue(x.intValue))),
                  InstructionBlock(
                    trackMe(x.intValue, br.getComputer.getName + "." + x.intValue),
                    if (action.hasTable()) {
                      new EnterOFTable(Nil : List[Action], 
                          br.getConfig.getTables.find { x => x.getTableId == action.getTable.intValue() }.get, 
                          ofPort,
                          br,
                          iface,
                          world)
                    } else {
                      NoOp
                    }
                  ),
                  acc
                )
              })
              ii
            }
          }
          InstructionBlock(Forward(action.toString()),ib)
          
        }
        case _ =>     throw new UnsupportedOperationException("Cannot handle " + action.getClass.getName + "... Yet")
      }
    )
  }
  
  
  override def toString : String = {
    "ApplyAction(" + action.getType + "," + table.getTableId + "," + br.getName + "," + br.getComputer.getName + ")"
  }
  
  override def apply(state2 : State, verbose : Boolean) : (List[State], List[State]) = {
    System.out.println(toString)
    generateInstruction()(state2, verbose)
  }
}


object FieldNameTranslator
{
  def apply(name : String) : Either[String, Intable] = {
     name match {
       case "in_port" => Left("InPort")
       case "dl_vlan" => Right(VLANTag)
       case "dl_vlan_pcp" => Right(VLANTag)
       case "dl_src" => Right(EtherSrc)
       case "dl_dst" => Right(EtherDst)
       case "dl_type" => Left("EtherType")
       case "nw_src" => Right(IPSrc)
       case "nw_dst" => Right(IPDst)
       case "nw_proto" => Right(Proto)
       case "nw_ttl" => Right(TTL)
       case "tp_src" => Right(TcpSrc)
       case "tp_dst" => Right(TcpDst)
       case "icmp_type" => Right(ICMPType)
       case "icmp_code" => Right(ICMPCode)
       case "metadata" => Left("OFMetadata")
       case "ipv6_src" => Right(Tag("L3Src"))
       case "ipv6_dst" => Right(Tag("L3Dst"))
       case "ipv6_label" => Right(Tag("IPv6Label"))
       case "tun_id" => Right(TunId) 
       case "tun_src" => Right(Tag("TunnelSrc"))
       case "tun_dst" => Right(Tag("TunnelDst"))
       case "pkt_mark" => Left("NfMark")
       case "out_port" => Left("OutPort")
       case "vlan_tci" => Right(VLANTag)
       case "nd_target" => Left("NDTarget")
       case "arp_spa" => Right(ARPProtoSender)
       case "NXM_OF_IN_PORT" => Left("InPort")
       case "NXM_OF_ETH_DST" => Right(EtherSrc)
       case "NXM_OF_ETH_SRC" => Right(EtherDst)
       case "NXM_OF_ETH_TYPE" => Left("EtherType") 
       case "NXM_OF_VLAN_TCI" => Right(VLANTag)
       case "NXM_OF_IP_PROTO" => Right(Proto) 
       case "NXM_OF_IP_SRC" => Right(IPSrc)
       case "NXM_OF_IP_DST" => Right(IPDst) 
       case "NXM_OF_TCP_SRC" => Right(TcpSrc)
       case "NXM_OF_TCP_DST" => Right(TcpDst)
       case "NXM_OF_UDP_SRC" => Right(UDPSrc)
       case "NXM_OF_UDP_DST" => Right(UDPDst)
       case "NXM_OF_ICMP_TYPE" => Right(ICMPType) 
       case "NXM_OF_ICMP_CODE" => Right(ICMPCode)
       case "NXM_NX_TUN_ID" => Right(TunId)
       case "ct_state" => Left("State")
       case "ct_mark" => Left("CtMark")
       case "ct_zone" => Left("CtZone")
       case _ =>    
         if (name.matches("reg([0-9]+)"))
           Left(name)
         else if (name.matches("NXM_NX_REG([0-9]+)"))
           Left(name.replace("NXM_NX_REG", "reg"))
         else
           throw new UnsupportedOperationException("Field " + name + " is not translatable in Symnet... Yet")
    }
  }
}


class EnterTableMiss(table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
  override def toString = s"EnterTableMiss(${table.getTableId}, ${ofPort.getPortNo}, ${ofPort.getName}, ${br.getName}, ${br.getComputer.getName})"
  
  def generateInstruction() : Instruction = {
    If (Constrain("FFFMatched", :==:(ConstantValue(-1))),
        Fail("Table miss encountered"),
        NoOp)
  }
    override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
      System.out.println(toString)
      generateInstruction()(state, verbose)
      //TODO : add code here to distinguish between the actual table misses
    }
}

class EnterFlowEntry(flowEntry : FlowEntry, index : Int, table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction {
  
  override def toString : String = {
    "EnterFlowEntry(" + index + "," + table.getTableId + "," + ofPort.getPortNo + "," + br.getName +"," + br.getComputer.getName + ")"
  }
  
  def generateInstruction() : Instruction = {
    val matches = flowEntry.getMatches
    val actions = flowEntry.getActions
    val gotoTable = actions.find { x => x.getType == ActionType.Resubmit || x.getType == ActionType.GotoTable }
    val clearAction = actions.find { x => x.getType == ActionType.ClearActions || x.getType == ActionType.Drop }
    val applyAction = actions.find { x => x.getType == ActionType.ApplyActions }
    val filteredActions = actions.filter { x => x.getType != ActionType.ClearActions &&  
      x.getType != ActionType.ApplyActions &&
      x.getType != ActionType.Resubmit && 
      x.getType != ActionType.GotoTable
    }
    
    val listOfInstructions = 
    if (clearAction.isDefined)
    {
      Fail("Failed because of clear action at bridge " + br.getName + " at PC " + br.getComputer.getName)
    }
    else
    {
      val jumpTarget =
      if (gotoTable.isDefined)
      {
        val act = gotoTable.get.asInstanceOf[ResubmitAction]
        val destPort =
        if (act.getInPort.isPresent())
        {
         br.getOpenFlowPort(act.getInPort.get.intValue())
        } else {
          ofPort
        }
        val destTable = 
        if (act.getTable.isPresent())
        {
          br.getConfig.getTables.find { x => x.getTableId == act.getTable.get.intValue() }.get
        }
        else 
        {
          table
        }
        new EnterOFTable(Nil : List[Action], destTable, destPort, br, iface, world).generateInstruction
      }
      else
      {
        NoOp
      }

      InstructionBlock(
          Assign("FFFMatched", ConstantValue(index)),
          InstructionBlock(
              filteredActions.toList.map { x => new ApplyAction(x, table, ofPort, br, iface, world).generateInstruction }
          ),
          jumpTarget
      )
    }
    
    val instr = if (!matches.isEmpty())
      If (Constrain("FFFMatched", :==:(ConstantValue(-1))),
        InstructionBlock(
          Forward(toString),
          matches.map { x => FieldNameTranslator(x.getField.getName) match {
            
              case Left(v) if x.getField.getName == "ct_mark" || x.getField.getName == "NXM_NX_CT_STATE" =>
              {
                List[Instruction](
                  Constrain(IPTablesConstants.CTMARK_BOTTOM, :==:(ConstantValue(x.getValue & 0xFFFFL))),
                  Constrain(IPTablesConstants.CTMARK_TOP, :==:(ConstantValue(x.getValue & 0xFFFF0000L)))
                )
              }
              case Left(v) if x.getField.getName == "ct_state" || x.getField.getName == "NXM_NX_CT_STATE" => {

      					val trkmb = 0l;
      					val trkpb = 1l;
      					val newmb = 2l;
      					val newpb = 3l;
      					val estmb = 4l;
      					val estpb = 5l;
      					val rplmb = 6l;
      					val rplpb = 7l;
      					val relmb = 8l;
      					val relpb = 9l;
      					val invmb = 10l;
      					val invpb = 11l;
      					val forWhat = x.getValue
      					if ((forWhat & (1 << trkmb)) != 0L &&
      					    (forWhat & ~(1 << trkmb)) == 0L)
      					{
      					  //					OF_STATE_NOT_TRACKED = "-trk"
      					  List[Instruction](
      					    Constrain("IsTracked", :==:(ConstantValue(0)))
      					  )
      					}
      					else if ((forWhat & (1 << trkpb)) != 0L &&
      					    (forWhat & ~(1 << trkpb)) == 0L)
      					{
      					  //					OF_STATE_TRACKED = "+trk"
      					  List[Instruction](
        					  Constrain("IsTracked", :==:(ConstantValue(1)))
        					)
      					}
      					else if ((forWhat & (1 << newpb)) != 0L &&
      					    (forWhat & (1 << estmb)) != 0L && 
      					    (forWhat & ~(1 << newpb | 1 << estmb)) == 0L)
      					{
      					  //					OF_STATE_NEW_NOT_ESTABLISHED = "+new-est"
      					  List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(1))),
    					      Constrain("IsTracked", :==:(ConstantValue(1)))
  					      )
      					}
      					else if ((forWhat & (1 << estmb)) != 0L && 
      					    (forWhat & ~(1 << estmb)) == 0L)
      					{
                  //					OF_STATE_NOT_ESTABLISHED = "-est"
                  List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(1))),
    					      Constrain("IsTracked", :==:(ConstantValue(1)))
  					      )
      					}
      					else if ((forWhat & (1 << estpb)) != 0L && 
      					    (forWhat & ~(1 << estpb)) == 0L)
      					{
                //					OF_STATE_ESTABLISHED = "+est"
                  List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(0))),
    					      Constrain("IsTracked", :==:(ConstantValue(1)))
  					      )
      					}
      					else if ((forWhat & (1 << estpb)) != 0L &&
        					(forWhat & (1 << relmb)) != 0L &&
        					(forWhat & (1 << rplmb)) != 0L &&
        					(forWhat & ~(1 << estpb | 1 << relmb | 1 << rplmb)) == 0L)
      					{
                //					OF_STATE_ESTABLISHED_NOT_REPLY = "+est-rel-rpl"
                  List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(0))),
    					      Constrain("IsTracked", :==:(ConstantValue(1))),
    					      Constrain("IsForward", :==:(ConstantValue(1)))
  					      )
      					}
      					else if ((forWhat & (1 << estpb)) != 0L &&
        					(forWhat & (1 << relmb)) != 0L &&
        					(forWhat & (1 << rplpb)) != 0L &&
        					(forWhat & ~(1 << estpb | 1 << relmb | 1 << rplpb)) == 0L)
      					{
                //					OF_STATE_ESTABLISHED_REPLY = "+est-rel+rpl"
                   List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(0))),
    					      Constrain("IsTracked", :==:(ConstantValue(1))),
    					      Constrain("IsBackward", :==:(ConstantValue(1)))
  					       )     					  
      					}
      					else if ((forWhat & (1 << newmb)) != 0L &&
        					(forWhat & (1 << relpb)) != 0L &&
        					(forWhat & (1 << estmb)) != 0L &&
        					(forWhat & (1 << invmb)) != 0L &&
        					(forWhat & ~(1 << newmb | 1 << relpb | 1 << estmb | 1 << invmb)) == 0L)
      					{
                //					OF_STATE_RELATED = "-new-est+rel-inv"
                   List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(0))),
    					      Constrain("IsTracked", :==:(ConstantValue(1))),
    					      Constrain("IsBackward", :==:(ConstantValue(1)))
  					       )
      					}
      					else if ((forWhat & (1 << trkpb)) != 0L &&
      					    (forWhat & (1 << invpb)) != 0L &&
      					    (forWhat & ~(1 << trkpb | 1 << invpb)) == 0L)
      					{
                //					OF_STATE_INVALID = "+trk+inv"
      					  List[Instruction](
    					      Constrain("IsTracked", :==:(ConstantValue(0)))
      					  )
      					}
      					else if ((forWhat & (1 << newpb)) != 0L &&
      					    (forWhat & ~(1 << newpb)) == 0L)
      					{
                //					OF_STATE_NEW = "+new"
      					  List[Instruction](
    					      Constrain("IsTracked", :==:(ConstantValue(0))),
    					      Constrain("IsFirst", :==:(ConstantValue(0)))
      					  )
      					}
      					else if ((forWhat & (1 << newmb)) != 0L &&
      					    (forWhat & (1 << rplmb)) != 0L &&
      					    (forWhat & ~(1 << newmb | 1 << rplmb)) == 0L)
      					{
                //					OF_STATE_NOT_REPLY_NOT_NEW = "-new-rpl"
      					  List[Instruction](
    					      Constrain("IsFirst", :==:(ConstantValue(0))),
    					      Constrain("IsBackward", :==:(ConstantValue(0)))
      					  )
      					}
      					else throw new UnsupportedOperationException(s"Cannot handle this state ${forWhat}... Yet")
              }
              case Right(tag) if tag == EtherDst && x.getMask == 0x010000000000L  => {
                List[Instruction](
                  if (x.getValue == 0x010000000000L)
                    Constrain("IsUnicast", :==:(ConstantValue(0)))
                  else
                    Constrain("IsUnicast", :==:(ConstantValue(1)))
                )
              }
              case Right(tag) if x.getField.getName == "vlan_tci" => {
                List[Instruction](
                  if (x.getValue == 0)
                  {
                    Constrain(EtherType, :~:(:==:(ConstantValue(EtherProtoVLAN))))
                  }
                  else
                  {
                    if (x.getValue == 0x1000L && x.getMask == 0x1000L)
                      Constrain(EtherType, :==:(ConstantValue(EtherProtoVLAN)))
                    else
                      Constrain(VLANTag, :==:(ConstantValue(x.getValue)))
                  }
                )
              }
              case Right(tag) => List[Instruction](Constrain(tag, :==:(ConstantValue(x.getValue))))
              case Left(mem) => List[Instruction](Constrain(mem, :==:(ConstantValue(x.getValue))))
            }
          }.foldRight(listOfInstructions)((u, acc) => {
            u.foldRight(acc)( (t, acc2) => 
              If (t, acc2, NoOp)
            )
          })
        ),
        NoOp)
    else
      If (Constrain("FFFMatched", :==:(ConstantValue(-1))), listOfInstructions, NoOp)
    instr
  }
  
  override def apply(state2 : State, verbose : Boolean) : (List[State], List[State]) = {
    System.out.println(toString)
    generateInstruction()(state2, verbose)
  }
}
