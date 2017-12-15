package org.change.parser.p4

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.p4.model.ISwitchInstance

import scala.collection.mutable

object FullTableFactory {

  private val registrar = mutable.Map[Class[_], Function3[ISwitchInstance, String, String, Instruction]]()

  def register[T<:ISwitchInstance](classof : Class[T], fun : Function3[ISwitchInstance, String, String, Instruction]) : Unit =
    registrar.put(classof, fun)
  def get(classof : Class[_<:ISwitchInstance]): Function3[ISwitchInstance, String, String, Instruction] =
    (x: ISwitchInstance, tabName: String, id: String) =>
      registrar.getOrElse(classof, (x: ISwitchInstance, tabName: String, id: String) => NoOp)(x, tabName, id)
}
