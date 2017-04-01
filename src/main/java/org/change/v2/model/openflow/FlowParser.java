package org.change.v2.model.openflow;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FlowParser {

	
	public void parseFlowEntries(InputStream is) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ((line = br.readLine()) != null)
		{
			parseFlowEntry(line);
		}
	}
	
	public void parseFlowEntry(String theString)
	{
		String[] split = theString.split("[ ,]");
		for (String spl : split)
		{
			String[] args = spl.split("=");
			String field = args[0].trim();
			String value = args[1].trim();
		}
	}
	
	public static void main(String[] argv) throws FileNotFoundException, IOException
	{
		FlowParser fp = new FlowParser();
		fp.parseFlowEntries(new FileInputStream("openflow-config.txt"));
	}
	
}
