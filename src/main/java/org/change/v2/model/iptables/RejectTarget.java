/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of RejectTarget.
 * 
 * @author Dragos
 */
public class RejectTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for RejectTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public RejectTarget() {
		// Start of user code constructor for RejectTarget)
		super("REJECT");
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);		
	}

	@Override
	public String toString() {
		return "RejectTarget []";
	}
	
	// Start of user code (user defined methods for RejectTarget)
	
	// End of user code


}
