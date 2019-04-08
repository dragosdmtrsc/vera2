// Generated from C:/Users/a-drdum/source/repos/symnet-neutron/src/main/resources/p4_grammar\P4Commands.g4 by ANTLR 4.7.2
package generated.parser.p4.commands;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link P4CommandsParser}.
 */
public interface P4CommandsListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#statements}.
	 * @param ctx the parse tree
	 */
	void enterStatements(P4CommandsParser.StatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#statements}.
	 * @param ctx the parse tree
	 */
	void exitStatements(P4CommandsParser.StatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(P4CommandsParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(P4CommandsParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#table_default}.
	 * @param ctx the parse tree
	 */
	void enterTable_default(P4CommandsParser.Table_defaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#table_default}.
	 * @param ctx the parse tree
	 */
	void exitTable_default(P4CommandsParser.Table_defaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#table_add}.
	 * @param ctx the parse tree
	 */
	void enterTable_add(P4CommandsParser.Table_addContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#table_add}.
	 * @param ctx the parse tree
	 */
	void exitTable_add(P4CommandsParser.Table_addContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#mirroring_add}.
	 * @param ctx the parse tree
	 */
	void enterMirroring_add(P4CommandsParser.Mirroring_addContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#mirroring_add}.
	 * @param ctx the parse tree
	 */
	void exitMirroring_add(P4CommandsParser.Mirroring_addContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#act_prof_create_member}.
	 * @param ctx the parse tree
	 */
	void enterAct_prof_create_member(P4CommandsParser.Act_prof_create_memberContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#act_prof_create_member}.
	 * @param ctx the parse tree
	 */
	void exitAct_prof_create_member(P4CommandsParser.Act_prof_create_memberContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#action_parm}.
	 * @param ctx the parse tree
	 */
	void enterAction_parm(P4CommandsParser.Action_parmContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#action_parm}.
	 * @param ctx the parse tree
	 */
	void exitAction_parm(P4CommandsParser.Action_parmContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Masked}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void enterMasked(P4CommandsParser.MaskedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Masked}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void exitMasked(P4CommandsParser.MaskedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Prefix}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void enterPrefix(P4CommandsParser.PrefixContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Prefix}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void exitPrefix(P4CommandsParser.PrefixContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Range}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void enterRange(P4CommandsParser.RangeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Range}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void exitRange(P4CommandsParser.RangeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Exact}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void enterExact(P4CommandsParser.ExactContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Exact}
	 * labeled alternative in {@link P4CommandsParser#match_key}.
	 * @param ctx the parse tree
	 */
	void exitExact(P4CommandsParser.ExactContext ctx);
	/**
	 * Enter a parse tree produced by the {@code memberAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void enterMemberAction(P4CommandsParser.MemberActionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code memberAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void exitMemberAction(P4CommandsParser.MemberActionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code namedAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void enterNamedAction(P4CommandsParser.NamedActionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code namedAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void exitNamedAction(P4CommandsParser.NamedActionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code emptyAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void enterEmptyAction(P4CommandsParser.EmptyActionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code emptyAction}
	 * labeled alternative in {@link P4CommandsParser#act_spec}.
	 * @param ctx the parse tree
	 */
	void exitEmptyAction(P4CommandsParser.EmptyActionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BinaryUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterBinaryUValue(P4CommandsParser.BinaryUValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BinaryUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitBinaryUValue(P4CommandsParser.BinaryUValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DecimalUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterDecimalUValue(P4CommandsParser.DecimalUValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DecimalUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitDecimalUValue(P4CommandsParser.DecimalUValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code HexadecimalUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterHexadecimalUValue(P4CommandsParser.HexadecimalUValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code HexadecimalUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitHexadecimalUValue(P4CommandsParser.HexadecimalUValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MacUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterMacUValue(P4CommandsParser.MacUValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MacUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitMacUValue(P4CommandsParser.MacUValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IPUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterIPUValue(P4CommandsParser.IPUValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IPUValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitIPUValue(P4CommandsParser.IPUValueContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IP6UValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void enterIP6UValue(P4CommandsParser.IP6UValueContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IP6UValue}
	 * labeled alternative in {@link P4CommandsParser#unsigned_value}.
	 * @param ctx the parse tree
	 */
	void exitIP6UValue(P4CommandsParser.IP6UValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link P4CommandsParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(P4CommandsParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link P4CommandsParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(P4CommandsParser.IdContext ctx);
}