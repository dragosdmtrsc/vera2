/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of PortOption.
 * 
 * @author Dragos
 */
public class PortOption extends SimpleOption {
	/**
	 * Description of the property name.
	 */
	public String name = "";
	
	/**
	 * Description of the property portNo.
	 */
	public Integer portNo = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for PortOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public PortOption() {
		// Start of user code constructor for PortOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for PortOption)
	
	public PortOption(boolean b, String string, int parseInt) {
		this.name = string;
		this.portNo = parseInt;
		this.setNeg(b);
	}

	// End of user code
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

	/**
	 * Returns portNo.
	 * @return portNo 
	 */
	public Integer getPortNo() {
		return this.portNo;
	}
	
	/**
	 * Sets a value to attribute portNo. 
	 * @param newPortNo 
	 */
	public void setPortNo(Integer newPortNo) {
	    this.portNo = newPortNo;
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}



}
