package org.change.parser.p4.factories

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.p4.model.ISwitchInstance

import scala.collection.mutable

object FullTableFactory {

  private val registrar = mutable.Map[Class[_], (ISwitchInstance, String, String) => Instruction]()

  def register[T<:ISwitchInstance](classof : Class[T], fun : Function3[T, String, String, Instruction]) : Unit =
    registrar.put(classof, (x : ISwitchInstance, t : String, id : String) => x match {
      case v : T => fun(v, t, id)
      case _ => throw new ClassCastException(s"Supply an argument of kind $classof")
    })
  def get[T<:ISwitchInstance](classof : Class[T]): (T, String, String) => Instruction =
    (x: T, tabName: String, id: String) =>
      registrar.getOrElse(classof, (_: T, _: String, _: String) => NoOp)(x, tabName, id)
}
