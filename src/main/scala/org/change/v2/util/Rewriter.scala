package org.change.v2.util

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

import scala.annotation.tailrec


case class Rewriter(instructions : Map[String, Instruction], links : Map[String, String]) {
  def prepend(prefix : String): (Map[String, Instruction], Map[String, String]) = {
    (instructions.map(r => {
      prefix + r._1 -> renamePorts(r._2, (s) => prefix + s)
    }), links.map(r => (prefix + r._1) -> (prefix + r._2)))
  }

  def renamePorts(instruction: Instruction, mapper : String => String) : Instruction = instruction match {
    case Fork(forkBlocks) => Fork(forkBlocks.map(renamePorts(_, mapper)))
    case InstructionBlock(instructions) => InstructionBlock(instructions.map(renamePorts(_, mapper)))
    case Forward(place) => Forward(mapper(place))
    case If(testInstr, thenWhat, elseWhat) => If (testInstr,
      renamePorts(thenWhat, mapper),
      renamePorts(elseWhat, mapper)
    )
    case t : Translatable => renamePorts(t.generateInstruction(), mapper)
    case _ => instruction
  }

  def append(suffix : String): (Map[String, Instruction], Map[String, String]) = {
    (instructions.map(r => {
      r._1 + suffix -> renamePorts(r._2, (s) => s + suffix)
    }), links.map(r => (r._1 + suffix) -> (r._2 + suffix)))
  }

  def transform()(mapper : (String) => String) : (Map[String, Instruction], Map[String, String]) = {
    (instructions.map(r => {
      mapper(r._1) -> renamePorts(r._2, mapper)
    }), links.map(r => mapper(r._1) -> mapper(r._2)))
  }

}

class InstructionRewrite(instruction: Instruction) {

  @tailrec
  private def renameIbPorts(instructions : List[Instruction],
                          crt : List[Instruction],
                          mapper : String => String) : List[Instruction] = instructions match {
    case InstructionBlock(instrs) :: tail => renameIbPorts(instrs.toList ++ tail, crt, mapper)
    case Forward(place) :: _ => crt :+ Forward(mapper(place))
    case head :: tail => renameIbPorts(tail, crt :+ renamePorts(head, mapper), mapper)
    case Nil => crt
  }
  @tailrec
  private def renameForkPorts(instructions : List[Instruction],
                            crt : List[Instruction],
                            mapper : String => String) : List[Instruction] = instructions match {
    case head :: tail => renameForkPorts(tail, crt :+ renamePorts(head, mapper), mapper)
    case Nil => crt
  }

  private def renamePorts(instruction: Instruction, mapper : String => String) : Instruction = instruction match {
    case Fork(forkBlocks) => Fork(
      renameForkPorts(forkBlocks.toList, Nil, mapper)
    )
    case InstructionBlock(instructions) => InstructionBlock(
      renameIbPorts(instructions.toList, Nil, mapper)
    )
    case Forward(place) => Forward(mapper(place))
    case If(testInstr, thenWhat, elseWhat) => If (testInstr,
      renamePorts(thenWhat, mapper),
      renamePorts(elseWhat, mapper)
    )
    case t : Translatable => renamePorts(t.generateInstruction(), mapper)
    case _ => instruction
  }
  def rename(mapper : String => String): Instruction = renamePorts(instruction, mapper)

}

object Rewriter {
  implicit def rewrite(instrAndLinks : (Map[String, Instruction], Map[String, String])): Rewriter = Rewriter(instrAndLinks._1, instrAndLinks._2)
  implicit def rewrite(instrAndLinks : Map[String, Instruction]): Rewriter = Rewriter(instrAndLinks, Map.empty)
  implicit def linkRewriter(instrAndLinks : Map[String, String]): Rewriter = Rewriter(Map.empty, instrAndLinks)
  implicit def rewrite(instruction : Instruction): InstructionRewrite = new InstructionRewrite(instruction)
}
