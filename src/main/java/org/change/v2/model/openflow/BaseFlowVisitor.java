package org.change.v2.model.openflow;

import org.change.v2.model.OpenFlowTable;
import org.change.v2.model.openflow.actions.AllAction;
import org.change.v2.model.openflow.actions.ApplyActionsAction;
import org.change.v2.model.openflow.actions.ClearActionsAction;
import org.change.v2.model.openflow.actions.ControllerAction;
import org.change.v2.model.openflow.actions.DecMplsTtlAction;
import org.change.v2.model.openflow.actions.DecTtlAction;
import org.change.v2.model.openflow.actions.DropAction;
import org.change.v2.model.openflow.actions.EnqueueAction;
import org.change.v2.model.openflow.actions.ExitAction;
import org.change.v2.model.openflow.actions.FinTimeoutAction;
import org.change.v2.model.openflow.actions.FloodAction;
import org.change.v2.model.openflow.actions.GotoTableAction;
import org.change.v2.model.openflow.actions.InPortAction;
import org.change.v2.model.openflow.actions.LearnAction;
import org.change.v2.model.openflow.actions.LoadAction;
import org.change.v2.model.openflow.actions.LocalAction;
import org.change.v2.model.openflow.actions.ModDlDstAction;
import org.change.v2.model.openflow.actions.ModDlSrcAction;
import org.change.v2.model.openflow.actions.ModNwDstAction;
import org.change.v2.model.openflow.actions.ModNwSrcAction;
import org.change.v2.model.openflow.actions.ModNwTosAction;
import org.change.v2.model.openflow.actions.ModTpDstAction;
import org.change.v2.model.openflow.actions.ModTpSrcAction;
import org.change.v2.model.openflow.actions.ModVlanPcpAction;
import org.change.v2.model.openflow.actions.ModVlanVidAction;
import org.change.v2.model.openflow.actions.MoveAction;
import org.change.v2.model.openflow.actions.NormalAction;
import org.change.v2.model.openflow.actions.OutputAction;
import org.change.v2.model.openflow.actions.PopAction;
import org.change.v2.model.openflow.actions.PopMplsAction;
import org.change.v2.model.openflow.actions.PopQueueAction;
import org.change.v2.model.openflow.actions.PushAction;
import org.change.v2.model.openflow.actions.PushMplsAction;
import org.change.v2.model.openflow.actions.PushVlanAction;
import org.change.v2.model.openflow.actions.ResubmitAction;
import org.change.v2.model.openflow.actions.SampleAction;
import org.change.v2.model.openflow.actions.SetFieldAction;
import org.change.v2.model.openflow.actions.SetMplsTtlAction;
import org.change.v2.model.openflow.actions.SetQueueAction;
import org.change.v2.model.openflow.actions.SetTunnel64Action;
import org.change.v2.model.openflow.actions.SetTunnelAction;
import org.change.v2.model.openflow.actions.StripVlanAction;
import org.change.v2.model.openflow.actions.WriteMetadataAction;

public class BaseFlowVisitor implements FlowVisitor {

	@Override
	public void visit(FlowEntry flowEntry) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OutputAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(EnqueueAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NormalAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FloodAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(AllAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ControllerAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LocalAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(InPortAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DropAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModVlanVidAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModVlanPcpAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(StripVlanAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PushVlanAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PushMplsAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PopMplsAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModDlSrcAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModDlDstAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModNwSrcAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModNwDstAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModTpSrcAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModTpDstAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ModNwTosAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ResubmitAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetTunnelAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetTunnel64Action action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetQueueAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PopQueueAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DecTtlAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetMplsTtlAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(DecMplsTtlAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(MoveAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LoadAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PushAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PopAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SetFieldAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ApplyActionsAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ClearActionsAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(WriteMetadataAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(GotoTableAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(FinTimeoutAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(SampleAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(LearnAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ExitAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(OpenFlowTable openFlowTable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Match match) {
		// TODO Auto-generated method stub
		
	}

}
