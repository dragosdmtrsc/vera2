/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
// Start of user code (user defined imports)
import java.util.Map;
import java.util.Set;

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
	@Override
	public List<OVSNIC> getNICs() {
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
	
	
	public OpenFlowNIC getOpenFlowPort(String iface)
	{
		for (OpenFlowNIC nic : this.config.getNics())
		{
			if (nic.getName().equals(iface))
			{
				return nic;
			}
		}
		return null;
	}
	
	
	public OpenFlowNIC getOpenFlowPort(int at)
	{
		for (OpenFlowNIC nic : this.config.getNics())
		{
			if (nic.getPortNo() == at)
			{
				return nic;
			}
		}
		return null;
	}
	
	public OVSNIC getOvsNic(OpenFlowNIC ofNic)
	{
		for (OVSNIC nic : this.getNICs())
		{
			if (nic.getName().equalsIgnoreCase(ofNic.getName()))
			{
				return nic;
			}
		}
		return null;
	}
	
	
	public OVSNIC getOvsNic(String byName)
	{
		return this.getNICs().stream().filter(s -> s.getName().equalsIgnoreCase(byName)).findAny().get();
	}
	
	public boolean isVxlan(String iface) 
	{
		return false;
	}
	
	public boolean isBridged(String iface)
	{
		return false;
	}
	
	public NIC getPeer(String iface)
	{
		return null;
	}
	
	public OVSNIC getOvsNic(int forOfPort)
	{
		OpenFlowNIC ofNic = this.getOpenFlowPort(forOfPort);
		return this.getOvsNic(ofNic);
	}
	
	private HashMap<String, List<Integer>> ifaceToVlans = null;
	public Map<String, List<Integer>> getMappedIfaces()
	{
		if (ifaceToVlans == null)
		{
			ifaceToVlans = new HashMap<String, List<Integer>>();
			for (OVSNIC nic : this.getNICs())
			{
				if (!ifaceToVlans.containsKey(nic.getName()))
					ifaceToVlans.put(nic.getName(), new ArrayList<Integer>());
				ifaceToVlans.get(nic.getName()).addAll(nic.getVlans());
			}
		}
		return this.ifaceToVlans;
	}
	
	
	public List<Integer> getVlansForInterface(String iface)
	{
		return this.getMappedIfaces().containsKey(iface) ? this.getMappedIfaces().get(iface) : null;
	}
	
	private HashSet<Integer> distinctVlans ;
	public Set<Integer> getDistinctVlans()
	{
		if (distinctVlans == null)
		{
			distinctVlans = new HashSet<Integer>();
			for (OVSNIC nic : this.getNICs())
				distinctVlans.addAll(nic.getVlans());
		}
		return distinctVlans;
	}
	
	
	private Map<Integer, List<String>> reverseMap;
	public List<String> getIfacesForVlan(int vlan)
	{
		if (reverseMap == null)
		{
			reverseMap = new HashMap<Integer, List<String>>();
			for (OVSNIC nic : this.getNICs())
			{
				for (int vv : nic.getVlans())
				{
					if (!reverseMap.containsKey(vv))
					{
						reverseMap.put(vv, new ArrayList<String>());
					}
					reverseMap.get(vv).add(nic.getName());
				}
			}
		}
		return reverseMap.containsKey(vlan) ? reverseMap.get(vlan) : null;
		
	}
	
}
