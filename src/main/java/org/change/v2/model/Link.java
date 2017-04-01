/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.HashSet;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of Link.
 * 
 * @author Dragos
 */
public class Link {
	/**
	 * Description of the property nICs.
	 */
	public HashSet<NIC> nICs = new HashSet<NIC>();
	
	// Start of user code (user defined attributes for Link)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Link() {
		// Start of user code constructor for Link)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Link)
	
	// End of user code
	/**
	 * Returns nICs.
	 * @return nICs 
	 */
	public HashSet<NIC> getNICs() {
		return this.nICs;
	}



}
