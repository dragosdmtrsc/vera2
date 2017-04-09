package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;

public class ModNwDstAction extends Action {
	private Long ipAddress;

	public Long getIpAddress() {
		return ipAddress;
	}

	public ModNwDstAction() {
		super(ActionType.ModNwDst);
	}

	public ModNwDstAction(Long decodeIP4) {
		this();
		this.ipAddress = decodeIP4;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}