package org.change.parser.p4

import org.change.parser.p4.factories.{GlobalInitFactory, InitCodeFactory}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.InstanceType._
import org.change.v2.p4.model._

import scala.collection.JavaConversions._


/**
  * Created by dragos on 01.09.2017.
  */
class InitializeCode[T<:ISwitchInstance](switchInstance : T,
                                         swSpec : Switch,
                                         additionalInitCode : Function2[T, Int, Instruction],
                                         initFactory: Function1[T, Instruction]) {
  def this(switchInstance : T,
           swSpec : Switch) = this(switchInstance, swSpec,
    InitCodeFactory.get(switchInstance.getClass.asInstanceOf[Class[T]]),
    GlobalInitFactory.get(switchInstance.getClass.asInstanceOf[Class[T]]))

  def initializeMetadata(butFor : List[String] = Nil) : Instruction = {
    InstructionBlock(
      Allocate("Truncate", 1) :: Assign("Truncate", ConstantValue(0)) :: swSpec.getInstances.filter(_.isMetadata).flatMap(x => {
      if (!butFor.contains(x.getName)) {
        x.getLayout.getFields.map(f => {
          if (!butFor.contains(x.getName + "." + f.getName)) {
            InstructionBlock(
              Allocate(x.getName + "." + f.getName, f.getLength),
              if (x.getInitializer.containsKey(f.getName))
                Assign(x.getName + "." + f.getName, ConstantValue(x.getInitializer()(f.getName).longValue()))
              else
                Assign(x.getName + "." + f.getName, ConstantValue(0))
            )
          } else {
            NoOp
          }
        })
      } else {
        List[Instruction](NoOp)
      }
    }).toList
    )
  }

  def initStruct(isMeta : Boolean, base : String, hdr : Header) : Iterable[Instruction] = {
    if (isMeta) {
      Nil
    } else {
      List(
        Allocate(base + ".IsValid", 1),
        Assign(base + ".IsValid", ConstantValue(0))
      )
    } ++ hdr.getFields.flatMap(fld => {
      List(
        Allocate(base + s".${fld.getName}", fld.getLength),
        if (isMeta)
          Assign(base + s".${fld.getName}", ConstantValue(0))
        else NoOp
      )
    })
  }

  def initializeFields() : Instruction = {
    InstructionBlock(
      swSpec.getInstances.filter(!_.isMetadata).flatMap(x => {
        x match {
          case ai: ArrayInstance =>
            val len = ai.getLength
            (0 until len).flatMap(z => {
               initStruct(ai.isMetadata, x.getName + "[" + z + "]", ai.getLayout)
            })
          case _ => initStruct(x.isMetadata, x.getName, x.getLayout)
        }
      }) ++ swSpec.getInstances.collect {
        case ai : ArrayInstance =>
          val len = ai.getLength
          Allocate(ai.getName + ".next", 32) :: Assign(ai.getName + ".next", ConstantValue(0)) :: Nil
      }.flatten
    )
  }

  def switchInitializePacketEnter(port: Int) : Instruction = {
    InstructionBlock(
      initializeMetadata(),
      initializeFields(),
      Assign("standard_metadata.ingress_port", ConstantValue(port)),
      Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_NORMAL.value)),
      additionalInitCode(switchInstance, port),
      Forward(s"${switchInstance.getName}.input.$port.out")
    )
  }

  def switchInitializeGlobally() : Instruction = initFactory(switchInstance)
}
