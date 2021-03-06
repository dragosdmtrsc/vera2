package org.change.p4.control

import java.util.logging.Logger

import generated.parser.p4.commands.{P4CommandsBaseListener, P4CommandsLexer, P4CommandsParser}
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import org.change.p4.ValueSpecificationParser
import org.change.p4.model.Switch
import org.change.p4.model.table.{MatchKind, TableDeclaration}
import org.change.utils.RepresentationConversion

import scala.collection.JavaConverters._

class P4Commands(switch: Switch) extends P4CommandsBaseListener {
  var holder = Instance()
  switch
    .tables()
    .asScala
    .foreach(t => {
      holder = holder
        .setDefault(TableFlow(t, Action(switch.action("no_op")), Nil, Nil, 0))
    })
  var tab: TableDeclaration = null

  override def exitAction_parm(
    ctx: P4CommandsParser.Action_parmContext
  ): Unit = {
    ctx.actionParam = ParamValue(ctx.unsigned_value().unsignedValue)
  }
  override def enterTable_add(ctx: P4CommandsParser.Table_addContext): Unit = {
    tab = switch.table(ctx.id().getText)
  }

  override def enterTable_default(
    ctx: P4CommandsParser.Table_defaultContext
  ): Unit = {
    tab = switch.table(ctx.id().getText)
  }

  override def exitAct_prof_create_member(
               ctx: P4CommandsParser.Act_prof_create_memberContext): Unit = {
    val profileName = ctx.NAME(0).getText
    val member = ctx.unsigned_value().unsignedValue.toInt
    val actionName = ctx.NAME(1).getText
    val parms = ctx.action_parm().asScala.map(_.actionParam)
    val actProfile = ProfileEntry(
      profileName,
      member,
      Action(switch.action(actionName)),
      parms.toList
    )
    holder = holder.add(actProfile)
  }

  override def exitEmptyAction(ctx: P4CommandsParser.EmptyActionContext): Unit = {
    Logger.getLogger("vera")
      .info("empty action detected, resolve to noop")
    ctx.actionSpec = null
  }

  override def exitTable_default(
    ctx: P4CommandsParser.Table_defaultContext
  ): Unit = {
    tab = null
    val table = ctx.id().getText
    val td = switch.table(table)
    if (td == null)
      throw new IllegalStateException(
        s"referencing table $table which is not there"
      )
    val act = ctx.act_spec().actionSpec
    if (act != null) {
      val parmlist = ctx.action_parm().asScala.map(_.actionParam).toList
      holder = holder.setDefault(TableFlow(td, act, Nil, parmlist, 0))
    }
  }
  override def exitMemberAction(
    ctx: P4CommandsParser.MemberActionContext
  ): Unit = {
    val idx = ctx.unsigned_value().unsignedValue.toInt
    val prof = tab.actionProfile()
    ctx.actionSpec = ProfileMember(prof, idx)
  }
  override def exitNamedAction(
    ctx: P4CommandsParser.NamedActionContext
  ): Unit = {
    val act = switch.action(ctx.id().getText)
    if (act == null)
      throw new IllegalArgumentException(s"got action ${ctx.id().getText}, but it was not found")
    ctx.actionSpec = Action(act)
  }
  override def exitMasked(ctx: P4CommandsParser.MaskedContext): Unit = {
    val v = ctx.unsigned_value(0).unsignedValue
    val m = ctx.unsigned_value(1).unsignedValue
    ctx.matchParam = Masked(v, m)
  }
  override def exitPrefix(ctx: P4CommandsParser.PrefixContext): Unit = {
    val v = ctx.unsigned_value(0).unsignedValue
    val m = ctx.unsigned_value(1).unsignedValue
    ctx.matchParam = Prefix(v, m.toInt)
  }
  override def exitRange(ctx: P4CommandsParser.RangeContext): Unit = {
    val v = ctx.unsigned_value(0).unsignedValue
    val m = ctx.unsigned_value(1).unsignedValue
    ctx.matchParam = Range(v, m)
  }
  override def exitExact(ctx: P4CommandsParser.ExactContext): Unit = {
    val v = ctx.unsigned_value().unsignedValue
    ctx.matchParam = Exact(v)
  }

  override def exitTable_add(ctx: P4CommandsParser.Table_addContext): Unit = {
    tab = null
    val table = ctx.id().getText
    val td = switch.table(table)
    if (td == null)
      throw new IllegalStateException(
        s"referencing table $table which is not there"
      )
    val act = ctx.act_spec().actionSpec
    val matches = ctx.match_key().asScala.map(_.matchParam).toList
    assert(td.getMatches.size() == matches.size)
    val zipped = td.getMatches.asScala.zip(matches)
    val parmlist = ctx.action_parm().asScala.map(_.actionParam).toList
    val (prio, allParms) = zipped
      .find(_._1.getMatchKind == MatchKind.Lpm)
      .map(x => {
        (-x._2.asInstanceOf[Prefix].prefix, parmlist)
      })
      .getOrElse({
        if (zipped.exists(x => {
              x._1.getMatchKind == MatchKind.Range ||
              x._1.getMatchKind == MatchKind.Ternary
            }))
          (
            ctx
              .action_parm()
              .asScala
              .last
              .actionParam
              .asInstanceOf[ParamValue]
              .v
              .toInt,
            parmlist.take(parmlist.size - 1)
          )
        else (0, parmlist)
      })
    holder = holder.add(TableFlow(td, act, matches, allParms, prio))
  }
  override def exitBinaryUValue(
    ctx: P4CommandsParser.BinaryUValueContext
  ): Unit = {
    ctx.unsignedValue =
      ValueSpecificationParser.binaryToInt(ctx.Binary_value().getText)
  }
  override def exitDecimalUValue(
    ctx: P4CommandsParser.DecimalUValueContext
  ): Unit = {
    ctx.unsignedValue =
      ValueSpecificationParser.decimalToInt(ctx.Decimal_value().getText)
  }
  override def exitHexadecimalUValue(
    ctx: P4CommandsParser.HexadecimalUValueContext
  ): Unit = {
    ctx.unsignedValue = ValueSpecificationParser.hexToInt(
      ctx.Hexadecimal_value().getText.substring(2)
    )
  }
  override def exitMacUValue(ctx: P4CommandsParser.MacUValueContext): Unit = {
    ctx.unsignedValue = RepresentationConversion.macToNumber(ctx.getText)
  }
  override def exitIPUValue(ctx: P4CommandsParser.IPUValueContext): Unit = {
    ctx.unsignedValue = RepresentationConversion.ipToNumber(ctx.getText)
  }
  override def exitIP6UValue(ctx: P4CommandsParser.IP6UValueContext): Unit = {
    ctx.unsignedValue = RepresentationConversion.ip6ToNumber(ctx.getText)
  }
}

object P4Commands {
  def fromFile(switch: Switch, commandstxt: String): Instance = {
    val p4Input = CharStreams.fromFileName(commandstxt)
    val lexer = new P4CommandsLexer(p4Input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new P4CommandsParser(tokens)
    val listener = new P4Commands(switch)
    val tree = parser.statements()
    val walker = new ParseTreeWalker
    walker.walk(listener, tree)
    listener.holder.normalize()
  }
}
