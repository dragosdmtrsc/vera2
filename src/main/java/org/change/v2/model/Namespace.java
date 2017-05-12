/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.HashSet;

import org.change.v2.model.iptables.IPTablesTable;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of Namespace.
 * 
 * @author Dragos
 */
public class Namespace {
	/**
	 * Description of the property iPTablesTables.
	 */
	public HashSet<IPTablesTable> iPTablesTables = new HashSet<IPTablesTable>();
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	/**
	 * Description of the property routingTables.
	 */
	public HashSet<RoutingTable> routingTables = new HashSet<RoutingTable>();
	
	/**
	 * Description of the property symnetNICs.
	 */
	public HashSet<NIC> symnetNICs = new HashSet<NIC>();
	
	/**
	 * Description of the property symnetComputers.
	 */
	public Computer symnetComputers = null;
	
	// Start of user code (user defined attributes for Namespace)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Namespace() {
		// Start of user code constructor for Namespace)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Namespace)
	
	// End of user code
	/**
	 * Returns iPTablesTables.
	 * @return iPTablesTables 
	 */
	public HashSet<IPTablesTable> getIPTablesTables() {
		return this.iPTablesTables;
	}

	/**
	 * Returns name.
	 * @return name 
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Sets a value to attribute name. 
	 * @param newName 
	 */
	public void setName(String newName) {
	    this.name = newName;
	}

	/**
	 * Returns routingTables.
	 * @return routingTables 
	 */
	public HashSet<RoutingTable> getRoutingTables() {
		return this.routingTables;
	}

	/**
	 * Returns symnetNICs.
	 * @return symnetNICs 
	 */
	public HashSet<NIC> getNICs() {
		return this.symnetNICs;
	}

	/**
	 * Returns symnetComputers.
	 * @return symnetComputers 
	 */
	public Computer getComputer() {
		return this.symnetComputers;
	}
	
	/**
	 * Sets a value to attribute symnetComputers. 
	 * @param newSymnetComputers 
	 */
	public void setComputer(Computer newSymnetComputers) {
	    this.symnetComputers = newSymnetComputers;
	}



}
