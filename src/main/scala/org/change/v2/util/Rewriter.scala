package org.change.v2.util

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

case class Rewriter(instructions : Map[String, Instruction], links : Map[String, String]) {
  def prepend(prefix : String): (Map[String, Instruction], Map[String, String]) = {
    (instructions.map(r => {
      prefix + r._1 -> renamePorts(r._2, (s) => prefix + s)
    }), links.map(r => (prefix + r._1) -> (prefix + r._2)))
  }

  private def renamePorts(instruction: Instruction, mapper : String => String) : Instruction = instruction match {
    case Fork(forkBlocks) => Fork(forkBlocks.map(renamePorts(_, mapper)))
    case InstructionBlock(instructions) => InstructionBlock(instructions.map(renamePorts(_, mapper)))
    case Forward(place) => Forward(mapper(place))
    case If(testInstr, thenWhat, elseWhat) => If (testInstr, renamePorts(thenWhat, mapper), renamePorts(elseWhat, mapper))
//    case Let(string, instruction) => Let(string, renamePorts(instruction, mapper))
//    case Unfail(instruction) => Unfail(renamePorts(instruction, mapper))
//    case SuperFork(instructions) => SuperFork(instructions.map(renamePorts(_, mapper)))
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



object Rewriter {
  implicit def rewrite(instrAndLinks : (Map[String, Instruction], Map[String, String])): Rewriter = Rewriter(instrAndLinks._1, instrAndLinks._2)
}
