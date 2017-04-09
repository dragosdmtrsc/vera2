package org.change.v2.model.openflow.actions;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.FlowVisitor;

import org.change.v2.model.openflow.ActionType;

public class WriteMetadataAction extends Action {
	
	public WriteMetadataAction(long metadata, long mask) {
		this();
		this.metadata = metadata;
		this.mask = mask;
	}
	
	public WriteMetadataAction(long metadata)
	{
		this(metadata, 32);
	}

	private long metadata, mask;
	
	
	public long getMetadata() {
		return metadata;
	}

	public long getMask() {
		return mask;
	}

	public WriteMetadataAction() {
		super(ActionType.WriteMetadata);
	}

	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
}