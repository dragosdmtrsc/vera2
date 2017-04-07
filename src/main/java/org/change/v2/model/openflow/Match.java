package org.change.v2.model.openflow;

public class Match implements FlowAcceptor {

	private QualifiedField field;
	private long value;
	private long mask;
	private boolean hasMask;
	
	public boolean isMasked() {
		return hasMask;
	}
	public void setMasked(boolean hasMask) {
		this.hasMask = hasMask;
	}
	public QualifiedField getField() {
		return field;
	}
	public void setField(QualifiedField field) {
		this.field = field;
	}
	public long getValue() {
		return value;
	}
	public void setValue(long ivalue) {
		this.value = ivalue;
	}
	public long getMask() {
		return mask;
	}
	public void setMask(long mask) {
		this.mask = mask;
	}
	@Override
	public void accept(FlowVisitor visitor) {
	}
	@Override
	public String toString() {
		return "Match [field=" + field + ", value=" + value + ", mask=" + mask + "]";
	}
	
	
}
