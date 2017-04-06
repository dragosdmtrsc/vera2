package org.change.v2.listeners.openflow;

import java.util.ArrayList;
import java.util.List;

import org.change.v2.model.openflow.FlowEntry;

import generated.openflow_grammar.OpenflowBaseListener;
import generated.openflow_grammar.OpenflowParser.CookieContext;
import generated.openflow_grammar.OpenflowParser.FlowContext;
import generated.openflow_grammar.OpenflowParser.FlowsContext;
import generated.openflow_grammar.OpenflowParser.LearnContext;
import generated.openflow_grammar.OpenflowParser.LearnTableContext;
import generated.openflow_grammar.OpenflowParser.MatchFieldContext;
import generated.openflow_grammar.OpenflowParser.PriorityContext;

public class FlowEntryListener extends OpenflowBaseListener {

	private List<FlowEntry> entries = new ArrayList<FlowEntry>();

	public List<FlowEntry> getEntries() {
		return entries;
	}
	
	private FlowEntry currentEntry = null;
	
	public void enterFlow(FlowContext ctx) {
		currentEntry = new FlowEntry();
	}
	
	
	
	/**
	 * Start parsing the matches
	 */
	public void enterMatchField(MatchFieldContext ctx) {
		System.out.println("Entering the matchfield context " + ctx.getText());
		
		
	}
	
	/**
	 * Start parsing the actions
	 */
	
	

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
		this.currentEntry.setPriority(Integer.decode(ctx.NUMBER().getText()));

	}
	
	
}
