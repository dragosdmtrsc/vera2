package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;

public class ModNwSrcAction extends Action {
	private Long ipAddress;

	public Long getIpAddress() {
		return ipAddress;
	}

	public ModNwSrcAction() {
		super(ActionType.ModNwSrc);
	}

	public ModNwSrcAction(Long decodeIP4) {
		this();
		this.ipAddress = decodeIP4;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}