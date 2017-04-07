package org.change.v2.model.openflow;

import java.util.HashMap;
import java.util.Map;

public abstract class Action implements FlowAcceptor {

	private String name;
	private ActionType type;

	public Action(ActionType type) {
		super();
		this.type = type;
	}



	public ActionType getType() {
		return type;
	}
	
	

	public void setType(ActionType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
