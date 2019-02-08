package org.change.v2.util

import java.io.{OutputStream, PrintWriter}
import java.util.UUID

import org.change.v2.analysis.processingmodels.{Instruction, SuperFork}
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward, If, InstructionBlock}

import scala.collection.mutable

object ToDot {
  def normalize(str : String): String = "\"" + str + "\""
  def apply(name : String, instructions : Map[String, Instruction],
            startingFrom : Set[String] = Set.empty,
            outputStream : PrintWriter): Unit = {
    def gatherForwards(instruction: Instruction) : Set[String] = instruction match {
      case Forward(a) => Set(a)
      case InstructionBlock(is) => is.flatMap(gatherForwards).toSet
      case Fork(is) => is.flatMap(gatherForwards).toSet
      case If (a, b, c) => gatherForwards(b) ++ gatherForwards(c)
      case SuperFork(is) => is.flatMap(gatherForwards).toSet
      case _ => Set.empty
    }

    def dfs(port : String, edges : Map[String, Set[String]],
            nodes : Set[String]) : (Map[String, Set[String]], Set[String]) = {
      if (nodes contains port)
        (edges, nodes)
      else {
        val newnodes = nodes + port
        if (!instructions.contains(port)) {
          (edges, newnodes)
        } else {
          val extras = gatherForwards(instructions(port))
          val init = (
            if (extras.nonEmpty)
              edges + (port -> extras)
            else
              edges, newnodes)
          extras.foldLeft(init)((acc, dst) => {
            dfs(dst, acc._1, acc._2)
          })
        }
      }
    }

    val (eds, ns) = startingFrom.foldLeft((Map.empty[String, Set[String]], Set.empty[String]))((acc, port) => {
      dfs(port, acc._1, acc._2)
    })
    outputStream.println(s"digraph $name {")
    for (x <- ns) {
      val sb = new mutable.StringBuilder()
      sb.append(x.hashCode.toLong + "[label=" + normalize(x))
      if (startingFrom.contains(x)) {
        sb.append(",style=filled,fillcolor=blue,fontcolor=white")
      } else if (!eds.contains(x) || eds(x).isEmpty) {
        sb.append(",style=filled,fillcolor=darkgreen,fontcolor=white")
      }
      sb.append("];")
      outputStream.println(sb.toString())
    }
    for (e <- eds) {
      for (t <- e._2)
        outputStream.println(e._1.hashCode.toLong + " -> " + t.hashCode.toLong + ";")
    }
    outputStream.println("}")
  }
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
