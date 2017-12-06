package org.change.v2.abstractnet.click.sefl

import org.change.v2.abstractnet.generic.{ConfigParameter, ElementBuilder, GenericElement, Port}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}

class AddTCPOptions(name: String,
                    elementType: String,
                    inputPorts: List[Port],
                    outputPorts: List[Port],
                    configParams: List[ConfigParameter])
  extends GenericElement(name,
    elementType,
    inputPorts,
    outputPorts,
    configParams) {

  override def instructions: Map[LocationId, Instruction] = Map(
    inputPortName(0) -> InstructionBlock(
      //check that TCP header is allocated, modify TCP options field
      State.tcpOptions,
      Forward(outputPortName(0))
    )
  )
}

class AddTCPOptionsElementBuilder(name: String, elementType: String)
  extends ElementBuilder(name, elementType) {

  addInputPort(Port())
  addOutputPort(Port())

  override def buildElement: GenericElement = {
    new AddTCPOptions(name, elementType, getInputPorts, getOutputPorts, getConfigParameters)
  }
}

object AddTCPOptions {
  private var unnamedCount = 0

  private val genericElementName = "AddTCPOptions"

  private def increment {
    unnamedCount += 1
  }

  def getBuilder(name: String): AddTCPOptionsElementBuilder = {
    increment;
    new AddTCPOptionsElementBuilder(name, "AddTCPOptions")
  }

  def getBuilder: AddTCPOptionsElementBuilder =
    getBuilder(s"$genericElementName-$unnamedCount")
}
