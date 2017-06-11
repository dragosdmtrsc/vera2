/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of IcmpCodeOption.
 * 
 * @author Dragos
 */
public class IcmpCodeOption extends SimpleOption {
	/**
	 * Description of the property theCode.
	 */
	public Integer theCode = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for IcmpCodeOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IcmpCodeOption() {
		// Start of user code constructor for IcmpCodeOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for IcmpCodeOption)
	
	public IcmpCodeOption(Boolean currentIcmpNeg, Integer code) {
		this.setNeg(currentIcmpNeg);
		this.setTheCode(code);
	}

	// End of user code
	/**
	 * Returns theCode.
	 * @return theCode 
	 */
	public Integer getTheCode() {
		return this.theCode;
	}
	
	/**
	 * Sets a value to attribute theCode. 
	 * @param newTheCode 
	 */
	public void setTheCode(Integer newTheCode) {
	    this.theCode = newTheCode;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "IcmpCodeOption [theCode=" + theCode + "]";
	}



}
