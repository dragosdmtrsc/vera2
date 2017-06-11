package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class ModDlSrcAction extends Action {
	private Long macAddr;

	public Long getMacAddr() {
		return macAddr;
	}

	public ModDlSrcAction() {
		super(ActionType.ModDlSrc);
	}

	public ModDlSrcAction(Long decodeMAC) {
		this();
		this.macAddr = decodeMAC;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}