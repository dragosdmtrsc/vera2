package org.change.v2.model.routing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.change.v2.model.IPAddress;
import org.change.v2.model.NIC;
import org.change.v2.model.Route;
import org.change.v2.model.RoutingTable;
import org.change.v2.model.openflow.Decoder;

public class InterfaceParser {

	
	
	public static RoutingTable parseRoutingTable(InputStream is) throws IOException
	{
		List<Route> routes = new ArrayList<Route>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			Pattern p = Pattern.compile("[ ]*([0-9\\.]+)[ ]+([0-9\\.]+)[ ]+([0-9\\.]+)[ ]+.*[ ]+.*[ ]+.*[ ]+([a-zA-Z0-9\\-_]+)");
			Matcher m = p.matcher(line);
			if (m.matches())
			{
				String ip = m.group(1);
				String gw = m.group(2);
				String mask = m.group(3);
				String iface = m.group(4);
				Route r = new Route();
				r.setNetAddress(IPAddress.fromString(ip));
				r.getNetAddress().setMask((long) Decoder.convertNetmaskToCIDR(mask));
				r.setNextHop(IPAddress.fromString(gw));
				NIC theNic = new NIC();
				theNic.setName(iface);
				r.setNic(theNic);
				routes.add(r);
			}
		}
		RoutingTable rt = new RoutingTable();
		rt.getRoutes().addAll(routes);
		return rt;
	}
	
	public static List<NIC> parseNics(InputStream is) throws IOException
	{
		List<NIC> nics = new ArrayList<NIC>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		NIC nic = null;
		while ((line = br.readLine()) != null)
		{
			line = line.trim();
			Pattern p = Pattern.compile("[ ]*([0-9]+):[ ]*(.*):.*");
			Matcher m = p.matcher(line);
			if (m.matches())
			{
				String index = m.group(1);
				String name = m.group(2);
				nic = new NIC();
				nic.setName(name);
				nics.add(nic);
			}
			else
			{
				p = Pattern.compile("[ ]*link/ether[ ]+([0-9A-Fa-f:]+).*");
				m = p.matcher(line);
				if (m.matches())
				{
					if (nic != null)
						nic.setMacAddress(m.group(1));
				}
				else
				{
					p = Pattern.compile("[ ]*inet[ ]+([0-9\\./]+).*");
					m = p.matcher(line);
					if (m.matches())
					{
						if (nic != null)
						{
							String theGroup = m.group(1);
							String[] ipmask= theGroup.split("/");
							IPAddress addr = new IPAddress();
							addr.setAddress(Decoder.decodeIP4(ipmask[0]));
							addr.setMask(Decoder.decodeLong(ipmask[1]));
							nic.getIPAddresss().add(addr);
						}
					}
				}
			}
		}
		return nics;
	}
	
	public static void main(String []argv) throws FileNotFoundException, IOException
	{
		System.out.println(parseRoutingTable(new FileInputStream("stack-inputs/sample-route.txt")));
	}
	
}
