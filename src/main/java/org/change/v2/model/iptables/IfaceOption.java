/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of IfaceOption.
 * 
 * @author Dragos
 */
public class IfaceOption extends SimpleOption {
	/**
	 * Description of the property ifaceNo.
	 */
	public Integer ifaceNo = Integer.valueOf(0);
	
	/**
	 * Description of the property io.
	 */
	public String io = "";
	
	// Start of user code (user defined attributes for IfaceOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IfaceOption() {
		// Start of user code constructor for IfaceOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for IfaceOption)
	
	public IfaceOption(Boolean neg, String io2, int hashCode) {
		this.setIfaceNo(hashCode);
		this.setNeg(neg);
		this.io = io2;
	}

	// End of user code
	/**
	 * Returns ifaceNo.
	 * @return ifaceNo 
	 */
	public Integer getIfaceNo() {
		return this.ifaceNo;
	}
	
	/**
	 * Sets a value to attribute ifaceNo. 
	 * @param newIfaceNo 
	 */
	public void setIfaceNo(Integer newIfaceNo) {
	    this.ifaceNo = newIfaceNo;
	}

	/**
	 * Returns io.
	 * @return io 
	 */
	public String getIo() {
		return this.io;
	}
	
	/**
	 * Sets a value to attribute io. 
	 * @param newIo 
	 */
	public void setIo(String newIo) {
	    this.io = newIo;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}



}
