package org.change.v2.model.openflow;

public interface FlowAcceptor {

	void accept(FlowVisitor visitor);
}
