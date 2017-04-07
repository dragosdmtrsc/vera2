package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class ExitAction extends Action {
	public ExitAction() {
		super(ActionType.Exit);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}