/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of PhysdevIsBridgedOption.
 * 
 * @author Dragos
 */
public class PhysdevIsBridgedOption extends SimpleOption {
	// Start of user code (user defined attributes for PhysdevIsBridgedOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public PhysdevIsBridgedOption() {
		// Start of user code constructor for PhysdevIsBridgedOption)
		super();
		// End of user code
	}

	public PhysdevIsBridgedOption(boolean neg) {
		super();
		this.setNeg(neg);
	}

	@Override
	public void accept(IVisitor visitor) {
		// TODO Auto-generated method stub
		super.accept(visitor);
		visitor.visit(this);
	}
	
	// Start of user code (user defined methods for PhysdevIsBridgedOption)
	
	// End of user code


}
