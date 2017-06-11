package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class ModVlanVidAction extends Action {
	private Long vlanId;

	public Long getVlanId() {
		return vlanId;
	}

	public ModVlanVidAction() {
		super(ActionType.ModVlanVid);
	}

	public ModVlanVidAction(Long decodeLong) {
		this();
		this.vlanId = decodeLong;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}