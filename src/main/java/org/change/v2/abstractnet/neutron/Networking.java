package org.change.v2.abstractnet.neutron;

import java.util.List;

import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;

public interface Networking {
	
	List<? extends Router> getRouters();
	Router getRouter(String id);
	
	List<? extends Port> getPorts();
	Port getPort(String id);
	
	List<? extends SecurityGroup> getSecurityGroups();
	SecurityGroup getSecurityGroup(String id);

	List<? extends SecurityGroupRule> getSecurityGroupRules();
	SecurityGroupRule getSecurityGroupRule(String id);
	
	List<? extends Network> getNetworks();
	Network getNetwork(String id);
	
	List<? extends Subnet> getSubnets();
	Subnet getSubnet(String id);
	
	List<? extends NetFloatingIP> getFloatingIps();
	NetFloatingIP getFloatingIp(String id);
}
