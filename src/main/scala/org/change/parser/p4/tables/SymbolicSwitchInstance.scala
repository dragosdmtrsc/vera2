package org.change.parser.p4.tables

import java.util
import java.util.regex.Pattern

import org.change.parser.p4.factories.FullTableFactory
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantValue}
import org.change.v2.p4.model.actions.P4ActionCall
import org.change.v2.p4.model.table.{MatchKind, TableMatch}
import org.change.v2.p4.model.{ISwitchInstance, Switch, SwitchInstance}
import org.change.v2.util.conversion.RepresentationConversion
import sun.net.util.IPAddressUtil

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

case class SymbolicSwitchInstance(name : String,
                             ifaces : Map[Int, String],
                             cloneSpec : Map[Int, Int],
                             switch: Switch,
                             tableDefinitions : Map[String, P4TableDefinition],
                             symbolicTableParams: Set[String]) extends ISwitchInstance {
  override lazy val getCloneSpec2EgressSpec: util.Map[Integer, Integer] = cloneSpec.map(r => {
    new Integer(r._1) -> new Integer(r._2)
  }).asJava

  override lazy val getIfaceSpec: util.Map[Integer, String] = ifaces.map(r => new Integer(r._1) -> r._2).asJava

  override def getName: String = name

  def getSwitch : Switch = switch
}

object SymbolicSwitchInstance {
  FullTableFactory.register(classOf[SymbolicSwitchInstance], (switchInstance: SymbolicSwitchInstance, tableName : String, id : String) => {
    new FullTableWithInstances(tableName = tableName,
      id = id,
      switch = switchInstance.getSwitch,
      flowDefinitions = switchInstance.tableDefinitions(tableName).flowInstances,
      defaultAction = switchInstance.tableDefinitions(tableName).defaultAction,
      switchInstance = switchInstance
    ).fullAction()
  })


  private def parseMatchParam(value : String, symbolName: String):  FloatingExpression = {
    if (value.equalsIgnoreCase("x")) {
      // If a symbolic param is used, a non empty symbol name must be provided
      assert(symbolName != "")
      :@(symbolName)
    }
    else if (IPAddressUtil.isIPv4LiteralAddress(value))
      ConstantValue(RepresentationConversion.ipToNumber(value))
    else {
      val p: Pattern = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})")
      if (p.matcher(value.toUpperCase).matches)
        ConstantValue(RepresentationConversion.macToNumber(value.toUpperCase()))
      else {
        if (value.startsWith("0x"))
          ConstantBValue(s"#x${value.substring(2)}", size = value.substring(2).length / 2 * 8)
        else ConstantValue(value.toLong)
      }
    }
  }
  private def matchKindAndParamsToDef(
                                       matchKind: TableMatch,
                                       value : String,
                                       symbolName: String): (ParmInstance, Set[String]) = matchKind.getMatchKind match {
    case MatchKind.Exact => Equal(parseMatchParam(value, symbolName)) ->
      mapValuesToIntroducedSymbolNames(Seq(value),Seq(symbolName))
    case MatchKind.Ternary => val values = value.split("&&&")
      val leftSymbolName = symbolName + ".l"
      val rightSymbolName = symbolName + ".r"
      TernaryMatch(parseMatchParam(values.head, leftSymbolName), parseMatchParam(values(1), rightSymbolName)) ->
        mapValuesToIntroducedSymbolNames(values, Seq(leftSymbolName, rightSymbolName))
    case MatchKind.Lpm => val values = value.split("/")
      val leftSymbolName = symbolName + ".l"
      val rightSymbolName = symbolName + ".r"
      LPMMatch(parseMatchParam(values.head, leftSymbolName), parseMatchParam(values(1), rightSymbolName)) ->
        mapValuesToIntroducedSymbolNames(values, Seq(leftSymbolName, rightSymbolName))
    case MatchKind.Range => val values = value.split(",")
      val leftSymbolName = symbolName + ".l"
      val rightSymbolName = symbolName + ".r"
      RangeMatch(parseMatchParam(values.head, leftSymbolName), parseMatchParam(values(1), rightSymbolName)) ->
        mapValuesToIntroducedSymbolNames(values, Seq(leftSymbolName, rightSymbolName))
    case MatchKind.Valid => ValidMatch(parseMatchParam(value, symbolName)) ->
      mapValuesToIntroducedSymbolNames(Seq(value),Seq(symbolName))
  }

  def mapValuesToIntroducedSymbolNames(values: Iterable[String], symbols: Iterable[String]): Set[String] =
    (values zip symbols).filter(_._1.toLowerCase.contains("x")).map(_._2).toSet

  private  def fromActionCall(p4ActionCall: P4ActionCall): ActionDefinition = {
    import scala.collection.JavaConversions._
    if (p4ActionCall != null)
      ActionDefinition(p4ActionCall.getP4Action.getActionName, p4ActionCall.parameterInstances().map(r => {
        r.getParameter.getParamName -> parseMatchParam(r.getValue, "")
      }).toMap)
    else
      DropAction
  }

  def fromFileWithSyms(name: String,
                       ifaces: Map[Int, String],
                       cloneSpec: Map[Int, Int],
                       switch: Switch, file : String, forceSymbolic : Boolean = false): SymbolicSwitchInstance = {
    val switchInstance = SwitchInstance.populateSwitchInstance(file, switch,
      new SwitchInstance(name, switch, ifaces.map(r => Integer.valueOf(r._1) -> r._2).asJava))
    import scala.collection.JavaConversions._

    import scala.collection.mutable.{Set => MSet}
    val introducedSymbolicTableParams = MSet[String]()

    val tables = switch.getDeclaredTables.map(tableName => {
      tableName -> P4TableDefinition(switchInstance.flowInstanceIterator(tableName).zipWithIndex.map(entryIndexPair => {
        val (tableEntry, perTableIndex) = entryIndexPair
        val actionName = tableEntry.getFireAction
        val actionDef = switch.getActionRegistrar.getAction(actionName)
        val actionParams = tableEntry.getActionParams.zip(actionDef.getParameterList.map(_.getParamName)).map(r => r._2 -> (r._1 match {
          // Add a symbolic entry
          case s: String if s.equalsIgnoreCase("x") =>
            val symbolName = s"actionParam.$tableName.$perTableIndex.$actionName.${r._2}"
            introducedSymbolicTableParams.add(symbolName)
            :@(symbolName) : FloatingExpression
          case lg: java.lang.Long => if (!forceSymbolic)
            ConstantValue(lg): FloatingExpression
          else {
            val symbolName = s"actionParam.$tableName.$perTableIndex.$actionName.${r._2}"
            introducedSymbolicTableParams.add(symbolName)
            :@(symbolName) : FloatingExpression
          }
          case in: java.lang.Integer => if (!forceSymbolic)
              ConstantValue(in.intValue()): FloatingExpression
          else {
            val symbolName = s"actionParam.$tableName.$perTableIndex.$actionName.${r._2}"
            introducedSymbolicTableParams.add(symbolName)
            :@(symbolName) : FloatingExpression
          }
          case _ => ???
        })).toMap

        P4FlowInstance(
          switch.getTableMatches(tableName)
            .zip(tableEntry.getMatchParams)
            .map(ma => {
              ma._1.getKey -> {
                val matchSymbolPrefix = s"matchParam.$tableName.$perTableIndex.${ma._1.getKey}"
                val (constructedMatch, introducedSymbols) = matchKindAndParamsToDef(ma._1, ma._2.toString, matchSymbolPrefix)
                introducedSymbolicTableParams.addAll(introducedSymbols)
                constructedMatch
              }
            }).toMap, ActionDefinition(action = actionName, actionParams = actionParams))
      }).toList, fromActionCall(switchInstance.getDefaultAction(tableName)))
    }).toMap

    SymbolicSwitchInstance(name, ifaces, cloneSpec, switch, tables, introducedSymbolicTableParams.toSet)
  }

}