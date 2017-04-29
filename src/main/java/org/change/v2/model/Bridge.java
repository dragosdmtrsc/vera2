/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.HashSet;
import java.util.List;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of Bridge.
 * 
 * @author Dragos
 */
public abstract class Bridge {
	/**
	 * Description of the property computers.
	 */
	public Computer computers = null;
	
	/**
	 * Description of the property kind.
	 */
	private String kind = "";
	
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for Bridge)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Bridge() {
		// Start of user code constructor for Bridge)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Bridge)
	
	// End of user code
	/**
	 * Returns computers.
	 * @return computers 
	 */
	public Computer getComputer() {
		return this.computers;
	}
	
	/**
	 * Sets a value to attribute computers. 
	 * @param newComputers 
	 */
	public void setComputers(Computer newComputers) {
	    this.computers = newComputers;
	}

	/**
	 * Returns kind.
	 * @return kind 
	 */
	public String getKind() {
		return this.kind;
	}
	
	/**
	 * Sets a value to attribute kind. 
	 * @param newKind 
	 */
	public void setKind(String newKind) {
	    this.kind = newKind;
	}

	/**
	 * Returns nICs.
	 * @return nICs 
	 */
	public abstract List<? extends NIC> getNICs();

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



}
