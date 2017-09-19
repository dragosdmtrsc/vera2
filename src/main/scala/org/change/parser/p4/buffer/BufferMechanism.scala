package org.change.parser.p4.buffer

import org.change.parser.p4.parser.{DFSState, Graph}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.parser._

import scala.collection.JavaConversions._
import scala.collection.immutable.Stack

/**
  * Created by dragos on 15.09.2017.
  */
class BufferMechanism(switchInstance: SwitchInstance) {

  def cloneCase() = InstructionBlock(
    switchInstance.getCloneSpec2EgressSpec.foldRight(Fail("No clone mapping found. Resolve to drop") : Instruction)((x, acc) => {
      If (Constrain(s"${switchInstance.getName}.CloneCookie", :==:(ConstantValue(x._1.longValue()))),
        Assign("standard_metadata.egress_port", ConstantValue(x._2.longValue())),
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


class DeparserRev(switchInstance: SwitchInstance) {
  val startState = switchInstance.getSwitchSpec.getParserState("start")
  def symnetCode() : Instruction = {
    val asList = switchInstance.getSwitchSpec.parserStates().toList
    val indexedSeq = asList.zipWithIndex.toMap
    val (edges, nodes) = asList.foldLeft((List[(String, String)](), Map[String, Iterable[String]]()))((acc, x) => {
      val (edges, extracts) = symnetCode(switchInstance.getSwitchSpec.getParserState(x))
      (acc._1 ++ edges, acc._2 + (x -> extracts))
    })

    val graph = new Graph(asList.size)
    for (e <- edges)
      if (e._2 != "ingress")
        graph.addEdge(indexedSeq(e._1), indexedSeq(e._2))
    val popper = graph.topologicalSort()
    def generateCode(stack : List[Int], offset : Int) : Instruction = stack match {
      case Nil => NoOp
      case head :: tail =>
        val headers = nodes(asList(head))
        def generateHeaderCode(headerInstances : List[String], off : Int) : Instruction = headerInstances match {
          case Nil => generateCode(tail, off)
          case h2 :: t2 =>
            val ib = switchInstance.getSwitchSpec.getInstance(h2).getLayout.getFields.
              foldLeft((off, List[Instruction]()))((acc, f) => {
                (acc._1 + f.getLength,  acc._2 ++ List[Instruction](
                  Allocate(Tag("START") + acc._1, f.getLength),
                  Assign(Tag("START") + acc._1, :@(s"$h2.${f.getName}"))
                ))
              })
            If (Constrain(s"$h2.IsValid", :==:(ConstantValue(1))),
              InstructionBlock(
                ib._2 :+ generateHeaderCode(t2, ib._1)
              ),
              generateHeaderCode(t2, off)
            )
        }
        generateHeaderCode(headers.toList, offset)
    }
    InstructionBlock(
      new Instruction() {
        // TODO: MASSIVE HACK here => let's figure out a clean way for this one
        override def apply(s: State, verbose: Boolean): (List[State], List[State]) = (List[State](s.destroyPacket()), Nil)
      }, generateCode(popper.map(_.intValue()).toList.reverse, 0), Forward(outName())
    )
  }

  def symnetCode(ss : org.change.v2.p4.model.parser.State) = {
    val edges = ss.getStatements.filter(x => x.isInstanceOf[ReturnStatement] || x.isInstanceOf[ReturnSelectStatement]).flatMap {
      case rs : ReturnStatement => List[(String, String)]((ss.getName, rs.getWhere))
      case rss : ReturnSelectStatement => rss.getCaseEntryList.map(x => {
        (ss.getName, x.getReturnStatement.getWhere)
      })
    }
    val extracts = ss.getStatements.filter(x => x.isInstanceOf[ExtractStatement]).map(_.asInstanceOf[ExtractStatement].getExpression.asInstanceOf[StringRef].getRef)
    (edges, extracts)
  }

  def outName() = s"${switchInstance.getName}.deparser.out"
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