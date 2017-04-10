/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of ConnmarkOption.
 * 
 * @author Dragos
 */
public class ConnmarkOption extends SimpleOption {
	/**
	 * Description of the property forTag.
	 */
	private String forTag = "";
	
	/**
	 * Description of the property start.
	 */
	private Integer value = Integer.valueOf(0);
	
	/**
	 * Description of the property end.
	 */
	private Integer mask = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for ConnmarkOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public ConnmarkOption() {
		// Start of user code constructor for ConnmarkOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for ConnmarkOption)
	
	// End of user code
	/**
	 * Returns forTag.
	 * @return forTag 
	 */
	public String getForTag() {
		return this.forTag;
	}
	
	/**
	 * Sets a value to attribute forTag. 
	 * @param newForTag 
	 */
	public void setForTag(String newForTag) {
	    this.forTag = newForTag;
	}

	/**
	 * Returns start.
	 * @return start 
	 */
	public Integer getValue() {
		return this.value;
	}
	
	/**
	 * Sets a value to attribute start. 
	 * @param newStart 
	 */
	public void setValue(Integer newStart) {
	    this.value = newStart;
	}

	/**
	 * Returns end.
	 * @return end 
	 */
	public Integer getMask() {
		return this.mask;
	}
	
	/**
	 * Sets a value to attribute end. 
	 * @param newEnd 
	 */
	public void setMask(Integer newEnd) {
	    this.mask = newEnd;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "ConnmarkOption [forTag=" + forTag + ", value=" + value + ", mask=" + mask + "]";
	}



}
