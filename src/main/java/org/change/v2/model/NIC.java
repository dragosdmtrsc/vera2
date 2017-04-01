/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.HashSet;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of NIC.
 * 
 * @author Dragos
 */
public class NIC {
	/**
	 * Description of the property bridges.
	 */
	public Bridge bridges = null;
	
	/**
	 * Description of the property symnetIPAddresss.
	 */
	public HashSet<IPAddress> symnetIPAddresss = new HashSet<IPAddress>();
	
	/**
	 * Description of the property links.
	 */
	public Link links = null;
	
	/**
	 * Description of the property macAddress.
	 */
	private String macAddress = "";
	
	/**
	 * Description of the property Name.
	 */
	private String Name = "";
	
	// Start of user code (user defined attributes for NIC)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public NIC() {
		// Start of user code constructor for NIC)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for NIC)
	
	// End of user code
	/**
	 * Returns bridges.
	 * @return bridges 
	 */
	public Bridge getBridges() {
		return this.bridges;
	}
	
	/**
	 * Sets a value to attribute bridges. 
	 * @param newBridges 
	 */
	public void setBridges(Bridge newBridges) {
	    this.bridges = newBridges;
	}

	/**
	 * Returns symnetIPAddresss.
	 * @return symnetIPAddresss 
	 */
	public HashSet<IPAddress> getSymnetIPAddresss() {
		return this.symnetIPAddresss;
	}

	/**
	 * Returns links.
	 * @return links 
	 */
	public Link getLinks() {
		return this.links;
	}
	
	/**
	 * Sets a value to attribute links. 
	 * @param newLinks 
	 */
	public void setLinks(Link newLinks) {
	    this.links = newLinks;
	}

	/**
	 * Returns macAddress.
	 * @return macAddress 
	 */
	public String getMacAddress() {
		return this.macAddress;
	}
	
	/**
	 * Sets a value to attribute macAddress. 
	 * @param newMacAddress 
	 */
	public void setMacAddress(String newMacAddress) {
	    this.macAddress = newMacAddress;
	}

	/**
	 * Returns Name.
	 * @return Name 
	 */
	public String getName() {
		return this.Name;
	}
	
	/**
	 * Sets a value to attribute Name. 
	 * @param newName 
	 */
	public void setName(String newName) {
	    this.Name = newName;
	}



}
