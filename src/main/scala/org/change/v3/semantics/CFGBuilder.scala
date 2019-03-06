package org.change.v3.semantics

import org.change.utils.graph.LabeledGraph
import org.change.v3.syntax._


class CFGBuilder {
  var edges = Map.empty[Instruction, List[(Instruction, Option[BExpr])]]
  var outstanding = List.empty[(Instruction, Option[BExpr])]
  var outgoing = Map.empty[Instruction, String]
  var bad = Map.empty[Instruction, String]
  var first = Option.empty[Instruction]

  def build(instr : Instruction) : Unit = {
    if (!instr.isInstanceOf[InstructionBlock] &&
      !instr.isInstanceOf[Fork] &&
      !instr.isInstanceOf[Clone] &&
      instr != NoOp) {
      if (first.isEmpty) {
        first = Some(instr)
      }
      outstanding.foreach(o => {
        val crt = edges.getOrElse(o._1, Nil)
        edges = edges + (o._1 -> ((instr, o._2) :: crt))
      })
      outstanding = Nil
    }
    instr match {
      case NoOp => ;
      case Forward(to) => outgoing = outgoing + (instr -> to)
      case Fail(message) => ;
      case Drop(message) => ;
      case Assign(left, right) => outstanding = (instr, None) :: outstanding
      case Allocate(left, sz) => outstanding = (instr, None) :: outstanding
      case Deallocate(left) => outstanding = (instr, None) :: outstanding
      case CreateTag(name, right) => outstanding = (instr, None) :: outstanding
      case DestroyTag(name) => outstanding = (instr, None) :: outstanding
      case Assume(bExpr) => outstanding = (instr, Some(bExpr)) :: outstanding
      case If(bExpr, thn, els) =>
        outstanding = (instr, Some(bExpr)) :: outstanding
        build(thn)
        var keep = outstanding
        outstanding = (instr, Some(LNot(bExpr))) :: Nil
        build(els)
        outstanding = outstanding ++ keep
      case InstructionBlock(is) =>
        is.foreach(in => build(in))
      case Clone(is) =>
        val init = outstanding
        outstanding = is.foldLeft(List.empty[(Instruction, Option[BExpr])])((acc, in) => {
          outstanding = init
          build(in)
          acc ++ outstanding
        })
      case Fork(is) => val init = outstanding
        outstanding = is.foldLeft(List.empty[(Instruction, Option[BExpr])])((acc, in) => {
          outstanding = init
          build(in)
          acc ++ outstanding
        })
    }
  }
}
object CFGBuilder {
  def build(topo : Map[String, Instruction], start : String):
      (LabeledGraph[Instruction, Option[BExpr]], Instruction) = {
    var graph = Map.empty[Instruction, List[(Instruction, Option[BExpr])]]
    var outstandingEdges = Map.empty[Instruction, String]
    var first = Map.empty[String, Instruction]
    var q = List.empty[String]
    for (x <- topo) {
      q = x._1 :: q
      while (q.nonEmpty) {
        val instr = q.head
        q = q.tail
        val cfbuilder = new CFGBuilder()
        cfbuilder.build(x._2)
        cfbuilder.first.foreach(f => {
          first = first + (x._1 -> f)
        })
        graph = graph ++ cfbuilder.edges
        outstandingEdges = outstandingEdges ++ cfbuilder.outgoing
      }
    }
    outstandingEdges.foreach(x => {
      first.get(x._2).foreach(v => {
        graph = graph + (x._1 -> ((v, None) :: Nil))
      })
    })
    (new LabeledGraph[Instruction, Option[BExpr]](graph), first(start))
  }
}
