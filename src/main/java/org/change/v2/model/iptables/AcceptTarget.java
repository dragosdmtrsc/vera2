/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of AcceptTarget.
 * 
 * @author Dragos
 */
public class AcceptTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for AcceptTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public AcceptTarget() {
		// Start of user code constructor for AcceptTarget)
		super("ACCEPT");
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	// Start of user code (user defined methods for AcceptTarget)
	
	// End of user code


}
