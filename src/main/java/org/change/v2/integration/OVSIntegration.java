package org.change.v2.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.change.v2.model.Link;
import org.change.v2.model.NIC;
import org.change.v2.model.OVSBridge;
import org.change.v2.model.OpenFlowConfig;
import org.change.v2.model.OpenFlowTable;
import org.change.v2.model.ovs.OVSParser;

public class OVSIntegration {

	public static class OVSConfiguration
	{
		private List<OVSBridge> bridges = new ArrayList<OVSBridge>();
		
		private Set<Link> links = new HashSet<Link>();
		public List<OVSBridge> getBridge() {
			return bridges;
		}
		public Set<Link> getLinks() {
			return links;
		}
		
		public OVSConfiguration addOpenFlowPorts(InputStream is, String brName) throws IOException
		{
			OpenFlowConfig cfg = OpenFlowTable.appendPorts(is, bridges.stream().filter(s -> s.getName().equals(brName)).findFirst().get().getConfig());
			return this;
		}
		
		public OVSConfiguration addOpenFlowFlows(InputStream is, String brName) throws IOException
		{
			OpenFlowConfig cfg = OpenFlowTable.fromStream(is, bridges.stream().filter(s -> s.getName().equals(brName)).findFirst().get().getConfig());
			return this;
		}
		
		public OVSConfiguration addOvsDb(InputStream is) throws IOException
		{
			bridges.addAll(OVSParser.getBridges(is, new HashSet<NIC>(), links));
			return this;
		}
		@Override
		public String toString() {
			return "OVSConfiguration [bridges=" + bridges + ", links=" + links
					+ "]";
		}
		
		
		
		
	}

	public static void main(String[] argv) throws FileNotFoundException, IOException
	{
		OVSConfiguration cfg = new OVSConfiguration();
		File f = new File("stack-inputs");
		for (String fn : f.list())
		{
			if (fn.startsWith("ovsdb"))
			{
				cfg = cfg.addOvsDb(new FileInputStream(new File(f, fn)));
			}
		}
		
		for (String fn : f.list())
			if (fn.startsWith("flows"))
			{
				String brName = fn.substring("flows-".length());
				brName = brName.substring(0, brName.lastIndexOf('-'));
				cfg = cfg.addOpenFlowFlows(new FileInputStream(new File(f, fn)), brName);
			}
			else if (fn.startsWith("ports"))
			{
				String brName = fn.substring("ports-".length());
				brName = brName.substring(0, brName.lastIndexOf('-'));
				cfg = cfg.addOpenFlowPorts(new FileInputStream(new File(f, fn)), brName);
			}
		System.out.println(cfg);
	}
	
}
