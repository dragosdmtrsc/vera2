package org.change.v2.model.openflow;

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

import org.change.v2.model.OpenFlowNIC;

public class PortParser {

	
	public static List<OpenFlowNIC> parseNics(InputStream is) throws IOException
	{
		List<OpenFlowNIC> nics = new ArrayList<OpenFlowNIC>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		String line = null;
		while ((line = br.readLine()) != null)
		{
			Pattern rex = Pattern.compile("[ ]*(.*)\\((.*)\\).*addr:(.*)");
			Matcher m = rex.matcher(line);
			if (m.matches())
			{
				String number = m.group(1);
				String name = m.group(2);
				String addr = m.group(3);
				OpenFlowNIC nic = new OpenFlowNIC();
				nic.setMacAddress(addr);
				nic.setName(name);
				nic.setPortNo(Decoder.decodePort(number));
				nics.add(nic);
			}
		}
		
		return nics;
	}
	
	
	public static void main(String[] argv) throws FileNotFoundException, IOException
	{
		System.out.println(parseNics(new FileInputStream("stack-inputs/ports-br-int-controller.txt")));
		System.out.println(parseNics(new FileInputStream("stack-inputs/ports-br-tun-controller.txt")));
		System.out.println(parseNics(new FileInputStream("stack-inputs/ports-br-provider-controller.txt")));

	}
	
}
