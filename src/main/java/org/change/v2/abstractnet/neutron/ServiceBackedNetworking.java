package org.change.v2.abstractnet.neutron;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.Subnet;

public class ServiceBackedNetworking implements Networking {
	private OSClient client;
	public ServiceBackedNetworking(OSClient client) 
	{
		this.client = client;
	}
	
	
	@Override
	public List<? extends Router> getRouters() {
		return this.client.networking().router().list();
	}

	@Override
	public Router getRouter(String id) {
		return this.client.networking().router().get(id);
	}

	@Override
	public List<? extends Port> getPorts() {
		return this.client.networking().port().list();
	}

	@Override
	public Port getPort(String id) {
		return this.client.networking().port().get(id);
	}

	@Override
	public List<? extends SecurityGroup> getSecurityGroups() {
		return this.client.networking().securitygroup().list();
	}

	@Override
	public SecurityGroup getSecurityGroup(String id) {
	    return this.client.networking().securitygroup().get(id);
	}

	@Override
	public List<? extends SecurityGroupRule> getSecurityGroupRules() {
		return this.client.networking().securityrule().list();
	}

	@Override
	public SecurityGroupRule getSecurityGroupRule(String id) {
		return this.client.networking().securityrule().get(id);
	}

	@Override
	public List<? extends Network> getNetworks() {
		return this.client.networking().network().list();

	}

	@Override
	public Network getNetwork(String id) {
		return this.client.networking().network().get(id);

	}

	@Override
	public List<? extends Subnet> getSubnets() {
		return this.client.networking().subnet().list();

	}

	@Override
	public Subnet getSubnet(String id) {
		return this.client.networking().subnet().get(id);

	}

	@Override
	public List<? extends NetFloatingIP> getFloatingIps() {
		return this.client.networking().floatingip().list();
	}

	@Override
	public NetFloatingIP getFloatingIp(String id) {
		return this.client.networking().floatingip().get(id);

	}

}
