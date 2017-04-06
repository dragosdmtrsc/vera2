package org.change.v2.model.openflow;

public abstract class Action implements FlowAcceptor {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
