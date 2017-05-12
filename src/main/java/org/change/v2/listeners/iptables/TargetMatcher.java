package org.change.v2.listeners.iptables;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.change.v2.model.iptables.AcceptTarget;
import org.change.v2.model.iptables.ChecksumTarget;
import org.change.v2.model.iptables.ConnmarkTarget;
import org.change.v2.model.iptables.DNATTarget;
import org.change.v2.model.iptables.DropTarget;
import org.change.v2.model.iptables.IPTablesTarget;
import org.change.v2.model.iptables.JumpyTarget;
import org.change.v2.model.iptables.MarkTarget;
import org.change.v2.model.iptables.NoTrackTarget;
import org.change.v2.model.iptables.RedirectTarget;
import org.change.v2.model.iptables.ReturnTarget;
import org.change.v2.model.iptables.SNATTarget;
import org.change.v2.model.openflow.Decoder;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesParser.AcceptTargetContext;
import generated.iptables_grammar.IptablesParser.ChecksumTargetContext;
import generated.iptables_grammar.IptablesParser.ConnmarkTargetContext;
import generated.iptables_grammar.IptablesParser.CtMaskContext;
import generated.iptables_grammar.IptablesParser.CtNotrackContext;
import generated.iptables_grammar.IptablesParser.CtTargetContext;
import generated.iptables_grammar.IptablesParser.CtTargetOptsContext;
import generated.iptables_grammar.IptablesParser.CtZoneContext;
import generated.iptables_grammar.IptablesParser.DnatTargetContext;
import generated.iptables_grammar.IptablesParser.DropTargetContext;
import generated.iptables_grammar.IptablesParser.JumpyTargetContext;
import generated.iptables_grammar.IptablesParser.MarkTargetContext;
import generated.iptables_grammar.IptablesParser.NfCtMaskContext;
import generated.iptables_grammar.IptablesParser.NfMaskContext;
import generated.iptables_grammar.IptablesParser.NotrackTargetContext;
import generated.iptables_grammar.IptablesParser.RedirectTargetContext;
import generated.iptables_grammar.IptablesParser.RejectTargetContext;
import generated.iptables_grammar.IptablesParser.RestoreCtMarkContext;
import generated.iptables_grammar.IptablesParser.ReturnTargetContext;
import generated.iptables_grammar.IptablesParser.SaveCtMarkContext;
import generated.iptables_grammar.IptablesParser.SetTargetContext;
import generated.iptables_grammar.IptablesParser.SnatTargetContext;

public class TargetMatcher extends IptablesBaseListener {

	private IPTablesTarget target = null;
	private ConnmarkTarget connmarkTarget = null;
    private Boolean nfMask = false;

	public IPTablesTarget getTarget() {
		return target;
	}
	
	public void enterJumpyTarget(JumpyTargetContext context) {
		this.target = new JumpyTarget(context.NAME().getText());
	}
	
	public void enterAcceptTarget(AcceptTargetContext context) { 
		this.target = new AcceptTarget();
	}
	public void enterDropTarget(DropTargetContext context) { 
		this.target = new DropTarget();
	}
	public void enterReturnTarget(ReturnTargetContext context) { 
		this.target = new ReturnTarget();
	}
	public void enterChecksumTarget(ChecksumTargetContext context) { 
		this.target = new ChecksumTarget();
	}
	public void enterConnmarkTarget(ConnmarkTargetContext context) { 
		connmarkTarget = new ConnmarkTarget(0xFFFFFFFF, 
		        0xFFFFFFFF, 
		        false);
	}
	
	public void exitConnmarkTarget(ConnmarkTargetContext context) {
		this.target = connmarkTarget;
		connmarkTarget = null;
	}
	public void enterCtTarget(CtTargetContext context) { 
		
	}
	
	public void enterCtMask(CtMaskContext context) {
		nfMask = false;
	}
	
	public void enterCtTargetOpts(CtTargetOptsContext context) {
		
	}
	
	public void enterNfCtMask(NfCtMaskContext context) {
		int inte = 0xFFFFFFFF;
		inte = Decoder.decodeLong(context.INT().getText()).intValue();
	    if (nfMask) connmarkTarget.setNfMask(inte);
	    else connmarkTarget.setCtMask(inte);
	}
	
	public void enterNfMask(NfMaskContext context) {
		nfMask = true;
	}
	
	public void enterRestoreCtMark(RestoreCtMarkContext context) {
	    connmarkTarget.setRestore( true);
	}
	
	public void enterSaveCtMask(SaveCtMarkContext context) {
	    connmarkTarget.setRestore(false);
	}
	
	public void enterCtNotrack(CtNotrackContext context) {
		this.target = new NoTrackTarget();
	}
	
	public void enterCtZone(CtZoneContext context) {
		this.target = new JumpyTarget("CTZONE"); 
	}
	
	
	public void enterSnatTarget(SnatTargetContext context) { 
		this.target = new SNATTarget(context.IP().getText());
	}
	public void enterMarkTarget(MarkTargetContext context) {
		MarkTarget theTarget = new MarkTarget(Decoder.decodeLong(context.INT(0).getText())); 
		this.target = theTarget;
		if (context.INT().size() > 1)
		{
			theTarget.setMask(Decoder.decodeLong(context.INT(1).getText()));
		}
	}
	public void enterNotrackTarget(NotrackTargetContext context) { 
		this.target = new NoTrackTarget();
	}
	public void enterRedirectTarget(RedirectTargetContext context) { 
		RedirectTarget target = new RedirectTarget(Integer.decode(context.INT(0).getText()));
		if (context.INT().size() > 1)
		{
			target.setToPortEnd(Integer.decode(context.INT(1).getText()));
		}
	}
	public void enterRejectTarget(RejectTargetContext context) { 
		this.target = new JumpyTarget("REJECT");
	}
	public void enterSetTarget(SetTargetContext context) { 
		// TODO: Maybe later.
	}
	public void enterDnatTarget(DnatTargetContext context) { 
		this.target = new DNATTarget(context.IP().getText());
	}
	


}
