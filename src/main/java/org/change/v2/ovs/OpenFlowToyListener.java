package org.change.v2.ovs;

import generated.openflow_grammar.OpenflowBaseListener;
import generated.openflow_grammar.OpenflowParser.CookieContext;
import generated.openflow_grammar.OpenflowParser.FlowContext;
import generated.openflow_grammar.OpenflowParser.FlowsContext;
import generated.openflow_grammar.OpenflowParser.LearnContext;
import generated.openflow_grammar.OpenflowParser.LearnTableContext;
import generated.openflow_grammar.OpenflowParser.MatchFieldContext;
import generated.openflow_grammar.OpenflowParser.PriorityContext;

public class OpenFlowToyListener extends OpenflowBaseListener {

	@Override
	public void enterFlow(FlowContext ctx) {
		super.enterFlow(ctx);
		System.out.println("Entered FlowContext:: " + ctx.getText());
	}
	public void enterMatchField(MatchFieldContext ctx) {
		System.out.println("Entering the matchfield context " + ctx.getText());
	}
	@Override
	public void enterFlows(FlowsContext ctx) {
		super.enterFlows(ctx);
//		System.out.println("Entered flows context " + ctx.getText());
	}

	@Override
	public void enterCookie(CookieContext ctx) {
		super.enterCookie(ctx);
		System.out.println("Entered CookieContext:: " + ctx.getText());
	}

	@Override
	public void enterLearn(LearnContext ctx) {
		super.enterLearn(ctx);
		System.out.println("Entered LearnContext:: " + ctx.getText());

	}

	@Override
	public void enterLearnTable(LearnTableContext ctx) {
		super.enterLearnTable(ctx);
		System.out.println("Entered enterLearnTable:: " + ctx.getText());
	} 
	
	public void enterPriority(PriorityContext ctx)
	{
		System.out.println("Entered PriorityContext:: " + ctx.getText());

	}
	
}
