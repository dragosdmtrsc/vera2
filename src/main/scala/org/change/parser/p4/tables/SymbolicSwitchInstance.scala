package org.change.parser.p4.tables

import java.util
import java.util.regex.Pattern

import org.change.parser.p4.factories.FullTableFactory
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
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
                             tableDefinitions : Map[String, P4TableDefinition]) extends ISwitchInstance {
  def this(name : String,
           ifaces : Map[Int, String],
           cloneSpec : Map[Int, Int],
           switch: Switch) = {
    this(name, ifaces, cloneSpec, switch, switch.getDeclaredTables.map(r => {
      r -> P4TableDefinition()
    }).toMap)
  }
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


  private def fromString(value : String):  FloatingExpression = {
    if (value.equalsIgnoreCase("x"))
      SymbolicValue()
    else if (IPAddressUtil.isIPv4LiteralAddress(value))
      ConstantValue(RepresentationConversion.ipToNumber(value))
    else {
      val p: Pattern = Pattern.compile("([0-9A-F]{2}[:-]){5}([0-9A-F]{2})")
      if (p.matcher(value.toUpperCase).matches)
        ConstantValue(RepresentationConversion.macToNumber(value.toUpperCase()))
      else
        ConstantValue(value.toLong)
    }
  }
  private def matchKindAndParamsToDef(matchKind: TableMatch, value : String): ParmInstance =  matchKind.getMatchKind match {
    case MatchKind.Exact => Equal(fromString(value))
    case MatchKind.Ternary => val values = value.split("&&&")
      TernaryMatch(fromString(values.head), fromString(values(1)))
    case MatchKind.Lpm => val values = value.split("/")
      LPMMatch(fromString(values.head), fromString(values(1)))
    case MatchKind.Range => val values = value.split(",")
      RangeMatch(fromString(values.head), fromString(values(1)))
    case MatchKind.Valid => ValidMatch(fromString(value))
  }

  private  def fromActionCall(p4ActionCall: P4ActionCall): ActionDefinition = {
    import scala.collection.JavaConversions._
    if (p4ActionCall != null)
      ActionDefinition(p4ActionCall.getP4Action.getActionName, p4ActionCall.parameterInstances().map(r => {
        r.getParameter.getParamName -> fromString(r.getValue)
      }).toMap)
    else
      DropAction
  }


  def fromFileWithSyms(name: String,
                       ifaces: Map[Int, String],
                       cloneSpec: Map[Int, Int],
                       switch: Switch, file : String): SymbolicSwitchInstance = {
    val switchInstance = SwitchInstance.populateSwitchInstance(file, switch,
      new SwitchInstance(name, switch, ifaces.map(r => Integer.valueOf(r._1) -> r._2).asJava))
    import scala.collection.JavaConversions._
    val tables = switch.getDeclaredTables.map(r => {
      r -> P4TableDefinition(switchInstance.flowInstanceIterator(r).map(u => {
        val actionName = u.getFireAction
        val actionDef = switch.getActionRegistrar.getAction(actionName)
        val actionParams = u.getActionParams.zip(actionDef.getParameterList.map(u => u.getParamName)).map(r => r._2 -> (r._1 match {
          case s: String if s.equalsIgnoreCase("x") => SymbolicValue(): FloatingExpression
          case lg: java.lang.Long => ConstantValue(lg): FloatingExpression
          case in: java.lang.Integer => ConstantValue(in.intValue()): FloatingExpression
          case _ => ???
        })).toMap
        P4FlowInstance(switch.getTableMatches(r).zip(u.getMatchParams).map(ma => {
          ma._1.getKey -> matchKindAndParamsToDef(ma._1, ma._2.toString)
        }).toMap, ActionDefinition(action = actionName, actionParams = actionParams))
      }).toList, fromActionCall(switchInstance.getDefaultAction(r)))
    }).toMap
    SymbolicSwitchInstance(name, ifaces, cloneSpec, switch, tables)
  }

  def apply(name: String,
            ifaces: Map[Int, String],
            cloneSpec: Map[Int, Int],
            switch: Switch): SymbolicSwitchInstance =
    new SymbolicSwitchInstance(name, ifaces, cloneSpec, switch)

}