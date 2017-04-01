package org.change.v2.iptables;

import java.util.HashMap;
import java.util.Map;

public class ICMPDefinitions {

	public static class ICMPTypeCode
	{
		private int type, code = -1;

		public ICMPTypeCode(int type, int code) {
			super();
			this.type = type;
			this.code = code;
		}

		public ICMPTypeCode(int type) {
			super();
			this.type = type;
		}

		public int getType() {
			return type;
		}
		
		public boolean hasCode()
		{
			return this.code != -1;
		}

		public int getCode() {
			return code;
		}
		
	}
	

	
	public static final Map<String, ICMPTypeCode> NAMES = new HashMap<String, ICMPTypeCode>();
	static {
		NAMES.put("echo-reply", new ICMPTypeCode(0, -1));
		NAMES.put("destination-unreachable", new ICMPTypeCode(3, -1));
		NAMES.put("network-unreachable", new ICMPTypeCode(3, 0));
		NAMES.put("host-unreachable", new ICMPTypeCode(3, 1));
		NAMES.put("protocol-unreachable", new ICMPTypeCode(3, 2));
		NAMES.put("port-unreachable", new ICMPTypeCode(3, 3));
		NAMES.put("fragmentation-needed", new ICMPTypeCode(3, 4));
		NAMES.put("source-route-failed", new ICMPTypeCode(3, 5));
		NAMES.put("network-unknown", new ICMPTypeCode(3, 6));
		NAMES.put("host-unknown", new ICMPTypeCode(3, 7));
		NAMES.put("network-prohibited", new ICMPTypeCode(3, 9));
		NAMES.put("host-prohibited", new ICMPTypeCode(3, 10));
		NAMES.put("TOS-network-unreachable", new ICMPTypeCode(3, 11));
		NAMES.put("TOS-host-unreachable", new ICMPTypeCode(3, 12));
		NAMES.put("communication-prohibited", new ICMPTypeCode(3, 13));
		NAMES.put("host-precedence-violation", new ICMPTypeCode(3, 14));
		NAMES.put("precedence-cutoff", new ICMPTypeCode(3, 15));
		NAMES.put("source-quench", new ICMPTypeCode(4, -1));
		NAMES.put("redirect", new ICMPTypeCode(5, -1));
		NAMES.put("network-redirect", new ICMPTypeCode(5, 0));
		NAMES.put("host-redirect", new ICMPTypeCode(5, 1));
		NAMES.put("TOS-network-redirect", new ICMPTypeCode(5, 2));
		NAMES.put("TOS-host-redirect", new ICMPTypeCode(5, 3));
		NAMES.put("echo-request", new ICMPTypeCode(8, -1));
		NAMES.put("router-advertisement", new ICMPTypeCode(9, -1));
		NAMES.put("router-solicitation", new ICMPTypeCode(10, -1));
		NAMES.put("time-exceeded (ttl-exceeded)", new ICMPTypeCode(11, -1));
		NAMES.put("ttl-zero-during-transit", new ICMPTypeCode(11, 0));
		NAMES.put("ttl-zero-during-reassembly", new ICMPTypeCode(11, 1));
		NAMES.put("parameter-problem", new ICMPTypeCode(12, -1));
		NAMES.put("ip-header-bad", new ICMPTypeCode(12, 0));
		NAMES.put("required-option-missing", new ICMPTypeCode(12, 1));
		NAMES.put("timestamp-request", new ICMPTypeCode(13, -1));
		NAMES.put("timestamp-reply", new ICMPTypeCode(14, -1));
		NAMES.put("address-mask-request", new ICMPTypeCode(17, -1));
		NAMES.put("address-mask-reply", new ICMPTypeCode(18, -1));
	}
}
