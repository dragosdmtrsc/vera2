package org.change.v2.model.openflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlowEntry implements FlowAcceptor {

	private int priority, table;
	private long cookie;
	
	private List<Match> matches = new ArrayList<Match>();
	private List<Action> actions = new ArrayList<Action>();

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
	
	public int hashCode() {
		return Objects.hash(priority, table, matches.hashCode(), actions.hashCode());
	}
	private String flowDescription;
	
	public void setFlowDescription(String f)
	{
		this.flowDescription = f;
	}
	
	public String getFlowDescription()
	{
		return this.flowDescription;
	}
	
	@Override
	public String toString() {
		return "FlowEntry [table=" +  table + ", priority=" + priority + ", matches=" + 
				matches.stream()
					.map(Object::toString)
                	.collect(Collectors.joining(", "))
				+ ", actions=" + actions.stream()
				.map(Object::toString)
            	.collect(Collectors.joining(", ")) + "]";
	}
	
	
	
}
