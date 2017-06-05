// Generated from Openflow.g4 by ANTLR 4.3

package generated.openflow_grammar;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OpenflowParser}.
 */
public interface OpenflowListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by the {@code obsPointParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterObsPointParam(@NotNull OpenflowParser.ObsPointParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code obsPointParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitObsPointParam(@NotNull OpenflowParser.ObsPointParamContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#reason}.
	 * @param ctx the parse tree
	 */
	void enterReason(@NotNull OpenflowParser.ReasonContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#reason}.
	 * @param ctx the parse tree
	 */
	void exitReason(@NotNull OpenflowParser.ReasonContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setDlDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetDlDst(@NotNull OpenflowParser.SetDlDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setDlDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetDlDst(@NotNull OpenflowParser.SetDlDstContext ctx);

	/**
	 * Enter a parse tree produced by the {@code controllerWithParams}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterControllerWithParams(@NotNull OpenflowParser.ControllerWithParamsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerWithParams}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitControllerWithParams(@NotNull OpenflowParser.ControllerWithParamsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code modVlanPcp}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterModVlanPcp(@NotNull OpenflowParser.ModVlanPcpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code modVlanPcp}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitModVlanPcp(@NotNull OpenflowParser.ModVlanPcpContext ctx);

	/**
	 * Enter a parse tree produced by the {@code udpDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterUdpDst(@NotNull OpenflowParser.UdpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code udpDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitUdpDst(@NotNull OpenflowParser.UdpDstContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#action}.
	 * @param ctx the parse tree
	 */
	void enterAction(@NotNull OpenflowParser.ActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#action}.
	 * @param ctx the parse tree
	 */
	void exitAction(@NotNull OpenflowParser.ActionContext ctx);

	/**
	 * Enter a parse tree produced by the {@code controllerWithId}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterControllerWithId(@NotNull OpenflowParser.ControllerWithIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerWithId}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitControllerWithId(@NotNull OpenflowParser.ControllerWithIdContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#flow}.
	 * @param ctx the parse tree
	 */
	void enterFlow(@NotNull OpenflowParser.FlowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#flow}.
	 * @param ctx the parse tree
	 */
	void exitFlow(@NotNull OpenflowParser.FlowContext ctx);

	/**
	 * Enter a parse tree produced by the {@code icmp6Type}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmp6Type(@NotNull OpenflowParser.Icmp6TypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmp6Type}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmp6Type(@NotNull OpenflowParser.Icmp6TypeContext ctx);

	/**
	 * Enter a parse tree produced by the {@code hardTimeoutParam}
	 * labeled alternative in {@link OpenflowParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void enterHardTimeoutParam(@NotNull OpenflowParser.HardTimeoutParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hardTimeoutParam}
	 * labeled alternative in {@link OpenflowParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void exitHardTimeoutParam(@NotNull OpenflowParser.HardTimeoutParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code cookie}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterCookie(@NotNull OpenflowParser.CookieContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cookie}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitCookie(@NotNull OpenflowParser.CookieContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setDlSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetDlSrc(@NotNull OpenflowParser.SetDlSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setDlSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetDlSrc(@NotNull OpenflowParser.SetDlSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code firstFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterFirstFrag(@NotNull OpenflowParser.FirstFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code firstFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitFirstFrag(@NotNull OpenflowParser.FirstFragContext ctx);

	/**
	 * Enter a parse tree produced by the {@code inPort}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterInPort(@NotNull OpenflowParser.InPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inPort}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitInPort(@NotNull OpenflowParser.InPortContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ipProto}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpProto(@NotNull OpenflowParser.IpProtoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipProto}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpProto(@NotNull OpenflowParser.IpProtoContext ctx);

	/**
	 * Enter a parse tree produced by the {@code popMpls}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPopMpls(@NotNull OpenflowParser.PopMplsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code popMpls}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPopMpls(@NotNull OpenflowParser.PopMplsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code probabilityParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterProbabilityParam(@NotNull OpenflowParser.ProbabilityParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code probabilityParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitProbabilityParam(@NotNull OpenflowParser.ProbabilityParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code finTimeout}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterFinTimeout(@NotNull OpenflowParser.FinTimeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code finTimeout}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitFinTimeout(@NotNull OpenflowParser.FinTimeoutContext ctx);

	/**
	 * Enter a parse tree produced by the {@code idleTimeoutParam}
	 * labeled alternative in {@link OpenflowParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void enterIdleTimeoutParam(@NotNull OpenflowParser.IdleTimeoutParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idleTimeoutParam}
	 * labeled alternative in {@link OpenflowParser#timeoutParam}.
	 * @param ctx the parse tree
	 */
	void exitIdleTimeoutParam(@NotNull OpenflowParser.IdleTimeoutParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code priority}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterPriority(@NotNull OpenflowParser.PriorityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code priority}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitPriority(@NotNull OpenflowParser.PriorityContext ctx);

	/**
	 * Enter a parse tree produced by the {@code arpOp}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpOp(@NotNull OpenflowParser.ArpOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpOp}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpOp(@NotNull OpenflowParser.ArpOpContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#matches}.
	 * @param ctx the parse tree
	 */
	void enterMatches(@NotNull OpenflowParser.MatchesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#matches}.
	 * @param ctx the parse tree
	 */
	void exitMatches(@NotNull OpenflowParser.MatchesContext ctx);

	/**
	 * Enter a parse tree produced by the {@code flood}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterFlood(@NotNull OpenflowParser.FloodContext ctx);
	/**
	 * Exit a parse tree produced by the {@code flood}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitFlood(@NotNull OpenflowParser.FloodContext ctx);

	/**
	 * Enter a parse tree produced by the {@code push}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPush(@NotNull OpenflowParser.PushContext ctx);
	/**
	 * Exit a parse tree produced by the {@code push}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPush(@NotNull OpenflowParser.PushContext ctx);

	/**
	 * Enter a parse tree produced by the {@code writeMetadata}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterWriteMetadata(@NotNull OpenflowParser.WriteMetadataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code writeMetadata}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitWriteMetadata(@NotNull OpenflowParser.WriteMetadataContext ctx);

	/**
	 * Enter a parse tree produced by the {@code exit}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterExit(@NotNull OpenflowParser.ExitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code exit}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitExit(@NotNull OpenflowParser.ExitContext ctx);

	/**
	 * Enter a parse tree produced by the {@code outputPort}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterOutputPort(@NotNull OpenflowParser.OutputPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputPort}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitOutputPort(@NotNull OpenflowParser.OutputPortContext ctx);

	/**
	 * Enter a parse tree produced by the {@code decTTLWithParams}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecTTLWithParams(@NotNull OpenflowParser.DecTTLWithParamsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decTTLWithParams}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecTTLWithParams(@NotNull OpenflowParser.DecTTLWithParamsContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(@NotNull OpenflowParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(@NotNull OpenflowParser.FieldContext ctx);

	/**
	 * Enter a parse tree produced by the {@code learnAssignSelf}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnAssignSelf(@NotNull OpenflowParser.LearnAssignSelfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnAssignSelf}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnAssignSelf(@NotNull OpenflowParser.LearnAssignSelfContext ctx);

	/**
	 * Enter a parse tree produced by the {@code udpSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterUdpSrc(@NotNull OpenflowParser.UdpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code udpSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitUdpSrc(@NotNull OpenflowParser.UdpSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code collectorSetParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterCollectorSetParam(@NotNull OpenflowParser.CollectorSetParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collectorSetParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitCollectorSetParam(@NotNull OpenflowParser.CollectorSetParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code pushVlan}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPushVlan(@NotNull OpenflowParser.PushVlanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pushVlan}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPushVlan(@NotNull OpenflowParser.PushVlanContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#varName}.
	 * @param ctx the parse tree
	 */
	void enterVarName(@NotNull OpenflowParser.VarNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#varName}.
	 * @param ctx the parse tree
	 */
	void exitVarName(@NotNull OpenflowParser.VarNameContext ctx);

	/**
	 * Enter a parse tree produced by the {@code controllerIdParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterControllerIdParam(@NotNull OpenflowParser.ControllerIdParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controllerIdParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitControllerIdParam(@NotNull OpenflowParser.ControllerIdParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code icmp6Code}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmp6Code(@NotNull OpenflowParser.Icmp6CodeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmp6Code}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmp6Code(@NotNull OpenflowParser.Icmp6CodeContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setField}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetField(@NotNull OpenflowParser.SetFieldContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setField}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetField(@NotNull OpenflowParser.SetFieldContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setTpSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTpSrc(@NotNull OpenflowParser.SetTpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTpSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTpSrc(@NotNull OpenflowParser.SetTpSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code idle_age}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterIdle_age(@NotNull OpenflowParser.Idle_ageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idle_age}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitIdle_age(@NotNull OpenflowParser.Idle_ageContext ctx);

	/**
	 * Enter a parse tree produced by the {@code hard_age}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterHard_age(@NotNull OpenflowParser.Hard_ageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hard_age}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitHard_age(@NotNull OpenflowParser.Hard_ageContext ctx);

	/**
	 * Enter a parse tree produced by the {@code resubmitSecond}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmitSecond(@NotNull OpenflowParser.ResubmitSecondContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmitSecond}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmitSecond(@NotNull OpenflowParser.ResubmitSecondContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ipTos}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpTos(@NotNull OpenflowParser.IpTosContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipTos}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpTos(@NotNull OpenflowParser.IpTosContext ctx);

	/**
	 * Enter a parse tree produced by the {@code learnFinIdleTo}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnFinIdleTo(@NotNull OpenflowParser.LearnFinIdleToContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnFinIdleTo}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnFinIdleTo(@NotNull OpenflowParser.LearnFinIdleToContext ctx);

	/**
	 * Enter a parse tree produced by the {@code local}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLocal(@NotNull OpenflowParser.LocalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code local}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLocal(@NotNull OpenflowParser.LocalContext ctx);

	/**
	 * Enter a parse tree produced by the {@code pop}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPop(@NotNull OpenflowParser.PopContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pop}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPop(@NotNull OpenflowParser.PopContext ctx);

	/**
	 * Enter a parse tree produced by the {@code goto}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterGoto(@NotNull OpenflowParser.GotoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code goto}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitGoto(@NotNull OpenflowParser.GotoContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setQueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetQueue(@NotNull OpenflowParser.SetQueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setQueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetQueue(@NotNull OpenflowParser.SetQueueContext ctx);

	/**
	 * Enter a parse tree produced by the {@code stripVlan}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterStripVlan(@NotNull OpenflowParser.StripVlanContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stripVlan}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitStripVlan(@NotNull OpenflowParser.StripVlanContext ctx);

	/**
	 * Enter a parse tree produced by the {@code modVlanVid}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterModVlanVid(@NotNull OpenflowParser.ModVlanVidContext ctx);
	/**
	 * Exit a parse tree produced by the {@code modVlanVid}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitModVlanVid(@NotNull OpenflowParser.ModVlanVidContext ctx);

	/**
	 * Enter a parse tree produced by the {@code learnAssign}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnAssign(@NotNull OpenflowParser.LearnAssignContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnAssign}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnAssign(@NotNull OpenflowParser.LearnAssignContext ctx);

	/**
	 * Enter a parse tree produced by the {@code table}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterTable(@NotNull OpenflowParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitTable(@NotNull OpenflowParser.TableContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ndTll}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNdTll(@NotNull OpenflowParser.NdTllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ndTll}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNdTll(@NotNull OpenflowParser.NdTllContext ctx);

	/**
	 * Enter a parse tree produced by the {@code controller}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterController(@NotNull OpenflowParser.ControllerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code controller}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitController(@NotNull OpenflowParser.ControllerContext ctx);

	/**
	 * Enter a parse tree produced by the {@code popQueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPopQueue(@NotNull OpenflowParser.PopQueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code popQueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPopQueue(@NotNull OpenflowParser.PopQueueContext ctx);

	/**
	 * Enter a parse tree produced by the {@code icmpCode}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmpCode(@NotNull OpenflowParser.IcmpCodeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmpCode}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmpCode(@NotNull OpenflowParser.IcmpCodeContext ctx);

	/**
	 * Enter a parse tree produced by the {@code learn}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLearn(@NotNull OpenflowParser.LearnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learn}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLearn(@NotNull OpenflowParser.LearnContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ethSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthSrc(@NotNull OpenflowParser.EthSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthSrc(@NotNull OpenflowParser.EthSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setNwSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwSrc(@NotNull OpenflowParser.SetNwSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwSrc}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwSrc(@NotNull OpenflowParser.SetNwSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code applyActions}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterApplyActions(@NotNull OpenflowParser.ApplyActionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code applyActions}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitApplyActions(@NotNull OpenflowParser.ApplyActionsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code sample}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSample(@NotNull OpenflowParser.SampleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sample}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSample(@NotNull OpenflowParser.SampleContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setTunnel64}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTunnel64(@NotNull OpenflowParser.SetTunnel64Context ctx);
	/**
	 * Exit a parse tree produced by the {@code setTunnel64}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTunnel64(@NotNull OpenflowParser.SetTunnel64Context ctx);

	/**
	 * Enter a parse tree produced by the {@code notLaterFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterNotLaterFrag(@NotNull OpenflowParser.NotLaterFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notLaterFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitNotLaterFrag(@NotNull OpenflowParser.NotLaterFragContext ctx);

	/**
	 * Enter a parse tree produced by the {@code pushMpls}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterPushMpls(@NotNull OpenflowParser.PushMplsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pushMpls}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitPushMpls(@NotNull OpenflowParser.PushMplsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code outputReg}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterOutputReg(@NotNull OpenflowParser.OutputRegContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outputReg}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitOutputReg(@NotNull OpenflowParser.OutputRegContext ctx);

	/**
	 * Enter a parse tree produced by the {@code resubmitTable}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmitTable(@NotNull OpenflowParser.ResubmitTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmitTable}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmitTable(@NotNull OpenflowParser.ResubmitTableContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ethDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthDst(@NotNull OpenflowParser.EthDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthDst(@NotNull OpenflowParser.EthDstContext ctx);

	/**
	 * Enter a parse tree produced by the {@code vlanTci}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterVlanTci(@NotNull OpenflowParser.VlanTciContext ctx);
	/**
	 * Exit a parse tree produced by the {@code vlanTci}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitVlanTci(@NotNull OpenflowParser.VlanTciContext ctx);

	/**
	 * Enter a parse tree produced by the {@code arpTpa}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpTpa(@NotNull OpenflowParser.ArpTpaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpTpa}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpTpa(@NotNull OpenflowParser.ArpTpaContext ctx);

	/**
	 * Enter a parse tree produced by the {@code nxCtState}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNxCtState(@NotNull OpenflowParser.NxCtStateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nxCtState}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNxCtState(@NotNull OpenflowParser.NxCtStateContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setTunnel}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTunnel(@NotNull OpenflowParser.SetTunnelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTunnel}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTunnel(@NotNull OpenflowParser.SetTunnelContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setNwDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwDst(@NotNull OpenflowParser.SetNwDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwDst(@NotNull OpenflowParser.SetNwDstContext ctx);

	/**
	 * Enter a parse tree produced by the {@code decMplsTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecMplsTTL(@NotNull OpenflowParser.DecMplsTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decMplsTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecMplsTTL(@NotNull OpenflowParser.DecMplsTTLContext ctx);

	/**
	 * Enter a parse tree produced by the {@code hard_timeout}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterHard_timeout(@NotNull OpenflowParser.Hard_timeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hard_timeout}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitHard_timeout(@NotNull OpenflowParser.Hard_timeoutContext ctx);

	/**
	 * Enter a parse tree produced by the {@code arpTha}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpTha(@NotNull OpenflowParser.ArpThaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpTha}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpTha(@NotNull OpenflowParser.ArpThaContext ctx);

	/**
	 * Enter a parse tree produced by the {@code forwardToPortTarget}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterForwardToPortTarget(@NotNull OpenflowParser.ForwardToPortTargetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code forwardToPortTarget}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitForwardToPortTarget(@NotNull OpenflowParser.ForwardToPortTargetContext ctx);

	/**
	 * Enter a parse tree produced by the {@code tunId}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTunId(@NotNull OpenflowParser.TunIdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tunId}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTunId(@NotNull OpenflowParser.TunIdContext ctx);

	/**
	 * Enter a parse tree produced by the {@code idle_timeout}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterIdle_timeout(@NotNull OpenflowParser.Idle_timeoutContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idle_timeout}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitIdle_timeout(@NotNull OpenflowParser.Idle_timeoutContext ctx);

	/**
	 * Enter a parse tree produced by the {@code all}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterAll(@NotNull OpenflowParser.AllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code all}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitAll(@NotNull OpenflowParser.AllContext ctx);

	/**
	 * Enter a parse tree produced by the {@code icmpType}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIcmpType(@NotNull OpenflowParser.IcmpTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code icmpType}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIcmpType(@NotNull OpenflowParser.IcmpTypeContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ndSll}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNdSll(@NotNull OpenflowParser.NdSllContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ndSll}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNdSll(@NotNull OpenflowParser.NdSllContext ctx);

	/**
	 * Enter a parse tree produced by the {@code noFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterNoFrag(@NotNull OpenflowParser.NoFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code noFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitNoFrag(@NotNull OpenflowParser.NoFragContext ctx);

	/**
	 * Enter a parse tree produced by the {@code clearActions}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterClearActions(@NotNull OpenflowParser.ClearActionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code clearActions}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitClearActions(@NotNull OpenflowParser.ClearActionsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setTpDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetTpDst(@NotNull OpenflowParser.SetTpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTpDst}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetTpDst(@NotNull OpenflowParser.SetTpDstContext ctx);

	/**
	 * Enter a parse tree produced by the {@code nxRegIdx}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNxRegIdx(@NotNull OpenflowParser.NxRegIdxContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nxRegIdx}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNxRegIdx(@NotNull OpenflowParser.NxRegIdxContext ctx);

	/**
	 * Enter a parse tree produced by the {@code reasonParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterReasonParam(@NotNull OpenflowParser.ReasonParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code reasonParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitReasonParam(@NotNull OpenflowParser.ReasonParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code learnFinHardTo}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterLearnFinHardTo(@NotNull OpenflowParser.LearnFinHardToContext ctx);
	/**
	 * Exit a parse tree produced by the {@code learnFinHardTo}
	 * labeled alternative in {@link OpenflowParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitLearnFinHardTo(@NotNull OpenflowParser.LearnFinHardToContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#port}.
	 * @param ctx the parse tree
	 */
	void enterPort(@NotNull OpenflowParser.PortContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#port}.
	 * @param ctx the parse tree
	 */
	void exitPort(@NotNull OpenflowParser.PortContext ctx);

	/**
	 * Enter a parse tree produced by the {@code maxLenParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void enterMaxLenParam(@NotNull OpenflowParser.MaxLenParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code maxLenParam}
	 * labeled alternative in {@link OpenflowParser#ctrlParam}.
	 * @param ctx the parse tree
	 */
	void exitMaxLenParam(@NotNull OpenflowParser.MaxLenParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code tcpDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTcpDst(@NotNull OpenflowParser.TcpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tcpDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTcpDst(@NotNull OpenflowParser.TcpDstContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#actionset}.
	 * @param ctx the parse tree
	 */
	void enterActionset(@NotNull OpenflowParser.ActionsetContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#actionset}.
	 * @param ctx the parse tree
	 */
	void exitActionset(@NotNull OpenflowParser.ActionsetContext ctx);

	/**
	 * Enter a parse tree produced by the {@code drop}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDrop(@NotNull OpenflowParser.DropContext ctx);
	/**
	 * Exit a parse tree produced by the {@code drop}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDrop(@NotNull OpenflowParser.DropContext ctx);

	/**
	 * Enter a parse tree produced by the {@code nxInPort}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterNxInPort(@NotNull OpenflowParser.NxInPortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nxInPort}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitNxInPort(@NotNull OpenflowParser.NxInPortContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void enterFieldName(@NotNull OpenflowParser.FieldNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#fieldName}.
	 * @param ctx the parse tree
	 */
	void exitFieldName(@NotNull OpenflowParser.FieldNameContext ctx);

	/**
	 * Enter a parse tree produced by the {@code arpSpa}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpSpa(@NotNull OpenflowParser.ArpSpaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpSpa}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpSpa(@NotNull OpenflowParser.ArpSpaContext ctx);

	/**
	 * Enter a parse tree produced by the {@code n_packets}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterN_packets(@NotNull OpenflowParser.N_packetsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code n_packets}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitN_packets(@NotNull OpenflowParser.N_packetsContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ipSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpSrc(@NotNull OpenflowParser.IpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpSrc(@NotNull OpenflowParser.IpSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code arpSha}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterArpSha(@NotNull OpenflowParser.ArpShaContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arpSha}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitArpSha(@NotNull OpenflowParser.ArpShaContext ctx);

	/**
	 * Enter a parse tree produced by the {@code duration}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterDuration(@NotNull OpenflowParser.DurationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code duration}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitDuration(@NotNull OpenflowParser.DurationContext ctx);

	/**
	 * Enter a parse tree produced by the {@code load}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterLoad(@NotNull OpenflowParser.LoadContext ctx);
	/**
	 * Exit a parse tree produced by the {@code load}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitLoad(@NotNull OpenflowParser.LoadContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#flows}.
	 * @param ctx the parse tree
	 */
	void enterFlows(@NotNull OpenflowParser.FlowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#flows}.
	 * @param ctx the parse tree
	 */
	void exitFlows(@NotNull OpenflowParser.FlowsContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#ipv6}.
	 * @param ctx the parse tree
	 */
	void enterIpv6(@NotNull OpenflowParser.Ipv6Context ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#ipv6}.
	 * @param ctx the parse tree
	 */
	void exitIpv6(@NotNull OpenflowParser.Ipv6Context ctx);

	/**
	 * Enter a parse tree produced by the {@code yesFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterYesFrag(@NotNull OpenflowParser.YesFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code yesFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitYesFrag(@NotNull OpenflowParser.YesFragContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ethType}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterEthType(@NotNull OpenflowParser.EthTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ethType}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitEthType(@NotNull OpenflowParser.EthTypeContext ctx);

	/**
	 * Enter a parse tree produced by the {@code tcpSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterTcpSrc(@NotNull OpenflowParser.TcpSrcContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tcpSrc}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitTcpSrc(@NotNull OpenflowParser.TcpSrcContext ctx);

	/**
	 * Enter a parse tree produced by the {@code laterFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void enterLaterFrag(@NotNull OpenflowParser.LaterFragContext ctx);
	/**
	 * Exit a parse tree produced by the {@code laterFrag}
	 * labeled alternative in {@link OpenflowParser#frag_type}.
	 * @param ctx the parse tree
	 */
	void exitLaterFrag(@NotNull OpenflowParser.LaterFragContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(@NotNull OpenflowParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(@NotNull OpenflowParser.ValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#mask}.
	 * @param ctx the parse tree
	 */
	void enterMask(@NotNull OpenflowParser.MaskContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#mask}.
	 * @param ctx the parse tree
	 */
	void exitMask(@NotNull OpenflowParser.MaskContext ctx);

	/**
	 * Enter a parse tree produced by the {@code normal}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterNormal(@NotNull OpenflowParser.NormalContext ctx);
	/**
	 * Exit a parse tree produced by the {@code normal}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitNormal(@NotNull OpenflowParser.NormalContext ctx);

	/**
	 * Enter a parse tree produced by the {@code move}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterMove(@NotNull OpenflowParser.MoveContext ctx);
	/**
	 * Exit a parse tree produced by the {@code move}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitMove(@NotNull OpenflowParser.MoveContext ctx);

	/**
	 * Enter a parse tree produced by the {@code ipDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void enterIpDst(@NotNull OpenflowParser.IpDstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ipDst}
	 * labeled alternative in {@link OpenflowParser#nxm_reg}.
	 * @param ctx the parse tree
	 */
	void exitIpDst(@NotNull OpenflowParser.IpDstContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#match}.
	 * @param ctx the parse tree
	 */
	void enterMatch(@NotNull OpenflowParser.MatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#match}.
	 * @param ctx the parse tree
	 */
	void exitMatch(@NotNull OpenflowParser.MatchContext ctx);

	/**
	 * Enter a parse tree produced by the {@code decTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterDecTTL(@NotNull OpenflowParser.DecTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code decTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitDecTTL(@NotNull OpenflowParser.DecTTLContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setMplsTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetMplsTTL(@NotNull OpenflowParser.SetMplsTTLContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setMplsTTL}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetMplsTTL(@NotNull OpenflowParser.SetMplsTTLContext ctx);

	/**
	 * Enter a parse tree produced by the {@code obsDomainParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void enterObsDomainParam(@NotNull OpenflowParser.ObsDomainParamContext ctx);
	/**
	 * Exit a parse tree produced by the {@code obsDomainParam}
	 * labeled alternative in {@link OpenflowParser#sampleParam}.
	 * @param ctx the parse tree
	 */
	void exitObsDomainParam(@NotNull OpenflowParser.ObsDomainParamContext ctx);

	/**
	 * Enter a parse tree produced by the {@code enqueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterEnqueue(@NotNull OpenflowParser.EnqueueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code enqueue}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitEnqueue(@NotNull OpenflowParser.EnqueueContext ctx);

	/**
	 * Enter a parse tree produced by the {@code setNwTos}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterSetNwTos(@NotNull OpenflowParser.SetNwTosContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNwTos}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitSetNwTos(@NotNull OpenflowParser.SetNwTosContext ctx);

	/**
	 * Enter a parse tree produced by the {@code resubmit}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void enterResubmit(@NotNull OpenflowParser.ResubmitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code resubmit}
	 * labeled alternative in {@link OpenflowParser#target}.
	 * @param ctx the parse tree
	 */
	void exitResubmit(@NotNull OpenflowParser.ResubmitContext ctx);

	/**
	 * Enter a parse tree produced by {@link OpenflowParser#matchField}.
	 * @param ctx the parse tree
	 */
	void enterMatchField(@NotNull OpenflowParser.MatchFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OpenflowParser#matchField}.
	 * @param ctx the parse tree
	 */
	void exitMatchField(@NotNull OpenflowParser.MatchFieldContext ctx);

	/**
	 * Enter a parse tree produced by the {@code n_bytes}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void enterN_bytes(@NotNull OpenflowParser.N_bytesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code n_bytes}
	 * labeled alternative in {@link OpenflowParser#flowMetadata}.
	 * @param ctx the parse tree
	 */
	void exitN_bytes(@NotNull OpenflowParser.N_bytesContext ctx);
}