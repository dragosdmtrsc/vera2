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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result
				+ ((fieldDst == null) ? 0 : fieldDst.hashCode());
		result = prime * result + (hasMask ? 1231 : 1237);
		result = prime * result + (isReg ? 1231 : 1237);
		result = prime * result + (int) (mask ^ (mask >>> 32));
		result = prime * result + (int) (value ^ (value >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Match other = (Match) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (fieldDst == null) {
			if (other.fieldDst != null)
				return false;
		} else if (!fieldDst.equals(other.fieldDst))
			return false;
		if (hasMask != other.hasMask)
			return false;
		if (isReg != other.isReg)
			return false;
		if (mask != other.mask)
			return false;
		if (value != other.value)
			return false;
		return true;
	}
	
	public static Match getMatchForSomething(String something, Long value)
	{
		Match m = new Match();
		m.setField(QualifiedField.fromString(something));
		m.setValue(value);
		return m;
	}
	public static Match getMatchForSomething(String something, int i) {
		return getMatchForSomething(something, (long)i);
	}
	
	
}
