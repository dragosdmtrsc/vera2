package org.change.v2.model.ovs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.change.v2.model.Link;
import org.change.v2.model.NIC;
import org.change.v2.model.OVSBridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OVSParser {
	
	private static final String BRIDGE_TABLE = "Bridge table";
	private static final String PORT_TABLE = "Port table";
	private static final String INTERFACE_TABLE = "Interface table";
	
	private static class IfaceLookup {
		public String bridgeId, bridgeName, portId, portName, type, mac;
		public NIC theNic;
		public String nicId, nicName;
		Map<String, String> nicOptions = new HashMap<String, String>();
	}
	
	public static class LinkLookup {
		private String ifaceOne, ifaceTwo;
		public void setEnds(String ifaceOne, String ifaceTwo)
		{
			String first = ifaceOne.compareTo(ifaceTwo) <= 0 ? ifaceOne : ifaceTwo;
			String second = ifaceOne.compareTo(ifaceTwo) > 0 ? ifaceOne : ifaceTwo;
			this.ifaceOne = first;
			this.ifaceTwo = second;
		}
		public boolean equals(Object other)
		{
			if (other == null) return false;
			if (! (other instanceof LinkLookup)) return false;
			return this.ifaceOne.equals(((LinkLookup)other).ifaceOne) &&
				   this.ifaceTwo.equals(((LinkLookup)other).ifaceTwo);
		}
		
		public int hashCode() {
			return (this.ifaceOne + ":" + this.ifaceTwo).hashCode();
		}
		
		public boolean hasEnd(String iface)
		{
			return this.ifaceOne.equals(iface) || this.ifaceTwo.equals(iface);
		}
		@Override
		public String toString() {
			return "LinkLookup [ifaceOne=" + ifaceOne + ", ifaceTwo="
					+ ifaceTwo + "]";
		}
		
		
	}
	
	private static class PortLookup {
		public String bridgeName, bridgeId, portName, portId;
		public NIC theNic;
		List<String> ifaces = new ArrayList<String>();
		List<String> tags = new ArrayList<String>();
	}
	
	private static class BridgeLookup {
		public String bridgeId, bridgeName;
		public OVSBridge bridge;
		List<String> ports = new ArrayList<String>();

		
	}
	
	private Map<String, Integer> getHeadings(JsonNode node) {
		JsonNode headings = node.path("headings");
		Map<String, Integer> list = new HashMap<String, Integer>();
		if (headings.isArray())
		{
			int i = 0;
			for (JsonNode child : headings)
			{
				list.put(child.asText(), i++);
			}
		}
		return list;
	}
	
	private static JsonNode getData(JsonNode data, Map<String, Integer> headers, String name)
	{
		return data.get(headers.get(name));
	}
	
	

	public List<OVSBridge> getBridges(InputStream stream, 
			Set<NIC> whatElse,
			Set<Link> realLinks) 
			throws JsonProcessingException, IOException {
		assert(realLinks != null);
		assert(whatElse != null);
		ObjectMapper mapper = new ObjectMapper();
		ArrayList<OVSBridge> bridges = new ArrayList<OVSBridge>();
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		List<BridgeLookup> bridgeLookups = null;
		List<IfaceLookup> ifaces = null;
		List<PortLookup> portLookups = null;
		Set<LinkLookup> links = new HashSet<LinkLookup>();
		while ((line = br.readLine()) != null){
			JsonNode theNode = mapper.readTree(line);
			String tableName = theNode.path("caption").asText();
			if (BRIDGE_TABLE.equals(tableName))
			{
				bridgeLookups = this.parseBridges(theNode);
			}
			else if (PORT_TABLE.equals(tableName))
			{
				portLookups = this.parsePorts(theNode);
			}
			else if (INTERFACE_TABLE.equals(tableName))
			{
				ifaces = this.parseIfaces(theNode, links);
			}
		}
		
		for (BridgeLookup bridge : bridgeLookups)
		{
			OVSBridge real = new OVSBridge();
			real.setKind("OVS");
			real.setName(bridge.bridgeName);
			
			for (String portId : bridge.ports)
			{
				PortLookup port = portLookups.
						stream().
						filter(s -> s.portId.equals(portId)).
						findFirst().
						get();
				if (port.ifaces != null && port.ifaces.size() == 1)
				{
					String ifaceName = port.ifaces.get(0);
					Optional<IfaceLookup> optIface = ifaces.
							stream().
							filter(s -> s.nicId.equals(ifaceName)).
							findFirst();
					Optional<NIC> maybeNic = whatElse.stream().filter(s -> s.getName().equals(ifaceName)).findFirst();
					NIC realNic = null;
					if (maybeNic.isPresent())
					{
						realNic = maybeNic.get();
					}
					else
					{
						realNic = new NIC();
					}
					
					if (!optIface.isPresent() && !maybeNic.isPresent())
					{
						throw new IllegalArgumentException(String.format("Cannot do that, nic %s doesn't exist", ifaceName));
					}
					else if (optIface.isPresent())
					{
						IfaceLookup theIface = optIface.get();
						realNic = new NIC();
						realNic.setBridge(real);
						realNic.setMacAddress(theIface.mac);
						realNic.setName(port.portName);
						realNic.setType(theIface.type);
						for (String s : port.tags)
						{
							realNic.getVlans().add(Integer.decode(s));
						}
						realNic.getOptions().putAll(theIface.nicOptions);
					}
					real.getNICs().add(realNic);
				}
				else if (port.ifaces.size() != 1)
				{
					throw new UnsupportedOperationException("Cannot parse LACP just yet");
				}
				else
				{
					throw new IllegalArgumentException("No interfaces associated with the named port");
				}
			}
			
			
			bridges.add(real);
		}
		
		
		for (LinkLookup ll : links)
		{
			NIC oneEnd = null;
			NIC theOther = null;
			Link realLink = new Link();

			for (OVSBridge real : bridges)
			{
				Optional<NIC> nicOne = real.getNICs().stream().filter(s -> s.getName().equals(ll.ifaceOne)).findFirst();
				Optional<NIC> nicTwo = real.getNICs().stream().filter(s -> s.getName().equals(ll.ifaceTwo)).findFirst();
				if (nicOne.isPresent())
				{
					oneEnd = nicOne.get();
				}
				if (nicTwo.isPresent())
				{
					theOther = nicTwo.get();
				}
			}
			if (oneEnd == null || theOther == null)
				throw new IllegalArgumentException("Cannot find port for link " + ll);
			
			realLink.setNics(oneEnd, theOther);
			realLinks.add(realLink);
		}
		return bridges;
	}
	
	private List<BridgeLookup> parseBridges(JsonNode node)
	{
		List<BridgeLookup> bridges = new ArrayList<BridgeLookup>();
		Map<String, Integer> headers = this.getHeadings(node);
		System.out.println(headers);
		JsonNode dataNode = node.path("data");
		for (JsonNode br : dataNode)
		{
			String uuid = getData(br, headers, "_uuid").path(1).asText();
			String name = getData(br, headers, "name").asText();

			BridgeLookup lookup = new BridgeLookup();
			lookup.bridgeId = uuid;
			lookup.bridgeName = name;
			JsonNode portList = getData(br, headers, "ports").path(1);
			for (JsonNode port : portList)
			{
				lookup.ports.add(port.path(1).asText());
			}
			System.out.println(lookup.bridgeName);
			System.out.println(lookup.ports);
			bridges.add(lookup);
		}
		return bridges;
	}
	
	
	private List<PortLookup> parsePorts(JsonNode node) {
		List<PortLookup> bridges = new ArrayList<PortLookup>();
		Map<String, Integer> headers = this.getHeadings(node);
		System.out.println(headers);
		JsonNode dataNode = node.path("data");
		for (JsonNode br : dataNode)
		{
			String uuid = getData(br, headers, "_uuid").path(1).asText();
			String name = getData(br, headers, "name").asText();
			System.out.println("Port " + name);
			PortLookup lookup = new PortLookup();
			lookup.portName = name;
			lookup.portId = uuid;
			getData(br, headers, "tag").path(1).forEach(s -> lookup.tags.add(s.asText()));
			JsonNode ifaceList = getData(br, headers, "interfaces");
			int i = 0;
			for (JsonNode iface : ifaceList)
			{
				if (i % 2 == 1)
					lookup.ifaces.add(iface.asText());
				i++;
			}
			bridges.add(lookup);
		}
		return bridges;
	}
	
	private List<IfaceLookup> parseIfaces(JsonNode node, 
			Set<LinkLookup> links) {
		assert(links != null);
		List<IfaceLookup> bridges = new ArrayList<IfaceLookup>();
		Map<String, Integer> headers = this.getHeadings(node);
		System.out.println(headers);
		JsonNode dataNode = node.path("data");
		for (JsonNode br : dataNode)
		{
			String uuid = getData(br, headers, "_uuid").path(1).asText();
			String name = getData(br, headers, "name").asText();
			System.out.println("Iface " + name);
			IfaceLookup lookup = new IfaceLookup();
			lookup.nicId = uuid;
			lookup.nicName = name;
			lookup.type = getData(br, headers, "type").asText();
			lookup.mac = getData(br, headers, "mac_in_use").asText();
			JsonNode options = getData(br, headers, "options").path(1);
			for (JsonNode nnn : options)
			{
				if ("peer".equals(nnn.path(0).asText()))
				{
					LinkLookup ll = new LinkLookup();
					ll.setEnds(name, nnn.path(1).asText());
					links.add(ll);
				}
				else
					lookup.nicOptions.put(nnn.path(0).asText(), 
							nnn.path(1).asText());
			}
			bridges.add(lookup);
			System.out.println(lookup.nicOptions);
		}
		return bridges;
	}
	
	public static void main(String[] args) throws JsonProcessingException, IOException
	{
		InputStream str = new FileInputStream("ovsdb-config.json");
		Set<Link> links = new HashSet<Link>();
		new OVSParser().getBridges(str, new HashSet<NIC>(), links).stream().forEach(s -> {
			System.out.println(s.getName() + " (" + s.getKind() + ")");
			s.getNICs().forEach(u -> {
				System.out.println("\t" + u.getName() + " (" + u.getType() + ")");
			});
		});
		for (Link l : links)
		{
			System.out.println(l);
		}
	}
	
}
