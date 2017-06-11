package org.change.v2.iptables;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List; 

import org.change.v2.listeners.iptables.TableMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestAllIptables {

	List<FileInputStream> fis;
	List<String> names;
	@Before
	public void setUp() throws Exception {
		fis  = new ArrayList<FileInputStream>();
		names = new ArrayList<String>();
		File dir = new File("stack-inputs/generated");
		for (File f : dir.listFiles())
		{
			if (f.getName().contains("iptables"))
			{
				names.add(f.getName());
				fis.add(new FileInputStream(f));
			}
		}
	} 

	@After
	public void tearDown() throws Exception {
		for (FileInputStream f : fis)
		{
			f.close();
		}
		
	}

	@Test(timeout = 30000)
	public void test() throws Exception {
		int i = 0;
		for (FileInputStream f : fis)
		{
			try
			{
				Assert.assertNotNull(TableMatcher.fromFile(f, "nat"));
			}
			catch (Exception ex)
			{
				throw new Exception("Fail for " + names.get(i), ex);
			}
			System.out.println("Success for " + names.get(i++));
		}
	}

}
