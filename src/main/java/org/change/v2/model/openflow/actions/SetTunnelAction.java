package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;

public class SetTunnelAction extends Action {
	private Long tunId;

	public Long getTunId() {
		return tunId;
	}

	public SetTunnelAction(Long long1) {
		super(ActionType.SetTunnel);
		this.tunId = long1;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "SetTunnelAction [tunId=" + tunId + "]";
	}
	
	
}