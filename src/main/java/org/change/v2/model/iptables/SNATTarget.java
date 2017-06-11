package org.change.v2.model.iptables;

import org.change.v2.model.IVisitor;

import sun.net.util.IPAddressUtil;

public class SNATTarget extends IPTablesTarget {

	private long ipAddress;
	
	public long getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(long ipAddress) {
		this.ipAddress = ipAddress;
	}

	public SNATTarget(long ipAddress) {
		super("SNAT");
		this.ipAddress = ipAddress;
	}
	
	static long ipToLong(String ipAddress) {
		long result = 0;
		String[] ipAddressInArray = ipAddress.split("\\.");
		for (int i = 3; i >= 0; i--) {
			long ip = Long.parseLong(ipAddressInArray[3 - i]);
			result |= ip << (i * 8);	
		}
		return result;
    }
	public SNATTarget(String ip) {
		this(ipToLong(ip));
	}

	public SNATTarget() {
		super("SNAT");
	}

	@Override
	public void accept(IVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public String toString() {
		return "SNATTarget [ipAddress=" + ipAddress + "]";
	}

}
