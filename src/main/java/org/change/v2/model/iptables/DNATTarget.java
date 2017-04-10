/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;
import org.change.v2.model.iptables.IPTablesTarget;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of DNATTarget.
 * 
 * @author Dragos
 */
public class DNATTarget extends IPTablesTarget {
	// Start of user code (user defined attributes for DNATTarget)
	private long ipAddress;
	// End of user code
	
	/**
	 * The constructor.
	 */
	public DNATTarget() {
		super("DNAT");
	}
	
	public DNATTarget(long ipAddress) {
		this();
		this.ipAddress = ipAddress;
	}
	
	public DNATTarget(String ipAddress) {
		this(SNATTarget.ipToLong(ipAddress));
	}
	
	public long getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(long ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "DNATTarget [ipAddress=" + ipAddress + "]";
	}
	
	// Start of user code (user defined methods for DNATTarget)
	
	// End of user code


}
