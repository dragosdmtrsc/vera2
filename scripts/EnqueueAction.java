package org.change.v2.model.openflow.actions;
import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class EnqueueAction extends Action {
public EnqueueAction() { super(ActionType.Enqueue); }
@Override
	public void accept(FlowVisitor visitor) {
visitor.visit(this);
}
}