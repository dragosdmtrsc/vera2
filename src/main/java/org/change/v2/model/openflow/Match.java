package org.change.v2.model.openflow;

public class Match implements FlowAcceptor {

	private String field;
	private int value;
	private int mask;
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public int getMask() {
		return mask;
	}
	public void setMask(int mask) {
		this.mask = mask;
	}
	@Override
	public void accept(FlowVisitor visitor) {
	}
	
	
}
