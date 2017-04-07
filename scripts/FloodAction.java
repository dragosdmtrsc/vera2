package org.change.v2.model.openflow.actions;
import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class FloodAction extends Action {
public FloodAction() { super(ActionType.Flood); }
@Override
	public void accept(FlowVisitor visitor) {
visitor.visit(this);
}
}