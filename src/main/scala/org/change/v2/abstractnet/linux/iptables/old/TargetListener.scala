package org.change.v2.abstractnet.linux.iptables.old

import generated.iptables_grammar.IptablesBaseListener
import generated.iptables_grammar.IptablesParser.AcceptTargetContext
import generated.iptables_grammar.IptablesParser.DropTargetContext
import generated.iptables_grammar.IptablesParser.RejectTargetContext
import generated.iptables_grammar.IptablesParser.ReturnTargetContext
import generated.iptables_grammar.IptablesParser.ConnmarkTargetContext
import generated.iptables_grammar.IptablesParser.ChecksumTargetContext
import generated.iptables_grammar.IptablesParser.NotrackTargetContext
import generated.iptables_grammar.IptablesParser.SnatTargetContext
import generated.iptables_grammar.IptablesParser.MarkTargetContext
import generated.iptables_grammar.IptablesParser.CtTargetContext
import generated.iptables_grammar.IptablesParser.DnatTargetContext
import generated.iptables_grammar.IptablesParser.RedirectTargetContext
import generated.iptables_grammar.IptablesParser.SetTargetContext
import org.change.v2.analysis.processingmodels.instructions.NoOp
import generated.iptables_grammar.IptablesParser.RestoreCtMarkContext
import generated.iptables_grammar.IptablesParser.SaveCtMarkContext
import generated.iptables_grammar.IptablesParser.NfMaskContext
import generated.iptables_grammar.IptablesParser.CtMaskContext
import generated.iptables_grammar.IptablesParser.NfCtMaskContext
import scala.collection.JavaConversions._
import generated.iptables_grammar.IptablesParser.CtNotrackContext
import generated.iptables_grammar.IptablesParser.CtZoneContext
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.abstractnet.linux.iptables.old.NoTrackTarget
import org.change.v2.abstractnet.linux.iptables.old.MarkTarget
import org.change.v2.abstractnet.linux.iptables.old.JumpyTarget
import org.change.v2.abstractnet.linux.iptables.old.IpTablesTarget
import org.change.v2.abstractnet.linux.iptables.old.InstructionTarget
import org.change.v2.abstractnet.linux.iptables.old.DNATTarget
import org.change.v2.abstractnet.linux.iptables.old.ConnmarkTarget

class TargetListener  extends IptablesBaseListener {
  private var target : IpTablesTarget = null;
  
  def getTarget() : IpTablesTarget = {
    target
  }
  
  
  override def enterAcceptTarget(accept : AcceptTargetContext) {
    this.target = new JumpyTarget("ACCEPT")
  }
  
  override def enterDropTarget(drop : DropTargetContext) {
    this.target = new JumpyTarget("DROP")
  }
  
  override def enterRejectTarget(drop : RejectTargetContext) {
    this.target = new JumpyTarget("REJECT")    
  }
  
  override def enterReturnTarget(ret : ReturnTargetContext) {
    this.target = new JumpyTarget("RETURN")
  }
  
  override def enterChecksumTarget(checksum : ChecksumTargetContext) {
    this.target = new InstructionTarget("CHECKSUM", NoOp)
  }
  
  /**
   * Start handling that connmark thing
   */
  
  private var connmarkTarget : ConnmarkTarget = null;
  private var nfMask : Boolean = false;
  
  override def enterConnmarkTarget(connmark : ConnmarkTargetContext) {
    connmarkTarget = new ConnmarkTarget(0xFFFFFFFF, 
        0xFFFFFFFF, 
        false)
  }
  
  override def exitConnmarkTarget(connmark : ConnmarkTargetContext) {
    this.target = connmarkTarget
    connmarkTarget = null
  }
  
  override def enterRestoreCtMark(restoreCtMark : RestoreCtMarkContext) {
    connmarkTarget.restore = true
  }
  
  override def enterSaveCtMark(saveCtMark : SaveCtMarkContext) {
    connmarkTarget.restore = false
  }
  
  override def enterNfMask(maskingOption : NfMaskContext) {
    nfMask = true
  }
  override def enterCtMask(maskingOption : CtMaskContext) {
    nfMask = false
  }
  
  override def enterNfCtMask (theOption : NfCtMaskContext) {
    val inte = theOption.HEX_DIGIT().foldLeft(0)((acc, v) => {
      val txt = v.getText
      val chr = txt.toUpperCase()
      acc * 16 + Integer.parseInt(chr, 16)
    })
    if (nfMask) connmarkTarget.nfMask = inte
    else connmarkTarget.ctMask = inte
  }  
  
  /**
   * End handling that connmark thing
   */
  
  /**
   * Start handling that CT thing
   */
  
  override def enterCtTarget(ct : CtTargetContext) {
    
  }
  
  override def enterCtZone(ct : CtZoneContext) {
    // TODO : No idea what that is. Please defer
  }
  
  override def enterCtNotrack(ct : CtNotrackContext) {
    this.target = new NoTrackTarget()
  }
  
    
  /**
   * Enter handling that CT thing
   */
  
  /**
   * Start handling DNAT
   */
  override def enterDnatTarget(dnat : DnatTargetContext) {
    val theText = dnat.IP().getText
    this.target = new DNATTarget(RepresentationConversion.ipToNumber(theText))
  }
  
  /**
   * End handling DNAT
   */
  
  /**
   * Start handling MARK
   */
  
  override def enterMarkTarget(mark : MarkTargetContext) {
    val vv = Integer.decode(mark.INT(0).getText).asInstanceOf[Int]
    val mask = if (mark.INT().size() > 1)
      Integer.decode(mark.INT(1).getText).asInstanceOf[Int]
    else 0xFFFFFFFF
    this.target = new MarkTarget(vv, mask, "NFMark")
  }
  
  /**
   * End handling MARK
   */
  

  override def enterNotrackTarget(notrack : NotrackTargetContext) {
    this.target = NoTrackTarget()
  }
  
  override def enterRedirectTarget(redirect : RedirectTargetContext) {
    
  }
  
  override def enterSetTarget(set : SetTargetContext) {
    
  }
  
  /**
   * Start handling SNAT 
   */
  override def enterSnatTarget(snat : SnatTargetContext) {
    
  }
  
  /**
   * End handling SNAT 
   */
  
}

