package org.change.v2.iptables;

import java.util.HashMap;
import java.util.Map;

public class StateDefinitions {
	public static final Map<String, Integer> NAMES = new HashMap<String, Integer>();
	
	public static final int NEW = 1,
			INVALID = 0,
			ESTABLISHED = 2,
			RELATED = 3,
			UNTRACKED = 4,
			SNAT = 5,
			DNAT = 6;
	
	
	static
	{
		NAMES.put("INVALID", INVALID);
		NAMES.put("NEW" , NEW);
		NAMES.put("ESTABLISHED", ESTABLISHED);
		NAMES.put("RELATED", RELATED);
		NAMES.put("UNTRACKED", UNTRACKED);
		NAMES.put("SNAT" , SNAT);
		NAMES.put("DNAT", DNAT);
	}
	

}
