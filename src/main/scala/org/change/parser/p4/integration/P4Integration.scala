package org.change.parser.p4.integration

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}
import org.change.v2.p4.model.Switch
import org.change.parser.p4.anonymizeAndForward
import org.change.v2.p4.model.integration.P4Graph

import scala.collection.JavaConversions._

class  P4Integration(val nodes : Map[String, ControlFlowInterpreter[_]],
                     val edges : Map[(String, Int), (String, Int)]) {

  private lazy val integrationInstruction : Map[String, Instruction] = edges.
    map(r => s"${r._1._1}.output.${r._1._2}" -> InstructionBlock(
      anonymizeAndForward(s"${r._2._1}.input.${r._2._2}")
    ))

  lazy val links: Map[String, String] = nodes.flatMap(_._2.links())

  lazy val instructions : Map[String, Instruction] = nodes.values.flatMap(r => r.instructions()).toMap ++
    integrationInstruction

}


object P4Integration {
  def apply(dir : String, integration : String): P4Integration = {
    val p4Graph = P4Graph.fromYaml(dir + "/" + integration)
    apply(p4Graph, dir)
  }

  def apply(p4Graph: P4Graph, dir : String): P4Integration = {
    val nodes = p4Graph.getNodes
    val cfis = nodes.map(r => {
      val switch = Switch.fromFile(dir + "/" + r.getP4)
      r.getName -> new ControlFlowInterpreter(
        switch = switch,
        switchInstance = SymbolicSwitchInstance.fromFileWithSyms(
          r.getName,
          r.getIfaces.map(r => r._1.intValue() -> r._2).toMap,
          Map.empty,
          switch,
          dir + "/" + r.getDataplane
        )
      )
    })
    val edges = p4Graph.getEdges.map(r => (r.getFrom.getNode, r.getFrom.getPort) ->
      (r.getTo.getNode, r.getTo.getPort))
    new P4Integration(cfis.toMap, edges.toMap)
  }
//    new P4Integration(p4Graph)
}
