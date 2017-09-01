package org.change.parser.p4

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Assign, InstructionBlock, NoOp}
import org.change.v2.p4.model.{ArrayInstance, HeaderInstance, SwitchInstance}

import scala.collection.JavaConversions._


/**
  * Created by dragos on 01.09.2017.
  */
class InitializeCode(switchInstance : SwitchInstance) {

  val swSpec = switchInstance.getSwitchSpec
  val ctx = swSpec.getCtx

  def initializeMetadata() : Instruction = {
    InstructionBlock(switchInstance.getSwitchSpec.getCtx.instances.values().filter(_.isMetadata).flatMap(x => {
      x.getLayout.getFields.map(f => {
        if (x.getInitializer.containsKey(f.getName))
          Assign(x.getName + "." + f.getName, ConstantValue(x.getInitializer()(f.getName).longValue()))
        else
          Assign(x.getName + "." + f.getName, ConstantValue(0))
      })
    })
    )
  }

  def initializeFields() : Instruction = {
    InstructionBlock(
      switchInstance.getSwitchSpec.getCtx.instances.values().filter(!_.isMetadata).flatMap(x => x match {
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
      Assign("standard_metadata.instance_type", ConstantValue(0))
    )
  }


  def switchInitializeGlobally() : Instruction = {
    // handle registers and other stuff like that
    InstructionBlock(swSpec.getRegisterSpecificationMap.values().filter(r => !r.isDirect && !r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        Assign(s"${switchInstance.getName}.reg.${x.getName}[$i]", ConstantValue(0))
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isStatic).flatMap(x => {
      (0 until x.getCount).map(i => {
        Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", ConstantValue(0))
      })
    }) ++ swSpec.getRegisterSpecificationMap.values().filter(r => r.isDirect).flatMap(x => {
      (0 until switchInstance.flowInstanceIterator(x.getStaticTable).size()).map(i => {
        Assign(s"${switchInstance.getName}.reg[${x.getStaticTable}].${x.getName}[$i]", ConstantValue(0))
      })
    }))

  }
}
