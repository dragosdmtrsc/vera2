package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

public class MarkOption extends SimpleOption {

	private int value, mask;
	private String name;
	public MarkOption(boolean neg, int value, int mask, String name) {
		super();
		this.setNeg(neg);
		this.value = value;
		this.mask = mask;
		this.name = name;
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	
	
	
	
}
