/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of IPAddress.
 * 
 * @author Dragos
 */
public class IPAddress {
	/**
	 * Description of the property address.
	 */
	private Integer address = Integer.valueOf(0);
	
	/**
	 * Description of the property symnetNICs.
	 */
	public NIC symnetNICs = null;
	
	/**
	 * Description of the property mask.
	 */
	private Integer mask = Integer.valueOf(0);
	
	// Start of user code (user defined attributes for IPAddress)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public IPAddress() {
		// Start of user code constructor for IPAddress)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for IPAddress)
	
	// End of user code
	/**
	 * Returns address.
	 * @return address 
	 */
	public Integer getAddress() {
		return this.address;
	}
	
	/**
	 * Sets a value to attribute address. 
	 * @param newAddress 
	 */
	public void setAddress(Integer newAddress) {
	    this.address = newAddress;
	}

	/**
	 * Returns symnetNICs.
	 * @return symnetNICs 
	 */
	public NIC getSymnetNICs() {
		return this.symnetNICs;
	}
	
	/**
	 * Sets a value to attribute symnetNICs. 
	 * @param newSymnetNICs 
	 */
	public void setSymnetNICs(NIC newSymnetNICs) {
	    this.symnetNICs = newSymnetNICs;
	}

	/**
	 * Returns mask.
	 * @return mask 
	 */
	public Integer getMask() {
		return this.mask;
	}
	
	/**
	 * Sets a value to attribute mask. 
	 * @param newMask 
	 */
	public void setMask(Integer newMask) {
	    this.mask = newMask;
	}



}
