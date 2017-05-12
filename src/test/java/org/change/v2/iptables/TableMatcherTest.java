package org.change.v2.iptables;

import java.io.FileInputStream;
import java.io.IOException;

import org.change.v2.listeners.iptables.TableMatcher;
import org.change.v2.model.iptables.IPTablesTable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.FileInputStream;

public class TableMatcherTest {

	FileInputStream fis;
	@Before
	public void setUp() throws Exception {
		 fis = new FileInputStream("stack-inputs/generated/iptables-nat-root-controller.txt");
	}

	@After
	public void tearDown() throws Exception {
		fis.close();
	}
 
	@Test(timeout = 3000)
	public void testTableMatcher() throws IOException {
		
		IPTablesTable table = TableMatcher.fromFile(fis, "nat");
		Assert.assertNotNull(table);
	}

}
