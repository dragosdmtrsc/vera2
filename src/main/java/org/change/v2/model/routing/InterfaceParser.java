package org.change.v2.model.routing;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.change.v2.model.NIC;
import org.change.v2.model.openflow.Decoder;

public class InterfaceParser {

	
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
							nic.getIPAddresss().setAddress(Decoder.decodeIP4(ipmask[0]));
							nic.getIPAddresss().setMask(Decoder.decodeLong(ipmask[1]));
						}
					}
				}
			}
			
		}
		return nics;
	}
	
	public static void main(String []argv) throws FileNotFoundException, IOException
	{
		System.out.println(parseNics(new FileInputStream("stack-inputs/sample-ipas.txt")));
	}
	
}
