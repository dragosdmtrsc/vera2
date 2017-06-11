package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowVisitor;

public class NormalAction extends Action {
	public NormalAction() {
		super(ActionType.Normal);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}