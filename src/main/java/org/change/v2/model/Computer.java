/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.HashSet;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of Computer.
 * 
 * @author Dragos
 */
public class Computer implements Acceptor {
	/**
	 * Description of the property bridges.
	 */
	public HashSet<Bridge> bridges = new HashSet<Bridge>();
	
	/**
	 * Description of the property symnetNamespaces.
	 */
	public HashSet<Namespace> symnetNamespaces = new HashSet<Namespace>();
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	/**
	 * Description of the property id.
	 */
	private String id = "";
	
	// Start of user code (user defined attributes for Computer)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Computer() {
		// Start of user code constructor for Computer)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Computer)
	
	// End of user code
	/**
	 * Returns bridges.
	 * @return bridges 
	 */
	public HashSet<Bridge> getBridges() {
		return this.bridges;
	}

	/**
	 * Returns symnetNamespaces.
	 * @return symnetNamespaces 
	 */
	public HashSet<Namespace> getSymnetNamespaces() {
		return this.symnetNamespaces;
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
	 * Returns id.
	 * @return id 
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Sets a value to attribute id. 
	 * @param newId 
	 */
	public void setId(String newId) {
	    this.id = newId;
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
}
