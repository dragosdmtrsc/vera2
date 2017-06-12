package org.change.v2.abstractnet.neutron;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.change.v2.util.CSVUtils;
import org.junit.Assert;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.builder.PortBuilder;
import org.openstack4j.model.network.builder.RouterBuilder;
import org.openstack4j.openstack.networking.domain.NeutronAllowedAddressPair;
import org.openstack4j.openstack.networking.domain.NeutronExternalGateway;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronIP;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
public class CSVBackedNetworking implements Networking {

	private Map<String, Router> routers = new HashMap<String, Router>();
	private Map<String, Port> ports = new HashMap<String, Port>();
	private Map<String, Network> rts = new HashMap<String, Network>();
	private Map<String, Subnet> subnets = new HashMap<String, Subnet>();
	private Map<String, NetFloatingIP> fips = new HashMap<String, NetFloatingIP>();
	private Map<String, SecurityGroup> secGroups = new HashMap<String, SecurityGroup>();
	private Map<String, SecurityGroupRule> secGroupRules = new HashMap<String, SecurityGroupRule>();

	
	
	private static ObjectMapper mapper = new ObjectMapper()
	.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	
	public static void main(String[] argv) throws JsonParseException, JsonMappingException, IOException
	{
		CSVBackedNetworking cbn = loadFromFolder("stack-inputs/generated2/");
		System.out.println(cbn.getRouters());
		System.out.println(cbn.getPorts());
		System.out.println(cbn.getNetworks());
		System.out.println(cbn.getSubnets());
		System.out.println(cbn.getFloatingIps());
		System.out.println(cbn.getSecurityGroups());
		System.out.println(cbn.getSecurityGroupRules());
	}
	
	public static class ExtendedExternalGWInfo extends NeutronExternalGateway {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7770399990769101150L;
		private ArrayList<NeutronIP> ips = new ArrayList<NeutronIP>();
		public ExtendedExternalGWInfo(String networkId, boolean enableSnat,
				ArrayList<NeutronIP> ips) {
			super(networkId, enableSnat);
			this.ips = ips;
		}
		
		public ExtendedExternalGWInfo(String networkId, boolean enableSnat) {
			this(networkId, enableSnat, new ArrayList<NeutronIP>());
		} 
		public Collection<NeutronIP> getIps() {
			return ips;
		}
	}
	
	private static void loadSecurityGroup(CSVBackedNetworking nnn,
			JsonNode node)
	{
		NeutronSecurityGroup p = new NeutronSecurityGroup();
		p.setId(node.path("id").asText());
		p.setName(node.path("name").asText());
		p.setTenantId(node.path("tenant_id").asText());
		if (p.getRules() == null) {
			if (p.getClass().equals(NeutronSecurityGroup.class)) {
				try {
					for (java.lang.reflect.Field f : NeutronSecurityGroup.class.getDeclaredFields())
					{
						if (f.getName().equals("rules")) {
							f.setAccessible(true);
							f.set(p, new ArrayList<NeutronSecurityGroupRule>());
							break;
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException
						| SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		nnn.secGroups.put(node.path("id").asText(), p);
	}
	
	
	@SuppressWarnings("unchecked")
	private static void loadPortFromResName(CSVBackedNetworking nnn,
			JsonNode node) throws IOException {
		PortBuilder pb = NeutronPort.builder();
		String ala = node.path("allowed_address_pairs").asText();
		pb.tenantId(node.path("tenant_id").asText())
		  .name(node.path("name").asText())
		  .deviceId(node.path("device_id").asText())
		  .deviceOwner(node.path("device_owner").asText())
		  .macAddress(node.path("mac_address").asText())
		  .networkId(node.path("network_id").asText());
		String fIps = node.path("fixed_ips").asText();
		if (fIps != null && fIps.length() != 0)
		{
			JsonNode fixedIps = mapper.readTree("[" + fIps + "]");
			for (JsonNode n : fixedIps)
			{
				pb = pb.fixedIp(n.path("ip_address").asText(), n.path("subnet_id").asText());
			}
		}
		
		String secGrps = node.path("security_groups").asText();
		if (!secGrps.isEmpty()) {
			if (!secGrps.startsWith("["))
				secGrps = "[\"" + secGrps + "\"]";
			JsonNode sNode = mapper.readTree(secGrps);
			for (JsonNode child : sNode)
				pb = pb.securityGroup(child.asText());
		}

		Port p = pb.build();
		p.setId(node.path("id").asText());
		if (p.getAllowedAddressPairs() == null) {
			if (p.getClass().equals(NeutronPort.class)) {
				try {
					for (java.lang.reflect.Field f : NeutronPort.class.getDeclaredFields())
					{
						if (f.getName().equals("allowedAddressPairs")) {
							f.setAccessible(true);
							f.set(p, new HashSet<NeutronAllowedAddressPair>());
						} else if (f.getName().equals("securityGroups")) {
							f.setAccessible(true);
							if (f.get(p) == null) {
								f.set(p, new ArrayList<String>());
							}
						}
					}
				} catch (IllegalArgumentException | IllegalAccessException
						| SecurityException e) {
					e.printStackTrace();
				}
			}
		}
		if (ala != null && ala.length() != 0)
		{
			JsonNode alaNode = mapper.readTree(ala);
			for (JsonNode child : alaNode) {
				((Set<NeutronAllowedAddressPair>)p.getAllowedAddressPairs()).add(
					new NeutronAllowedAddressPair(child.path("ip_address").asText(),
							child.path("mac_address").asText()));
			}
		}
		nnn.ports.put(node.path("id").asText(), p);
	}
	
	private static void loadRouterFromNode(CSVBackedNetworking nnn, JsonNode node) throws JsonProcessingException, IOException {
		String egs = node.path("external_gateway_info").asText();
		String id = node.path("id").asText();
		ExtendedExternalGWInfo eg = null;
		
		if (egs != null && !egs.isEmpty()) {
			JsonNode egNode = mapper.readTree(egs);
			
			eg = new ExtendedExternalGWInfo(egNode.path("network_id").asText(),
	    			egNode.path("enable_snat").asBoolean());
			int l = egNode.path("external_fixed_ips").size(); 
			for (int i = 0; i < l; i++) {
				JsonNode ipNode = egNode.path("external_fixed_ips").path(i);
				NeutronIP nip = new NeutronIP(ipNode.path("ip_address").asText(),
						ipNode.path("subnet_id").asText());
				eg.getIps().add(nip);
			}
		}
		
		String routesAsText = node.path("routes").asText();
		RouterBuilder bld = NeutronRouter.builder();

		if (routesAsText != null && routesAsText.length() != 0)
		{
			JsonNode routesNode = mapper.readTree(routesAsText);
    		int l = routesNode.size();
    		for (int i = 0; i < l; i++)
    		{
    			bld = bld.route(routesNode.path(i).path("destination").asText(), 
    					routesNode.path(i).path("nexthop").asText());
    		}
		}
		else {
			bld.route("", "");
			bld.build().getRoutes().clear();
		}
		nnn.routers.put(id, 
				bld.adminStateUp(node.path("admin_state_up").asBoolean())
					.id(id)
					.name(node.path("name").asText())
					.externalGateway(eg)
					.build()
		);
	}
	
	
	@SuppressWarnings("unchecked")
	private static void loadSecurityGroupRule(CSVBackedNetworking nnn, JsonNode node) throws JsonProcessingException, IOException {
		String id = node.path("id").asText();
		NeutronSecurityGroupRule nsgr =  mapper.readerFor(NeutronSecurityGroupRule.class).readValue(node);
		nnn.secGroupRules.put(id, nsgr);
		SecurityGroup grp = nnn.getSecurityGroup(nsgr.getSecurityGroupId());
		Assert.assertNotNull(grp);
		Assert.assertNotNull(grp.getRules());
		((List<NeutronSecurityGroupRule>)grp.getRules()).add(nsgr);
	}
	
	private static void loadNetwork(CSVBackedNetworking nnn, JsonNode node) throws JsonProcessingException, IOException {
		String id = node.path("id").asText();
		nnn.rts.put(id, mapper.readerFor(NeutronNetwork.class).readValue(node));
	}
	
	
	private static CSVBackedNetworking loadRouters(File dir, String resName, CSVBackedNetworking nnn) 
			throws JsonProcessingException, IOException {
		File fList = new File(dir, String.format("symnet-neutron-%s-list.csv", resName));
		Scanner scanner = new Scanner(fList);
		boolean firstLine = true;
        while (scanner.hasNext()) {
        	String read = scanner.nextLine();
        	if (firstLine) {
        		firstLine = false;
        		continue;
        	}
        	List<String> listRouter = CSVUtils.parseLine(read);
        	String id = listRouter.get(0);
        	
    		JsonNode node = mapper.readTree(new File(dir, 
    				String.format("symnet-neutron-%s-show-" + id + ".txt", resName)));
    		if ("router".equals(resName))
    			loadRouterFromNode(nnn, node);
    		else if ("port".equals(resName))
    			loadPortFromResName(nnn, node);
    		else if ("security-group".equals(resName))
    			loadSecurityGroup(nnn, node);
    		else if ("security-group-rule".equals(resName))
    			loadSecurityGroupRule(nnn, node);
    		else if ("net".equals(resName))
    			loadNetwork(nnn, node);
    		else if ("subnet".equals(resName))
    			loadSubnet(nnn, node);
    		else if ("floatingip".equals(resName))
    			loadFip(nnn, node);
    	}
        scanner.close();
        
        return nnn;
	}



	private static void loadSubnet(CSVBackedNetworking nnn, JsonNode node) throws JsonProcessingException, IOException {
		String id = node.path("id").asText();
		ObjectNode obj = (ObjectNode) node;
		obj.remove("host_routes");
		obj.remove("allocation_pools");
		nnn.subnets.put(id, mapper.readerFor(NeutronSubnet.class).readValue(obj));
	}
	
	private static void loadFip(CSVBackedNetworking nnn, JsonNode node) throws IOException {
		String id = node.path("id").asText();
		nnn.fips.put(id, mapper.readerFor(NeutronFloatingIP.class).readValue(node));
	}
	

	public static CSVBackedNetworking loadFromFolder(String folder) 
			throws JsonParseException, JsonMappingException, IOException {
		File dir = new File(folder);
		CSVBackedNetworking nnn = new CSVBackedNetworking();
		loadRouters(dir, "router", nnn);
		loadRouters(dir, "port", nnn);
		loadRouters(dir, "security-group", nnn);
		loadRouters(dir, "security-group-rule", nnn);
		loadRouters(dir, "net", nnn);
		loadRouters(dir, "subnet", nnn);
		loadRouters(dir, "floatingip", nnn);
		return nnn;
	}
	
	
	@Override
	public List<? extends Router> getRouters() {
		return new ArrayList<Router>(this.routers.values());
	}

	@Override
	public Router getRouter(String id) {
		return routers.get(id);
	}

	@Override
	public List<? extends Port> getPorts() {
		return new ArrayList<Port>(this.ports.values());
	}

	@Override
	public Port getPort(String id) {
		return ports.get(id);
	}
	
	
	@Override
	public List<? extends SecurityGroup> getSecurityGroups() {
		return 	new ArrayList<SecurityGroup>(secGroups.values());
	}

	@Override
	public SecurityGroup getSecurityGroup(String id) {
		return secGroups.get(id);
	}

	@Override
	public List<? extends SecurityGroupRule> getSecurityGroupRules() {
		return new ArrayList<SecurityGroupRule>(secGroupRules.values());
	}

	@Override
	public SecurityGroupRule getSecurityGroupRule(String id) {
		return secGroupRules.get(id);
	}

	@Override
	public List<? extends Network> getNetworks() {
		return new ArrayList<Network>(rts.values());
	}

	@Override
	public Network getNetwork(String id) {
		return rts.get(id);
	}

	@Override
	public List<? extends Subnet> getSubnets() {
		return new ArrayList<Subnet>(subnets.values());
	}

	@Override
	public Subnet getSubnet(String id) {
		return subnets.get(id);
	}

	@Override
	public List<? extends NetFloatingIP> getFloatingIps() {
		return 	new ArrayList<NetFloatingIP>(fips.values());
	}

	@Override
	public NetFloatingIP getFloatingIp(String id) {
		return fips.get(id);
	}

}
