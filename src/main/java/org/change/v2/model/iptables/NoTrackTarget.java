/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of NoTrackTarget.
 * 
 * @author Dragos
 */
public class NoTrackTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for NoTrackTarget)
	
	// End of user code
	
	@Override
	public String toString() {
		return "NoTrackTarget []";
	}

	/**
	 * The constructor.
	 */
	public NoTrackTarget() {
		// Start of user code constructor for NoTrackTarget)
		super("NOTRACK");
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	// Start of user code (user defined methods for NoTrackTarget)
	
	// End of user code


}
