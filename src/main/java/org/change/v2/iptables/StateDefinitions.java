package org.change.v2.iptables;

import java.util.HashMap;
import java.util.Map;

public class StateDefinitions {
	public static final Map<String, Integer> NAMES = new HashMap<String, Integer>();
	
	static
	{
		NAMES.put("INVALID", 0);
		NAMES.put("NEW" , 1);
		NAMES.put("ESTABLISHED", 2);
		NAMES.put("RELATED", 3);
		NAMES.put("UNTRACKED", 4);
		NAMES.put("SNAT" , 5);
		NAMES.put("DNAT", 6);
	}
	

}
