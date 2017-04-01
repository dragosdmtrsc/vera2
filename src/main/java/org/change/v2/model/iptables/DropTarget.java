/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of DropTarget.
 * 
 * @author Dragos
 */
public class DropTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for DropTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public DropTarget() {
		// Start of user code constructor for DropTarget)
		super("DROP");
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	// Start of user code (user defined methods for DropTarget)
	
	// End of user code


}
