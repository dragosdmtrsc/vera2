/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of PhysdevInOption.
 * 
 * @author Dragos
 */
public class PhysdevInOption extends SimpleOption {
	/**
	 * Description of the property value.
	 */
	public Integer value = Integer.valueOf(0);
	private String name = "PhysdevIn";
	// Start of user code (user defined attributes for PhysdevInOption)
	
	// End of user code
	
	public String getName() {
		return name;
	}

	/**
	 * The constructor.
	 */
	public PhysdevInOption() {
		// Start of user code constructor for PhysdevInOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for PhysdevInOption)
	
	public PhysdevInOption(boolean neg, int hashCode, String name) {
		this.value = hashCode;
		this.setNeg(neg);
		this.name = name;
	}
	public PhysdevInOption(boolean neg, int hashCode) {
		this.value = hashCode;
		this.setNeg(neg);
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
		return "PhysdevInOption [value=" + value + ", name=" + name + "]";
	}



}
