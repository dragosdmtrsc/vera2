/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import java.util.HashSet;

import org.change.v2.model.Acceptor;
// Start of user code (user defined imports)
import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of IPTablesMatch.
 * 
 * @author Dragos
 */
public class IPTablesMatch implements Acceptor {
	/**
	 * Description of the property iPTablesRule.
	 */
	public IPTablesRule iPTablesRule = null;
	
	/**
	 * Description of the property matchOptions.
	 */
	public HashSet<MatchOption> matchOptions = new HashSet<MatchOption>();
	
	// Start of user code (user defined attributes for IPTablesMatch)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IPTablesMatch() {
		// Start of user code constructor for IPTablesMatch)
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
	 
	// Start of user code (user defined methods for IPTablesMatch)
	
	// End of user code
	/**
	 * Returns iPTablesRule.
	 * @return iPTablesRule 
	 */
	public IPTablesRule getIPTablesRule() {
		return this.iPTablesRule;
	}
	
	/**
	 * Sets a value to attribute iPTablesRule. 
	 * @param newIPTablesRule 
	 */
	public void setIPTablesRule(IPTablesRule newIPTablesRule) {
	    this.iPTablesRule = newIPTablesRule;
	}

	/**
	 * Returns matchOptions.
	 * @return matchOptions 
	 */
	public HashSet<MatchOption> getMatchOptions() {
		return this.matchOptions;
	}



}
