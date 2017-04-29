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
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fork

class EnterIface(pcName : String, iface : String, world : WorldModel) extends Instruction {
  override def apply(state : State, verbose : Boolean) : 
    (List[State], List[State]) = {
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(iface)
    if (pc.isBridged(iface))
    {
      val br = pc.getBridge(iface)
      if (br.isInstanceOf[OVSBridge])
        new EnterBridge(br.asInstanceOf[OVSBridge], nic, world)(state)
      else
        throw new UnsupportedOperationException("Not implemented for Linux Bridge")
    }
    else
    {
       throw new UnsupportedOperationException("Not implemented for iptables")
    }
  }
}


class EnterBridge(br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
    override def apply(state : State, verbose : Boolean) : 
      (List[State], List[State]) = {
      val ofPort = br.getOpenFlowPort(iface.getName)
      new EnterOFPort(ofPort, br, iface, world)(state, verbose)
    }
}


class EnterBridgeNormal(br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction {
  override def apply(state : State, verbose : Boolean) : 
      (List[State], List[State]) = {
       //TODO: Add code here
      val collected = br.getDistinctVlans()
      val instr = InstructionBlock(collected.toArray().map { 
          x  => If(Constrain(Tag("VLANId"), :==:(ConstantValue(x.asInstanceOf[Integer].intValue()))),
          Fork(
              br.getIfacesForVlan(x.asInstanceOf[Integer].intValue()).map { 
                  y => new ExitBridge(br, br.getOvsNic(y), world) 
                }
              ), 
              NoOp)
      })
      instr(state, verbose)
    }}

class ExitOFPort(ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
    override def apply(state : State, verbose : Boolean) : 
      (List[State], List[State]) = {
      val thePort = br.getOvsNic(ofPort)
      new ExitBridge(br, thePort, world)(state, verbose)
    }
}


class ExitBridge(br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
  override def apply(state : State, verbose : Boolean) : 
    (List[State], List[State]) = 
  {
    val isInterbridge = iface.getType == "patch"
    val isVxlan = iface.getType == "vxlan"
    val isGre = iface.getType == "gre"
    if (isInterbridge)
    {
      val peer = iface.getOptions.get("peer")
      new EnterIface(br.getComputer.getName, peer, world)(state, verbose)
    }
    else if (isVxlan || isGre)
    {
      val peer2 : String = iface.getOptions.get("remote-ip")
      val pc = world.getComputerByIp(peer2)
      val ifacePeer = world.getNICByIp(peer2)
      new EnterIface(pc.getName, iface.getName, world)(state, verbose)
    }
    else
    {
      throw new UnsupportedOperationException("iptables not implemented... Yet")
    }
  }
}



class EnterOFPort(ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel) extends Instruction
{
    override def apply(state : State, verbose : Boolean) : 
      (List[State], List[State]) = {
      val table = br.getConfig.getTables.get(0)
      new EnterOFTable(Nil, table, ofPort, br, iface, world)(state, verbose)
    }
}

class EnterOFTable(actionSet : List[Action], table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
    extends Instruction {
  
  private def merge(older : List[Action], newer : List[Action]) : List[Action] = {
    older.map { x => {
        val nx = newer.find { y => y.getType == x.getType } 
        if (nx.isDefined)
        {
          nx.get
        }
        else
        {
          x
        }
      }
    } ++ newer.filter { x => !older.exists { y => y.getType == x.getType } }
  }
  
  override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
      val table = br.getConfig.getTables.get(0)
      val st = Assign("Matched", ConstantValue(-1))(state)._1(0)
      val flows = table.getEntries
      val doFlows = InstructionBlock(flows.zipWithIndex.map { 
        v => new EnterFlowEntry(v._1, v._2, ofPort, br, iface, world) 
       }.:+(new EnterTableMiss(table, ofPort, br, iface, world)) )(state, verbose)

      doFlows._1.foldLeft((List[State](), doFlows._2))((acc, s) => {
            val matched = s.memory.eval("Matched").get.asInstanceOf[ConstantValue].value
            val actions = flows.get(matched.asInstanceOf[Int]).getActions
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
              if (gotoTable.isDefined)
              {
                val prior = 
                if (applyAction.isDefined)
                {
                  new ApplyAction(applyAction.get, table, ofPort, br, iface, world)
                }
                else
                {
                  NoOp
                }
                val theTable = gotoTable.get.asInstanceOf[ResubmitAction]
                val inPort = if (theTable.getInPort.isPresent()) br.getOpenFlowPort(theTable.getInPort.get.asInstanceOf[Int]) 
                  else ofPort
                val inTable = if (theTable.getTable.isPresent())
                  br.getConfig.getTables.get(theTable.getTable.get.asInstanceOf[Int])
                  else table
                InstructionBlock(prior, new EnterOFTable(merge(actionSet, filteredActions.toList), inTable, inPort, br, iface, world))
              }
              else
              {
                InstructionBlock(merge(actionSet, actions.toList).map { x => new ApplyAction(x, table, ofPort, br, iface, world) })
              }
            }
            val execd = listOfInstructions(state, verbose)
            
            (acc._1 ++ execd._1, acc._2 ++ execd._2)
          })
    }
}

class ApplyAction(action : Action, table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
  override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    action match {
      case action : ApplyActionsAction => InstructionBlock(action.getActions.map { x => 
        new ApplyAction(x, table, ofPort, br, iface, world)
        })(state, verbose)
      case action : OutputAction => 
        if (action.isReg())
    		{
    		  throw new UnsupportedOperationException("Don't know how to handle registers... Yet")
    		}
    		else
    		{
    		  new ExitOFPort(br.getOpenFlowPort(action.getPort.asInstanceOf[Int]), br, iface, world)(state, verbose)
    		}
      case action : NormalAction => new EnterBridgeNormal(br, iface, world)(state, verbose)
      case action : DropAction => Fail("Drop action encountered")(state, verbose)
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
        	  t(state, verbose)
      }
      case action : ModDlDstAction => Assign(Tag("L2Dst"), ConstantValue(action.getMacAddr))(state, verbose)
      case action : ModDlSrcAction => Assign(Tag("L2Src"), ConstantValue(action.getMacAddr))(state, verbose)
      case action : ModNwSrcAction => Assign(Tag("L3Src"), ConstantValue(action.getIpAddress))(state, verbose)
      case action : ModNwDstAction => Assign(Tag("L3Dst"), ConstantValue(action.getIpAddress))(state, verbose)
      case action : ModTpDstAction => Assign(Tag("L4Dst"), ConstantValue(action.getIpAddress))(state, verbose)
      case action : ModTpSrcAction => Assign(Tag("L4Src"), ConstantValue(action.getIpAddress))(state, verbose)
      case action : ModVlanVidAction => Assign(Tag("VLANId"), ConstantValue(action.getVlanId))(state, verbose)
      case action : MoveAction => Assign(action.getTo.getName, :@(action.getFrom.getName))(state, verbose)
      case action : PushVlanAction => Allocate(Tag("VLANId"), 4)(state, verbose)
      case action : SetFieldAction => Assign(action.getField.getName, ConstantValue(action.getValue))(state, verbose)
      case action : SetTunnelAction => Assign(Tag("TunnelId"), ConstantValue(action.getTunId))(state, verbose)
      case action : StripVlanAction => Deallocate(Tag("VLANId"), 4)(state, verbose)
      case _ =>     throw new UnsupportedOperationException("Cannot handle " + action.getClass.getName + "... Yet")
    }
    
    
  }
}


object FieldNameTranslator
{
  def apply(name : String) : Either[String, Tag] = {
     name match {
       case "in_port" => Left("InPort")
       case "dl_vlan" => Right(Tag("VlanID"))
       case "dl_vlan_pcp" => Right(Tag("VlanPCP"))
       case "dl_src" => Right(Tag("L2Src"))
       case "dl_dst" => Right(Tag("L2Dst"))
       case "dl_type" => Right(Tag("L2Type"))
       case "nw_src" => Right(Tag("L3Src"))
       case "nw_dst" => Right(Tag("L3Dst"))
       case "nw_proto" => Right(Tag("L3Type"))
       case "nw_ttl" => Right(Tag("L3TTL"))
       case "tp_src" => Right(Tag("L4Src"))
       case "tp_dst" => Right(Tag("L4Dst"))
       case "icmp_type" => Right(Tag("ICMPType"))
       case "icmp_code" => Right(Tag("ICMPCode"))
       case "metadata" => Left("OFMetadata")
       case "ipv6_src" => Right(Tag("L3Src"))
       case "ipv6_dst" => Right(Tag("L3Dst"))
       case "ipv6_label" => Right(Tag("IPv6Label"))
       case "tun_id" => Right(Tag("TunnelId")) 
       case "tun_src" => Right(Tag("TunnelSrc"))
       case "tun_dst" => Right(Tag("TunnelDst"))
       case "pkt_mark" => Left("PacketMark")
       case "out_port" => Left("OutPort")
       case _ =>    throw new UnsupportedOperationException("Field " + name + " is not translatable in Symnet... Yet")
    }
  }
}


class EnterTableMiss(table : OpenFlowTable, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction
{
    override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
      Fail("Table miss encountered")(state, verbose)
      //TODO : add code here to distinguish between the actual table misses
    }
}

class EnterFlowEntry(flowEntry : FlowEntry, index : Int, ofPort : OpenFlowNIC, br : OVSBridge, iface : NIC, world : WorldModel)
  extends Instruction {
  override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    
    val matches = flowEntry.getMatches
    matches.map { x => FieldNameTranslator(x.getField.getName) match {
        case Right(tag) => Constrain(tag, :==:(ConstantValue(x.getValue))) 
        case Left(mem) => Constrain(mem, :==:(ConstantValue(x.getValue)))
      }
    }.foldRight(Assign("Matched", ConstantValue(index)))((u, acc) => {
      If (u, acc, NoOp)
    })(state, verbose)
  }
}

object OVSInstructionExecutor
{
  
  def main(argv : Array[String]) {    
  }
}