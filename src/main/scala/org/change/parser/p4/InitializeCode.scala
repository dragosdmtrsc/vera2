package org.change.parser.p4

import org.change.parser.p4.factories.GlobalInitFactory
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.InstanceType._
import org.change.v2.p4.model._

import scala.collection.JavaConversions._


/**
  * Created by dragos on 01.09.2017.
  */
class InitializeCode(switchInstance : ISwitchInstance,
                     swSpec : Switch) {
  def initializeMetadata(butFor : List[String] = Nil) : Instruction = {
    InstructionBlock(swSpec.getInstances.filter(_.isMetadata).flatMap(x => {
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
    })
    )
  }

  def initializeFields() : Instruction = {
    InstructionBlock(
      swSpec.getInstances.filter(!_.isMetadata).flatMap(x => x match {
        case ai: ArrayInstance =>
          val len = ai.getLength
          (0 to len).map(z => {
            val baseLine = x.getName + z
            Assign(x.getName + z + ".IsValid", ConstantValue(0))
          })
        case _ =>
          Assign(x.getName + ".IsValid", ConstantValue(0)) :: Nil
      })
    )
  }

  def switchInitializePacketEnter(port: Int) : Instruction = {
    InstructionBlock(
      initializeMetadata(),
      initializeFields(),
      Assign("standard_metadata.ingress_port", ConstantValue(port)),
      Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_NORMAL.value)),
      Forward(s"${switchInstance.getName}.input.$port.out")
    )
  }

  def switchInitializeGlobally() : Instruction = GlobalInitFactory.get(switchInstance.getClass)(switchInstance)
}
