package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class PopQueueAction extends Action {
	public PopQueueAction() {
		super(ActionType.PopQueue);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}