package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

public class MarkOption extends SimpleOption {

	@Override
	public String toString() {
		return "MarkOption [value=" + value + ", mask=" + mask + ", name=" + name + "]";
	}
	Long value;
	private long mask;
	private String name;
	public MarkOption(boolean neg, Long ip, long mask2, String name) {
		super();
		this.setNeg(neg);
		this.value = ip;
		this.mask = mask2;
		this.name = name;
	}
	public Long getValue() {
		return value;
	}
	public void setValue(Long value) {
		this.value = value;
	}
	public long getMask() {
		return mask;
	}
	public void setMask(long mask) {
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
