package org.change.parser.p4

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
    return new ActionInstance(action, args.toList, switchInstance, s"Dropped by flow $flowNumber@$tableName").sefl()
  }

}

class FireDefaultAction(tableName : String, switchInstance: SwitchInstance) {
  val p4ActionCall =
    if (switchInstance.getDefaultAction(tableName) == null)
      new P4ActionCall(new Drop())
    else
        switchInstance.getDefaultAction(tableName)
   def symnetCode() : Instruction = {
    return new ActionInstance(
      p4ActionCall.getP4Action, p4ActionCall.parameterInstances().map( x => x.getValue).toList,
      switchInstance, s"dropped by default@$tableName").sefl()
  }
}
