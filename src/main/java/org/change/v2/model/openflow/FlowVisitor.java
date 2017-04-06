package org.change.v2.model.openflow;

public interface FlowVisitor {

	void visit(FlowEntry flowEntry);
	
}
