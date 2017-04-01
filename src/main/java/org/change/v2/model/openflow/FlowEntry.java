package org.change.v2.model.openflow;

import java.util.HashMap;
import java.util.Map;

import org.change.v2.model.Acceptor;
import org.change.v2.model.IVisitor;

public class FlowEntry implements Acceptor {

	private Map<String, String> matches = new HashMap<String, String>();
	private Map<String, String> actions = new HashMap<String, String>();
	private int tableNumber = 0;

	public int getTableNumber() {
		return tableNumber;
	}
	public void setTableNumber(int tableNumber) {
		this.tableNumber = tableNumber;
	}
	public Map<String, String> getMatches() {
		return matches;
	}
	public Map<String, String> getActions() {
		return actions;
	}
	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
}
