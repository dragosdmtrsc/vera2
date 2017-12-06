// Generated from Iptables.g4 by ANTLR 4.3

package generated.iptables_grammar;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link IptablesParser}.
 */
public interface IptablesListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link IptablesParser#frgm}.
     *
     * @param ctx the parse tree
     */
    void enterFrgm(@NotNull IptablesParser.FrgmContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#frgm}.
     *
     * @param ctx the parse tree
     */
    void exitFrgm(@NotNull IptablesParser.FrgmContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ctTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterCtTargetOpts(@NotNull IptablesParser.CtTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ctTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitCtTargetOpts(@NotNull IptablesParser.CtTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#dropTarget}.
     *
     * @param ctx the parse tree
     */
    void enterDropTarget(@NotNull IptablesParser.DropTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#dropTarget}.
     *
     * @param ctx the parse tree
     */
    void exitDropTarget(@NotNull IptablesParser.DropTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#stateopts}.
     *
     * @param ctx the parse tree
     */
    void enterStateopts(@NotNull IptablesParser.StateoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#stateopts}.
     *
     * @param ctx the parse tree
     */
    void exitStateopts(@NotNull IptablesParser.StateoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#dstaddr}.
     *
     * @param ctx the parse tree
     */
    void enterDstaddr(@NotNull IptablesParser.DstaddrContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#dstaddr}.
     *
     * @param ctx the parse tree
     */
    void exitDstaddr(@NotNull IptablesParser.DstaddrContext ctx);

    /**
     * Enter a parse tree produced by the {@code ctMask}
     * labeled alternative in {@link IptablesParser#maskingOption}.
     *
     * @param ctx the parse tree
     */
    void enterCtMask(@NotNull IptablesParser.CtMaskContext ctx);

    /**
     * Exit a parse tree produced by the {@code ctMask}
     * labeled alternative in {@link IptablesParser#maskingOption}.
     *
     * @param ctx the parse tree
     */
    void exitCtMask(@NotNull IptablesParser.CtMaskContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#nfCtMask}.
     *
     * @param ctx the parse tree
     */
    void enterNfCtMask(@NotNull IptablesParser.NfCtMaskContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#nfCtMask}.
     *
     * @param ctx the parse tree
     */
    void exitNfCtMask(@NotNull IptablesParser.NfCtMaskContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#dnatTarget}.
     *
     * @param ctx the parse tree
     */
    void enterDnatTarget(@NotNull IptablesParser.DnatTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#dnatTarget}.
     *
     * @param ctx the parse tree
     */
    void exitDnatTarget(@NotNull IptablesParser.DnatTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#snatTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterSnatTargetOpts(@NotNull IptablesParser.SnatTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#snatTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitSnatTargetOpts(@NotNull IptablesParser.SnatTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#notrackTarget}.
     *
     * @param ctx the parse tree
     */
    void enterNotrackTarget(@NotNull IptablesParser.NotrackTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#notrackTarget}.
     *
     * @param ctx the parse tree
     */
    void exitNotrackTarget(@NotNull IptablesParser.NotrackTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#snatTarget}.
     *
     * @param ctx the parse tree
     */
    void enterSnatTarget(@NotNull IptablesParser.SnatTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#snatTarget}.
     *
     * @param ctx the parse tree
     */
    void exitSnatTarget(@NotNull IptablesParser.SnatTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#commentopts}.
     *
     * @param ctx the parse tree
     */
    void enterCommentopts(@NotNull IptablesParser.CommentoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#commentopts}.
     *
     * @param ctx the parse tree
     */
    void exitCommentopts(@NotNull IptablesParser.CommentoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#protocol}.
     *
     * @param ctx the parse tree
     */
    void enterProtocol(@NotNull IptablesParser.ProtocolContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#protocol}.
     *
     * @param ctx the parse tree
     */
    void exitProtocol(@NotNull IptablesParser.ProtocolContext ctx);

    /**
     * Enter a parse tree produced by the {@code ipMasked}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void enterIpMasked(@NotNull IptablesParser.IpMaskedContext ctx);

    /**
     * Exit a parse tree produced by the {@code ipMasked}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void exitIpMasked(@NotNull IptablesParser.IpMaskedContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#connmarkopts}.
     *
     * @param ctx the parse tree
     */
    void enterConnmarkopts(@NotNull IptablesParser.ConnmarkoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#connmarkopts}.
     *
     * @param ctx the parse tree
     */
    void exitConnmarkopts(@NotNull IptablesParser.ConnmarkoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#rejectTarget}.
     *
     * @param ctx the parse tree
     */
    void enterRejectTarget(@NotNull IptablesParser.RejectTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#rejectTarget}.
     *
     * @param ctx the parse tree
     */
    void exitRejectTarget(@NotNull IptablesParser.RejectTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#statelist}.
     *
     * @param ctx the parse tree
     */
    void enterStatelist(@NotNull IptablesParser.StatelistContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#statelist}.
     *
     * @param ctx the parse tree
     */
    void exitStatelist(@NotNull IptablesParser.StatelistContext ctx);

    /**
     * Enter a parse tree produced by the {@code restoreCtMark}
     * labeled alternative in {@link IptablesParser#connmarkTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterRestoreCtMark(@NotNull IptablesParser.RestoreCtMarkContext ctx);

    /**
     * Exit a parse tree produced by the {@code restoreCtMark}
     * labeled alternative in {@link IptablesParser#connmarkTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitRestoreCtMark(@NotNull IptablesParser.RestoreCtMarkContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#macaddress}.
     *
     * @param ctx the parse tree
     */
    void enterMacaddress(@NotNull IptablesParser.MacaddressContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#macaddress}.
     *
     * @param ctx the parse tree
     */
    void exitMacaddress(@NotNull IptablesParser.MacaddressContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#returnTarget}.
     *
     * @param ctx the parse tree
     */
    void enterReturnTarget(@NotNull IptablesParser.ReturnTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#returnTarget}.
     *
     * @param ctx the parse tree
     */
    void exitReturnTarget(@NotNull IptablesParser.ReturnTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#state}.
     *
     * @param ctx the parse tree
     */
    void enterState(@NotNull IptablesParser.StateContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#state}.
     *
     * @param ctx the parse tree
     */
    void exitState(@NotNull IptablesParser.StateContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#icmpopts}.
     *
     * @param ctx the parse tree
     */
    void enterIcmpopts(@NotNull IptablesParser.IcmpoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#icmpopts}.
     *
     * @param ctx the parse tree
     */
    void exitIcmpopts(@NotNull IptablesParser.IcmpoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#tcpopts}.
     *
     * @param ctx the parse tree
     */
    void enterTcpopts(@NotNull IptablesParser.TcpoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#tcpopts}.
     *
     * @param ctx the parse tree
     */
    void exitTcpopts(@NotNull IptablesParser.TcpoptsContext ctx);

    /**
     * Enter a parse tree produced by the {@code bareType}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void enterBareType(@NotNull IptablesParser.BareTypeContext ctx);

    /**
     * Exit a parse tree produced by the {@code bareType}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void exitBareType(@NotNull IptablesParser.BareTypeContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#module}.
     *
     * @param ctx the parse tree
     */
    void enterModule(@NotNull IptablesParser.ModuleContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#module}.
     *
     * @param ctx the parse tree
     */
    void exitModule(@NotNull IptablesParser.ModuleContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ctTarget}.
     *
     * @param ctx the parse tree
     */
    void enterCtTarget(@NotNull IptablesParser.CtTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ctTarget}.
     *
     * @param ctx the parse tree
     */
    void exitCtTarget(@NotNull IptablesParser.CtTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#markTarget}.
     *
     * @param ctx the parse tree
     */
    void enterMarkTarget(@NotNull IptablesParser.MarkTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#markTarget}.
     *
     * @param ctx the parse tree
     */
    void exitMarkTarget(@NotNull IptablesParser.MarkTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#redirectTarget}.
     *
     * @param ctx the parse tree
     */
    void enterRedirectTarget(@NotNull IptablesParser.RedirectTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#redirectTarget}.
     *
     * @param ctx the parse tree
     */
    void exitRedirectTarget(@NotNull IptablesParser.RedirectTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#conntrackopts}.
     *
     * @param ctx the parse tree
     */
    void enterConntrackopts(@NotNull IptablesParser.ConntrackoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#conntrackopts}.
     *
     * @param ctx the parse tree
     */
    void exitConntrackopts(@NotNull IptablesParser.ConntrackoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#connmarkTarget}.
     *
     * @param ctx the parse tree
     */
    void enterConnmarkTarget(@NotNull IptablesParser.ConnmarkTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#connmarkTarget}.
     *
     * @param ctx the parse tree
     */
    void exitConnmarkTarget(@NotNull IptablesParser.ConnmarkTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#setTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterSetTargetOpts(@NotNull IptablesParser.SetTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#setTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitSetTargetOpts(@NotNull IptablesParser.SetTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by the {@code nfMask}
     * labeled alternative in {@link IptablesParser#maskingOption}.
     *
     * @param ctx the parse tree
     */
    void enterNfMask(@NotNull IptablesParser.NfMaskContext ctx);

    /**
     * Exit a parse tree produced by the {@code nfMask}
     * labeled alternative in {@link IptablesParser#maskingOption}.
     *
     * @param ctx the parse tree
     */
    void exitNfMask(@NotNull IptablesParser.NfMaskContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#checksumTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterChecksumTargetOpts(@NotNull IptablesParser.ChecksumTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#checksumTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitChecksumTargetOpts(@NotNull IptablesParser.ChecksumTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#physdevopts}.
     *
     * @param ctx the parse tree
     */
    void enterPhysdevopts(@NotNull IptablesParser.PhysdevoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#physdevopts}.
     *
     * @param ctx the parse tree
     */
    void exitPhysdevopts(@NotNull IptablesParser.PhysdevoptsContext ctx);

    /**
     * Enter a parse tree produced by the {@code physdevOut}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void enterPhysdevOut(@NotNull IptablesParser.PhysdevOutContext ctx);

    /**
     * Exit a parse tree produced by the {@code physdevOut}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void exitPhysdevOut(@NotNull IptablesParser.PhysdevOutContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#status}.
     *
     * @param ctx the parse tree
     */
    void enterStatus(@NotNull IptablesParser.StatusContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#status}.
     *
     * @param ctx the parse tree
     */
    void exitStatus(@NotNull IptablesParser.StatusContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ctZone}.
     *
     * @param ctx the parse tree
     */
    void enterCtZone(@NotNull IptablesParser.CtZoneContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ctZone}.
     *
     * @param ctx the parse tree
     */
    void exitCtZone(@NotNull IptablesParser.CtZoneContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#icmp6opts}.
     *
     * @param ctx the parse tree
     */
    void enterIcmp6opts(@NotNull IptablesParser.Icmp6optsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#icmp6opts}.
     *
     * @param ctx the parse tree
     */
    void exitIcmp6opts(@NotNull IptablesParser.Icmp6optsContext ctx);

    /**
     * Enter a parse tree produced by the {@code hostName}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void enterHostName(@NotNull IptablesParser.HostNameContext ctx);

    /**
     * Exit a parse tree produced by the {@code hostName}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void exitHostName(@NotNull IptablesParser.HostNameContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#targetName}.
     *
     * @param ctx the parse tree
     */
    void enterTargetName(@NotNull IptablesParser.TargetNameContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#targetName}.
     *
     * @param ctx the parse tree
     */
    void exitTargetName(@NotNull IptablesParser.TargetNameContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#statuslist}.
     *
     * @param ctx the parse tree
     */
    void enterStatuslist(@NotNull IptablesParser.StatuslistContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#statuslist}.
     *
     * @param ctx the parse tree
     */
    void exitStatuslist(@NotNull IptablesParser.StatuslistContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#flag}.
     *
     * @param ctx the parse tree
     */
    void enterFlag(@NotNull IptablesParser.FlagContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#flag}.
     *
     * @param ctx the parse tree
     */
    void exitFlag(@NotNull IptablesParser.FlagContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#sourceaddr}.
     *
     * @param ctx the parse tree
     */
    void enterSourceaddr(@NotNull IptablesParser.SourceaddrContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#sourceaddr}.
     *
     * @param ctx the parse tree
     */
    void exitSourceaddr(@NotNull IptablesParser.SourceaddrContext ctx);

    /**
     * Enter a parse tree produced by the {@code physdevIsBridged}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void enterPhysdevIsBridged(@NotNull IptablesParser.PhysdevIsBridgedContext ctx);

    /**
     * Exit a parse tree produced by the {@code physdevIsBridged}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void exitPhysdevIsBridged(@NotNull IptablesParser.PhysdevIsBridgedContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#chainname}.
     *
     * @param ctx the parse tree
     */
    void enterChainname(@NotNull IptablesParser.ChainnameContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#chainname}.
     *
     * @param ctx the parse tree
     */
    void exitChainname(@NotNull IptablesParser.ChainnameContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#markopts}.
     *
     * @param ctx the parse tree
     */
    void enterMarkopts(@NotNull IptablesParser.MarkoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#markopts}.
     *
     * @param ctx the parse tree
     */
    void exitMarkopts(@NotNull IptablesParser.MarkoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#dport}.
     *
     * @param ctx the parse tree
     */
    void enterDport(@NotNull IptablesParser.DportContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#dport}.
     *
     * @param ctx the parse tree
     */
    void exitDport(@NotNull IptablesParser.DportContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#setvars}.
     *
     * @param ctx the parse tree
     */
    void enterSetvars(@NotNull IptablesParser.SetvarsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#setvars}.
     *
     * @param ctx the parse tree
     */
    void exitSetvars(@NotNull IptablesParser.SetvarsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#masqueradeTarget}.
     *
     * @param ctx the parse tree
     */
    void enterMasqueradeTarget(@NotNull IptablesParser.MasqueradeTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#masqueradeTarget}.
     *
     * @param ctx the parse tree
     */
    void exitMasqueradeTarget(@NotNull IptablesParser.MasqueradeTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ipv4}.
     *
     * @param ctx the parse tree
     */
    void enterIpv4(@NotNull IptablesParser.Ipv4Context ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ipv4}.
     *
     * @param ctx the parse tree
     */
    void exitIpv4(@NotNull IptablesParser.Ipv4Context ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ipv6}.
     *
     * @param ctx the parse tree
     */
    void enterIpv6(@NotNull IptablesParser.Ipv6Context ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ipv6}.
     *
     * @param ctx the parse tree
     */
    void exitIpv6(@NotNull IptablesParser.Ipv6Context ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#acceptTarget}.
     *
     * @param ctx the parse tree
     */
    void enterAcceptTarget(@NotNull IptablesParser.AcceptTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#acceptTarget}.
     *
     * @param ctx the parse tree
     */
    void exitAcceptTarget(@NotNull IptablesParser.AcceptTargetContext ctx);

    /**
     * Enter a parse tree produced by the {@code codeName}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void enterCodeName(@NotNull IptablesParser.CodeNameContext ctx);

    /**
     * Exit a parse tree produced by the {@code codeName}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void exitCodeName(@NotNull IptablesParser.CodeNameContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#setTarget}.
     *
     * @param ctx the parse tree
     */
    void enterSetTarget(@NotNull IptablesParser.SetTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#setTarget}.
     *
     * @param ctx the parse tree
     */
    void exitSetTarget(@NotNull IptablesParser.SetTargetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#iniface}.
     *
     * @param ctx the parse tree
     */
    void enterIniface(@NotNull IptablesParser.InifaceContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#iniface}.
     *
     * @param ctx the parse tree
     */
    void exitIniface(@NotNull IptablesParser.InifaceContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#macopts}.
     *
     * @param ctx the parse tree
     */
    void enterMacopts(@NotNull IptablesParser.MacoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#macopts}.
     *
     * @param ctx the parse tree
     */
    void exitMacopts(@NotNull IptablesParser.MacoptsContext ctx);

    /**
     * Enter a parse tree produced by the {@code saveCtMark}
     * labeled alternative in {@link IptablesParser#connmarkTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterSaveCtMark(@NotNull IptablesParser.SaveCtMarkContext ctx);

    /**
     * Exit a parse tree produced by the {@code saveCtMark}
     * labeled alternative in {@link IptablesParser#connmarkTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitSaveCtMark(@NotNull IptablesParser.SaveCtMarkContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#event}.
     *
     * @param ctx the parse tree
     */
    void enterEvent(@NotNull IptablesParser.EventContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#event}.
     *
     * @param ctx the parse tree
     */
    void exitEvent(@NotNull IptablesParser.EventContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ctrs}.
     *
     * @param ctx the parse tree
     */
    void enterCtrs(@NotNull IptablesParser.CtrsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ctrs}.
     *
     * @param ctx the parse tree
     */
    void exitCtrs(@NotNull IptablesParser.CtrsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#udpopts}.
     *
     * @param ctx the parse tree
     */
    void enterUdpopts(@NotNull IptablesParser.UdpoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#udpopts}.
     *
     * @param ctx the parse tree
     */
    void exitUdpopts(@NotNull IptablesParser.UdpoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#table}.
     *
     * @param ctx the parse tree
     */
    void enterTable(@NotNull IptablesParser.TableContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#table}.
     *
     * @param ctx the parse tree
     */
    void exitTable(@NotNull IptablesParser.TableContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#ctNotrack}.
     *
     * @param ctx the parse tree
     */
    void enterCtNotrack(@NotNull IptablesParser.CtNotrackContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#ctNotrack}.
     *
     * @param ctx the parse tree
     */
    void exitCtNotrack(@NotNull IptablesParser.CtNotrackContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#dnatTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterDnatTargetOpts(@NotNull IptablesParser.DnatTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#dnatTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitDnatTargetOpts(@NotNull IptablesParser.DnatTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#conntrackvars}.
     *
     * @param ctx the parse tree
     */
    void enterConntrackvars(@NotNull IptablesParser.ConntrackvarsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#conntrackvars}.
     *
     * @param ctx the parse tree
     */
    void exitConntrackvars(@NotNull IptablesParser.ConntrackvarsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#setopts}.
     *
     * @param ctx the parse tree
     */
    void enterSetopts(@NotNull IptablesParser.SetoptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#setopts}.
     *
     * @param ctx the parse tree
     */
    void exitSetopts(@NotNull IptablesParser.SetoptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#policy}.
     *
     * @param ctx the parse tree
     */
    void enterPolicy(@NotNull IptablesParser.PolicyContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#policy}.
     *
     * @param ctx the parse tree
     */
    void exitPolicy(@NotNull IptablesParser.PolicyContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#mask}.
     *
     * @param ctx the parse tree
     */
    void enterMask(@NotNull IptablesParser.MaskContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#mask}.
     *
     * @param ctx the parse tree
     */
    void exitMask(@NotNull IptablesParser.MaskContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#rejectTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterRejectTargetOpts(@NotNull IptablesParser.RejectTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#rejectTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitRejectTargetOpts(@NotNull IptablesParser.RejectTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#chain}.
     *
     * @param ctx the parse tree
     */
    void enterChain(@NotNull IptablesParser.ChainContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#chain}.
     *
     * @param ctx the parse tree
     */
    void exitChain(@NotNull IptablesParser.ChainContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#jumpyTarget}.
     *
     * @param ctx the parse tree
     */
    void enterJumpyTarget(@NotNull IptablesParser.JumpyTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#jumpyTarget}.
     *
     * @param ctx the parse tree
     */
    void exitJumpyTarget(@NotNull IptablesParser.JumpyTargetContext ctx);

    /**
     * Enter a parse tree produced by the {@code physdevIn}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void enterPhysdevIn(@NotNull IptablesParser.PhysdevInContext ctx);

    /**
     * Exit a parse tree produced by the {@code physdevIn}
     * labeled alternative in {@link IptablesParser#physdevvars}.
     *
     * @param ctx the parse tree
     */
    void exitPhysdevIn(@NotNull IptablesParser.PhysdevInContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#address}.
     *
     * @param ctx the parse tree
     */
    void enterAddress(@NotNull IptablesParser.AddressContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#address}.
     *
     * @param ctx the parse tree
     */
    void exitAddress(@NotNull IptablesParser.AddressContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#match}.
     *
     * @param ctx the parse tree
     */
    void enterMatch(@NotNull IptablesParser.MatchContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#match}.
     *
     * @param ctx the parse tree
     */
    void exitMatch(@NotNull IptablesParser.MatchContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#markTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void enterMarkTargetOpts(@NotNull IptablesParser.MarkTargetOptsContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#markTargetOpts}.
     *
     * @param ctx the parse tree
     */
    void exitMarkTargetOpts(@NotNull IptablesParser.MarkTargetOptsContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#rle}.
     *
     * @param ctx the parse tree
     */
    void enterRle(@NotNull IptablesParser.RleContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#rle}.
     *
     * @param ctx the parse tree
     */
    void exitRle(@NotNull IptablesParser.RleContext ctx);

    /**
     * Enter a parse tree produced by the {@code ipNormal}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void enterIpNormal(@NotNull IptablesParser.IpNormalContext ctx);

    /**
     * Exit a parse tree produced by the {@code ipNormal}
     * labeled alternative in {@link IptablesParser#addressExpression}.
     *
     * @param ctx the parse tree
     */
    void exitIpNormal(@NotNull IptablesParser.IpNormalContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#target}.
     *
     * @param ctx the parse tree
     */
    void enterTarget(@NotNull IptablesParser.TargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#target}.
     *
     * @param ctx the parse tree
     */
    void exitTarget(@NotNull IptablesParser.TargetContext ctx);

    /**
     * Enter a parse tree produced by the {@code typeCode}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void enterTypeCode(@NotNull IptablesParser.TypeCodeContext ctx);

    /**
     * Exit a parse tree produced by the {@code typeCode}
     * labeled alternative in {@link IptablesParser#icmptype}.
     *
     * @param ctx the parse tree
     */
    void exitTypeCode(@NotNull IptablesParser.TypeCodeContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#portno}.
     *
     * @param ctx the parse tree
     */
    void enterPortno(@NotNull IptablesParser.PortnoContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#portno}.
     *
     * @param ctx the parse tree
     */
    void exitPortno(@NotNull IptablesParser.PortnoContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#flagset}.
     *
     * @param ctx the parse tree
     */
    void enterFlagset(@NotNull IptablesParser.FlagsetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#flagset}.
     *
     * @param ctx the parse tree
     */
    void exitFlagset(@NotNull IptablesParser.FlagsetContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#outiface}.
     *
     * @param ctx the parse tree
     */
    void enterOutiface(@NotNull IptablesParser.OutifaceContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#outiface}.
     *
     * @param ctx the parse tree
     */
    void exitOutiface(@NotNull IptablesParser.OutifaceContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#sport}.
     *
     * @param ctx the parse tree
     */
    void enterSport(@NotNull IptablesParser.SportContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#sport}.
     *
     * @param ctx the parse tree
     */
    void exitSport(@NotNull IptablesParser.SportContext ctx);

    /**
     * Enter a parse tree produced by {@link IptablesParser#checksumTarget}.
     *
     * @param ctx the parse tree
     */
    void enterChecksumTarget(@NotNull IptablesParser.ChecksumTargetContext ctx);

    /**
     * Exit a parse tree produced by {@link IptablesParser#checksumTarget}.
     *
     * @param ctx the parse tree
     */
    void exitChecksumTarget(@NotNull IptablesParser.ChecksumTargetContext ctx);
}