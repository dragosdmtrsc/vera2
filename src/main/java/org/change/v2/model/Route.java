/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import org.change.v2.model.IPAddress;
import org.change.v2.model.NIC;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of Route.
 * 
 * @author Dragos
 */
public class Route {
	/**
	 * Description of the property nextHop.
	 */
	private IPAddress nextHop = null;
	
	/**
	 * Description of the property netAddress.
	 */
	private IPAddress netAddress = null;
	
	/**
	 * Description of the property nic.
	 */
	private NIC nic = null;
	
	// Start of user code (user defined attributes for Route)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Route() {
		// Start of user code constructor for Route)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Route)
	
	// End of user code
	/**
	 * Returns nextHop.
	 * @return nextHop 
	 */
	public IPAddress getNextHop() {
		return this.nextHop;
	}
	
	/**
	 * Sets a value to attribute nextHop. 
	 * @param newNextHop 
	 */
	public void setNextHop(IPAddress newNextHop) {
	    this.nextHop = newNextHop;
	}

	/**
	 * Returns netAddress.
	 * @return netAddress 
	 */
	public IPAddress getNetAddress() {
		return this.netAddress;
	}
	
	/**
	 * Sets a value to attribute netAddress. 
	 * @param newNetAddress 
	 */
	public void setNetAddress(IPAddress newNetAddress) {
	    this.netAddress = newNetAddress;
	}

	/**
	 * Returns nic.
	 * @return nic 
	 */
	public NIC getNic() {
		return this.nic;
	}
	
	/**
	 * Sets a value to attribute nic. 
	 * @param newNic 
	 */
	public void setNic(NIC newNic) {
	    this.nic = newNic;
	}

	@Override
	public String toString() {
		return "Route [nextHop=" + nextHop + ", netAddress=" + netAddress + ", nic=" + nic + "]";
	} 



}
