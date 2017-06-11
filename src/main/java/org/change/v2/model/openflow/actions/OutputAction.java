package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowVisitor;
import org.change.v2.model.openflow.QualifiedField;

public class OutputAction extends Action {

	public OutputAction() {
		super(ActionType.Output);
	}

	private boolean isReg;
	private QualifiedField regName;

	public boolean isReg() {
		return isReg;
	}

	public void setReg(boolean isReg) {
		this.isReg = isReg;
	}

	public QualifiedField getRegName() {
		return regName;
	}

	public void setRegName(QualifiedField regName) {
		this.regName = regName;
	}

	private Long port;

	public Long getPort() {
		return port;
	}

	public void setPort(Long port) {
		this.port = port;
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "OutputAction [regName=" + regName
				+ ", port=" + port + "]";
	}
	
	
	
}