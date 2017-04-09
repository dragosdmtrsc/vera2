package org.change.v2.model.openflow.actions;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.Match;

public class LearnAction extends Action {
	private List<Action> actions = new ArrayList<Action>();
	private List<Match> matches = new ArrayList<Match>();

	public LearnAction() {
		super(ActionType.Learn);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	public List<Match> getMatches() {
		// TODO Auto-generated method stub
		return matches;
	}
	
	public List<Action> getActions() {
		return this.actions;
	}

	@Override
	public String toString() {
		return "LearnAction [matches=" + matches + ", actions=" + actions  + "]";
	}
	
	
	
}