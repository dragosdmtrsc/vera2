/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of IcmpTypeOption.
 * 
 * @author Dragos
 */
public class IcmpTypeOption extends SimpleOption {
	/**
	 * Description of the property theType.
	 */
	public Integer theType = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for IcmpTypeOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IcmpTypeOption() {
		// Start of user code constructor for IcmpTypeOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for IcmpTypeOption)
	
	public IcmpTypeOption(Boolean currentIcmpNeg, int add) {
		this.setNeg(currentIcmpNeg);
		this.theType = (add);
	}

	// End of user code
	/**
	 * Returns theType.
	 * @return theType 
	 */
	public Integer getTheType() {
		return this.theType;
	}
	
	/**
	 * Sets a value to attribute theType. 
	 * @param newTheType 
	 */
	public void setTheType(Integer newTheType) {
	    this.theType = newTheType;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "IcmpTypeOption [theType=" + theType + "]";
	}



}
