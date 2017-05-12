/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of MarkTarget.
 * 
 * @author Dragos
 */
public class MarkTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for MarkTarget)
	private Long value;
	private long mask = 0xFFFFFFFF;
	// End of user code
	
	/**
	 * The constructor.
	 */
	public MarkTarget() {
		// Start of user code constructor for MarkTarget)
		super("MARK");
		// End of user code
	}

	public MarkTarget(Long long1, long mask) {
		this();
		this.value = long1;
		this.mask = mask;
	}

	public MarkTarget(Long long1) {
		this(long1, 0xFFFFFFFF);
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

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "MarkTarget [value=" + value + ", mask=" + mask + "]";
	}
	
	// Start of user code (user defined methods for MarkTarget)
	
	// End of user code


}
