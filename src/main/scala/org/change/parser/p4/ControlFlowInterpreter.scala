package org.change.parser.p4

import org.change.v2.p4.model.SwitchInstance
import org.stringtemplate.v4.compiler.Bytecode.Instruction
import scala.collection.JavaConversions._
import scala.util.matching.Regex
/**
  * Created by dragos on 07.09.2017.
  */
class ControlFlowInterpreter(switchInstance: SwitchInstance) {
  val tables = switchInstance.getDeclaredTables.map(new FullTable(_, switchInstance))
  val switch = switchInstance.getSwitchSpec

  for (l <- switch.getCtx.links) {
    if (l._1.contains("table.")) {
      val tableExactMatcher = "table\\.(.*?)\\.out\\.(.*)".r
      tableExactMatcher.findAllMatchIn(l._1).foreach(x => {
        val tabName = x.group(1)
        val id = x.group(2)
        switch.getCtx.instructions.put(s"table.$tabName.in.$id", new FullTable(tabName, switchInstance, id).fullAction())
      })
    }
  }
  def instructions() = {
    switch.getCtx.instructions
  }
  def links() = {
    switch.getCtx.links
  }
}

object ControlFlowInterpreter {
  def apply(switchInstance: SwitchInstance): ControlFlowInterpreter = new ControlFlowInterpreter(switchInstance)

  def apply(p4File: String, dataplane: String, ifaces: List[String], name : String = "") : ControlFlowInterpreter =
    ControlFlowInterpreter(SwitchInstance.fromP4AndDataplane(p4File, dataplane, name, ifaces))

}
