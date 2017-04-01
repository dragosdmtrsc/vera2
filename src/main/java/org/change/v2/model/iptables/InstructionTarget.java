/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of InstructionTarget.
 * 
 * @author Dragos
 */
public class InstructionTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for InstructionTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public InstructionTarget() {
		// Start of user code constructor for InstructionTarget)
		super();
		// End of user code
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	// Start of user code (user defined methods for InstructionTarget)
	
	// End of user code


}
