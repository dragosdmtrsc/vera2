package org.change.parser.p4

import java.util.UUID

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :@, LAnd}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.table.MatchKind
import org.change.v2.p4.model.{ISwitchInstance, Switch, SwitchInstance}
import org.change.v2.verification.P4Action

import scala.collection.JavaConversions._

trait ParmInstance
case class LPMMatch(va : FloatingExpression, mask : FloatingExpression) extends ParmInstance
case class RangeMatch(min : FloatingExpression, max : FloatingExpression) extends ParmInstance
case class TernaryMatch(va : FloatingExpression, mask : FloatingExpression) extends ParmInstance
case class ValidMatch(v : FloatingExpression) extends ParmInstance
case class Equal(va : FloatingExpression) extends ParmInstance

case class FullTableGetter(matchAndActions : List[(P4MatchInstance, ActionInstance)],
                           defaultAction: ActionInstance) {
  def sefl(): Instruction = {
    matchAndActions.foldRight(defaultAction.sefl())((r, acc) => {
      r._1.sefl()(acc, r._2.sefl())
    })
  }
}

object FullTableGetter {
  def apply(switchInstance: SwitchInstance, table : String): FullTableGetter = {
    val spec = switchInstance.getSwitchSpec
    switchInstance.flowInstanceIterator(table).map(r => {
      val theAction = switchInstance.getSwitchSpec.getActionRegistrar.getAction(r.getFireAction)
      val parmDefs = r.getMatchParams.map(_.toString).zip(spec.getTableMatches(table)).map(chi => {
        chi._2.getMatchKind match {
          case MatchKind.Exact => Equal(ConstantValue(chi._1.toLong))
          case MatchKind.Ternary =>  ???
          case MatchKind.Lpm => ???
          case MatchKind.Range => ???
          case MatchKind.Valid => ValidMatch(ConstantValue(chi._1.toLong))
        }
      })
      parmDefs
    })
  }
}

case class P4MatchInstance(matchDef : List[(String, Int, ParmInstance)]) {
  def sefl() : (Instruction, Instruction) => Instruction = (i : Instruction, action : Instruction) => {
    matchDef.foldRight(action)((r, acc) => r._3 match {
      case LPMMatch(va, mask) => ???
      case TernaryMatch(va, mask) =>
        val iid = UUID.randomUUID().toString
        InstructionBlock(
          Allocate(s"tmp$iid", r._2),
          Assign(s"tmp$iid",  :&&:(:@(r._1), mask)),
          If (Constrain(s"tmp$iid", :==:(va)),
            acc,
            i)
        )
      case Equal(va) => InstructionBlock(
        If (Constrain(r._1, :==:(va)),
          acc,
          i)
      )
      case RangeMatch(min, max) => {
        If (Constrain(r._1, :&:(:<=:(max), :>=:(min))),
          acc,
          i
        )
      }
      case _ => ???
    })
  }
}

