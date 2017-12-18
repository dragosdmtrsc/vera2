package org.change.parser.p4

import org.change.v2.p4.model.ISwitchInstance
import org.change.v2.util.{ToDot, ToVis}

object P4PrettyPrinter {
  implicit def toDotable(controlFlowInterpreter: ControlFlowInterpreter[_<:ISwitchInstance]): ToDot = ToDot(controlFlowInterpreter.switchInstance.getName,
    controlFlowInterpreter.instructions(),
    controlFlowInterpreter.links())
  implicit def toVisable(controlFlowInterpreter: ControlFlowInterpreter[_<:ISwitchInstance]): ToVis = ToVis(controlFlowInterpreter.instructions(),
    controlFlowInterpreter.links())
}
