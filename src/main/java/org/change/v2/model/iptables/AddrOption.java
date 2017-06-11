/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of AddrOption.
 * 
 * @author Dragos
 */
public class AddrOption extends SimpleOption {
	/**
	 * Description of the property end.
	 */
	private Long end = null;
	
	/**
	 * Description of the property start.
	 */
	private Long start = null;
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for AddrOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public AddrOption() {
		// Start of user code constructor for AddrOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for AddrOption)
	
	public AddrOption(boolean b, String name) {
		this.setNeg(b);
		this.name = name;
	}

	// End of user code
	/**
	 * Returns end.
	 * @return end 
	 */
	public Long getEnd() {
		return this.end;
	}
	
	/**
	 * Sets a value to attribute end. 
	 * @param newEnd 
	 */
	public void setEnd(Long newEnd) {
	    this.end = newEnd;
	}

	/**
	 * Returns start.
	 * @return start 
	 */
	public Long getStart() {
		return this.start;
	}
	
	/**
	 * Sets a value to attribute start. 
	 * @param newStart 
	 */
	public void setStart(Long newStart) {
	    this.start = newStart;
	}

	/**
	 * Returns name.
	 * @return name 
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Sets a value to attribute name. 
	 * @param newName 
	 */
	public void setName(String newName) {
	    this.name = newName;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "AddrOption [end=" + end + ", start=" + start + ", name=" + name + "]";
	}



}
