package org.change.v2.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.change.v2.util.conversion.RepresentationConversion;

public class WorldModel {

	private List<Computer> pcs = new ArrayList<Computer>();
	private List<Link> links = new ArrayList<Link>();
	public List<Computer> getPcs() {
		return pcs;
	}
	public List<Link> getLinks() {
		return links;
	}
	
	
	private Map<String, Computer> computersPreload = null;
	public Computer getComputer(String name) {
		if (computersPreload == null)
		{
			this.computersPreload = new HashMap<String, Computer>();
			for (Computer c : pcs)
			{
				this.computersPreload.put(c.getName(), c);
			}
		}
		return this.computersPreload.containsKey(name) ? computersPreload.get(name) : null;
	}
	
	
	public Computer getComputerByIp(String ip)
	{
		long theAddress = RepresentationConversion.ipToNumber(ip);
		return getComputerByIp(theAddress);
	}
	public Computer getComputerByIp(long theAddress) {
		for (Computer c : this.pcs)
		{
			for (Namespace ns : c.getSymnetNamespaces())
			{
				for (NIC nic : ns.getSymnetNICs())
				{
					if (nic.getIPAddresss().getAddress() == theAddress)
						return c;
				}
			}
		}
		return null;
	}
	
	public NIC getNICByIp(String ip)
	{
		long theAddress = RepresentationConversion.ipToNumber(ip);
		return getNICByIp(theAddress);
	}
	public NIC getNICByIp(long theAddress) {
		for (Computer c : this.pcs)
		{
			for (Namespace ns : c.getSymnetNamespaces())
			{
				for (NIC nic : ns.getSymnetNICs())
				{
					if (nic.getIPAddresss().getAddress() == theAddress)
						return nic;
				}
			}
		}
		return null;
	}
	
}
