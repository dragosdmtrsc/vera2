package org.change.parser.p4.factories

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.p4.model.ISwitchInstance

import scala.collection.mutable

object InitCodeFactory {
  private val registrar = mutable.Map[Class[_], (ISwitchInstance, Int) => Instruction]()

  def register[T<:ISwitchInstance](classof : Class[T], fun : Function2[T, Int, Instruction]) : Unit =
    registrar.put(classof, (x : ISwitchInstance, t : Int) => x match {
      case v : T => fun(v, t)
      case _ => throw new ClassCastException(s"Supply an argument of kind $classof")
    })
  def get[T<:ISwitchInstance](classof : Class[T]): (T, Int) => Instruction =
    (x: T, portNo : Int) =>
      registrar.getOrElse(classof, (_: T,  _ : Int) => NoOp)(x, portNo)
}
