/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.SimpleOption;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of ProtocolOption.
 * 
 * @author Dragos
 */
public class ProtocolOption extends SimpleOption {
	/**
	 * Description of the property proto.
	 */
	private int proto = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for ProtocolOption)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public ProtocolOption() {
		// Start of user code constructor for ProtocolOption)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for ProtocolOption)
	
	public ProtocolOption(boolean neg, int intProto) {
		this.setNeg(neg);
		this.proto = intProto;
	}

	// End of user code
	/**
	 * Returns proto.
	 * @return proto 
	 */
	public int getProto() {
		return this.proto;
	}
	
	/**
	 * Sets a value to attribute proto. 
	 * @param newProto 
	 */
	public void setProto(Integer newProto) {
	    this.proto = newProto;
	}

	@Override
	public void accept(IVisitor visitor) {
		super.accept(visitor);
		visitor.visit(this);
	}



}
