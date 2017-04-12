package org.change.v2.abstractnet.linux.ovs

import org.change.v2.model.OVSBridge
import scala.collection.JavaConversions._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.model.openflow.BaseFlowVisitor
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.OpenFlowTable
import org.change.v2.model.openflow.Match
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.model.openflow.FlowEntry
import org.change.v2.model.openflow.actions.ModVlanPcpAction
import org.change.v2.model.openflow.actions.SetQueueAction
import org.change.v2.model.openflow.actions.PushVlanAction
import org.change.v2.model.openflow.actions.PopAction
import org.change.v2.model.openflow.actions.LoadAction
import org.change.v2.model.openflow.actions.StripVlanAction
import org.change.v2.model.openflow.actions.FinTimeoutAction
import org.change.v2.model.openflow.actions.ClearActionsAction
import org.change.v2.model.openflow.actions.LocalAction
import org.change.v2.model.openflow.actions.DropAction
import org.change.v2.model.openflow.actions.SetTunnelAction
import org.change.v2.model.openflow.actions.SetTunnel64Action
import org.change.v2.model.openflow.actions.SetFieldAction
import org.change.v2.model.openflow.actions.AllAction
import org.change.v2.model.openflow.actions.NormalAction
import org.change.v2.model.openflow.actions.PushAction
import org.change.v2.model.openflow.actions.OutputAction
import org.change.v2.model.openflow.actions.SampleAction
import org.change.v2.model.openflow.actions.ResubmitAction
import org.change.v2.model.openflow.actions.InPortAction
import org.change.v2.model.openflow.actions.ModVlanVidAction
import org.change.v2.model.openflow.actions.PushMplsAction
import org.change.v2.model.openflow.actions.DecTtlAction
import org.change.v2.model.openflow.actions.ExitAction
import org.change.v2.model.openflow.actions.ModTpDstAction
import org.change.v2.model.openflow.actions.GotoTableAction
import org.change.v2.model.openflow.actions.SetMplsTtlAction
import org.change.v2.model.openflow.actions.EnqueueAction
import org.change.v2.model.openflow.actions.ApplyActionsAction
import org.change.v2.model.openflow.actions.DecMplsTtlAction
import org.change.v2.model.openflow.actions.WriteMetadataAction
import org.change.v2.model.openflow.actions.ModNwTosAction
import org.change.v2.model.openflow.actions.PopMplsAction
import org.change.v2.model.openflow.actions.ModTpSrcAction
import org.change.v2.model.openflow.actions.ModNwDstAction
import org.change.v2.model.openflow.actions.ControllerAction
import org.change.v2.model.openflow.actions.FloodAction
import org.change.v2.model.openflow.actions.ModDlDstAction
import org.change.v2.model.openflow.actions.ModDlSrcAction
import org.change.v2.model.openflow.actions.ModNwSrcAction
import org.change.v2.model.openflow.actions.LearnAction
import org.change.v2.model.openflow.actions.PopQueueAction
import org.change.v2.model.openflow.actions.MoveAction
import org.change.v2.model.openflow.Action
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:, :<:, :>:, :<=:, :>=:}

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
    vi.getInstruction
  }
  
}

class ActionVisitor extends BaseFlowVisitor {
  private var instruction : Instruction = NoOp
  def getInstruction = instruction

  	
	override def visit(action : OutputAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : EnqueueAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : NormalAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : FloodAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : AllAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ControllerAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : LocalAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : InPortAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : DropAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModVlanVidAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModVlanPcpAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : StripVlanAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PushVlanAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PushMplsAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : PopMplsAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModDlSrcAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModDlDstAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModNwSrcAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModNwDstAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModTpSrcAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModTpDstAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ModNwTosAction) {
		// TODO Auto-generated method stub
		
	}

	
	override def visit(action : ResubmitAction) {
		// TODO Auto-generated method stub
		
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
