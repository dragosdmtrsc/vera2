package org.change.v2.listeners.iptables;

import java.util.Optional;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.model.iptables.IPTablesChain;
import org.change.v2.model.iptables.IPTablesTable;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesParser.ChainContext;
import generated.iptables_grammar.IptablesParser.PolicyContext;
import generated.iptables_grammar.IptablesParser.RleContext;

public class TableMatcher extends IptablesBaseListener {
	private String name;
	private IPTablesTable table = new IPTablesTable();
		
	
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
	
	
	
	
}
