/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import org.change.v2.model.Acceptor;
// Start of user code (user defined imports)
import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of IPTablesTarget.
 * 
 * @author Dragos
 */
public abstract class IPTablesTarget implements Acceptor {
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	/**
	 * Description of the property iPTablesRules.
	 */
	private IPTablesRule iPTablesRules = null;
	
	// Start of user code (user defined attributes for IPTablesTarget)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IPTablesTarget() {
		// Start of user code constructor for IPTablesTarget)
		super();
		// End of user code
	}
	
	public IPTablesTarget(String name) {
		super();
		this.name = name;
	}
	 
	// Start of user code (user defined methods for IPTablesTarget)
	
	// End of user code
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
	 * Returns iPTablesRules.
	 * @return iPTablesRules 
	 */
	public IPTablesRule getIPTablesRules() {
		return this.iPTablesRules;
	}
	
	/**
	 * Sets a value to attribute iPTablesRules. 
	 * @param newIPTablesRules 
	 */
	public void setIPTablesRules(IPTablesRule newIPTablesRules) {
	    this.iPTablesRules = newIPTablesRules;
	}



}
