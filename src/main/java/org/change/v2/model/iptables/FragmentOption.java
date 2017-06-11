/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of FragmentOption.
 * 
 * @author Dragos
 */
public class FragmentOption extends SimpleOption {
	// Start of user code (user defined attributes for FragmentOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public FragmentOption() {
		// Start of user code constructor for FragmentOption)
		super();
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		super.accept(visitor);
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "FragmentOption []";
	}

}
