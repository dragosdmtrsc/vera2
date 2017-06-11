package org.change.v2.listeners.iptables;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.change.v2.model.iptables.IPTablesMatch;
import org.change.v2.model.iptables.IPTablesRule;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesParser.MatchContext;
import generated.iptables_grammar.IptablesParser.RleContext;
import generated.iptables_grammar.IptablesParser.TargetContext;

public class RuleListener extends IptablesBaseListener {

	private IPTablesRule rule = new IPTablesRule();

	public IPTablesRule getRule() {
		return rule;
	}

	@Override
	public void enterRle(RleContext ctx) {
		super.enterRle(ctx);
	}
	
	public void enterMatch(MatchContext ctx) {
		MatchListener listener = new MatchListener();
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(listener, ctx);
		IPTablesMatch option = listener.getMatch();
		if (option != null)
		{
			rule.getIPTablesMatches().add(option);
		}
	}
	
	public void enterTarget(TargetContext ctx) {
		ParseTreeWalker walker = new ParseTreeWalker();
		TargetMatcher theMatcher = new TargetMatcher();
		walker.walk(theMatcher, ctx);
		rule.setIPTablesTarget(theMatcher.getTarget());
	}
	
	
}
