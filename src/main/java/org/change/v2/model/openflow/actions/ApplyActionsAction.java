package org.change.v2.model.openflow.actions;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowVisitor;

public class ApplyActionsAction extends Action {
	private List<Action> actions = new ArrayList<Action>();

	public List<Action> getActions() {
		return actions;
	}

	public ApplyActionsAction() {
		super(ActionType.ApplyActions);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	public void addAction(Action act) {
		this.actions .add(act);
	}
}