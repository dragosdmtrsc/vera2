/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model.iptables;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.Acceptor;
import org.change.v2.model.IVisitor;

// End of user code

/**
 * Description of IPTablesChain.
 * 
 * @author Dragos
 */
public class IPTablesChain implements Acceptor {
	/**
	 * Description of the property iPTablesTables.
	 */
	public IPTablesTable iPTablesTables = null;
	
	/**
	 * Description of the property iPTablesRules.
	 */
	public List<IPTablesRule> iPTablesRules = new ArrayList<IPTablesRule>();
	
	/**
	 * Description of the property policy.
	 */
	private IPTablesTarget policy = new ReturnTarget();
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	// Start of user code (user defined attributes for IPTablesChain)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IPTablesChain() {
		// Start of user code constructor for IPTablesChain)
		super();
		// End of user code
	}
	
	/**
	 * Description of the method accept.
	 * @param visitor 
	 */
	public void accept(IVisitor visitor) {
		visitor.visit(this);
		
		for (IPTablesRule rule : this.iPTablesRules)
		{
			rule.accept(visitor);
		}
		if (this.getPolicy() != null)
		{
			this.getPolicy().accept(visitor);
		}
		else
		{
			visitor.visit((ReturnTarget) null);
		}
		// Start of user code for method accept
		// End of user code
	}
	 
	// Start of user code (user defined methods for IPTablesChain)
	
	// End of user code
	/**
	 * Returns iPTablesTables.
	 * @return iPTablesTables 
	 */
	public IPTablesTable getIPTablesTables() {
		return this.iPTablesTables;
	}
	
	/**
	 * Sets a value to attribute iPTablesTables. 
	 * @param newIPTablesTables 
	 */
	public void setIPTablesTables(IPTablesTable newIPTablesTables) {
	    this.iPTablesTables = newIPTablesTables;
	}

	/**
	 * Returns iPTablesRules.
	 * @return iPTablesRules 
	 */
	public List<IPTablesRule> getIPTablesRules() {
		return this.iPTablesRules;
	}

	/**
	 * Returns policy.
	 * @return policy 
	 */
	public IPTablesTarget getPolicy() {
		return this.policy;
	}
	
	/**
	 * Sets a value to attribute policy. 
	 * @param newPolicy 
	 */
	public void setPolicy(IPTablesTarget newPolicy) {
	    this.policy = newPolicy;
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

	private static final String[] DEFAULT_CHAINS = new String[] {
		"PREROUTING", "FORWARD", "POSTROUTING", "INPUT"
	};
	
	public boolean isDefault()
	{
		for (String defChain : DEFAULT_CHAINS)
		{
			if (defChain.equals(name))
				return true;
		}
		return false;
	}
	
	
	@Override
	public String toString() {
		return "IPTablesChain [iPTablesRules=" + iPTablesRules + ", policy="
				+ policy + ", name=" + name + "]";
	} 
	
	
}
