package org.change.v2.util

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._

case class ToVis(instructions : Map[String, Instruction], links : Map[String, String]) {

  private def normalize(str : String) = str.replace(".", "_").
    replace("[", "_").
    replace("]", "_").
    replace("-", "_")

  def toVis: String = {
    val sb = new StringBuilder()
    sb.append("<!doctype html>\n<html>\n<head>\n  <title>Network | Basic usage</title>\n\n  <script type=\"text/javascript\" src=\"../../Vis.js/dist/vis.js\"></script>\n  <link href=\"../../Vis.js/dist/vis-network.min.css\" rel=\"stylesheet\" type=\"text/css\" />\n\n  <style type=\"text/css\">\n    #mynetwork {\n      width: 1500px;\n      height: 1000px;\n      border: 1px solid lightgray;\n    }\n  </style>\n</head>\n<body>")
    sb.append("<div id=\"mynetwork\"></div>\n\n<script type=\"text/javascript\">\n")

    val blockId = new scala.collection.mutable.HashMap[String,Int]()
    var crtID = 1

    for (x <- links) {
      if (!blockId.contains(x._1)) {
        blockId(x._1) = crtID
        crtID = crtID+1
      }
      if (!blockId.contains(x._2)) {
        blockId(x._2) = crtID
        crtID = crtID+1
      }
    }
    sb.append("  var edges = new vis.DataSet([\n")

    var first = true

    for (x <- links) {
      if (!first)
        sb.append(",\n")

      first = false

      sb.append(s"{from: ${blockId(x._1)}, to: ${blockId(x._2)}, arrows:'to'}")
    }

    for (x <- instructions) {
      if (!blockId.contains(normalize(x._1))){
        blockId(x._1) = crtID
        crtID = crtID + 1
      }

      def parseInstrGraph(parent : String, i : Instruction) : Unit = {
        i match {
          case ib : InstructionBlock => for (c <- ib.instructions) parseInstrGraph(parent, c)
          case fw : Forward =>
            if (!first)
              sb.append(",\n")
            first = false

            if (!blockId.contains(normalize(parent))){
              blockId(parent) = crtID
              crtID = crtID + 1
            }
            if (!blockId.contains(fw.place)){
              blockId(fw.place) = crtID
              crtID = crtID + 1
            }

            sb.append(s"{ from: ${blockId(parent)}, to: ${blockId(fw.place)}, arrows:'to' }")

          case frk : Fork =>
            for (fb <- frk.forkBlocks)
              parseInstrGraph(parent, fb)
          case iif : If =>
            parseInstrGraph(parent, iif.thenWhat)
            parseInstrGraph(parent, iif.elseWhat)
          //          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
          case _ => Unit
        }
      }
      parseInstrGraph(x._1, x._2)
    }
    sb.append("  ]);\n\n")

    def genInstrTitle(i : Instruction) : String = {
      val sb2 = new StringBuilder()

      i match {
        case ib : InstructionBlock =>
          if (ib.instructions.size > 1) {
            sb2.append("{<br>")
            for (c <- ib.instructions) {
              sb2.append(s"${genInstrTitle(c)}<br>")
            }
            sb2.append("}<br>")
          }
          else if (ib.instructions.size==1)
            genInstrTitle(ib.instructions.head)

        case fw : Forward =>
          sb2.append(s"FORWARD ${fw.place}")

        case frk : Fork =>
          sb2.append("Fork {<br>")
          for (fb <- frk.forkBlocks)
            sb2.append(s"| ${genInstrTitle(fb)}<br>")
          sb2.append("}<br>")
        case iif : If =>
          sb2.append(s"if ( ${genInstrTitle(iif.testInstr)} ) then ${genInstrTitle(iif.thenWhat)} <br> else ${genInstrTitle(iif.elseWhat)} <br>")
        //          case fl : Fail => sb.append(s"${normalize(parent)} -> Fail_${normalize(UUID.randomUUID().toString)};\n")
        case c : ConstrainNamedSymbol =>
          sb2.append(s"ConstrainNamedSymbol (${c.id} ${c.dc})")

        case d : ConstrainRaw =>
          sb2.append(s"ConstrainRaw (${d.a} ${d.dc})")

        case d : AllocateSymbol =>
          sb2.append(s"AllocateSymbol(${d.id}, size ${d.size})")

        case d : AllocateRaw =>
          sb2.append(s"AllocateRaw(at ${d.a}, size ${d.size})")

        case d : DeallocateNamedSymbol =>
          sb2.append(s"DellocateNamedSymbol(${d.id})")

        case d : DeallocateRaw =>
          sb2.append(s"DeallocateRaw(at ${d.a}, size ${d.size})")

        case d : CreateTag =>
          sb2.append(s"CreateTag(${d.name},${d.value})")

        case d : DestroyTag =>
          sb2.append(s"DestroyTag(${d.name})")

        case d : Check =>
          sb2.append(s"Check(at ${d.offset}, size ${d.size})")

        case d : AssignRaw =>
          sb2.append(s"Header(${d.a}) = ${d.exp}")

        case d : AssignNamedSymbol =>
          sb2.append(s"${d.id} = ${d.exp}")

        case d: Fail =>
          sb2.append(s"Fail<")

        case _ => sb2.append(s"GenericInstruction")
      }
      sb2.toString()
    }

    sb.append("  var nodes = new vis.DataSet([\n")
    first = true
    for ((k,v)<- blockId) {
      if (!first)
        sb.append(",\n")

      first = false
      if (instructions.contains(k))
        sb.append(s"{id: $v, label: '${normalize(k)}', title: '${genInstrTitle(instructions(k))}'}")
      else
        sb.append(s"{id: $v, label: '${normalize(k)}', title: 'NULL'}")
    }
    sb.append("\n]);\n\n")

    sb.append("// create a network\n  var container = document.getElementById('mynetwork');\n  var data = {\n    nodes: nodes,\n    edges: edges\n  };\n  var options = {};\n  var network = new vis.Network(container, data, options);\n</script>\n\n\n</body>\n</html>")
    sb.toString()
  }
}
