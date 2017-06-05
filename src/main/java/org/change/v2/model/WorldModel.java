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
import java.util.Optional;

import junit.framework.Assert;

import org.change.v2.listeners.iptables.TableMatcher;
import org.change.v2.model.openflow.FlowEntry;
import org.change.v2.model.openflow.Match;
import org.change.v2.model.ovs.OVSParser;
import org.change.v2.model.routing.InterfaceParser;
import org.change.v2.util.conversion.RepresentationConversion;

import com.google.common.collect.ImmutableMap;

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
					for (IPAddress ipa : nic.getIPAddresss())
					{
						if (ipa.getAddress() == theAddress)
							return c;
					}
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
					for (IPAddress ipa : nic.getIPAddresss())
					{
						if (ipa.getAddress() == theAddress)
							return nic;
					}
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
	
	private Map<Long, Long> inferredArpTable = null;
	private static final int ARP_SPOOF_TABLE = 24;
	private void inferArpTables() {
		inferredArpTable=  new HashMap<Long, Long>();
		for (Computer pc : this.getPcs())
		{
			for (Bridge br : pc.getBridges())
			{
				if (br instanceof OVSBridge)
				{
					OVSBridge ovbr = (OVSBridge) br;
					if (ovbr.getName().equals("br-int"))
					{
						List<OpenFlowNIC> nics = ovbr.getConfig().getNics();
						Optional<OpenFlowTable> tab = ovbr.getConfig().getTables().stream().filter(s -> s.getTableId() == ARP_SPOOF_TABLE).findAny();
						if (tab.isPresent()) {
							OpenFlowTable table = tab.get();
							for (FlowEntry fe : table.getEntries()) {
								if (fe.getPriority() == 2) {
									boolean isArp = false;
									long ipaddr = 0;
									long port = -1024;
									List<Match> matches = fe.getMatches();
									for (Match m : matches) {
										String name = m.getField().getName();
										if (name.equals("dl_type")) {
											if (m.getValue() == 0x806) {
												isArp = true;
											}
										} else if (name.equals("arp_spa")) {
											ipaddr = m.getValue();
										} else if (name.equals("in_port")) {
											port = m.getValue();
										}
									}
									if (isArp && port >= 0 && ipaddr != 0) {
										OVSNIC ovsNic = ovbr.getOvsNic((int) port);
										long mac = ovsNic.getVmMac(); 
										if (ovsNic.isVmConnected() && mac != -1) {
											this.inferredArpTable.put(ipaddr, mac);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public Map<Long, Long> getInferredArpTables() {
		if (this.inferredArpTable == null)
			this.inferArpTables();
		return this.inferredArpTable;
	}
	
	public static void main(String[] argv) throws IOException
	{
		WorldModel wm = fromFolder("stack-inputs/generated");
		Assert.assertNotNull(wm.getComputer("controller"));
	}
	
}
