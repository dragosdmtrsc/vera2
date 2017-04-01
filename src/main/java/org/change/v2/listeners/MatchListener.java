package org.change.v2.listeners;

import java.util.stream.Collectors;

import org.change.v2.iptables.ICMPDefinitions;
import org.change.v2.iptables.ICMPDefinitions.ICMPTypeCode;
import org.change.v2.iptables.StateDefinitions;
import org.change.v2.model.iptables.AddrOption;
import org.change.v2.model.iptables.CtStateOption;
import org.change.v2.model.iptables.IPTablesMatch;
import org.change.v2.model.iptables.IcmpCodeOption;
import org.change.v2.model.iptables.IcmpTypeOption;
import org.change.v2.model.iptables.IfaceOption;
import org.change.v2.model.iptables.MACOption;
import org.change.v2.model.iptables.MarkOption;
import org.change.v2.model.iptables.PhysdevInOption;
import org.change.v2.model.iptables.PhysdevIsBridgedOption;
import org.change.v2.model.iptables.PortOption;
import org.change.v2.model.iptables.ProtocolOption;
import org.change.v2.model.iptables.StateOption;
import org.change.v2.util.conversion.RepresentationConversion;

import generated.iptables_grammar.IptablesBaseListener;
import generated.iptables_grammar.IptablesParser.BareTypeContext;
import generated.iptables_grammar.IptablesParser.CodeNameContext;
import generated.iptables_grammar.IptablesParser.ConnmarkoptsContext;
import generated.iptables_grammar.IptablesParser.ConntrackvarsContext;
import generated.iptables_grammar.IptablesParser.DstaddrContext;
import generated.iptables_grammar.IptablesParser.IcmpoptsContext;
import generated.iptables_grammar.IptablesParser.InifaceContext;
import generated.iptables_grammar.IptablesParser.IpMaskedContext;
import generated.iptables_grammar.IptablesParser.IpNormalContext;
import generated.iptables_grammar.IptablesParser.MacoptsContext;
import generated.iptables_grammar.IptablesParser.MarkoptsContext;
import generated.iptables_grammar.IptablesParser.MatchContext;
import generated.iptables_grammar.IptablesParser.OutifaceContext;
import generated.iptables_grammar.IptablesParser.PhysdevInContext;
import generated.iptables_grammar.IptablesParser.PhysdevIsBridgedContext;
import generated.iptables_grammar.IptablesParser.PhysdevOutContext;
import generated.iptables_grammar.IptablesParser.PhysdevoptsContext;
import generated.iptables_grammar.IptablesParser.ProtocolContext;
import generated.iptables_grammar.IptablesParser.SourceaddrContext;
import generated.iptables_grammar.IptablesParser.StateoptsContext;
import generated.iptables_grammar.IptablesParser.TcpoptsContext;
import generated.iptables_grammar.IptablesParser.TypeCodeContext;
import generated.iptables_grammar.IptablesParser.UdpoptsContext;

public class MatchListener extends IptablesBaseListener {
	private IPTablesMatch match = new IPTablesMatch();
	private AddrOption currentSourceOption  = null;
    private Boolean currentIcmpNeg = false;
    

	public IPTablesMatch getMatch() {
		return match;
	}
	

	  @Override public void enterMatch(MatchContext m)
	  {
		  
	  }
	  
	  /**
	   * Start handling protocol
	   */
	  private Integer protoToInt(String proto)  
	  {
	    if (proto == "tcp") return 6;
	    else if (proto == "udp") return 17;
	    else if (proto == "icmp") return 1;
	    else return 0;
	  }
	  
	  @Override public void enterProtocol(ProtocolContext proto) {
	    boolean neg = proto.neg != null;
	    int intProto = (proto.proto != null) ? protoToInt(proto.proto.getText()) : Integer.decode(proto.INT().getText());
	    ProtocolOption matchProto = new ProtocolOption (neg, intProto);
	    match.getMatchOptions().add(matchProto);
	  }
	  /**
	   * End handling protocol
	   */
	  
	  /**
	   * Start handling source and destination addresses
	   */
	  @Override public void enterSourceaddr(SourceaddrContext sourceAddress) { 
	    currentSourceOption = new AddrOption(sourceAddress.neg != null,
	        "src"
	        );
	  }
	  
	  @Override public void enterDstaddr(DstaddrContext dstAddr) {
	    currentSourceOption = new AddrOption(dstAddr.neg != null,
	        "dst"
	        );
	  }
	  
	  
	  
	  
	  @Override public void exitDstaddr(DstaddrContext dstAddr) {
	    match.getMatchOptions().add(currentSourceOption);
	    currentSourceOption = null;
	  }
	  
	  @Override public void exitSourceaddr(SourceaddrContext sourceAddress) {
	    match.getMatchOptions().add(currentSourceOption);
	    currentSourceOption = null;
	  }
	  
	  @Override public void enterIpMasked(IpMaskedContext ip) {
	    long asInt = RepresentationConversion.ipToNumber(ip.IP_MASK().getText());
	    currentSourceOption.setStart(asInt);
	    currentSourceOption.setEnd(asInt);  
	  }
	  
	  @Override public void enterIpNormal(IpNormalContext ip) {
	    long asInt = RepresentationConversion.ipToNumber(ip.IP().getText());
	    currentSourceOption.setStart(asInt);
	    currentSourceOption.setEnd(asInt);
	  }
	   /**
	   * End handling source and destination addresses
	   */
	  
	  /**
	   * Start handling input and output interfaces
	   */
	  @Override public void enterIniface(InifaceContext iface) {
	    enterIfaceGen(iface.neg != null, iface.NAME().getText(), "in");
	  }
	 
	  @Override public void enterOutiface(OutifaceContext iface) {
	    enterIfaceGen(iface.neg != null, iface.NAME().getText(), "out");
	  }
	  
	  private void enterIfaceGen(Boolean neg, String ifaceName, String io) {
	    IfaceOption iface = new IfaceOption (neg,
	        io, ifaceName.hashCode());
	    match.getMatchOptions().add(iface);
	  }
	  /**
	   * End handling input and output interfaces
	   */
	  
	  /**
	   * Start handling tcp/udp options
	   */
	  @Override public void enterTcpopts(TcpoptsContext tcpOpts) {
	    boolean hasDport = tcpOpts.dport() != null && tcpOpts.dport().size() > 0;
		boolean hasSport = tcpOpts.sport() != null && tcpOpts.sport().size() > 0;
	    if (hasSport)
	    {
	     PortOption portOption = new PortOption (tcpOpts.sport(0).neg != null,
	         "sport", 
	         Integer.parseInt(tcpOpts.sport(0).portno().INT().getText()));
	     match.getMatchOptions().add(portOption);
	    }
	    
	    if (hasDport)
	    {
	      PortOption portOption = new PortOption (tcpOpts.dport(0).neg != null,
	         "dport", 
	         Integer.parseInt(tcpOpts.dport(0).portno().INT().getText()));
	      match.getMatchOptions().add(portOption);
	    }
	  }
	  
	  @Override public void enterUdpopts(UdpoptsContext udpOpts) {
	    boolean hasDport = udpOpts.dport() != null && udpOpts.dport().size() > 0;
		boolean hasSport = udpOpts.sport() != null && udpOpts.sport().size() > 0;
	    if (hasSport)
	    {
	     PortOption portOption = new PortOption (udpOpts.sport(0).neg != null,
	         "sport", 
	         Integer.parseInt(udpOpts.sport(0).portno().INT().getText()));
	     match.getMatchOptions().add(portOption);
	    }
	    
	    if (hasDport)
	    {
	      PortOption portOption = new PortOption (udpOpts.dport(0).neg != null,
	         "dport", 
	         Integer.parseInt(udpOpts.dport(0).portno().INT().getText()));
	      match.getMatchOptions().add(portOption);
	    }
	  }  
	  /**
	   * End handling tcp/udp options
	   */
	  
	  /**
	   * Start handling icmp options
	   */
	  
	  
	  @Override public void enterIcmpopts(IcmpoptsContext icmpOpts) {
	     currentIcmpNeg = icmpOpts.neg != null;
	  }
	  
	  @Override public void enterBareType(BareTypeContext bareType) {
	    this.inputType(Integer.parseInt(bareType.INT().getText()));
	  }
	  
	  @Override public void enterTypeCode(TypeCodeContext typeCode) {
	    this.inputType(Integer.parseInt(typeCode.INT(0).getText()));
	    this.inputCode(Integer.parseInt(typeCode.INT(1).getText()));
	  }
	  
	  private void inputType(Integer type) {
	    IcmpTypeOption icmpTypeOption = new IcmpTypeOption (this.currentIcmpNeg, type);
	    match.getMatchOptions().add(icmpTypeOption);
	  }
	  
	  private void inputCode(Integer code) {
	    IcmpCodeOption icmpCodeOption = new IcmpCodeOption (this.currentIcmpNeg, code);
	    match.getMatchOptions().add(icmpCodeOption);
	  }
	  
	  @Override public void enterCodeName(CodeNameContext codeName) {
	    String theName = codeName.NAME().getText();
	    if (ICMPDefinitions.NAMES.containsKey(theName))
	    {
	      ICMPTypeCode icmpDef = ICMPDefinitions.NAMES.get(theName);
	      this.inputType(icmpDef.getType());
	      if (icmpDef.getCode() != -1)
	      {
	        this.inputCode(icmpDef.getCode());
	      }
	    }
	  }
	  
	  /**
	   * End handling icmp options
	   */
	  
	  /**
	   * Start handling mac options
	   */
	  
	  @Override public void enterMacopts(MacoptsContext macOpts) {
	    boolean neg = macOpts.neg != null;
	    String source = macOpts.macaddress().getText();
	    MACOption opt = new MACOption (neg, "mac-source", source);
	    match.getMatchOptions().add(opt);

	  }
	  
	  /**
	   * End handling mac options
	   */
	  
	  /**
	   * Start handling physdev options
	   */
	  
	  
	  @Override public void enterPhysdevopts(PhysdevoptsContext physdev) {
	  }
	  
	  @Override public void enterPhysdevIn(PhysdevInContext physdevIn) {
	    boolean neg = physdevIn.neg != null;
	    PhysdevInOption opt = new PhysdevInOption (neg, physdevIn.NAME().getText().hashCode());
	    match.getMatchOptions().add(opt);

	  }
	  
	  @Override public void enterPhysdevOut(PhysdevOutContext physdevOut) {
	    boolean neg = physdevOut.neg != null;
	    PhysdevInOption opt = new PhysdevInOption (neg, physdevOut.NAME().getText().hashCode(), "PhysdevOut");
	    match.getMatchOptions().add(opt);
	  }
	  
	  @Override public void enterPhysdevIsBridged(PhysdevIsBridgedContext physdevIsBridged) {
	    boolean neg = physdevIsBridged.neg != null;
	    PhysdevIsBridgedOption opt = new PhysdevIsBridgedOption (neg);
	    match.getMatchOptions().add(opt);
	  }
	  
	    /**
	   * End handling physdev options
	   */
	  
	  /**
	   * Start handling the state options
	   */
	  
	  
	  @Override public void enterStateopts(StateoptsContext state) {
	    boolean neg = state.neg != null;
	    StateOption opt = new StateOption (neg, StateDefinitions.NAMES.get(state.state().getText()));
	    match.getMatchOptions().add(opt);
	  }
	  
	  @Override public void enterConntrackvars(ConntrackvarsContext stateVars) {
	    boolean neg = stateVars.neg != null;
	    CtStateOption opt = new CtStateOption (neg, 
	        "State", 
	        stateVars.statelist().state().stream().map(s -> StateDefinitions.NAMES.get(s.getText())).collect(Collectors.toList()),
	        false);
	    match.getMatchOptions().add(opt);
	  }
	  
	  
	  /**
	   * End handling the state options
	   */
	  
	  
	  /**
	   * Start handling mark options
	   */
	  
	  @Override public void enterMarkopts(MarkoptsContext mark) {
	    boolean neg = mark.neg != null;
	    int ip = Integer.decode(mark.INT(0).getText());
	    int mask = (mark.INT().size() > 1) ? Integer.decode(mark.INT(1).getText()) : 0xFFFFFFFF;
	    //TODO : Fix this crap
	    match.getMatchOptions().add(new MarkOption(neg, ip, mask, "nfmark"));
	  }
	  
	  @Override public void enterConnmarkopts(ConnmarkoptsContext mark) {
	    boolean neg = mark.neg != null;
	    int ip = Integer.decode(mark.INT(0).getText());
	    int mask = (mark.INT().size() > 1) ? Integer.decode(mark.INT(1).getText()) : 0xFFFFFFFF;
	    //TODO : Fix this crap	    
	    match.getMatchOptions().add(new MarkOption(neg, ip, mask, "ctmark"));
	  }
	  
	  /**
	   * End handling mark options
	   */
	
	
}
