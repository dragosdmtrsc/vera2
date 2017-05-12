package org.change.v2.listeners.iptables;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesLexer;
import generated.iptables_grammar.IptablesParser;
import generated.iptables_grammar.IptablesParser.ChainContext;
import generated.iptables_grammar.IptablesParser.PolicyContext;
import generated.iptables_grammar.IptablesParser.RleContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.model.iptables.IPTablesChain;
import org.change.v2.model.iptables.IPTablesTable;
import org.junit.Assert;

public class TableMatcher extends IptablesBaseListener {
	private String name;
	private IPTablesTable table = new IPTablesTable();
	
	public static IPTablesTable fromFile(InputStream is, String name) throws IOException
	{
		ANTLRInputStream input = new ANTLRInputStream(is);
		IptablesLexer lexer = new IptablesLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        IptablesParser parser = new IptablesParser(tokens);
        ParseTreeWalker walker = new ParseTreeWalker();
        TableMatcher listener = new TableMatcher(name);
        walker.walk(listener, parser.table());
        return listener.getTable();
	}
	
	public TableMatcher(String name) {
		super();
		table = new IPTablesTable();
		table.setName(name);
		this.createChain("PREROUTING");
		this.createChain("INPUT");
		this.createChain("OUTPUT");
		this.createChain("FORWARD");
		this.createChain("POSTROUTING");
	} 
	
	
	
	public String getName() {
		return name;
	}
	public IPTablesTable getTable() {
		return table;
	}

	@Override
	public void enterChain(ChainContext ctx) {
		super.enterChain(ctx);
		String name = ctx.chainname().NAME().getText();
		createChain(name);
	}



	@Override
	public void enterRle(RleContext ctx) {
		RuleListener listener = new RuleListener();
		ParseTreeWalker ptw = new ParseTreeWalker();
		ptw.walk(listener, ctx);
		String name = ctx.chainname().getText();
		Optional<IPTablesChain> opChain = this.table.getIPTablesChains().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
		IPTablesChain chain = null;
		if (opChain.isPresent())
		{
			chain = opChain.get();
		}
		else
		{
			chain = new IPTablesChain();
			chain.setName(name);
			this.table.getIPTablesChains().add(chain);
		}
		chain.getIPTablesRules().add(listener.getRule());
	}



	private void createChain(String name) {
		IPTablesChain chain = new IPTablesChain();
		chain.setName(name);
		if (!this.table.getIPTablesChains().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name)))
			this.table.getIPTablesChains().add(chain);
	}
	@Override
	public void enterPolicy(PolicyContext ctx) {
		super.enterPolicy(ctx);
		String name = ctx.chainname().NAME().getText();
		Optional<IPTablesChain> opChain = this.table.getIPTablesChains().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst();
		IPTablesChain chain = null;
		if (opChain.isPresent())
		{
			chain = opChain.get();
		}
		else
		{
			chain = new IPTablesChain();
			chain.setName(name);
			this.table.getIPTablesChains().add(chain);
		}
		ParseTreeWalker walker = new ParseTreeWalker();
		TargetMatcher matcher = new TargetMatcher();
		walker.walk(matcher, ctx.targetName());
		chain.setPolicy(matcher.getTarget());
	}
	
	
	public static void main(String[] argv) throws Exception
	{
		List<FileInputStream> fis;
		List<String> names;
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
		
		for (FileInputStream f : fis)
		{
			f.close();
		}
	}
	
	
}
