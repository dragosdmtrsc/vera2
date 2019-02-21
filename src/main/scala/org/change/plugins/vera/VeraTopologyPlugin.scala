package org.change.plugins.vera

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.{LightParserGenerator, ParserGenerator, SwitchBasedParserGenerator}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.plugins.eq.{PluginBuilder, TopologyPlugin}

import scala.util.matching.Regex

class VeraTopologyPlugin(switchName : String,
                         sw : Switch,
                         switchInstance: SymbolicSwitchInstance,
                         ifaces : Map[Int, String],
                         cloneSpec : Map[Int, Int],
                         parserGenerator : ParserGenerator,
                         generator : Boolean
                        ) extends TopologyPlugin {
  private lazy val controlFlowInterpreter = ControlFlowInterpreter.buildSymbolicInterpreter(switchInstance,
    sw,
    Some(parserGenerator))
  private lazy val instructions = CodeAwareInstructionExecutor.flattenProgram(controlFlowInterpreter.instructions(),
    controlFlowInterpreter.links())
  override def startNodes() : Set[String] =
    if (!generator)
      controlFlowInterpreter.startingPoints()
    else
      parserGenerator.generatorStartPoints()
  override def apply(): collection.Map[String, Instruction] = instructions
}

object VeraTopologyPlugin {
  class Builder extends PluginBuilder[VeraTopologyPlugin] {
    var commandsTxt = ""
    var p4file = ""
    var layoutFilter = ".*"
    val ifaces = scala.collection.mutable.Map.empty[Int, String]
    val cloneSpec = scala.collection.mutable.Map.empty[Int, Int]
    var switchName = "switch"
    var ninterfaces = 0
    var generator = false
    var noInputPacket = false
    var justParser = false
    override def set(parm: String, value: String): PluginBuilder[VeraTopologyPlugin] = {
      val pat = "interface(\\d+)".r
      val clonespec = "clonespec(\\d+)".r
      parm match {
        case "justparser" => justParser = value.toBoolean; this
        case "generator" => generator = value.toBoolean; this
        case "noinputpacket" => noInputPacket = value.toBoolean; this
        case "commands" => commandsTxt = value; this
        case "p4" => p4file = value; this
        case "layoutfilter" => layoutFilter = value; this
        case "name" => switchName = value; this
        case "ninterfaces" => ninterfaces = value.toInt; this
        case pat(nr) => ifaces.put(nr.toInt, value); this
        case clonespec(cs) => cloneSpec.put(cs.toInt, value.toInt); this
      }
    }

    override def build(): VeraTopologyPlugin = {
      val sw = Switch.fromFile(p4file)
      if (ifaces.isEmpty) {
        if (ninterfaces == 0)
          throw new IllegalArgumentException("specify parms: interfaceX NAME or ninterfaces NR")
        (0 until ninterfaces).foreach(h => {
          ifaces.put(h, s"eth$h")
        })
      }
      val switchInstance = SymbolicSwitchInstance.fromFileWithSyms(switchName,
        ifaces.toMap, cloneSpec.toMap,
        sw, commandsTxt, false)
      val parserGenerator = new LightParserGenerator(sw, switchInstance, noInputPacket, justParser)
      new VeraTopologyPlugin(switchName, sw, switchInstance, ifaces.toMap, cloneSpec.toMap, parserGenerator, generator)
    }
  }
}