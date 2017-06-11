package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.ActionType;

public class ModNwTosAction extends Action {
	private Long tos;

	public Long getTos() {
		return tos;
	}

	public ModNwTosAction() {
		super(ActionType.ModNwTos);
	}

	public ModNwTosAction(Long decodeLong) {
		this();
		this.tos = decodeLong;
		
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}