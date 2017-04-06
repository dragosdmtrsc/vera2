package org.change.v2.model.openflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowEntry implements FlowAcceptor {

	private int priority, table;
	private long cookie;
	
	private List<Match> matches = new ArrayList<Match>();
	private List<Action> actions = new ArrayList<Action>();
	private int tableNumber = 0;

	public int getTableNumber() {
		return tableNumber;
	}
	public void setTableNumber(int tableNumber) {
		this.tableNumber = tableNumber;
	}
	public List<Match> getMatches() {
		return matches;
	}
	public List<Action> getActions() {
		return actions;
	}
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
	public int getTable() {
		return table;
	}
	public void setTable(int table) {
		this.table = table;
	}
	
	public long getCookie() {
		return cookie;
	}
	public void setCookie(long cookie) {
		this.cookie = cookie;
	}
	
	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}
