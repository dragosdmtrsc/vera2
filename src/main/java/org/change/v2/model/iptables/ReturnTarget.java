/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of ReturnTarget.
 * 
 * @author Dragos
 */
public class ReturnTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for ReturnTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public ReturnTarget() {
		// Start of user code constructor for ReturnTarget)
		super("RETURN");
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "ReturnTarget []";
	}
	
	// Start of user code (user defined methods for ReturnTarget)
	
	// End of user code


}
