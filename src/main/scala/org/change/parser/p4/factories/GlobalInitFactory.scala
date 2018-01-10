package org.change.parser.p4.factories

import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Assign, InstructionBlock, NoOp}
import org.change.v2.p4.model.{ISwitchInstance, Switch, SwitchInstance}

import scala.collection.JavaConversions._
import scala.collection.mutable

object GlobalInitFactory {
  private val registrar = mutable.Map[Class[_<:ISwitchInstance], Function1[ISwitchInstance, Instruction]]()
  def register[T<:ISwitchInstance](ofClass: Class[T], factoryClass: T => Instruction) :  Unit =
    registrar.put(ofClass, {
      case v: T => factoryClass(v)
      case _ => throw new ClassCastException(s"Supply an argument of kind $ofClass")
    })
  def get[T<:ISwitchInstance](ofclass : Class[T]): (T) => Instruction = (r : T) => {
    registrar.getOrElse(ofclass, (_ : T) => NoOp)(r)
  }
}
class SymbolicRegistersInitFactory(switchInstance: SwitchInstance) {
  private val swSpec: Switch = switchInstance.getSwitchSpec
  def initCode() = // handle registers and other stuff like that
    InstructionBlock(swSpec.getRegisterSpecificationMap.values().filter(r => !r.isDirect && !r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg.${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg.${x.getName}[$i]", SymbolicValue())
        )
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", SymbolicValue())
        )
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isDirect).flatMap(x => {
      (0 until switchInstance.flowInstanceIterator(x.getStaticTable).size()).map(i => {
        InstructionBlock(
          Allocate(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", x.getWidth),
          Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", SymbolicValue())
        )
      })
    }))
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
