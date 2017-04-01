package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

public class ChecksumTarget extends IPTablesTarget {

	public ChecksumTarget() {
		super("CHECKSUM");
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
		
	}

}
