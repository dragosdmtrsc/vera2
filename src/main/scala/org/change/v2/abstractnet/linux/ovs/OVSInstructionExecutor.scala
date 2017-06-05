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

object TestThatCrap
{
  def main(argv : Array[String]) {
//    val enterIface = "tap737565c0-2c"
    //ingress traffic example:
//    val enterIface = "qg-09d66f0a-46"
    
    //floating ip example:
    val enterIface = "eth2"
    
//    val pcName = "compute1"
    val pcName = "controller"
//    val ipDst = "192.168.13.1"
    //floating ip example
    val ipDst = "203.0.113.103"
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true))
      )(State.bigBang, false)._1
    var init = System.currentTimeMillis()
    val world = WorldModel.fromFolder("stack-inputs/generated")
    System.out.println(System.currentTimeMillis() - init)
    init =System.currentTimeMillis()
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(enterIface)
    //ingress example
//    val iib = new EnterIPInterface(pcName, enterIface, world)
    val iib = new EnterIface(pcName, enterIface, world).generateInstruction()
    val iip = InstructionBlock(
      Assign("ShouldTrack", ConstantValue(1)),
      Assign("IsTracked", ConstantValue(0)),
      Assign("InputInterface", enterIface.hashCode()),
      Allocate("OutputInterface"),
      Assign("OutputInterface", SymbolicValue()),
      Allocate(IPTablesConstants.NFMARK_BOTTOM),
      Assign(IPTablesConstants.NFMARK_BOTTOM, SymbolicValue()),
      Allocate(IPTablesConstants.CTMARK_BOTTOM),
      Assign(IPTablesConstants.CTMARK_BOTTOM, SymbolicValue()),
      Allocate(IPTablesConstants.CTMARK_TOP),
      Assign(IPTablesConstants.CTMARK_TOP, SymbolicValue()),
      Allocate(IPTablesConstants.NFMARK_TOP),
      Assign(IPTablesConstants.NFMARK_TOP, SymbolicValue()),
      Assign("ShouldTrack", ConstantValue(1)),
      Assign("IsTracked", ConstantValue(0)),
      iib
    )
    val psCode = new PrintStream("generated-code.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream("generated-fail.out")
    val psout = new PrintStream("generated.out")
    initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
//        new EnterIPInterface(pcName, enterIface, world)(initial, false)
      System.out.println(ok.size + " " + fail.size)
      System.out.println(System.currentTimeMillis() - init)

      (ok, fail)
      ok.foreach { x => psout.println(x) }
      fail.foreach { x => psFailed.println(x) }
    }
    psout.close()
    psFailed.close()
  }
}

class EnterIface(pcName : String, iface : String, world : WorldModel) extends Instruction {
  override def toString : String = "EnterIface(" + iface + "," + pcName + ")" 
  
  
  var portNum : Int = 0;
  var mapTo : Map[String, String] = Map[String, String]()
  
  def generateInstruction() : Instruction = {
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(iface)
    if (pc.isBridged(iface))
    {
      val br = pc.getBridge(iface)
      if (br != null && br.isInstanceOf[OVSBridge])
      {
        mapTo = mapTo + (this.toString() + s"/$portNum/exit" -> new EnterBridge(br.asInstanceOf[OVSBridge], nic, world).toString())
        portNum += 1
        new EnterBridge(br.asInstanceOf[OVSBridge], nic, world).generateInstruction
      }
      else
        throw new UnsupportedOperationException("Not implemented for Linux Bridge")
    }
    else
    {
      val eii = new EnterIPInterface(pcName, iface, world)
      mapTo = mapTo + (this.toString() + s"/$portNum/exit" -> eii.toString)
      portNum += 1
      Forward(this.toString() + s"/$portNum/exit")
    }
  }
  
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    System.out.println(toString)
    val state = Forward(toString)(state2)._1(0)
    generateInstruction()(state, verbose)
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
          Allocate(VLANTag, 4),
          Assign(VLANTag, ConstantValue(vlan))
        )
      }
      else
        NoOp,
      InstructionBlock(
        if (!collected.isEmpty())
          collected.toArray().foldRight(Fail("VLAN unknown") : Instruction)((x, acc) =>  {
            val t  = x.asInstanceOf[Integer].intValue()
            val isvlans = br.getIfacesForVlan(t).filter { x => x != iface.getName }.map { x => br.getOvsNic(x) }
            If (Constrain(VLANTag, :==:(ConstantValue(t))),
              Fork(isvlans.map ( y => {
                if (y.isAccess())
                {
                   InstructionBlock(
                       Deallocate(VLANTag, 4), 
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
          Fork(br.getNICs.filter { x => x != iface.getName }.map { x => new ExitBridge(br, x, world) })
      )
    )
  }
  
  override def apply(state2 : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    System.out.println(toString)
    val ib = generateInstruction()
    ib(state2, verbose)
  }
}

class ExitOFPort(ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
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
          Fail("DeadEndInternalIface(" + iface.getName + "," + br.getName + "," + br.getComputer.getName + ")")
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
        Allocate("InPort"),
        Assign("InPort", ConstantValue(ofPort.getPortNo)),
        new EnterOFTable(Nil, table, ofPort, br, iface, world),
        Deallocate("InPort")
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
            v => new EnterFlowEntry(v._1, v._2, table, ofPort, br, iface, world)
           }
         )
       ), 
       new EnterTableMiss(table, ofPort, br, iface, world), 
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
              new EnterBridgeNormal(br, iface, world)
      		  }
      		  else
      		  {
        		  new ExitOFPort(br.getOpenFlowPort(action.getPort.intValue()), br, iface, world)
      		  }
      		}
        case action : NormalAction => new EnterBridgeNormal(br, iface, world)
        case action : DropAction => Fail("Drop action encountered")
        case action : LoadAction => { val t = 
              if (action.getTo.isPresent() && action.getFrom.isPresent())
          	  {
          	    Assign(action.getTo.get.getName, :@(action.getFrom.get.getName))
          	  }
          	  else if (action.getTo.isPresent() && !action.getFrom.isPresent())
          	  {
          	    Assign(action.getTo.get.getName, ConstantValue(action.getValue.get))
          	  }
          	  else if (!action.getTo.isPresent() && action.getFrom.isPresent)
          	  {
          	    Assign(action.getFrom.get.getName, :@(action.getFrom.get.getName))
          	  }
          	  else 
          	  {
          	    throw new IllegalArgumentException("Cannot do that, load action must have at least one parameter")
          	  }
          	  t
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
        case action : ModVlanVidAction => Assign(VLANTag, ConstantValue(action.getVlanId))
        case action : MoveAction => Assign(action.getTo.getName, :@(action.getFrom.getName))
        case action : PushVlanAction => Allocate(VLANTag, 4)
        case action : SetFieldAction => Assign(action.getField.getName, ConstantValue(action.getValue))
        case action : SetTunnelAction => Assign(TunId, ConstantValue(action.getTunId))
        case action : StripVlanAction => Deallocate(Tag("VLANId"), 4)
        case action : LearnAction => NoOp
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
       case "dl_type" => Right(EtherType)
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
       case "pkt_mark" => Left("PacketMark")
       case "out_port" => Left("OutPort")
       case "vlan_tci" => Right(VLANTag)
       case "NXM_OF_IN_PORT" => Left("InPort")
       case "NXM_OF_ETH_DST" => Right(EtherSrc)
       case "NXM_OF_ETH_SRC" => Right(EtherDst)
       case "NXM_OF_ETH_TYPE" => Right(EtherType) 
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
       case _ =>    throw new UnsupportedOperationException("Field " + name + " is not translatable in Symnet... Yet")
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
        new EnterOFTable(Nil : List[Action], destTable, destPort, br, iface, world)
      }
      else
      {
        NoOp
      }

      InstructionBlock(
          Assign("FFFMatched", ConstantValue(index)),
          InstructionBlock(
              filteredActions.toList.map { x => new ApplyAction(x, table, ofPort, br, iface, world) }
          ),
          jumpTarget
      )
    }
    
    val instr = if (!matches.isEmpty())
      If (Constrain("FFFMatched", :==:(ConstantValue(-1))),
        InstructionBlock(
          Forward(toString),
          matches.map { x => FieldNameTranslator(x.getField.getName) match {
              case Right(tag) if tag == EtherDst && x.getMask == 0x010000000000L  => {
                println("Found unicast/broadcast stuff")
                if (x.getValue == 0x010000000000L)
                  Constrain("IsUnicast", :==:(ConstantValue(0)))
                else
                  Constrain("IsUnicast", :==:(ConstantValue(1)))
              }
              case Right(tag) => Constrain(tag, :==:(ConstantValue(x.getValue))) 
              case Left(mem) => Constrain(mem, :==:(ConstantValue(x.getValue)))
            }
          }.foldRight(listOfInstructions)((u, acc) => {
            If (u, acc, NoOp)
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
