package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class PopAction extends Action {
	public PopAction() {
		super(ActionType.Pop);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}