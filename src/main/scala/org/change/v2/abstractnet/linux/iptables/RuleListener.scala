package org.change.v2.abstractnet.linux.iptables

import org.antlr.v4.runtime.tree.ParseTreeWalker

import generated.iptables_grammar.IptablesBaseListener
import generated.iptables_grammar.IptablesParser.MatchContext
import generated.iptables_grammar.IptablesParser.RleContext
import generated.iptables_grammar.IptablesParser.ProtocolContext
import org.change.v2.abstractnet.linux.iptables.MatchOption
import generated.iptables_grammar.IptablesParser.SourceaddrContext
import generated.iptables_grammar.IptablesParser.IpNormalContext
import org.change.v2.util.conversion.RepresentationConversion
import generated.iptables_grammar.IptablesParser.DstaddrContext
import generated.iptables_grammar.IptablesParser.IpMaskedContext
import generated.iptables_grammar.IptablesParser.InifaceContext
import generated.iptables_grammar.IptablesParser.OutifaceContext
import generated.iptables_grammar.IptablesParser.UdpoptsContext
import generated.iptables_grammar.IptablesParser.TcpoptsContext
import generated.iptables_grammar.IptablesParser.IcmpoptsContext
import generated.iptables_grammar.IptablesParser.BareTypeContext
import generated.iptables_grammar.IptablesParser.TypeCodeContext
import generated.iptables_grammar.IptablesParser.TypeCodeContext
import generated.iptables_grammar.IptablesParser.CodeNameContext

class RulesListener(chains : List[IPTablesChain]) extends IptablesBaseListener {
  private var rules : List[IPTablesRule] = List[IPTablesRule]();
  def getRules() : List[IPTablesRule] = {
    rules
  }
  
  override def enterRle(rle : RleContext) {
    val ruleListener = new RuleListener(chains)
    var treeWalker = new ParseTreeWalker()
    treeWalker.walk(ruleListener, rle)
    rules = rules.::(ruleListener.getRule())
  }
}

class RuleListener(chains : List[IPTablesChain]) extends IptablesBaseListener {
  private var rule : IPTablesRule = null
  def getRule() : IPTablesRule = {
    rule
  }
  
  override def enterRle(rle : RleContext) {
    var chain = chains.find(s => s.name == rle.chainname().NAME().getText).get
    rule = new IPTablesRule(Nil, null, chain)
  }
  
  override def enterMatch(m : MatchContext) {
    val matchListener = new MatchListener(chains)
    var treeWalker = new ParseTreeWalker()
    treeWalker.walk(matchListener, m)
    rule.theMatches = rule.theMatches.::(matchListener.getMatch())
  }
}

class MatchListener(chains : List[IPTablesChain]) extends IptablesBaseListener {
  private var matchOption : ListMatch = null;
  private var currentSourceOption : AddrOption = null
  
  def getMatch() : ListMatch = matchOption
  override def enterMatch(m : MatchContext)
  {
    matchOption = new ListMatch(List[MatchOption]())
  }
  
  /**
   * Start handling protocol
   */
  private def protoToInt(proto : String) : Int = 
  {
    if (proto == "tcp") 6
    else if (proto == "udp") 17
    else if (proto == "icmp") 1
    else 0
  }
  
  override def enterProtocol(proto : ProtocolContext) {
    val neg = proto.neg != null
    val intProto = if (proto.proto != null) protoToInt(proto.proto.getText)
    else Integer.parseInt(proto.INT().getText)
    val matchProto = new ProtocolOption(neg, intProto)
    matchOption.options = matchOption.options :+ matchProto
  }
  /**
   * End handling protocol
   */
  
  /**
   * Start handling source and destination addresses
   */
  override def enterSourceaddr(sourceAddress : SourceaddrContext) { 
    currentSourceOption = new AddrOption(sourceAddress.neg != null,
        "src"
        )
  }
  
  override def enterDstaddr(dstAddr : DstaddrContext) {
    currentSourceOption = new AddrOption(dstAddr.neg != null,
        "dst"
        )
  }
  
  override def exitDstaddr(dstAddr : DstaddrContext) {
    matchOption.options = matchOption.options :+ currentSourceOption
    currentSourceOption = null
  }
  
  override def exitSourceaddr(sourceAddress : SourceaddrContext) {
    matchOption.options = matchOption.options :+ currentSourceOption
    currentSourceOption = null
  }
  
  override def enterIpMasked(ip : IpMaskedContext) {
    val asInt = RepresentationConversion.ipAndMaskToInterval(ip.IP_MASK().getText)
    currentSourceOption.start = asInt._1
    currentSourceOption.end = asInt._2  }
  
  override def enterIpNormal(ip : IpNormalContext) {
    val asInt = RepresentationConversion.ipAndMaskToInterval(ip.IP().getText, 32)
    currentSourceOption.start = asInt._1
    currentSourceOption.end = asInt._2
  }
   /**
   * End handling source and destination addresses
   */
  
  /**
   * Start handling input and output interfaces
   */
  override def enterIniface(iface : InifaceContext) {
    enterIfaceGen(iface.neg != null, iface.NAME().getText, "in")
  }
 
  override def enterOutiface(iface : OutifaceContext) {
    enterIfaceGen(iface.neg != null, iface.NAME().getText, "out")
  }
  
  private def enterIfaceGen(neg : Boolean, ifaceName : String, io : String) {
    val iface = new IfaceOption(neg,
        io, ifaceName.hashCode)
    this.matchOption.options = this.matchOption.options :+ iface
  }
  /**
   * End handling input and output interfaces
   */
  
  /**
   * Start handling tcp/udp options
   */
  override def enterTcpopts(tcpOpts : TcpoptsContext) {
    val hasDport = tcpOpts.dport() != null && tcpOpts.dport().size() > 0
    val hasSport = tcpOpts.sport() != null && tcpOpts.sport().size() > 0
    if (hasSport)
    {
     val portOption = new PortOption(tcpOpts.sport(0).neg != null,
         "sport", 
         Integer.parseInt(tcpOpts.sport(0).portno().INT().getText))
     this.matchOption.options = this.matchOption.options :+ portOption
    }
    
    if (hasDport)
    {
      val portOption = new PortOption(tcpOpts.dport(0).neg != null,
         "dport", 
         Integer.parseInt(tcpOpts.dport(0).portno().INT().getText))
      this.matchOption.options = this.matchOption.options :+ portOption
    }
  }
  
  override def enterUdpopts(udpOpts : UdpoptsContext) {
    val hasDport = udpOpts.dport() != null && udpOpts.dport().size() > 0
    val hasSport = udpOpts.sport() != null && udpOpts.sport().size() > 0
    if (hasSport)
    {
     val portOption = new PortOption(udpOpts.sport(0).neg != null,
         "sport", 
         Integer.parseInt(udpOpts.sport(0).portno().INT().getText))
     this.matchOption.options = this.matchOption.options :+ portOption
    }
    
    if (hasDport)
    {
      val portOption = new PortOption(udpOpts.dport(0).neg != null,
         "dport", 
         Integer.parseInt(udpOpts.dport(0).portno().INT().getText))
      this.matchOption.options = this.matchOption.options :+ portOption
    }
  }  
  /**
   * End handling tcp/udp options
   */
  
  /**
   * Start handling icmp options
   */
  
  private var currentIcmpNeg : Boolean = false;
  
  override def enterIcmpopts(icmpOpts : IcmpoptsContext) {
     currentIcmpNeg = icmpOpts.neg != null
  }
  
  override def enterBareType(bareType : BareTypeContext) {
    val icmpTypeOption = new IcmpTypeOption(this.currentIcmpNeg, 
        Integer.parseInt(bareType.INT().getText))
    this.matchOption.options = this.matchOption.options :+ icmpTypeOption
  }
  
  override def enterTypeCode(typeCode : TypeCodeContext) {
    val icmpTypeOption = new IcmpTypeOption(this.currentIcmpNeg, 
        Integer.parseInt(typeCode.INT(0).getText))
    this.matchOption.options = this.matchOption.options :+ icmpTypeOption
    val icmpCodeOption = new IcmpCodeOption(this.currentIcmpNeg,
        Integer.parseInt(typeCode.INT(1).getText))
    this.matchOption.options = this.matchOption.options :+ icmpCodeOption
  }
  
  override def enterCodeName(codeName : CodeNameContext) {
    val theName = codeName.NAME().getText
    
  }
  
  /**
   * End handling icmp options
   */
  
  
  
}

