package org.change.v2.model.iptables;

import java.util.Optional;

public class NATConfig {
	
	
	private Optional<Long> ipStart = Optional.empty();
	private Optional<Long> ipEnd = Optional.empty();
	private Optional<Long> portStart = Optional.empty();
	private Optional<Long> portEnd = Optional.empty();

	
	
	public boolean isUniqueIp() {
		return this.ipStart.isPresent() && this.ipEnd.isPresent() &&
				this.ipStart.get().longValue() == this.ipEnd.get().longValue();
	}
	
	
	public boolean isUniquePort() {
		return this.portStart.isPresent() && this.portEnd.isPresent() &&
				this.portStart.get().longValue() == this.portEnd.get().longValue();
	}
	
	public boolean hasPortStart() {
		return this.portStart.isPresent();
	}
	public boolean hasPortEnd() {
		return this.portEnd.isPresent();
	}
	
	public boolean hasIpStart() {
		return this.ipStart.isPresent();
	}
	public boolean hasIpEnd() {
		return this.ipEnd.isPresent();
	}
	
	
	public long getIp() {
		if (isUniqueIp())
			return this.ipStart.get();
		else 
			if (ipStart.isPresent())
				return getIpStart();
			else
				return getIpEnd();
	}
	
	
	public long getPort() {
		if (isUniquePort())
			return this.portStart.get();
		else 
			if (portStart.isPresent())
				return getPortStart();
			else
				return getPortEnd();
	}
	
	
	public long getPortStart() {
		if (portStart.isPresent())
			return portStart.get();
		return 0;
	}
	
	public long getPortEnd() {
		if (portEnd.isPresent())
			return portEnd.get();
		return 0xFFFFL;
	}
	
	
	public long getIpStart() {
		if (ipStart.isPresent())
			return ipStart.get();
		return 0;
	}
	
	public long getIpEnd() {
		if (ipEnd.isPresent())
			return ipEnd.get();
		return 0xFFFFFFFFL;
	}
	
	public static NATConfig create() {
		return new NATConfig();
	}
	
	
	public NATConfig setIpStart(long ip) {
		ipStart = Optional.of(ip);
		return this;
	}

	public NATConfig setIpEnd(long ip) {
		ipEnd = Optional.of(ip);
		return this;
	}
	
	public NATConfig setIp(long ip)
	{
		return setIpStart(ip).setIpEnd(ip);
	}
	
	public NATConfig setPort(long port)
	{
		return this.setPortStart(port).setPortEnd(port);
	}
	
	public NATConfig setPortStart(long port) {
		portStart = Optional.of(port);
		return this;
	}

	public NATConfig setPortEnd(long port) {
		portEnd = Optional.of(port);
		return this;
	}
}
