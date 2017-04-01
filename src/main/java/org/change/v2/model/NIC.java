/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of NIC.
 * 
 * @author Dragos
 */
public class NIC {
	
	
	private Map<String, String> options = new HashMap<String, String>();
	
	public Map<String, String> getOptions() {
		return options;
	}

	
	@Override
	public String toString() {
		String brName = "NOBRIDGE";
		if (this.getBridge() != null)
			brName = this.getBridge().getName();
		return this.getName() + "(" + brName + ")";
	}


	private String type;
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	/**
	 * Description of the property bridges.
	 */
	private Bridge bridge = null;
	
	/**
	 * Description of the property symnetIPAddresss.
	 */
	private HashSet<IPAddress> symnetIPAddresss = new HashSet<IPAddress>();
	
	/**
	 * Description of the property links.
	 */
	private Link link = null;
	
	/**
	 * Description of the property macAddress.
	 */
	private String macAddress = "";
	
	/**
	 * Description of the property Name.
	 */
	private String Name = "";
	
	// Start of user code (user defined attributes for NIC)
	
	private List<Integer> vlans = new ArrayList<Integer>();
	
	
	// End of user code
	
	public List<Integer> getVlans() {
		return vlans;
	}

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
	public Bridge getBridge() {
		return this.bridge;
	}
	
	/**
	 * Sets a value to attribute bridges. 
	 * @param newBridges 
	 */
	public void setBridge(Bridge newBridges) {
	    this.bridge = newBridges;
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
	public Link getLink() {
		return this.link;
	}
	
	/**
	 * Sets a value to attribute links. 
	 * @param newLinks 
	 */
	public void setLink(Link newLinks) {
	    this.link = newLinks;
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
