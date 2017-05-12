/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashMap;
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
		return "NIC [symnetIPAddresss=" + symnetIPAddresss + ", macAddress="
				+ macAddress + ", Name=" + Name + "]";
	}


	private String type;
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Description of the property symnetIPAddresss.
	 */
	private IPAddress symnetIPAddresss = new IPAddress();
	

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


	private String vlanMode = "trunk";
	
	
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
	

	/**
	 * Returns symnetIPAddresss.
	 * @return symnetIPAddresss 
	 */
	public IPAddress getIPAddresss() {
		return this.symnetIPAddresss;
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
	
	public void setVlanMode(String portType) {
		this.vlanMode = portType;
	}

	public String getVlanMode() {
		return vlanMode;
	}
	
	
	public boolean isAllVlans()
	{
		return isTrunk() && this.vlans.size() == 0;
	}
	
	public boolean isAccess()
	{
		return !this.isTrunk();
	}
	
	public boolean isTrunk()
	{
		return this.vlanMode == null || this.vlanMode.equals("trunk");
	}
}
