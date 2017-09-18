package org.change.parser.p4.buffer

import org.change.parser.p4.parser.DFSState
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.parser.{CaseEntry, ExtractStatement, StringRef}

import scala.collection.JavaConversions._

/**
  * Created by dragos on 15.09.2017.
  */
class BufferMechanism(switchInstance: SwitchInstance) {

  def cloneCase() = InstructionBlock(
    switchInstance.getCloneSpec2EgressSpec.foldRight(Fail("No clone mapping found. Resolve to drop") : Instruction)((x, acc) => {
      If (Constrain(s"${switchInstance.getName}.CloneCookie", :==:(ConstantValue(x._1.longValue()))),
        Assign("egress_port", ConstantValue(x._2.longValue())),
        acc
      )
    })
  )

  def normalCase() = switchInstance.getIfaceSpec.keySet().foldRight(Fail("Egress spec not set. Resolve to drop") : Instruction)((x, acc) => {
    If (Constrain("standard_metadata.egress_spec", :==:(ConstantValue(x.longValue()))),
      Assign("standard_metadata.egress_port", ConstantValue(x.longValue())),
      acc
    )
  })

  def symnetCode() : Instruction = InstructionBlock(
      If(Constrain("standard_metadata.instance_type", :==:(ConstantValue(0))),
        normalCase(),
        cloneCase()
      ),
      out()
    )

  def out() = Forward(outName)

  def outName() = s"${switchInstance.getName}.buffer.out"
}


class OutputMechanism(switchInstance: SwitchInstance) {
  def symnetCode() : Instruction = {
    switchInstance.getIfaceSpec.foldRight(Fail("Cannot find egress_port match for current interfaces") : Instruction)((x, acc) => {
      If (Constrain("standard_metadata.egress_port", :==:(ConstantValue(x._1.longValue()))),
        Forward(s"${switchInstance.getName}.output.${x._1}"),
        acc
      )
    })
  }
}

class Deparser(switchInstance: SwitchInstance, expd : List[DFSState]) {
  def symnetCode() : Instruction = {
    InstructionBlock(new Instruction() {
      // TODO: MASSIVE HACK here => let's figure out a clean way for this one
      override def apply(s: State, verbose: Boolean): (List[State], List[State]) = (List[State](s.destroyPacket()), Nil)
    }, Fork(expd.map(x => {
      InstructionBlock(
        x.history.tail.reverse.map {
          case ce: CaseEntry =>
            val e = ce.getExpressions
            InstructionBlock(
              e.zip(ce.getValues).map(r => {
                r._1 match {
                  case ref: StringRef => Constrain(ref.getRef, :==:(ConstantValue(r._2.getValue)))
                  case _ => NoOp
                }
              })
            )
          case es: ExtractStatement =>
            val ref = es.getExpression.asInstanceOf[StringRef].getRef
            Constrain(s"$ref.IsValid", :==:(ConstantValue(1)))
          case _ => NoOp
        } ++ x.history.tail.reverse.flatMap {
          case es : ExtractStatement =>
            val ref = es.getExpression.asInstanceOf[StringRef].getRef
            val instance = switchInstance.getSwitchSpec.getInstance(ref)
            instance.getLayout.getFields.foldLeft((List[Instruction](), 0))((acc, r) => {
              (acc._1 ++ List[Instruction](
                Allocate(Tag("START") + acc._2, r.getLength),
                Assign(Tag("START") + acc._2, :@(ref + "." + r.getName))
              ), acc._2 + r.getLength)
            })._1
          case _ => List[Instruction](NoOp)
        }
      )
      })), Forward(outName())
    )
  }
  def outName() = s"${switchInstance.getName}.deparser.out"
}