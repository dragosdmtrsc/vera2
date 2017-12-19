package org.change.v2.util

import java.util.UUID

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward, If, InstructionBlock}

case class ToDot(name : String, instructions : Map[String, Instruction], links : Map[String, String]) {
  private def normalize(str : String) = "\"" + str + "\""
  def toDot: String = {
    val sb = new StringBuilder()
    sb.append(s"digraph $name {\n")

    for (x <- instructions) {
      sb.append("    " + normalize(x._1) + ";\n")
      def parseInstrGraph(parent : String, i : Instruction) : Unit = {
        i match {
          case ib : InstructionBlock => for (c <- ib.instructions) parseInstrGraph(parent, c)
          case fw : Forward => sb.append(s"${normalize(parent)} -> ${normalize(fw.place)};\n")
          case frk : Fork =>
            val crt = s"Fork_${UUID.randomUUID().toString}"
            //            sb.append(normalize(crt) + ";\n")
            //            sb.append(s"${normalize(parent)} -> ${normalize(crt)};\n")
            for (fb <- frk.forkBlocks)
              parseInstrGraph(parent, fb)
          case iif : If =>
            val crt = s"If_${UUID.randomUUID().toString}"
            //            sb.append(normalize(crt) + ";\n")
            //            sb.append(s"${normalize(parent)} -> ${normalize(crt)};\n")
            parseInstrGraph(parent, iif.thenWhat)
            parseInstrGraph(parent, iif.elseWhat)
          //          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
          case _ => Unit
        }
      }
      parseInstrGraph(x._1, x._2)
    }
    for (x <- links) {
      sb.append("    " + normalize(x._1) + ";\n")
      sb.append("    " + normalize(x._2) + ";\n")
      sb.append("    " + normalize(x._1) + " -> " + normalize(x._2) + ";\n")
    }

    sb.append("}")
    sb.toString()
  }
}
