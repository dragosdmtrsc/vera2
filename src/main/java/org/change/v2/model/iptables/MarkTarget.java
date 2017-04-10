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
	private int value;
	private int mask = 0xFFFFFFFF;
	// End of user code
	
	/**
	 * The constructor.
	 */
	public MarkTarget() {
		// Start of user code constructor for MarkTarget)
		super("MARK");
		// End of user code
	}

	public MarkTarget(int value, int mask) {
		this();
		this.value = value;
		this.mask = mask;
	}

	public MarkTarget(int value) {
		this(value, 0xFFFFFFFF);
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
