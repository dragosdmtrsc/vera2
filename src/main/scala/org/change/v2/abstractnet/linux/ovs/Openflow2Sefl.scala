package org.change.v2.abstractnet.linux.ovs

import scala.collection.JavaConversions._

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.:==:
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.OVSBridge
import org.change.v2.model.OpenFlowTable
import org.change.v2.model.openflow.Action
import org.change.v2.model.openflow.BaseFlowVisitor
import org.change.v2.model.openflow.FlowEntry
import org.change.v2.model.openflow.Match
import org.change.v2.model.openflow.actions.AllAction
import org.change.v2.model.openflow.actions.ApplyActionsAction
import org.change.v2.model.openflow.actions.ClearActionsAction
import org.change.v2.model.openflow.actions.ControllerAction
import org.change.v2.model.openflow.actions.DecMplsTtlAction
import org.change.v2.model.openflow.actions.DecTtlAction
import org.change.v2.model.openflow.actions.DropAction
import org.change.v2.model.openflow.actions.EnqueueAction
import org.change.v2.model.openflow.actions.ExitAction
import org.change.v2.model.openflow.actions.FinTimeoutAction
import org.change.v2.model.openflow.actions.FloodAction
import org.change.v2.model.openflow.actions.GotoTableAction
import org.change.v2.model.openflow.actions.InPortAction
import org.change.v2.model.openflow.actions.LearnAction
import org.change.v2.model.openflow.actions.LoadAction
import org.change.v2.model.openflow.actions.LocalAction
import org.change.v2.model.openflow.actions.ModDlDstAction
import org.change.v2.model.openflow.actions.ModDlSrcAction
import org.change.v2.model.openflow.actions.ModNwDstAction
import org.change.v2.model.openflow.actions.ModNwSrcAction
import org.change.v2.model.openflow.actions.ModNwTosAction
import org.change.v2.model.openflow.actions.ModTpDstAction
import org.change.v2.model.openflow.actions.ModTpSrcAction
import org.change.v2.model.openflow.actions.ModVlanPcpAction
import org.change.v2.model.openflow.actions.ModVlanVidAction
import org.change.v2.model.openflow.actions.MoveAction
import org.change.v2.model.openflow.actions.NormalAction
import org.change.v2.model.openflow.actions.OutputAction
import org.change.v2.model.openflow.actions.PopAction
import org.change.v2.model.openflow.actions.PopMplsAction
import org.change.v2.model.openflow.actions.PopQueueAction
import org.change.v2.model.openflow.actions.PushAction
import org.change.v2.model.openflow.actions.PushMplsAction
import org.change.v2.model.openflow.actions.PushVlanAction
import org.change.v2.model.openflow.actions.ResubmitAction
import org.change.v2.model.openflow.actions.SampleAction
import org.change.v2.model.openflow.actions.SetFieldAction
import org.change.v2.model.openflow.actions.SetMplsTtlAction
import org.change.v2.model.openflow.actions.SetQueueAction
import org.change.v2.model.openflow.actions.SetTunnel64Action
import org.change.v2.model.openflow.actions.SetTunnelAction
import org.change.v2.model.openflow.actions.StripVlanAction
import org.change.v2.model.openflow.actions.WriteMetadataAction
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.instructions.Deallocate
import org.change.v2.analysis.processingmodels.instructions.Allocate

class Openflow2Sefl(bridge : OVSBridge)  {
  
  private var tableVisitor = new TableVisitor()
  
  def executeTable(tab : Int) : Instruction = {
    val table = bridge.getConfig.getTables()(tab)
    table.accept(tableVisitor)
    tableVisitor.getInstruction
  }
}


class TableVisitor extends BaseFlowVisitor {
  
  private var instruction : Instruction = NoOp
  
  def getInstruction = instruction
  override def visit(table : OpenFlowTable) = {
    instruction = NoOp
  }
  
  
  override def visit(entry : FlowEntry) {
    val ifin = InstructionBlock(entry.getActions.map(visitAction).+:(Assign("Matched", ConstantValue(1)))) : Instruction
    If(Constrain("Matched", :~:(:==:(ConstantValue(1)))),
        InstructionBlock(entry.getMatches.map(visitMatch).foldRight(ifin)((s, t) => If (s, t, NoOp))),
        NoOp)
  }
  
  override def visit(m : Match) = {
    val iff = visitMatch(m)
  }
  
  protected def visitMatch(m : Match) : Instruction = {
    Constrain(m.getField.getName, :==:(ConstantValue(m.getValue)))
  }
  
  private def visitAction(a : Action) : Instruction = {
    var vi = new ActionVisitor()
    a.accept(vi)
    vi.getInstruction._1
  }
  
}

class ActionVisitor extends BaseFlowVisitor {
  private var instruction : (Instruction, String, Instruction) = (NoOp, "", NoOp)
  def getInstruction = instruction

  	
	override def visit(action : OutputAction) {
    this.instruction = (
        InstructionBlock(
            Assign("OutputAction", ConstantValue(1))),
        "OutputAction",
    		if (action.isReg())
    		{
    		  //TODO: Link to OVS Bridge to do this Fw logic
    		  If (Constrain(action.getRegName.getName, :==:(ConstantValue(1))),
    		      Forward("OFOut___" + 1),
    		      NoOp)
    		}
    		else
    		{
    		  Forward("OFOut___" + action.getPort)
    		})
	}

	
	override def visit(action : EnqueueAction) {
		
	}

	
	override def visit(action : NormalAction) {
    this.instruction = (
        InstructionBlock(
            Assign("NormalAction", ConstantValue(1))),
        "NormalAction",
    		Forward("L2"))
	}

	
	override def visit(action : FloodAction) {
    this.instruction = (
        InstructionBlock(
            Assign("FloodAction", ConstantValue(1))),
        "FloodAction",
    		Forward("Flood"))
	}

	
	override def visit(action : AllAction) {
    this.instruction = (
        InstructionBlock(
            Assign("AllAction", ConstantValue(1))),
        "AllAction",
    		Forward("All"))
	}

	
	override def visit(action : ControllerAction) {
    this.instruction = (
        InstructionBlock(
            Assign("ControllerAction", ConstantValue(1))),
        "ControllerAction",
    		Forward("Controller"))
	}

	
	override def visit(action : LocalAction) {
		this.instruction = (
        InstructionBlock(
            Assign("LocalAction", ConstantValue(1))),
        "LocalAction",
    		Forward("Local"))
	}

	
	override def visit(action : InPortAction) {
		 this.instruction = (
        InstructionBlock(
            Assign("InPortAction", ConstantValue(1))),
        "InPortAction",
  		  //TODO: Link to OVS Bridge to do this Fw logic
    		NoOp)
	}

	
	override def visit(action : DropAction) {
		this.instruction = (
        InstructionBlock(
            Assign("DropAction", ConstantValue(1))),
        "DropAction",
    		Fail("Explicit drop"))
	}

	
	override def visit(action : ModVlanVidAction) {
		this.instruction = (
        InstructionBlock(
            Assign("DropAction", ConstantValue(1))),
        "DropAction",
    		Assign(Tag("VLANId"), ConstantValue(action.getVlanId)))
	}

	
	override def visit(action : ModVlanPcpAction) {

	}

	
	override def visit(action : StripVlanAction) {
		this.instruction = (
        InstructionBlock(
            Assign("StripVlanAction", ConstantValue(1))),
        "StripVlanAction",
    		Deallocate(Tag("VLANId"), 4))		 //TODO: Verify VLANId length
	}

	
	override def visit(action : PushVlanAction) {
		this.instruction = (
        InstructionBlock(
            Assign("PushVlanAction", ConstantValue(1))),
        "PushVlanAction",
    		Allocate(Tag("VLANId"), 4))
	}

	
	override def visit(action : PushMplsAction) {
		
	}

	
	override def visit(action : PopMplsAction) {
		
	}

	
	override def visit(action : ModDlSrcAction) {
		this.instruction = (
        InstructionBlock(
            Assign("ModDlSrcAction", ConstantValue(1))),
        "ModDlSrcAction",
    		Assign(Tag("L2Src"), ConstantValue(action.getMacAddr)))
	}

	
	override def visit(action : ModDlDstAction) {
		this.instruction = (
        InstructionBlock(
            Assign("ModDlDstAction", ConstantValue(1))),
        "ModDlDstAction",
    		Assign(Tag("L2Dst"), ConstantValue(action.getMacAddr)))		
	}

	
	override def visit(action : ModNwSrcAction) {
		this.instruction = (
        InstructionBlock(
            Assign("ModNwSrcAction", ConstantValue(1))),
        "ModNwSrcAction",
    		Assign(Tag("L3Src"), ConstantValue(action.getIpAddress)))			
	}

	
	override def visit(action : ModNwDstAction) {
		this.instruction = (
        InstructionBlock(
            Assign("ModNwDstAction", ConstantValue(1))),
        "ModNwDstAction",
    		Assign(Tag("L3Dst"), ConstantValue(action.getIpAddress)))				
	}

	
	override def visit(action : ModTpSrcAction) {
		this.instruction = (
        InstructionBlock(
            Assign(action.getClass.getSimpleName, ConstantValue(1))),
        action.getClass.getSimpleName,
    		Assign(Tag("L4Src"), ConstantValue(action.getIpAddress)))		//TODO: Please refactor getter
	}

	
	override def visit(action : ModTpDstAction) {
		this.instruction = (
        InstructionBlock(
            Assign(action.getClass.getSimpleName, ConstantValue(1))),
        action.getClass.getSimpleName,
    		Assign(Tag("L4Dst"), ConstantValue(action.getIpAddress)))		//TODO: Please refactor getter
	}

	
	override def visit(action : ModNwTosAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ResubmitAction) {
		// TODO Auto-generated method stub
		this.instruction = (
        InstructionBlock(
            Assign(action.getClass.getSimpleName, ConstantValue(1))),
        action.getClass.getSimpleName,
        if (action.getInPort.isPresent() && action.getTable.isPresent())
        {
          InstructionBlock(Assign("InPort", ConstantValue(action.getInPort.get)), 
              Forward("OFTab___" + action.getTable.get))
        }
        else if (action.getInPort.isPresent() && !action.getTable.isPresent())
        {
          InstructionBlock(Assign("InPort", ConstantValue(action.getInPort.get)), 
              Forward("OFTab___" + 0))
        }
        else if (!action.getInPort.isPresent() && action.getTable.isPresent())
        {
          (Forward("OFTab___" + action.getTable.get))
        }
        else
        {
          (Forward("OFTab___" + 0))
        }
        //TODO: Please input logic for Forward to table and/or port
    		  
    		)
	}

	
	override def visit(action : SetTunnelAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : SetTunnel64Action) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : SetQueueAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PopQueueAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : DecTtlAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : SetMplsTtlAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : DecMplsTtlAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : MoveAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : LoadAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PushAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PopAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : SetFieldAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ApplyActionsAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ClearActionsAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : WriteMetadataAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : GotoTableAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : FinTimeoutAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : SampleAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : LearnAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ExitAction) {
		// TODO Auto-generated method stub
		
	}
  
}
