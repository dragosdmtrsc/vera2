package org.change.v2.model.openflow.actions;
import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class InPortAction extends Action {
public InPortAction() { super(ActionType.InPort); }
@Override
	public void accept(FlowVisitor visitor) {
visitor.visit(this);
}
}