// Generated from C:/Users/dragos/source/repos/symnet-neutron/src/main/resources/p4_grammar\P4Commands.g4 by ANTLR 4.7.2
package generated.parser.p4.commands;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class P4CommandsParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, Binary_value=11, Decimal_value=12, Hexadecimal_value=13, IP=14, 
		IP6=15, MAC=16, NAME=17, TABLE_SET=18, WS=19;
	public static final int
		RULE_statements = 0, RULE_statement = 1, RULE_table_default = 2, RULE_table_add = 3, 
		RULE_mirroring_add = 4, RULE_action_parm = 5, RULE_match_key = 6, RULE_act_spec = 7, 
		RULE_unsigned_value = 8, RULE_id = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"statements", "statement", "table_default", "table_add", "mirroring_add", 
			"action_parm", "match_key", "act_spec", "unsigned_value", "id"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'table_set_default'", "'=>'", "'table_add'", "'mirroring_add'", 
			"'&&&'", "'/'", "','", "'member'", "'('", "')'", null, null, null, null, 
			null, null, null, "'table_bla'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, "Binary_value", 
			"Decimal_value", "Hexadecimal_value", "IP", "IP6", "MAC", "NAME", "TABLE_SET", 
			"WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "P4Commands.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public P4CommandsParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class StatementsContext extends ParserRuleContext {
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public StatementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterStatements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitStatements(this);
		}
	}

	public final StatementsContext statements() throws RecognitionException {
		StatementsContext _localctx = new StatementsContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_statements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(21); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(20);
				statement();
				}
				}
				setState(23); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__2) | (1L << T__3))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public Table_defaultContext table_default() {
			return getRuleContext(Table_defaultContext.class,0);
		}
		public Table_addContext table_add() {
			return getRuleContext(Table_addContext.class,0);
		}
		public Mirroring_addContext mirroring_add() {
			return getRuleContext(Mirroring_addContext.class,0);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitStatement(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_statement);
		try {
			setState(28);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				enterOuterAlt(_localctx, 1);
				{
				setState(25);
				table_default();
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 2);
				{
				setState(26);
				table_add();
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 3);
				{
				setState(27);
				mirroring_add();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Table_defaultContext extends ParserRuleContext {
		public org.change.p4.control.TableFlow tableFlow;
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Act_specContext act_spec() {
			return getRuleContext(Act_specContext.class,0);
		}
		public List<Action_parmContext> action_parm() {
			return getRuleContexts(Action_parmContext.class);
		}
		public Action_parmContext action_parm(int i) {
			return getRuleContext(Action_parmContext.class,i);
		}
		public Table_defaultContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table_default; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterTable_default(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitTable_default(this);
		}
	}

	public final Table_defaultContext table_default() throws RecognitionException {
		Table_defaultContext _localctx = new Table_defaultContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_table_default);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			match(T__0);
			setState(31);
			id();
			setState(32);
			act_spec();
			setState(40);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(33);
				match(T__1);
				setState(37);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << Binary_value) | (1L << Decimal_value) | (1L << Hexadecimal_value) | (1L << IP) | (1L << IP6) | (1L << MAC))) != 0)) {
					{
					{
					setState(34);
					action_parm();
					}
					}
					setState(39);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Table_addContext extends ParserRuleContext {
		public org.change.p4.control.TableFlow tableFlow;
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public Act_specContext act_spec() {
			return getRuleContext(Act_specContext.class,0);
		}
		public List<Match_keyContext> match_key() {
			return getRuleContexts(Match_keyContext.class);
		}
		public Match_keyContext match_key(int i) {
			return getRuleContext(Match_keyContext.class,i);
		}
		public List<Action_parmContext> action_parm() {
			return getRuleContexts(Action_parmContext.class);
		}
		public Action_parmContext action_parm(int i) {
			return getRuleContext(Action_parmContext.class,i);
		}
		public Table_addContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table_add; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterTable_add(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitTable_add(this);
		}
	}

	public final Table_addContext table_add() throws RecognitionException {
		Table_addContext _localctx = new Table_addContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_table_add);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			match(T__2);
			setState(43);
			id();
			setState(44);
			act_spec();
			setState(48);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << Binary_value) | (1L << Decimal_value) | (1L << Hexadecimal_value) | (1L << IP) | (1L << IP6) | (1L << MAC))) != 0)) {
				{
				{
				setState(45);
				match_key();
				}
				}
				setState(50);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(58);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(51);
				match(T__1);
				setState(55);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << Binary_value) | (1L << Decimal_value) | (1L << Hexadecimal_value) | (1L << IP) | (1L << IP6) | (1L << MAC))) != 0)) {
					{
					{
					setState(52);
					action_parm();
					}
					}
					setState(57);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Mirroring_addContext extends ParserRuleContext {
		public List<Unsigned_valueContext> unsigned_value() {
			return getRuleContexts(Unsigned_valueContext.class);
		}
		public Unsigned_valueContext unsigned_value(int i) {
			return getRuleContext(Unsigned_valueContext.class,i);
		}
		public Mirroring_addContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mirroring_add; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterMirroring_add(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitMirroring_add(this);
		}
	}

	public final Mirroring_addContext mirroring_add() throws RecognitionException {
		Mirroring_addContext _localctx = new Mirroring_addContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_mirroring_add);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(60);
			match(T__3);
			setState(61);
			unsigned_value();
			setState(62);
			unsigned_value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Action_parmContext extends ParserRuleContext {
		public org.change.p4.control.ActionParam actionParam;
		public Unsigned_valueContext unsigned_value() {
			return getRuleContext(Unsigned_valueContext.class,0);
		}
		public Action_parmContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_action_parm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterAction_parm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitAction_parm(this);
		}
	}

	public final Action_parmContext action_parm() throws RecognitionException {
		Action_parmContext _localctx = new Action_parmContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_action_parm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			unsigned_value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_keyContext extends ParserRuleContext {
		public org.change.p4.control.MatchParam matchParam;
		public Match_keyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_key; }
	 
		public Match_keyContext() { }
		public void copyFrom(Match_keyContext ctx) {
			super.copyFrom(ctx);
			this.matchParam = ctx.matchParam;
		}
	}
	public static class MaskedContext extends Match_keyContext {
		public List<Unsigned_valueContext> unsigned_value() {
			return getRuleContexts(Unsigned_valueContext.class);
		}
		public Unsigned_valueContext unsigned_value(int i) {
			return getRuleContext(Unsigned_valueContext.class,i);
		}
		public MaskedContext(Match_keyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterMasked(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitMasked(this);
		}
	}
	public static class PrefixContext extends Match_keyContext {
		public List<Unsigned_valueContext> unsigned_value() {
			return getRuleContexts(Unsigned_valueContext.class);
		}
		public Unsigned_valueContext unsigned_value(int i) {
			return getRuleContext(Unsigned_valueContext.class,i);
		}
		public PrefixContext(Match_keyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterPrefix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitPrefix(this);
		}
	}
	public static class ExactContext extends Match_keyContext {
		public Unsigned_valueContext unsigned_value() {
			return getRuleContext(Unsigned_valueContext.class,0);
		}
		public ExactContext(Match_keyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterExact(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitExact(this);
		}
	}
	public static class RangeContext extends Match_keyContext {
		public List<Unsigned_valueContext> unsigned_value() {
			return getRuleContexts(Unsigned_valueContext.class);
		}
		public Unsigned_valueContext unsigned_value(int i) {
			return getRuleContext(Unsigned_valueContext.class,i);
		}
		public RangeContext(Match_keyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterRange(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitRange(this);
		}
	}

	public final Match_keyContext match_key() throws RecognitionException {
		Match_keyContext _localctx = new Match_keyContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_match_key);
		try {
			setState(79);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				_localctx = new MaskedContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(66);
				unsigned_value();
				setState(67);
				match(T__4);
				setState(68);
				unsigned_value();
				}
				break;
			case 2:
				_localctx = new PrefixContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(70);
				unsigned_value();
				setState(71);
				match(T__5);
				setState(72);
				unsigned_value();
				}
				break;
			case 3:
				_localctx = new RangeContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(74);
				unsigned_value();
				setState(75);
				match(T__6);
				setState(76);
				unsigned_value();
				}
				break;
			case 4:
				_localctx = new ExactContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(78);
				unsigned_value();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Act_specContext extends ParserRuleContext {
		public org.change.p4.control.ActionSpec actionSpec;
		public Act_specContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_act_spec; }
	 
		public Act_specContext() { }
		public void copyFrom(Act_specContext ctx) {
			super.copyFrom(ctx);
			this.actionSpec = ctx.actionSpec;
		}
	}
	public static class MemberActionContext extends Act_specContext {
		public Unsigned_valueContext unsigned_value() {
			return getRuleContext(Unsigned_valueContext.class,0);
		}
		public MemberActionContext(Act_specContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterMemberAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitMemberAction(this);
		}
	}
	public static class NamedActionContext extends Act_specContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public NamedActionContext(Act_specContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterNamedAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitNamedAction(this);
		}
	}

	public final Act_specContext act_spec() throws RecognitionException {
		Act_specContext _localctx = new Act_specContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_act_spec);
		try {
			setState(87);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__7:
				_localctx = new MemberActionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(81);
				match(T__7);
				setState(82);
				match(T__8);
				setState(83);
				unsigned_value();
				setState(84);
				match(T__9);
				}
				break;
			case NAME:
				_localctx = new NamedActionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(86);
				id();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Unsigned_valueContext extends ParserRuleContext {
		public scala.math.BigInt unsignedValue;
		public Unsigned_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unsigned_value; }
	 
		public Unsigned_valueContext() { }
		public void copyFrom(Unsigned_valueContext ctx) {
			super.copyFrom(ctx);
			this.unsignedValue = ctx.unsignedValue;
		}
	}
	public static class HexadecimalUValueContext extends Unsigned_valueContext {
		public TerminalNode Hexadecimal_value() { return getToken(P4CommandsParser.Hexadecimal_value, 0); }
		public HexadecimalUValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterHexadecimalUValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitHexadecimalUValue(this);
		}
	}
	public static class BinaryUValueContext extends Unsigned_valueContext {
		public TerminalNode Binary_value() { return getToken(P4CommandsParser.Binary_value, 0); }
		public BinaryUValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterBinaryUValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitBinaryUValue(this);
		}
	}
	public static class IP6UValueContext extends Unsigned_valueContext {
		public TerminalNode IP6() { return getToken(P4CommandsParser.IP6, 0); }
		public IP6UValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterIP6UValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitIP6UValue(this);
		}
	}
	public static class DecimalUValueContext extends Unsigned_valueContext {
		public TerminalNode Decimal_value() { return getToken(P4CommandsParser.Decimal_value, 0); }
		public DecimalUValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterDecimalUValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitDecimalUValue(this);
		}
	}
	public static class MacUValueContext extends Unsigned_valueContext {
		public TerminalNode MAC() { return getToken(P4CommandsParser.MAC, 0); }
		public MacUValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterMacUValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitMacUValue(this);
		}
	}
	public static class IPUValueContext extends Unsigned_valueContext {
		public TerminalNode IP() { return getToken(P4CommandsParser.IP, 0); }
		public IPUValueContext(Unsigned_valueContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterIPUValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitIPUValue(this);
		}
	}

	public final Unsigned_valueContext unsigned_value() throws RecognitionException {
		Unsigned_valueContext _localctx = new Unsigned_valueContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_unsigned_value);
		try {
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Binary_value:
				_localctx = new BinaryUValueContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(89);
				match(Binary_value);
				}
				break;
			case Decimal_value:
				_localctx = new DecimalUValueContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(90);
				match(Decimal_value);
				}
				break;
			case Hexadecimal_value:
				_localctx = new HexadecimalUValueContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(91);
				match(Hexadecimal_value);
				}
				break;
			case MAC:
				_localctx = new MacUValueContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(92);
				match(MAC);
				}
				break;
			case IP:
				_localctx = new IPUValueContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(93);
				match(IP);
				}
				break;
			case IP6:
				_localctx = new IP6UValueContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(94);
				match(IP6);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(P4CommandsParser.NAME, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof P4CommandsListener ) ((P4CommandsListener)listener).exitId(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_id);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			match(NAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\25f\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\3"+
		"\2\6\2\30\n\2\r\2\16\2\31\3\3\3\3\3\3\5\3\37\n\3\3\4\3\4\3\4\3\4\3\4\7"+
		"\4&\n\4\f\4\16\4)\13\4\5\4+\n\4\3\5\3\5\3\5\3\5\7\5\61\n\5\f\5\16\5\64"+
		"\13\5\3\5\3\5\7\58\n\5\f\5\16\5;\13\5\5\5=\n\5\3\6\3\6\3\6\3\6\3\7\3\7"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\bR\n\b\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\5\tZ\n\t\3\n\3\n\3\n\3\n\3\n\3\n\5\nb\n\n\3\13\3\13\3"+
		"\13\2\2\f\2\4\6\b\n\f\16\20\22\24\2\2\2l\2\27\3\2\2\2\4\36\3\2\2\2\6 "+
		"\3\2\2\2\b,\3\2\2\2\n>\3\2\2\2\fB\3\2\2\2\16Q\3\2\2\2\20Y\3\2\2\2\22a"+
		"\3\2\2\2\24c\3\2\2\2\26\30\5\4\3\2\27\26\3\2\2\2\30\31\3\2\2\2\31\27\3"+
		"\2\2\2\31\32\3\2\2\2\32\3\3\2\2\2\33\37\5\6\4\2\34\37\5\b\5\2\35\37\5"+
		"\n\6\2\36\33\3\2\2\2\36\34\3\2\2\2\36\35\3\2\2\2\37\5\3\2\2\2 !\7\3\2"+
		"\2!\"\5\24\13\2\"*\5\20\t\2#\'\7\4\2\2$&\5\f\7\2%$\3\2\2\2&)\3\2\2\2\'"+
		"%\3\2\2\2\'(\3\2\2\2(+\3\2\2\2)\'\3\2\2\2*#\3\2\2\2*+\3\2\2\2+\7\3\2\2"+
		"\2,-\7\5\2\2-.\5\24\13\2.\62\5\20\t\2/\61\5\16\b\2\60/\3\2\2\2\61\64\3"+
		"\2\2\2\62\60\3\2\2\2\62\63\3\2\2\2\63<\3\2\2\2\64\62\3\2\2\2\659\7\4\2"+
		"\2\668\5\f\7\2\67\66\3\2\2\28;\3\2\2\29\67\3\2\2\29:\3\2\2\2:=\3\2\2\2"+
		";9\3\2\2\2<\65\3\2\2\2<=\3\2\2\2=\t\3\2\2\2>?\7\6\2\2?@\5\22\n\2@A\5\22"+
		"\n\2A\13\3\2\2\2BC\5\22\n\2C\r\3\2\2\2DE\5\22\n\2EF\7\7\2\2FG\5\22\n\2"+
		"GR\3\2\2\2HI\5\22\n\2IJ\7\b\2\2JK\5\22\n\2KR\3\2\2\2LM\5\22\n\2MN\7\t"+
		"\2\2NO\5\22\n\2OR\3\2\2\2PR\5\22\n\2QD\3\2\2\2QH\3\2\2\2QL\3\2\2\2QP\3"+
		"\2\2\2R\17\3\2\2\2ST\7\n\2\2TU\7\13\2\2UV\5\22\n\2VW\7\f\2\2WZ\3\2\2\2"+
		"XZ\5\24\13\2YS\3\2\2\2YX\3\2\2\2Z\21\3\2\2\2[b\7\r\2\2\\b\7\16\2\2]b\7"+
		"\17\2\2^b\7\22\2\2_b\7\20\2\2`b\7\21\2\2a[\3\2\2\2a\\\3\2\2\2a]\3\2\2"+
		"\2a^\3\2\2\2a_\3\2\2\2a`\3\2\2\2b\23\3\2\2\2cd\7\23\2\2d\25\3\2\2\2\f"+
		"\31\36\'*\629<QYa";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}