package org.change.v2.model.openflow.actions;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowEntry;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.Match;

public class LearnAction extends Action {

	private FlowEntry flowEntry;

	public FlowEntry getFlowEntry() {
		return flowEntry;
	}

	public LearnAction() {
		super(ActionType.Learn);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	public void setFlow(FlowEntry fe)	
	{
		this.flowEntry = fe;
	}
	
}