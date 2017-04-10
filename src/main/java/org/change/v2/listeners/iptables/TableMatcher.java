package org.change.v2.listeners.iptables;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.model.iptables.IPTablesChain;
import org.change.v2.model.iptables.IPTablesTable;
import org.change.v2.model.iptables.JumpyTarget;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesParser.ChainContext;
import generated.iptables_grammar.IptablesParser.PolicyContext;
import generated.iptables_grammar.IptablesParser.RleContext;

public class TableMatcher extends IptablesBaseListener {
	private String name;
	private IPTablesTable table = new IPTablesTable();
	public TableMatcher(String name) {
		super();
		this.name = name;
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
		super.enterRle(ctx);
	}



	private void createChain(String name) {
		IPTablesChain chain = new IPTablesChain();
		chain.setName(name);
		this.table.getIPTablesChains().add(chain);
	}
	@Override
	public void enterPolicy(PolicyContext ctx) {
		super.enterPolicy(ctx);
		String name = ctx.chainname().NAME().getText();
		IPTablesChain chain = this.table.getIPTablesChains().stream().filter(s -> s.getName().equalsIgnoreCase(name)).findFirst().get();
		ParseTreeWalker walker = new ParseTreeWalker();
		TargetMatcher matcher = new TargetMatcher();
		walker.walk(matcher, ctx.targetName());
		chain.setPolicy(matcher.getTarget());
	}
	
	
	
	
}
