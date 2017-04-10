/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of StateOption.
 * 
 * @author Dragos
 */
public class StateOption extends SimpleOption {
	/**
	 * Description of the property value.
	 */
	public Integer value = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for StateOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public StateOption() {
		// Start of user code constructor for StateOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for StateOption)
	
	public StateOption(boolean neg, Integer integer) {
		// TODO Auto-generated constructor stub
	}

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
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "StateOption [value=" + value + "]";
	}



}
