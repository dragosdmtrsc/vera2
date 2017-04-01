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
	private NIC nic1, nic2;
	
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

	public NIC getNic1() {
		return nic1;
	}

	public NIC getNic2() {
		return nic2;
	}

	
	public Link(NIC nic1, NIC nic2) {
		super();
		this.nic1 = nic1;
		this.nic2 = nic2;
	}

	public void setNics(NIC nic1, NIC nic2)
	{
		String nic1Name = nic1.getName();
		String nic2Name = nic2.getName();
		this.nic1 = nic1Name.compareTo(nic2Name) <= 0 ? nic1 : nic2;
		this.nic2 = nic1Name.compareTo(nic2Name) <= 0 ? nic2 : nic1;
		
	}

	@Override
	public int hashCode() {
		return (this.toString()).hashCode(); 
	}

	
	@Override
	public String toString() {
		return this.nic1.toString() + "<->" + this.nic2.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof Link)) return false;
		Link other = (Link) obj;
		return this.nic1.getName().equals(other.nic1.getName()) &&
				this.nic2.getName().equals(other.nic2.getName());
	}
	
	public boolean hasEnd(NIC atNic)
	{
		return this.nic1.getName().equals(atNic.getName())
				|| this.nic2.getName().equals(atNic.getName());
	}
}
