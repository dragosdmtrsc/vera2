/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of SimpleOption.
 * 
 * @author Dragos
 */
public class SimpleOption implements MatchOption {
	/**
	 * Description of the property neg.
	 */
	private Boolean neg = false;
	
	// Start of user code (user defined attributes for SimpleOption)
	
	// End of user code
	
	
	/**
	 * Description of the method accept.
	 * @param visitor 
	 */
	public void accept(IVisitor visitor) {
		// Start of user code for method accept
		// End of user code
	}
	 
	// Start of user code (user defined methods for SimpleOption)
	
	// End of user code
	/**
	 * Returns neg.
	 * @return neg 
	 */
	public Boolean getNeg() {
		return this.neg;
	}
	
	/**
	 * Sets a value to attribute neg. 
	 * @param newNeg 
	 */
	public void setNeg(Boolean newNeg) {
		this.neg = newNeg;
	}

	@Override
	public String toString() {
		return "SimpleOption [neg=" + neg + "]";
	}



}
