package org.change.parser.p4.tables

import org.change.parser.p4.ActionInstance
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.actions.P4ActionCall
import org.change.v2.p4.model.actions.primitives.Drop

import scala.collection.JavaConversions._
/**
  * Created by dragos on 31.08.2017.
  */
class FireAction(tableName : String, flowNumber : Int, switchInstance: SwitchInstance) {
  private lazy val flow = switchInstance.flowInstanceIterator(tableName).get(flowNumber)
  private lazy val action = switchInstance.getSwitchSpec.getActionRegistrar.getAction(flow.getFireAction)
  private lazy val args = flow.getActionParams

  def symnetCode() : Instruction = {
    new ActionInstance(action, args.toList.map(r => ConstantValue(r.asInstanceOf[Long])),
      switchInstance,
      switchInstance.getSwitchSpec,
      tableName,
      flowNumber,
      s"Dropped by flow $flowNumber@$tableName").sefl()
  }
}

class FireDefaultAction(tableName : String, switchInstance: SwitchInstance) {
  val p4ActionCall =
    if (switchInstance.getDefaultAction(tableName) == null)
      new P4ActionCall(new Drop())
    else
        switchInstance.getDefaultAction(tableName)
   def symnetCode() : Instruction = new ActionInstance(
     p4ActionCall.getP4Action,
     p4ActionCall.parameterInstances().map( x => ConstantValue(x.getValue.toLong)).toList,
     switchInstance,
     switchInstance.getSwitchSpec,
     tableName,
     -1,
     s"dropped by default@$tableName").sefl()
}
