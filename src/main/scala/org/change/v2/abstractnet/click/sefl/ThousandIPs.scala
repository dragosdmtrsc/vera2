package org.change.v2.abstractnet.click.sefl

import org.change.v2.abstractnet.generic.{ConfigParameter, ElementBuilder, GenericElement, Port}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames.IPSrc

/**
 * Element corresponding to: "[name] :: ToDevice(deviceName) or FromNetPort(mac)"
 * @param name
 * @param deviceName
 */
class ThousandIPs(name: String,
               inputPorts: List[Port],
               outputPorts: List[Port],
               configParams: List[ConfigParameter])
  extends GenericElement(name,
    "ThousandIPs",
    inputPorts,
    outputPorts,
    configParams) {

  override def instructions: Map[LocationId, Instruction] = Map(
    (inputPortName(0), InstructionBlock(InstructionBlock ((1 to 1000).toList.map((i : Int) => Constrain(IPSrc,:~:(:==:(ConstantValue(i)))))),
                                        Constrain(IPSrc,(:==:(ConstantValue(1)))),
                                        Forward(outputPortName(0))))
  )

  }

class ThousandIPsElementBuilder(name: String)
  extends ElementBuilder(name, "ThousandIPs") {

  addInputPort(Port())
  addOutputPort(Port())

  override def buildElement: GenericElement = {
    new ThousandIPs(name, getInputPorts, getOutputPorts, getConfigParameters)
  }
}

object ThousandIPs {

  private var unnamedCount = 0

  private val genericElementName = "ThousandIPs"

  private def increment {
    unnamedCount += 1
  }

  def getBuilder(name: String): ThousandIPsElementBuilder = {
    increment ; new ThousandIPsElementBuilder(name)
  }

  def getBuilder: ThousandIPsElementBuilder =
    getBuilder(s"$genericElementName-$unnamedCount")
}