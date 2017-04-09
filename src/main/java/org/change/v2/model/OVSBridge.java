/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.List;
// Start of user code (user defined imports)

// End of user code

/**
 * Description of OVSBridge.
 * 
 * @author Dragos
 */
public class OVSBridge extends Bridge {
	// Start of user code (user defined attributes for OVSBridge)
	private List<OVSNIC> nics = new ArrayList<OVSNIC>();
	private OpenFlowConfig config = new OpenFlowConfig();
	
	// End of user code
	
	public List<OVSNIC> getNics() {
		return nics;
	}

	public OpenFlowConfig getConfig() {
		return config;
	}

	public OVSBridge() {
		super();
	}

	@Override
	public String toString() {
		return "OVSBridge [nics=" + nics + ", config=" + config + "]";
	}
	
}
