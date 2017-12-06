package org.change.v2.util

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{:==:, Constrain, If, NoOp}
import org.change.v2.util.canonicalnames._

object IpHelpers {
  def parseIpAddress(ip: String): (String, String) = {
    val ipAddr = ip.substring(0, ip.indexOf('/'))
    val ipMask = ip.substring(ip.indexOf('/') + 1)
    (ipAddr, ipMask)
  }
}

object IfProto {
  def apply(proto: Int, thenWhat: Instruction, elseWhat: Instruction = NoOp): Instruction = {
    If(Constrain(Proto, :==:(ConstantValue(proto))), thenWhat, elseWhat)
  }
}