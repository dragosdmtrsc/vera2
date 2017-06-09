/*******************************************************************************
 * 2017, All rights reserved.
 *******************************************************************************/
package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.ActionType;
import org.change.v2.model.openflow.FlowEntry;
import org.change.v2.model.openflow.actions.LoadAction;

// Start of user code (user defined imports)

// End of user code

/**
 * Description of Computer.
 * 
 * @author Dragos
 */
public class Computer implements Acceptor {
	/**
	 * Description of the property bridges.
	 */
	public List<Bridge> bridges = new ArrayList<Bridge>();
	
	/**
	 * Description of the property symnetNamespaces.
	 */
	public List<Namespace> symnetNamespaces = new ArrayList<Namespace>();
	
	/**
	 * Description of the property name.
	 */
	private String name = "";
	
	/**
	 * Description of the property id.
	 */
	private String id = "";
	
	// Start of user code (user defined attributes for Computer)
	
	// End of user code
	
	/**
	 * The constructor.
	 */
	public Computer() {
		// Start of user code constructor for Computer)
		super();
		// End of user code
	}
	
	// Start of user code (user defined methods for Computer)
	
	// End of user code
	/**
	 * Returns bridges.
	 * @return bridges 
	 */
	public List<Bridge> getBridges() {
		return this.bridges;
	}

	/**
	 * Returns symnetNamespaces.
	 * @return symnetNamespaces 
	 */
	public List<Namespace> getNamespaces() {
		return this.symnetNamespaces;
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

	/**
	 * Returns id.
	 * @return id 
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Sets a value to attribute id. 
	 * @param newId 
	 */
	public void setId(String newId) {
	    this.id = newId;
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}
	
	private Map<String, NIC> preloaded = null;
	private Map<String, NIC> ipIfaces = null;
	
	
	public NIC getIpNic(String name)
	{
		preloadNICs();
		if (ipIfaces.containsKey(name))
		{
			return ipIfaces.get(name);
		}
		return null;
	}
	
	public NIC getNic(String name)
	{
		preloadNICs();
		if (preloaded.containsKey(name))
		{
			return preloaded.get(name);
		}
		return null;
	}

	private void preloadNICs() {
		if (preloaded == null)
		{
			ipIfaces = new HashMap<String, NIC>();
			preloaded = new HashMap<String, NIC>();
			for (Namespace ns : symnetNamespaces)
			{
				List<NIC> nics = ns.getNICs();
				for (NIC nic : nics)
				{
					ipIfaces.put(nic.getName(), nic);
					preloaded.put(nic.getName(), nic);
				}
			}
			
			for (Bridge br : this.getBridges())
			{
				for (NIC n : br.getNICs())
				{
					if (!preloaded.containsKey(n.getName()))
					{
						preloaded.put(n.getName(), n);
					}
				}
			}
			
		}
	}
	
	
	public boolean isBridged(String iface)
	{
		NIC theNic = this.getNic(iface);
		if (theNic == null)
		{
			return false;
		}
		else
		{
			for (Bridge br : this.getBridges())
			{
				List<? extends NIC> nics = br.getNICs();
				for (NIC nic : nics)
				{
					if (nic.getName().equalsIgnoreCase(iface))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	
	public Bridge getBridge(String iface)
	{
		NIC theNic = this.getNic(iface);
		if (theNic == null)
		{
			return null;
		}
		else
		{
			for (Bridge br : this.getBridges())
			{
				List<? extends NIC> nics = br.getNICs();
				for (NIC nic : nics)
				{
					if (nic.getName().equalsIgnoreCase(iface))
					{
						return br;
					}
				}
			}
		}
		return null;
	}
	 
	
	public List<NIC> getNicsByFilter(String filter)
	{
		preloadNICs();
		List<NIC> lst = new ArrayList<NIC>();
		if (filter.endsWith("+"))
		{
			filter = filter.substring(0, filter.length() - 1);
			for (Entry<String, NIC> sn : preloaded.entrySet())
			{
				if (sn.getKey().startsWith(filter))
					lst.add(sn.getValue());
			}
		}
		else
		{
			lst.add(getNic(filter));
		} 
		return lst;
	}
	
	private Map<String, Namespace> nicToNamespace;
	public Namespace getNamespaceForNic(String nicName)
	{
		if (nicToNamespace == null)
		{
			nicToNamespace = new HashMap<String, Namespace>();
			for (Namespace ns : this.getNamespaces())
			{
				for (NIC nic : ns.getNICs())
				{
					nicToNamespace.put(nic.getName(), ns);
				}
			}
		}
		
		if (nicToNamespace.containsKey(nicName))
		{
			return nicToNamespace.get(nicName);
		}
		else
		{
			return null;
		}
	}
	
	public NIC getNICListeningOnIP(String ipAddress)
	{
		for (Bridge br : this.getBridges()) {
			for (NIC n : br.getNICs())
			{
				if (n.getOptions().containsKey("local_ip"))
				{
					if (n.getOptions().get("local_ip").trim().equals(ipAddress.trim()))
						return n;
				}
			}
		}
		return null;
	}
	
	
	private List<Integer> _zones = null;
	public List<Integer> getCtZones() {
		if (_zones == null)
		{
			this._zones = new ArrayList<Integer>();
			for (Bridge br : this.bridges)
			{
				if (br instanceof OVSBridge)
				{
					OVSBridge ovs = (OVSBridge) br;
					for (OpenFlowTable oft : ovs.getConfig().getTables())
					{
						for (FlowEntry fe : oft.getEntries())
						{
							for (Action a : fe.getActions())
							{
								if (a.getType() == ActionType.Load)
								{
									LoadAction la = (LoadAction) a;
									if (la.getTo().isPresent() && 
											(la.getTo().get().getName().equals("NXM_NX_REG6") ||
										    la.getTo().get().getName().equals("reg6")) && 
										    la.getValue().isPresent())
									{
										_zones.add(la.getValue().get().intValue());
									}
								}
							}
						}
					}
				}
			}
		}
		return this._zones;
	}
	
	
	
	public List<NIC> getBridgedNICs() {
		List<NIC> nics = new ArrayList<NIC>();
		for (Bridge br : this.getBridges())
			for (NIC n : br.getNICs())
				nics.add(n);
		return nics;
	}
}
