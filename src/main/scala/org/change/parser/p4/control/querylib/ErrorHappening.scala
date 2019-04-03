package org.change.parser.p4.control.querylib

import org.change.parser.p4.control.{BufferResult, P4Memory, SwitchTarget}
import org.change.parser.p4.parser.Instance
import org.change.v2.p4.model.Switch

class ErrorHappening(switch : Switch,
                     input : P4Memory,
                     postingress : P4Memory,
                     parsed : P4Memory,
                     postIngress : P4Memory,
                     postIngressBuffer : BufferResult[_<:P4Memory],
                     postEgress : P4Memory,
                     postEgressBuffer : BufferResult[_<:P4Memory],
                     deparsed : P4Memory,
                     maybeInstance : Option[Instance],
                     maybeTarget : Option[SwitchTarget],
                     produceHarness : Boolean) {

  def mayProduceHarness(): Boolean = maybeInstance.nonEmpty && maybeTarget.nonEmpty



}
