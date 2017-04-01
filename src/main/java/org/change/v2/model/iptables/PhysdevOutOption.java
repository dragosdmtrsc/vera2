/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of PhysdevOutOption.
 * 
 * @author Dragos
 */
public class PhysdevOutOption extends SimpleOption {
	/**
	 * Description of the property value.
	 */
	public Integer value = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for PhysdevOutOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public PhysdevOutOption() {
		// Start of user code constructor for PhysdevOutOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for PhysdevOutOption)
	
	// End of user code
	/**
	 * Returns value.
	 * @return value 
	 */
	public Integer getValue() {
		return this.value;
	}
	
	/**
	 * Sets a value to attribute value. 
	 * @param newValue 
	 */
	public void setValue(Integer newValue) {
	    this.value = newValue;
	}

	@Override
	public void accept(IVisitor visitor) {
		super.accept(visitor);
		visitor.visit(this);
	}



}
