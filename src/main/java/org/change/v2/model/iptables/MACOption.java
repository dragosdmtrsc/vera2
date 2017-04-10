/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of MACOption.
 * 
 * @author Dragos
 */
public class MACOption extends SimpleOption {
	/**
	 * Description of the property addr.
	 */
	private String addr = "";
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for MACOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public MACOption() {
		// Start of user code constructor for MACOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for MACOption)
	
	public MACOption(boolean neg, String string, String source) {
		this.setNeg(neg);
		this.name = string;
		this.addr = source;
	}

	// End of user code
	/**
	 * Returns addr.
	 * @return addr 
	 */
	public String getAddr() {
		return this.addr;
	}
	
	/**
	 * Sets a value to attribute addr. 
	 * @param newAddr 
	 */
	public void setAddr(String newAddr) {
	    this.addr = newAddr;
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
		return "MACOption [addr=" + addr + ", name=" + name + "]";
	}



}
