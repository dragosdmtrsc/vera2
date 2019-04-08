package org.change.p4.control

import org.change.p4.model.actions.{P4Action, P4ActionProfile}
import org.change.p4.model.table.TableDeclaration

trait MatchParam
case class Masked(v: BigInt, m: BigInt) extends MatchParam
case class Exact(v: BigInt) extends MatchParam
case class Prefix(v: BigInt, prefix: Int) extends MatchParam
case class Range(min: BigInt, max: BigInt) extends MatchParam

trait ActionParam
case class ParamValue(v: BigInt) extends ActionParam
trait ActionSpec
case class Action(p4Action: P4Action) extends ActionSpec
case class ProfileMember(actionProfile: P4ActionProfile, idx: Int)
    extends ActionSpec
case class ProfileCall(actionProfile: P4ActionProfile) extends ActionSpec

case class TableFlow(tableDeclaration: TableDeclaration,
                     action: ActionSpec,
                     matches: List[MatchParam],
                     parms: List[ActionParam],
                     prio: BigInt)
case class Instance(tables: Map[String, List[TableFlow]] = Map.empty,
                    defaults: Map[String, TableFlow] = Map.empty,
                    profiles: Map[String, List[ProfileEntry]] = Map.empty,
                    mirrors: Map[BigInt, List[BigInt]] = Map.empty) {
  def add(tableFlow: TableFlow): Instance = {
    val existing = tables.getOrElse(tableFlow.tableDeclaration.getName, Nil)
    copy(
      tables = tables + (tableFlow.tableDeclaration.getName -> (existing :+ tableFlow))
    )
  }
  def add(profileEntry: ProfileEntry) : Instance = {
    val old = profiles.getOrElse(profileEntry.profName, Nil)
    copy(profiles = profiles.updated(profileEntry.profName, old :+ profileEntry))
  }
  def setDefault(tableFlow: TableFlow): Instance = {
    copy(
      defaults = defaults + (tableFlow.tableDeclaration.getName -> tableFlow)
    )
  }
  def normalize(): Instance = {
    copy(tables = tables.mapValues(t => {
      t.sortBy(f => f.prio)
    }), profiles = profiles.mapValues(t => {
      t.sortBy(_.member)
    }))
  }
}
case class ProfileEntry(profName : String,
                        member : Int,
                        action: Action,
                        parms: List[ActionParam])
