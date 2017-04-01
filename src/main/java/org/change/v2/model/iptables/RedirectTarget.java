package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

public class RedirectTarget extends IPTablesTarget {

	private int toPortStart, toPortEnd = -2;
	
	
	public int getToPortStart() {
		return toPortStart;
	}


	public void setToPortStart(int toPortStart) {
		this.toPortStart = toPortStart;
	}


	public int getToPortEnd() {
		return toPortEnd;
	}


	public void setToPortEnd(int toPortEnd) {
		this.toPortEnd = toPortEnd;
	}


	public RedirectTarget(int toPortStart, int toPortEnd) {
		super();
		this.toPortStart = toPortStart;
		this.toPortEnd = toPortEnd;
	}


	public RedirectTarget(int toPortStart) {
		super();
		this.toPortStart = toPortStart;
	}


	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

}
