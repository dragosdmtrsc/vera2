package org.change.v2.listeners.openflow;

import generated.openflow_grammar.OpenflowBaseListener;
import generated.openflow_grammar.OpenflowParser.AllContext;
import generated.openflow_grammar.OpenflowParser.ApplyActionsContext;
import generated.openflow_grammar.OpenflowParser.ClearActionsContext;
import generated.openflow_grammar.OpenflowParser.ControllerContext;
import generated.openflow_grammar.OpenflowParser.ControllerWithIdContext;
import generated.openflow_grammar.OpenflowParser.ControllerWithParamsContext;
import generated.openflow_grammar.OpenflowParser.CookieContext;
import generated.openflow_grammar.OpenflowParser.DecMplsTTLContext;
import generated.openflow_grammar.OpenflowParser.DecTTLContext;
import generated.openflow_grammar.OpenflowParser.DecTTLWithParamsContext;
import generated.openflow_grammar.OpenflowParser.DropContext;
import generated.openflow_grammar.OpenflowParser.EnqueueContext;
import generated.openflow_grammar.OpenflowParser.ExitContext;
import generated.openflow_grammar.OpenflowParser.FinTimeoutContext;
import generated.openflow_grammar.OpenflowParser.FloodContext;
import generated.openflow_grammar.OpenflowParser.FlowContext;
import generated.openflow_grammar.OpenflowParser.FlowsContext;
import generated.openflow_grammar.OpenflowParser.ForwardToPortTargetContext;
import generated.openflow_grammar.OpenflowParser.GotoContext;
import generated.openflow_grammar.OpenflowParser.InPortContext;
import generated.openflow_grammar.OpenflowParser.LearnContext;
import generated.openflow_grammar.OpenflowParser.LoadContext;
import generated.openflow_grammar.OpenflowParser.LocalContext;
import generated.openflow_grammar.OpenflowParser.MatchFieldContext;
import generated.openflow_grammar.OpenflowParser.ModVlanPcpContext;
import generated.openflow_grammar.OpenflowParser.ModVlanVidContext;
import generated.openflow_grammar.OpenflowParser.MoveContext;
import generated.openflow_grammar.OpenflowParser.NormalContext;
import generated.openflow_grammar.OpenflowParser.OutputPortContext;
import generated.openflow_grammar.OpenflowParser.OutputRegContext;
import generated.openflow_grammar.OpenflowParser.PopContext;
import generated.openflow_grammar.OpenflowParser.PopMplsContext;
import generated.openflow_grammar.OpenflowParser.PopQueueContext;
import generated.openflow_grammar.OpenflowParser.PriorityContext;
import generated.openflow_grammar.OpenflowParser.PushContext;
import generated.openflow_grammar.OpenflowParser.PushMplsContext;
import generated.openflow_grammar.OpenflowParser.PushVlanContext;
import generated.openflow_grammar.OpenflowParser.ResubmitContext;
import generated.openflow_grammar.OpenflowParser.ResubmitSecondContext;
import generated.openflow_grammar.OpenflowParser.ResubmitTableContext;
import generated.openflow_grammar.OpenflowParser.SampleContext;
import generated.openflow_grammar.OpenflowParser.SetDlDstContext;
import generated.openflow_grammar.OpenflowParser.SetDlSrcContext;
import generated.openflow_grammar.OpenflowParser.SetFieldContext;
import generated.openflow_grammar.OpenflowParser.SetMplsTTLContext;
import generated.openflow_grammar.OpenflowParser.SetNwDstContext;
import generated.openflow_grammar.OpenflowParser.SetNwSrcContext;
import generated.openflow_grammar.OpenflowParser.SetNwTosContext;
import generated.openflow_grammar.OpenflowParser.SetQueueContext;
import generated.openflow_grammar.OpenflowParser.SetTpDstContext;
import generated.openflow_grammar.OpenflowParser.SetTpSrcContext;
import generated.openflow_grammar.OpenflowParser.SetTunnel64Context;
import generated.openflow_grammar.OpenflowParser.SetTunnelContext;
import generated.openflow_grammar.OpenflowParser.StripVlanContext;
import generated.openflow_grammar.OpenflowParser.TableContext;
import generated.openflow_grammar.OpenflowParser.WriteMetadataContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.change.v2.model.openflow.Action;
import org.change.v2.model.openflow.Decoder;
import org.change.v2.model.openflow.FlowEntry;
import org.change.v2.model.openflow.Match;
import org.change.v2.model.openflow.QualifiedField;
import org.change.v2.model.openflow.actions.AllAction;
import org.change.v2.model.openflow.actions.ApplyActionsAction;
import org.change.v2.model.openflow.actions.ClearActionsAction;
import org.change.v2.model.openflow.actions.ControllerAction;
import org.change.v2.model.openflow.actions.DropAction;
import org.change.v2.model.openflow.actions.ExitAction;
import org.change.v2.model.openflow.actions.FloodAction;
import org.change.v2.model.openflow.actions.InPortAction;
import org.change.v2.model.openflow.actions.LearnAction;
import org.change.v2.model.openflow.actions.LoadAction;
import org.change.v2.model.openflow.actions.LocalAction;
import org.change.v2.model.openflow.actions.ModDlDstAction;
import org.change.v2.model.openflow.actions.ModDlSrcAction;
import org.change.v2.model.openflow.actions.ModNwDstAction;
import org.change.v2.model.openflow.actions.ModNwSrcAction;
import org.change.v2.model.openflow.actions.ModNwTosAction;
import org.change.v2.model.openflow.actions.ModTpDstAction;
import org.change.v2.model.openflow.actions.ModTpSrcAction;
import org.change.v2.model.openflow.actions.ModVlanVidAction;
import org.change.v2.model.openflow.actions.MoveAction;
import org.change.v2.model.openflow.actions.NormalAction;
import org.change.v2.model.openflow.actions.OutputAction;
import org.change.v2.model.openflow.actions.ResubmitAction;
import org.change.v2.model.openflow.actions.SetFieldAction;
import org.change.v2.model.openflow.actions.SetTunnelAction;

public class FlowEntryListener extends OpenflowBaseListener {

	private List<FlowEntry> entries = new ArrayList<FlowEntry>();

	public List<FlowEntry> getEntries() {
		return entries;
	}
	
	private FlowEntry currentEntry = null;
	
	public void enterFlow(FlowContext ctx) {
		currentEntry = new FlowEntry();
	}
	
	public void exitFlow(FlowContext ctx) {
		System.out.println(currentEntry);
		this.entries.add(this.currentEntry);
		this.currentEntry = null;
	}
	
	
	
	/**
	 * Start parsing the matches
	 */
	public void enterMatchField(MatchFieldContext ctx) {
		String[] splits;
		if (!ctx.getText().contains("="))
			splits = new String[] { ctx.getText() };
		else
			splits = ctx.getText().split("=");
		String matchName = splits[0];
		String value = splits.length > 1 ? splits[1] : "";
		long mask = -1;
		boolean isMasked = false;
		if ("".equals(value))
		{
			Match theMatch = new Match();
			theMatch.setField(QualifiedField.fromString(matchName));
			this.currentEntry.getMatches().add(theMatch);
		}
		else
		{
			QualifiedField qf = QualifiedField.fromString(matchName);
			Match theMatch = new Match();
			theMatch.setField(qf);

			if (value.contains("/"))
			{
				String[] vmask = value.split("/");
				value = vmask[0];
				String smask = vmask[1];
				mask =Decoder.decodeType(qf.getName(), smask);
				isMasked = true;
			}
			else if (value.contains("["))
			{
				theMatch.setValue(QualifiedField.fromString(value));
			}
			else
			{
				long ivalue =Decoder.decodeType(qf.getName(), value);
				theMatch.setValue(ivalue);
				theMatch.setMask(mask);
			}
			theMatch.setMasked(isMasked);

			this.currentEntry.getMatches().add(theMatch);

		}

	}

	
	

	/**
	 * Start parsing some metadata
	 */
	@Override
	public void enterFlows(FlowsContext ctx) {
		super.enterFlows(ctx);
//		System.out.println("Entered flows context " + ctx.getText());
	}

	@Override
	public void enterCookie(CookieContext ctx) {
		super.enterCookie(ctx);
//		System.out.println("Entered CookieContext:: " + ctx.NUMBER().getText());
		String theHex = ctx.NUMBER().getText();
		if (theHex.startsWith("0x"))
		{
			theHex = theHex.substring(2);
		}
		this.currentEntry.setCookie(Long.parseUnsignedLong(theHex, 16));
	}


	public void enterPriority(PriorityContext ctx)
	{
		this.currentEntry.setPriority(Integer.decode(ctx.NUMBER().getText()));
	}
	
	public void enterTable(TableContext ctx)
	{
		this.currentEntry.setTable(Integer.decode(ctx.NUMBER().getText()));
	}
	
	
	
	/**
	 * Start parsing the actions
	 */

	public void enterOutputPort(OutputPortContext ctx) {
		OutputAction action = new OutputAction();
		action.setPort(Decoder.decodePort(ctx.port().getText()));
		this.currentEntry.getActions().add(action);
	}
	public void enterOutputReg(OutputRegContext ctx) {
		OutputAction action = new OutputAction();
		action.setReg(true);
		action.setRegName(QualifiedField.fromString(ctx.fieldName().getText()));
		this.currentEntry.getActions().add(action);
	}
	public void enterEnqueue(EnqueueContext ctx) {
		//TODO: No action
	}
	public void enterNormal(NormalContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new NormalAction());
	}
	public void enterFlood(FloodContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new FloodAction());

	}
	public void enterAll(AllContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new AllAction());

	}
	public void enterControllerWithParams(ControllerWithParamsContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new AllAction());
	}
	public void enterController(ControllerContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ControllerAction());
	}
	public void enterControllerWithId(ControllerWithIdContext ctx) {
		//TODO: No action
	}
	public void enterLocal(LocalContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new LocalAction());
	}
	public void enterInPort(InPortContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new InPortAction());
	}
	public void enterDrop(DropContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new DropAction());
	}
	public void enterModVlanVid(ModVlanVidContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new ModVlanVidAction(Decoder.decodeLong(ctx.NUMBER().getText())));
	}
	public void enterModVlanPcp(ModVlanPcpContext ctx) {
		//TODO: No action
	}
	public void enterStripVlan(StripVlanContext ctx) {
	}
	public void enterPushVlan(PushVlanContext ctx) {
		//TODO: No action
	}
	public void enterPushMpls(PushMplsContext ctx) {
		//TODO: No action
	}
	public void enterPopMpls(PopMplsContext ctx) {
		//TODO: No action
	}
	public void enterSetDlSrc(SetDlSrcContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new ModDlSrcAction(Decoder.decodeMAC(ctx.MAC().getText())));
	}
	public void enterSetDlDst(SetDlDstContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ModDlDstAction(Decoder.decodeMAC(ctx.MAC().getText())));
	}
	public void enterSetNwSrc(SetNwSrcContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ModNwSrcAction(Decoder.decodeIP4(ctx.ip().getText())));

	}
	public void enterSetNwDst(SetNwDstContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ModNwDstAction(Decoder.decodeIP4(ctx.ip().getText())));

	}
	public void enterSetTpSrc(SetTpSrcContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ModTpSrcAction(Decoder.decodeLong(ctx.NUMBER().getText())));
	}
	public void enterSetTpDst(SetTpDstContext ctx) {
		//TODO: No action
		this.currentEntry.getActions().add(new ModTpDstAction(Decoder.decodeLong(ctx.NUMBER().getText())));

	}
	public void enterSetNwTos(SetNwTosContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new ModNwTosAction(Decoder.decodeLong(ctx.NUMBER().getText())));
	}
	public void enterResubmit(ResubmitContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(ResubmitAction.fromString(ctx.getText()));

	}
	public void enterResubmitSecond(ResubmitSecondContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(ResubmitAction.fromString(ctx.getText()));

	}
	public void enterResubmitTable(ResubmitTableContext ctx) {
		//TODO: Code here	
		this.currentEntry.getActions().add(ResubmitAction.fromString(ctx.getText()));

	} 
	public void enterSetTunnel(SetTunnelContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new SetTunnelAction(Decoder.decodeLong(ctx.NUMBER().getText())));
	}
	public void enterSetTunnel64(SetTunnel64Context ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new SetTunnelAction(Decoder.decodeLong(ctx.NUMBER().getText())));
	}
	public void enterSetQueue(SetQueueContext ctx) {
		//TODO: No action

	}
	public void enterPopQueue(PopQueueContext ctx) {
		//TODO: No action

	} 
	public void enterDecTTL(DecTTLContext ctx) {
		//TODO: No action

	}
	public void enterDecTTLWithParams(DecTTLWithParamsContext ctx) {
		//TODO: No action

	}
	public void enterSetMplsTTL(SetMplsTTLContext ctx) {
		//TODO: No action
	}
	public void enterDecMplsTTL(DecMplsTTLContext ctx) {
		//TODO: No action
	}
	public void enterMove(MoveContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(MoveAction.fromString(ctx.getText()));
	}
	public void enterLoad(LoadContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(LoadAction.fromString(ctx.getText()));

	}
	
	public void enterPush(PushContext ctx) {
		//TODO: No action

	}
	public void enterPop(PopContext ctx) {
		//TODO: No action

	}
	public void enterSetField(SetFieldContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new SetFieldAction());
	}
	
	private Stack<FlowEntry> savedFlows = new Stack<FlowEntry>();
	
	public void enterApplyActions(ApplyActionsContext ctx) {
		//TODO: Code here
		savedFlows.push(this.currentEntry);
		this.currentEntry = new FlowEntry();
	}
	
	public void exitApplyActions(ApplyActionsContext ctx) {
		ApplyActionsAction action = new ApplyActionsAction();
		for (Action act : this.currentEntry.getActions())
		{
			action.addAction(act);
		}
		this.currentEntry = savedFlows.pop();
		this.currentEntry.getActions().add(action);
	}
	
	public void enterClearActions(ClearActionsContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new ClearActionsAction());
	}
	public void enterWriteMetadata(WriteMetadataContext ctx) {
		//TODO: No action
		
	}
	public void enterGoto(GotoContext ctx) {
		//TODO: No action

	}
	public void enterFinTimeout(FinTimeoutContext ctx) {
		//TODO: No action

	}
	public void enterSample(SampleContext ctx) {
		//TODO: No action

	}
	public void enterLearn(LearnContext ctx) {
		//TODO: Code here
		this.savedFlows.push(currentEntry);
		this.currentEntry = new FlowEntry();
	}
	
	public void exitLearn(LearnContext ctx)
	{
		LearnAction la = new LearnAction();
		for (Match m : this.currentEntry.getMatches())
		{
			la.getMatches().add(m);
		}
		for (Action a : this.currentEntry.getActions())
		{
			la.getActions().add(a);
		}

		this.currentEntry = savedFlows.pop();
		this.currentEntry.getActions().add(la);
	}
	
	
	public void enterExit(ExitContext ctx) {
		//TODO: Code here
		this.currentEntry.getActions().add(new ExitAction());
	}

	public void enterForwardToPortTarget(ForwardToPortTargetContext ctx) {
		//TODO: Code here
		OutputAction oa = new OutputAction();
		oa.setPort(Decoder.decodePort(ctx.port().getText()));
		this.currentEntry.getActions().add(oa);
	}
}
