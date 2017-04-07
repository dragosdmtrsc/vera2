package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class ModDlDstAction extends Action {
	public ModDlDstAction() {
		super(ActionType.ModDlDst);
	}
	private Long macAddr;

	public Long getMacAddr() {
		return macAddr;
	}
	
	public ModDlDstAction(Long decodeMAC) {
		this();
		this.macAddr = decodeMAC;
	}
	
	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}