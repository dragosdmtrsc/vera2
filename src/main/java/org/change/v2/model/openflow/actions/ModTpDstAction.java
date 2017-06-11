package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;

public class ModTpDstAction extends Action {
	private Long port;

	public Long getIpAddress() {
		return port;
	}

	public ModTpDstAction() {
		super(ActionType.ModTpDst);
	}

	public ModTpDstAction(Long decodeIP4) {
		this();
		this.port = decodeIP4;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}