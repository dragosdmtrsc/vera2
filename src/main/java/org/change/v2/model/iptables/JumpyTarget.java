/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of JumpyTarget.
 * 
 * @author Dragos
 */
public class JumpyTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for JumpyTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public JumpyTarget() {
		// Start of user code constructor for JumpyTarget)
		super();
		// End of user code
	}

	public JumpyTarget(String string) {
		super(string);
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "JumpyTarget []";
	}
	
	// Start of user code (user defined methods for JumpyTarget)
	
	// End of user code


}


