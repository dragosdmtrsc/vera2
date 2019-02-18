package org.change.parser.p4.buffer

import org.change.parser.p4.parser.{DFSState, Graph}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model._
import org.change.v2.p4.model.parser._

import scala.collection.JavaConversions._
import scala.collection.immutable.Stack

/**
  * Created by dragos on 15.09.2017.
  */
class BufferMechanism(switchInstance: ISwitchInstance) {

  def cloneCase() = InstructionBlock(
    switchInstance.getCloneSpec2EgressSpec.
      foldRight(Fail("No clone mapping found. Resolve to drop") : Instruction)((x, acc) => {
      If (Constrain(s"${switchInstance.getName}.CloneCookie", :==:(ConstantValue(x._1.longValue()))),
        Assign("standard_metadata.egress_port", ConstantValue(x._2.longValue())),
        acc
      )
    })
  )

  def normalCase(): Instruction = {
    val default = If (Constrain("standard_metadata.egress_spec", :==:(ConstantValue(511l))),
      Fail("Egress spec set to 511 <=> dropping packet post ingress"),
      Fail("Egress spec not set. Resolve to drop")
    )
    val cnstr = Fork(switchInstance.getIfaceSpec.keySet().map(x => {
      Constrain("standard_metadata.egress_spec", :==:(ConstantValue(x.longValue())))
    }))
    If (cnstr,
      Assign("standard_metadata.egress_port", :@("standard_metadata.egress_spec")),
      default
    )
  }

  def symnetCode() : Instruction = InstructionBlock(
      If(Constrain("standard_metadata.instance_type", :|:(
        :|:(
          :==:(ConstantValue(InstanceType.PKT_INSTANCE_TYPE_NORMAL.value)),
          :==:(ConstantValue(InstanceType.PKT_INSTANCE_TYPE_RESUBMIT.value))
        ),
        :==:(ConstantValue(InstanceType.PKT_INSTANCE_TYPE_RESUBMIT.value)))
      ),
        normalCase(),
        cloneCase()
      ),
      out()
    )

  def out() = Forward(outName())

  def outName() = s"${switchInstance.getName}.buffer.out"
}


class OutputMechanism(switchInstance: ISwitchInstance) {
  def symnetCode() : Instruction = {
    switchInstance.getIfaceSpec.foldRight(Fail("Cannot find egress_port match for current interfaces") : Instruction)((x, acc) => {
      If (Constrain("standard_metadata.egress_port", :==:(ConstantValue(x._1.longValue()))),
        Forward(s"${switchInstance.getName}.output.${x._1}"),
        acc
      )
    })
  }
}
