/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.Acceptor;
// Start of user code (user defined imports)
import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of IPTablesRule.
 * 
 * @author Dragos
 */
public class IPTablesRule implements Acceptor {
	/**
	 * Description of the property iPTablesTargets.
	 */
	public IPTablesTarget iPTablesTarget;
	
	/**
	 * Description of the property iPTablesMatches.
	 */
	public List<IPTablesMatch> iPTablesMatches = new ArrayList<IPTablesMatch>();
	
	/**
	 * Description of the property index.
	 */
	private Integer index = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for IPTablesRule)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IPTablesRule() {
		// Start of user code constructor for IPTablesRule)
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
	 
	// Start of user code (user defined methods for IPTablesRule)
	
	// End of user code
	/**
	 * Returns iPTablesTargets.
	 * @return iPTablesTargets 
	 */
	public IPTablesTarget getIPTablesTarget() {
		return this.iPTablesTarget;
	}
	
	

	public void setIPTablesTarget(IPTablesTarget iPTablesTarget) {
		this.iPTablesTarget = iPTablesTarget;
	}

	/**
	 * Returns iPTablesMatches.
	 * @return iPTablesMatches 
	 */
	public List<IPTablesMatch> getIPTablesMatches() {
		return this.iPTablesMatches;
	}

	/**
	 * Returns index.
	 * @return index 
	 */
	public Integer getIndex() {
		return this.index;
	}
	
	/**
	 * Sets a value to attribute index. 
	 * @param newIndex 
	 */
	public void setIndex(Integer newIndex) {
	    this.index = newIndex;
	}

	@Override
	public String toString() {
		return "IPTablesRule [iPTablesTarget=" + iPTablesTarget + ", iPTablesMatches=" + iPTablesMatches + ", index="
				+ index + "]";
	}



}
