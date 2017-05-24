// Generated from /0/projects/internal/symnet-stuff/Symnetic/src/main/resources/openflow_grammar/OpenFlowGrammar.g4 by ANTLR 4.7
package generated.parse.openflow;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OpenFlowGrammarParser}.
 */
public interface OpenFlowGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#flows}.
	 * @param ctx the parse tree
	 */
	void enterFlows(OpenFlowGrammarParser.FlowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#flows}.
	 * @param ctx the parse tree
	 */
	void exitFlows(OpenFlowGrammarParser.FlowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#flow}.
	 * @param ctx the parse tree
	 */
	void enterFlow(OpenFlowGrammarParser.FlowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#flow}.
	 * @param ctx the parse tree
	 */
	void exitFlow(OpenFlowGrammarParser.FlowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#matches}.
	 * @param ctx the parse tree
	 */
	void enterMatches(OpenFlowGrammarParser.MatchesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#matches}.
	 * @param ctx the parse tree
	 */
	void exitMatches(OpenFlowGrammarParser.MatchesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#matchField}.
	 * @param ctx the parse tree
	 */
	void enterMatchField(OpenFlowGrammarParser.MatchFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#matchField}.
	 * @param ctx the parse tree
	 */
	void exitMatchField(OpenFlowGrammarParser.MatchFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#varName}.
	 * @param ctx the parse tree
	 */
	void enterVarName(OpenFlowGrammarParser.VarNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#varName}.
	 * @param ctx the parse tree
	 */
	void exitVarName(OpenFlowGrammarParser.VarNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void enterFieldName(OpenFlowGrammarParser.FieldNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void exitFieldName(OpenFlowGrammarParser.FieldNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(OpenFlowGrammarParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(OpenFlowGrammarParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code idle_timeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterIdle_timeout(OpenFlowGrammarParser.Idle_timeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idle_timeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitIdle_timeout(OpenFlowGrammarParser.Idle_timeoutContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hard_timeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterHard_timeout(OpenFlowGrammarParser.Hard_timeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hard_timeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitHard_timeout(OpenFlowGrammarParser.Hard_timeoutContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterTable(OpenFlowGrammarParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitTable(OpenFlowGrammarParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cookie}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterCookie(OpenFlowGrammarParser.CookieContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cookie}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitCookie(OpenFlowGrammarParser.CookieContext ctx);
	/**
	 * Enter a parse tree produced by the {@code priority}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterPriority(OpenFlowGrammarParser.PriorityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priority}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitPriority(OpenFlowGrammarParser.PriorityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code duration}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterDuration(OpenFlowGrammarParser.DurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code duration}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitDuration(OpenFlowGrammarParser.DurationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code n_packets}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterN_packets(OpenFlowGrammarParser.N_packetsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code n_packets}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitN_packets(OpenFlowGrammarParser.N_packetsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code n_bytes}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterN_bytes(OpenFlowGrammarParser.N_bytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code n_bytes}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitN_bytes(OpenFlowGrammarParser.N_bytesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hard_age}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterHard_age(OpenFlowGrammarParser.Hard_ageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hard_age}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitHard_age(OpenFlowGrammarParser.Hard_ageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code idle_age}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterIdle_age(OpenFlowGrammarParser.Idle_ageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idle_age}
	 * labeled alternative in {@link OpenFlowGrammarParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitIdle_age(OpenFlowGrammarParser.Idle_ageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#match}.
	 * @param ctx the parse tree
	 */
	void enterMatch(OpenFlowGrammarParser.MatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#match}.
	 * @param ctx the parse tree
	 */
	void exitMatch(OpenFlowGrammarParser.MatchContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#action}.
	 * @param ctx the parse tree
	 */
	void enterAction(OpenFlowGrammarParser.ActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#action}.
	 * @param ctx the parse tree
	 */
	void exitAction(OpenFlowGrammarParser.ActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#actionset}.
	 * @param ctx the parse tree
	 */
	void enterActionset(OpenFlowGrammarParser.ActionsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#actionset}.
	 * @param ctx the parse tree
	 */
	void exitActionset(OpenFlowGrammarParser.ActionsetContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#ipv6}.
	 * @param ctx the parse tree
	 */
	void enterIpv6(OpenFlowGrammarParser.Ipv6Context ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#ipv6}.
	 * @param ctx the parse tree
	 */
	void exitIpv6(OpenFlowGrammarParser.Ipv6Context ctx);
	/**
	 * Enter a parse tree produced by the {@code maxLenParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterMaxLenParam(OpenFlowGrammarParser.MaxLenParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code maxLenParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitMaxLenParam(OpenFlowGrammarParser.MaxLenParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code reasonParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterReasonParam(OpenFlowGrammarParser.ReasonParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code reasonParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitReasonParam(OpenFlowGrammarParser.ReasonParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code controllerIdParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterControllerIdParam(OpenFlowGrammarParser.ControllerIdParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerIdParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitControllerIdParam(OpenFlowGrammarParser.ControllerIdParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#reason}.
	 * @param ctx the parse tree
	 */
	void enterReason(OpenFlowGrammarParser.ReasonContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#reason}.
	 * @param ctx the parse tree
	 */
	void exitReason(OpenFlowGrammarParser.ReasonContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#port}.
	 * @param ctx the parse tree
	 */
	void enterPort(OpenFlowGrammarParser.PortContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#port}.
	 * @param ctx the parse tree
	 */
	void exitPort(OpenFlowGrammarParser.PortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nxInPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNxInPort(OpenFlowGrammarParser.NxInPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nxInPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNxInPort(OpenFlowGrammarParser.NxInPortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ethDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthDst(OpenFlowGrammarParser.EthDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthDst(OpenFlowGrammarParser.EthDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ethSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthSrc(OpenFlowGrammarParser.EthSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthSrc(OpenFlowGrammarParser.EthSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ethType}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthType(OpenFlowGrammarParser.EthTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethType}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthType(OpenFlowGrammarParser.EthTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code vlanTci}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterVlanTci(OpenFlowGrammarParser.VlanTciContext ctx);
	/**
	 * Exit a parse tree produced by the {@code vlanTci}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitVlanTci(OpenFlowGrammarParser.VlanTciContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ipTos}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpTos(OpenFlowGrammarParser.IpTosContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipTos}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpTos(OpenFlowGrammarParser.IpTosContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ipProto}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpProto(OpenFlowGrammarParser.IpProtoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipProto}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpProto(OpenFlowGrammarParser.IpProtoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ipSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpSrc(OpenFlowGrammarParser.IpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpSrc(OpenFlowGrammarParser.IpSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ipDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpDst(OpenFlowGrammarParser.IpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpDst(OpenFlowGrammarParser.IpDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tcpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTcpSrc(OpenFlowGrammarParser.TcpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tcpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTcpSrc(OpenFlowGrammarParser.TcpSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tcpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTcpDst(OpenFlowGrammarParser.TcpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tcpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTcpDst(OpenFlowGrammarParser.TcpDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code udpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterUdpSrc(OpenFlowGrammarParser.UdpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code udpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitUdpSrc(OpenFlowGrammarParser.UdpSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code udpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterUdpDst(OpenFlowGrammarParser.UdpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code udpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitUdpDst(OpenFlowGrammarParser.UdpDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code icmpType}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmpType(OpenFlowGrammarParser.IcmpTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmpType}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmpType(OpenFlowGrammarParser.IcmpTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code icmpCode}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmpCode(OpenFlowGrammarParser.IcmpCodeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmpCode}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmpCode(OpenFlowGrammarParser.IcmpCodeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arpOp}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpOp(OpenFlowGrammarParser.ArpOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpOp}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpOp(OpenFlowGrammarParser.ArpOpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arpSpa}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpSpa(OpenFlowGrammarParser.ArpSpaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpSpa}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpSpa(OpenFlowGrammarParser.ArpSpaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arpTpa}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpTpa(OpenFlowGrammarParser.ArpTpaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpTpa}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpTpa(OpenFlowGrammarParser.ArpTpaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tunId}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTunId(OpenFlowGrammarParser.TunIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tunId}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTunId(OpenFlowGrammarParser.TunIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arpSha}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpSha(OpenFlowGrammarParser.ArpShaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpSha}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpSha(OpenFlowGrammarParser.ArpShaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arpTha}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpTha(OpenFlowGrammarParser.ArpThaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpTha}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpTha(OpenFlowGrammarParser.ArpThaContext ctx);
	/**
	 * Enter a parse tree produced by the {@code icmp6Type}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmp6Type(OpenFlowGrammarParser.Icmp6TypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmp6Type}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmp6Type(OpenFlowGrammarParser.Icmp6TypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code icmp6Code}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmp6Code(OpenFlowGrammarParser.Icmp6CodeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmp6Code}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmp6Code(OpenFlowGrammarParser.Icmp6CodeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ndSll}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNdSll(OpenFlowGrammarParser.NdSllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ndSll}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNdSll(OpenFlowGrammarParser.NdSllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ndTll}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNdTll(OpenFlowGrammarParser.NdTllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ndTll}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNdTll(OpenFlowGrammarParser.NdTllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nxRegIdx}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNxRegIdx(OpenFlowGrammarParser.NxRegIdxContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nxRegIdx}
	 * labeled alternative in {@link OpenFlowGrammarParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNxRegIdx(OpenFlowGrammarParser.NxRegIdxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outputPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterOutputPort(OpenFlowGrammarParser.OutputPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitOutputPort(OpenFlowGrammarParser.OutputPortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outputReg}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterOutputReg(OpenFlowGrammarParser.OutputRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputReg}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitOutputReg(OpenFlowGrammarParser.OutputRegContext ctx);
	/**
	 * Enter a parse tree produced by the {@code enqueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterEnqueue(OpenFlowGrammarParser.EnqueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code enqueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitEnqueue(OpenFlowGrammarParser.EnqueueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code normal}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterNormal(OpenFlowGrammarParser.NormalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code normal}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitNormal(OpenFlowGrammarParser.NormalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code flood}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterFlood(OpenFlowGrammarParser.FloodContext ctx);
	/**
	 * Exit a parse tree produced by the {@code flood}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitFlood(OpenFlowGrammarParser.FloodContext ctx);
	/**
	 * Enter a parse tree produced by the {@code all}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterAll(OpenFlowGrammarParser.AllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code all}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitAll(OpenFlowGrammarParser.AllContext ctx);
	/**
	 * Enter a parse tree produced by the {@code controllerWithParams}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterControllerWithParams(OpenFlowGrammarParser.ControllerWithParamsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerWithParams}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitControllerWithParams(OpenFlowGrammarParser.ControllerWithParamsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code controller}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterController(OpenFlowGrammarParser.ControllerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controller}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitController(OpenFlowGrammarParser.ControllerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code controllerWithId}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterControllerWithId(OpenFlowGrammarParser.ControllerWithIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerWithId}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitControllerWithId(OpenFlowGrammarParser.ControllerWithIdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code local}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLocal(OpenFlowGrammarParser.LocalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code local}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLocal(OpenFlowGrammarParser.LocalContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterInPort(OpenFlowGrammarParser.InPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inPort}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitInPort(OpenFlowGrammarParser.InPortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code drop}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDrop(OpenFlowGrammarParser.DropContext ctx);
	/**
	 * Exit a parse tree produced by the {@code drop}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDrop(OpenFlowGrammarParser.DropContext ctx);
	/**
	 * Enter a parse tree produced by the {@code modVlanVid}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterModVlanVid(OpenFlowGrammarParser.ModVlanVidContext ctx);
	/**
	 * Exit a parse tree produced by the {@code modVlanVid}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitModVlanVid(OpenFlowGrammarParser.ModVlanVidContext ctx);
	/**
	 * Enter a parse tree produced by the {@code modVlanPcp}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterModVlanPcp(OpenFlowGrammarParser.ModVlanPcpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code modVlanPcp}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitModVlanPcp(OpenFlowGrammarParser.ModVlanPcpContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stripVlan}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterStripVlan(OpenFlowGrammarParser.StripVlanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stripVlan}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitStripVlan(OpenFlowGrammarParser.StripVlanContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pushVlan}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPushVlan(OpenFlowGrammarParser.PushVlanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pushVlan}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPushVlan(OpenFlowGrammarParser.PushVlanContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pushMpls}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPushMpls(OpenFlowGrammarParser.PushMplsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pushMpls}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPushMpls(OpenFlowGrammarParser.PushMplsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code popMpls}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPopMpls(OpenFlowGrammarParser.PopMplsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code popMpls}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPopMpls(OpenFlowGrammarParser.PopMplsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setDlSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetDlSrc(OpenFlowGrammarParser.SetDlSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setDlSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetDlSrc(OpenFlowGrammarParser.SetDlSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setDlDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetDlDst(OpenFlowGrammarParser.SetDlDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setDlDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetDlDst(OpenFlowGrammarParser.SetDlDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNwSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwSrc(OpenFlowGrammarParser.SetNwSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwSrc(OpenFlowGrammarParser.SetNwSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNwDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwDst(OpenFlowGrammarParser.SetNwDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwDst(OpenFlowGrammarParser.SetNwDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTpSrc(OpenFlowGrammarParser.SetTpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTpSrc}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTpSrc(OpenFlowGrammarParser.SetTpSrcContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTpDst(OpenFlowGrammarParser.SetTpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTpDst}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTpDst(OpenFlowGrammarParser.SetTpDstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNwTos}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwTos(OpenFlowGrammarParser.SetNwTosContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwTos}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwTos(OpenFlowGrammarParser.SetNwTosContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resubmit}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmit(OpenFlowGrammarParser.ResubmitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmit}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmit(OpenFlowGrammarParser.ResubmitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resubmitSecond}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmitSecond(OpenFlowGrammarParser.ResubmitSecondContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmitSecond}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmitSecond(OpenFlowGrammarParser.ResubmitSecondContext ctx);
	/**
	 * Enter a parse tree produced by the {@code resubmitTable}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmitTable(OpenFlowGrammarParser.ResubmitTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmitTable}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmitTable(OpenFlowGrammarParser.ResubmitTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTunnel}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTunnel(OpenFlowGrammarParser.SetTunnelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTunnel}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTunnel(OpenFlowGrammarParser.SetTunnelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTunnel64}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTunnel64(OpenFlowGrammarParser.SetTunnel64Context ctx);
	/**
	 * Exit a parse tree produced by the {@code setTunnel64}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTunnel64(OpenFlowGrammarParser.SetTunnel64Context ctx);
	/**
	 * Enter a parse tree produced by the {@code setQueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetQueue(OpenFlowGrammarParser.SetQueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setQueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetQueue(OpenFlowGrammarParser.SetQueueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code popQueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPopQueue(OpenFlowGrammarParser.PopQueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code popQueue}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPopQueue(OpenFlowGrammarParser.PopQueueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecTTL(OpenFlowGrammarParser.DecTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecTTL(OpenFlowGrammarParser.DecTTLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decTTLWithParams}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecTTLWithParams(OpenFlowGrammarParser.DecTTLWithParamsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decTTLWithParams}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecTTLWithParams(OpenFlowGrammarParser.DecTTLWithParamsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setMplsTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetMplsTTL(OpenFlowGrammarParser.SetMplsTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setMplsTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetMplsTTL(OpenFlowGrammarParser.SetMplsTTLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code decMplsTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecMplsTTL(OpenFlowGrammarParser.DecMplsTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decMplsTTL}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecMplsTTL(OpenFlowGrammarParser.DecMplsTTLContext ctx);
	/**
	 * Enter a parse tree produced by the {@code move}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterMove(OpenFlowGrammarParser.MoveContext ctx);
	/**
	 * Exit a parse tree produced by the {@code move}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitMove(OpenFlowGrammarParser.MoveContext ctx);
	/**
	 * Enter a parse tree produced by the {@code load}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLoad(OpenFlowGrammarParser.LoadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code load}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLoad(OpenFlowGrammarParser.LoadContext ctx);
	/**
	 * Enter a parse tree produced by the {@code push}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPush(OpenFlowGrammarParser.PushContext ctx);
	/**
	 * Exit a parse tree produced by the {@code push}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPush(OpenFlowGrammarParser.PushContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pop}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPop(OpenFlowGrammarParser.PopContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pop}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPop(OpenFlowGrammarParser.PopContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setField}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetField(OpenFlowGrammarParser.SetFieldContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setField}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetField(OpenFlowGrammarParser.SetFieldContext ctx);
	/**
	 * Enter a parse tree produced by the {@code applyActions}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterApplyActions(OpenFlowGrammarParser.ApplyActionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyActions}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitApplyActions(OpenFlowGrammarParser.ApplyActionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code clearActions}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterClearActions(OpenFlowGrammarParser.ClearActionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearActions}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitClearActions(OpenFlowGrammarParser.ClearActionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code writeMetadata}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterWriteMetadata(OpenFlowGrammarParser.WriteMetadataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code writeMetadata}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitWriteMetadata(OpenFlowGrammarParser.WriteMetadataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code goto}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterGoto(OpenFlowGrammarParser.GotoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code goto}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitGoto(OpenFlowGrammarParser.GotoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code finTimeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterFinTimeout(OpenFlowGrammarParser.FinTimeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code finTimeout}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitFinTimeout(OpenFlowGrammarParser.FinTimeoutContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sample}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSample(OpenFlowGrammarParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sample}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSample(OpenFlowGrammarParser.SampleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code learn}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLearn(OpenFlowGrammarParser.LearnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learn}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLearn(OpenFlowGrammarParser.LearnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code exit}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterExit(OpenFlowGrammarParser.ExitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exit}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitExit(OpenFlowGrammarParser.ExitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code forwardToPortTarget}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void enterForwardToPortTarget(OpenFlowGrammarParser.ForwardToPortTargetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code forwardToPortTarget}
	 * labeled alternative in {@link OpenFlowGrammarParser#target}.
	 * @param ctx the parse tree
	 */
	void exitForwardToPortTarget(OpenFlowGrammarParser.ForwardToPortTargetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code learnFinIdleTo}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnFinIdleTo(OpenFlowGrammarParser.LearnFinIdleToContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnFinIdleTo}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnFinIdleTo(OpenFlowGrammarParser.LearnFinIdleToContext ctx);
	/**
	 * Enter a parse tree produced by the {@code learnFinHardTo}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnFinHardTo(OpenFlowGrammarParser.LearnFinHardToContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnFinHardTo}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnFinHardTo(OpenFlowGrammarParser.LearnFinHardToContext ctx);
	/**
	 * Enter a parse tree produced by the {@code learnAssign}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnAssign(OpenFlowGrammarParser.LearnAssignContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnAssign}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnAssign(OpenFlowGrammarParser.LearnAssignContext ctx);
	/**
	 * Enter a parse tree produced by the {@code learnAssignSelf}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnAssignSelf(OpenFlowGrammarParser.LearnAssignSelfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnAssignSelf}
	 * labeled alternative in {@link OpenFlowGrammarParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnAssignSelf(OpenFlowGrammarParser.LearnAssignSelfContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(OpenFlowGrammarParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(OpenFlowGrammarParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by the {@code idleTimeoutParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void enterIdleTimeoutParam(OpenFlowGrammarParser.IdleTimeoutParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idleTimeoutParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void exitIdleTimeoutParam(OpenFlowGrammarParser.IdleTimeoutParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hardTimeoutParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void enterHardTimeoutParam(OpenFlowGrammarParser.HardTimeoutParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hardTimeoutParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void exitHardTimeoutParam(OpenFlowGrammarParser.HardTimeoutParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code probabilityParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterProbabilityParam(OpenFlowGrammarParser.ProbabilityParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code probabilityParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitProbabilityParam(OpenFlowGrammarParser.ProbabilityParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collectorSetParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterCollectorSetParam(OpenFlowGrammarParser.CollectorSetParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collectorSetParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitCollectorSetParam(OpenFlowGrammarParser.CollectorSetParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code obsDomainParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterObsDomainParam(OpenFlowGrammarParser.ObsDomainParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code obsDomainParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitObsDomainParam(OpenFlowGrammarParser.ObsDomainParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code obsPointParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterObsPointParam(OpenFlowGrammarParser.ObsPointParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code obsPointParam}
	 * labeled alternative in {@link OpenFlowGrammarParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitObsPointParam(OpenFlowGrammarParser.ObsPointParamContext ctx);
	/**
	 * Enter a parse tree produced by the {@code noFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterNoFrag(OpenFlowGrammarParser.NoFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code noFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitNoFrag(OpenFlowGrammarParser.NoFragContext ctx);
	/**
	 * Enter a parse tree produced by the {@code yesFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterYesFrag(OpenFlowGrammarParser.YesFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yesFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitYesFrag(OpenFlowGrammarParser.YesFragContext ctx);
	/**
	 * Enter a parse tree produced by the {@code firstFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterFirstFrag(OpenFlowGrammarParser.FirstFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code firstFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitFirstFrag(OpenFlowGrammarParser.FirstFragContext ctx);
	/**
	 * Enter a parse tree produced by the {@code laterFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterLaterFrag(OpenFlowGrammarParser.LaterFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code laterFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitLaterFrag(OpenFlowGrammarParser.LaterFragContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notLaterFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterNotLaterFrag(OpenFlowGrammarParser.NotLaterFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notLaterFrag}
	 * labeled alternative in {@link OpenFlowGrammarParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitNotLaterFrag(OpenFlowGrammarParser.NotLaterFragContext ctx);
	/**
	 * Enter a parse tree produced by {@link OpenFlowGrammarParser#mask}.
	 * @param ctx the parse tree
	 */
	void enterMask(OpenFlowGrammarParser.MaskContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenFlowGrammarParser#mask}.
	 * @param ctx the parse tree
	 */
	void exitMask(OpenFlowGrammarParser.MaskContext ctx);
}