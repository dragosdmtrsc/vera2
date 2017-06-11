/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import org.change.v2.model.openflow.Decoder;

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
	private Long address = Long.valueOf(0);
	
	
	/**
	 * Description of the property mask.
	 */
	private Long mask = Long.valueOf(0);
	
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
	public Long getAddress() {
		return this.address;
	}
	
	/**
	 * Sets a value to attribute address. 
	 * @param long1 
	 */
	public void setAddress(Long long1) {
	    this.address = long1;
	}

	/**
	 * Returns mask.
	 * @return mask 
	 */
	public Long getMask() {
		return this.mask;
	}
	
	/**
	 * Sets a value to attribute mask. 
	 * @param long1 
	 */
	public void setMask(Long long1) {
	    this.mask = long1;
	}

	@Override
	public String toString() {
		return "IPAddress [address=" + address + ", mask=" + mask + "]";
	}

	public static IPAddress fromString(String ip) {
		IPAddress ipa = new IPAddress();
		if (ip.contains("/"))
		{
			String[] spl = ip.split("/");
			ip = spl[0];
			ipa.mask = Decoder.decodeLong(spl[1]);
		}
		ipa.address = Decoder.decodeIP4(ip);
		return ipa;
	}



}
