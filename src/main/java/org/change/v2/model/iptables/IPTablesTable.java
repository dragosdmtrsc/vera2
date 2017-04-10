/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import java.util.HashSet;

import org.change.v2.model.Acceptor;
// Start of user code (user defined imports)
import org.change.v2.model.IVisitor;
import org.change.v2.model.Namespace;

// End of user code

/**
 * Description of IPTablesTable.
 * 
 * @author Dragos
 */
public class IPTablesTable implements Acceptor {
	/**
	 * Description of the property iPTablesChains.
	 */
	private HashSet<IPTablesChain> iPTablesChains = new HashSet<IPTablesChain>();
	private Namespace parent = null;
	public Namespace getParent() {
		return parent;
	}

	public void setParent(Namespace parent) {
		this.parent = parent;
	} 
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for IPTablesTable)
	
	// End of user code
	
	public IPTablesTable(String name) {
		super();
		this.name = name;
	}

	/**
	 * The constructor.
	 */
	public IPTablesTable() {
		// Start of user code constructor for IPTablesTable)
		super();
		// End of user code
	}
	
	/**
	 * Description of the method accept.
	 * @param visitor 
	 */
	public void accept(IVisitor visitor) {
		visitor.visit(this);
		// Start of user code for method accept
		// End of user code
	}
	 
	// Start of user code (user defined methods for IPTablesTable)
	
	// End of user code
	/**
	 * Returns iPTablesChains.
	 * @return iPTablesChains 
	 */
	public HashSet<IPTablesChain> getIPTablesChains() {
		return this.iPTablesChains;
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

	@Override
	public String toString() {
		return "IPTablesTable [iPTablesChains=" + iPTablesChains + ", parent=" + parent + ", name=" + name + "]";
	}



}
