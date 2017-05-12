package org.change.v2.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.change.v2.listeners.iptables.TableMatcher;
import org.change.v2.model.openflow.PortParser;
import org.change.v2.model.ovs.OVSParser;
import org.change.v2.model.routing.InterfaceParser;
import org.change.v2.util.conversion.RepresentationConversion;

public class WorldModel {

	public static final String[] TABLES = new String[] { "nat", "mangle", "raw", "filter", "nat" };

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
			for (Namespace ns : c.getNamespaces())
			{
				for (NIC nic : ns.getNICs())
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
			for (Namespace ns : c.getNamespaces())
			{
				for (NIC nic : ns.getNICs())
				{
					if (nic.getIPAddresss().getAddress() == theAddress)
						return nic;
				}
			}
		}
		return null;
	}
	
	
	public static WorldModel fromFolder(String path) throws IOException
	{
		WorldModel world = new WorldModel();
		File dir = new File(path);
		File hosts = new File(dir, "hosts.txt");
		BufferedReader br = new BufferedReader(new FileReader(hosts));
		String currentHost = null;
		while ((currentHost = br.readLine()) != null)
		{
			currentHost = currentHost.trim();
			if (currentHost.length() == 0)
				continue;
			Computer pc = new Computer();
			pc.setName(currentHost);
			world.getPcs().add(pc);
			
			File nss = new File(dir, String.format("namespaces-%s.txt", currentHost));
			BufferedReader brNs = new BufferedReader(new FileReader(nss));
			String currentNs = null;
			List<String> namespaces = new ArrayList<String>();
			namespaces.add("root");
			while ((currentNs = brNs.readLine()) != null)
				namespaces.add(currentNs.trim());
		
			for (String crtNs : namespaces)
			{
				Namespace ns = new Namespace();
				ns.setName(crtNs);
				ns.setComputer(pc);
				pc.getNamespaces().add(ns);
				
				File ifaceFile = new File(dir, String.format("ifaces-%s-%s.txt", crtNs, currentHost));
				FileInputStream fis = new FileInputStream(ifaceFile);
				List<NIC> nics = InterfaceParser.parseNics(fis);
				ns.getNICs().addAll(nics);
				fis.close();
				
				fis = new FileInputStream(new File(dir, String.format("routes-%s-%s.txt", crtNs, currentHost)));
				RoutingTable table = InterfaceParser.parseRoutingTable(fis);
				ns.getRoutingTables().add(table);
				fis.close();
				
				for (String t : TABLES)
				{
					fis = new FileInputStream(new File(dir, String.format("iptables-%s-%s-%s.txt", t, crtNs, currentHost)));
					ns.getIPTablesTables().add(TableMatcher.fromFile(fis, t));
				}
			}
			brNs.close();
			FileInputStream fis = new FileInputStream(new File(dir, String.format("ovs-%s.txt", currentHost)));
			List<OVSBridge> bridges = OVSParser.getBridges(fis, 
					new HashSet<NIC>(), 
					new HashSet<Link>());
			pc.getBridges().addAll(bridges);
			for (Bridge bridge : pc.getBridges())
			{
				bridge.setComputers(pc);
				if (bridge instanceof OVSBridge)
				{
					
					OVSBridge ovsbr = (OVSBridge) bridge;
					fis = new FileInputStream(new File(dir, String.format("flows-%s-%s.txt", ovsbr.getName(), currentHost)));
					OpenFlowTable.fromStream(fis, ovsbr.getConfig());
					fis.close();
					
					fis = new FileInputStream(new File(dir, String.format("ports-%s-%s.txt", ovsbr.getName(), currentHost)));
					OpenFlowTable.appendPorts(fis, ovsbr.getConfig());
					fis.close();
				}
			}
		}
		br.close();
		return world;
	}
	
	
	public static void main(String[] argv) throws IOException
	{
		WorldModel wm = fromFolder("stack-inputs/generated");
		Assert.assertNotNull(wm.getComputer("controller"));
	}
	
}
