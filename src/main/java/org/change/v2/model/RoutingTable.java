/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of RoutingTable.
 * 
 * @author Dragos
 */
public class RoutingTable {
	/**
	 * Description of the property routes.
	 */
	public List<Route> routes = new ArrayList<Route>();
	
	// Start of user code (user defined attributes for RoutingTable)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public RoutingTable() {
		// Start of user code constructor for RoutingTable)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for RoutingTable)
	
	// End of user code
	/**
	 * Returns routes.
	 * @return routes 
	 */
	public List<Route> getRoutes() {
		return this.routes;
	}

	@Override
	public String toString() {
		return "RoutingTable [routes=" + routes + "]";
	}



}
