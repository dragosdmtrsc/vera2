package org.change.v2.util

import java.util.UUID

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward, If, InstructionBlock}

import scala.collection.mutable

object ToDot {
  def normalize(str : String): String = "\"" + str + "\""
}

case class ToDot(name : String, instructions : Map[String, Instruction], links : Map[String, String]) {
  import ToDot.normalize
  def toDot: String = {
    val sb = new StringBuilder()
    sb.append(s"digraph $name {\n")
    val edges = mutable.Set.empty[(String, String)]

    for (x <- instructions) {
      sb.append("    " + normalize(x._1) + ";\n")
      def parseInstrGraph(parent : String, i : Instruction) : Unit = {
        i match {
          case ib : InstructionBlock => for (c <- ib.instructions) parseInstrGraph(parent, c)
          case fw : Forward =>
            if (!edges.contains((parent, fw.place))) {
              sb.append(s"${normalize(parent)} -> ${normalize(fw.place)};\n")
              edges.add((parent, fw.place))
            }
          case frk : Fork =>
            for (fb <- frk.forkBlocks)
              parseInstrGraph(parent, fb)
          case iif : If =>
            parseInstrGraph(parent, iif.thenWhat)
            parseInstrGraph(parent, iif.elseWhat)
          case _ => Unit
        }
      }
      parseInstrGraph(x._1, x._2)
    }
    for (x <- links) {
      if (!edges.contains(x)) {
        sb.append(normalize(x._1) + " -> " + normalize(x._2) + ";\n")
        edges.add(x)
      }
    }

    sb.append("}")
    sb.toString()
  }
}
