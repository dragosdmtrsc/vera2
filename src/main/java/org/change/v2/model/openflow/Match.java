package org.change.v2.model.openflow;

public class Match implements FlowAcceptor {

	private QualifiedField field, fieldDst;

	public QualifiedField getFieldDst() {
		return fieldDst;
	}

	private long value;
	private long mask;
	private boolean hasMask;
	private boolean isReg;
	
	public boolean isReg() {
		return isReg;
	}
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
		this.isReg = false;
	}
	
	public void setValue(QualifiedField qf)
	{
		fieldDst = qf;
		this.isReg = true;
	}
	
	public long getMask() {
		return mask;
	}
	public void setMask(long mask) {
		this.mask = mask;
	}
	@Override
	public void accept(FlowVisitor visitor) {
		visitor.visit(this);
	}
	@Override
	public String toString() {
		return "Match [field=" + field + ", value=" + value + ", mask=" + mask + "]";
	}
	
	
}
