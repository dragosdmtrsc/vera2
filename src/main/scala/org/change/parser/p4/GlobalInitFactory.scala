package org.change.parser.p4

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Assign, InstructionBlock, NoOp}
import org.change.v2.p4.model.{ISwitchInstance, Switch, SwitchInstance}

import scala.collection.JavaConversions._
import scala.collection.mutable

object GlobalInitFactory {
  private val registrar = mutable.Map[Class[_<:ISwitchInstance], Function1[ISwitchInstance, Instruction]]()
  def register[T<:ISwitchInstance](ofClass: Class[T], factoryClass: ISwitchInstance => Instruction) :  Unit =
    registrar.put(ofClass, factoryClass)
  def get(ofclass : Class[_<:ISwitchInstance]): Function1[ISwitchInstance, Instruction] = (r : ISwitchInstance) => {
    registrar.getOrElse(ofclass, (_ : ISwitchInstance) => { NoOp })(r)
  }
}

class InstanceBasedInitFactory(switchInstance: SwitchInstance) {
  private val swSpec: Switch = switchInstance.getSwitchSpec
  def initCode() = // handle registers and other stuff like that
    InstructionBlock(swSpec.getRegisterSpecificationMap.values().filter(r => !r.isDirect && !r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg.${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg.${x.getName}[$i]", ConstantValue(0))
        )
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", ConstantValue(0))
        )
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isDirect).flatMap(x => {
      (0 until switchInstance.flowInstanceIterator(x.getStaticTable).size()).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", ConstantValue(0))
        )
      })
    }))
}
