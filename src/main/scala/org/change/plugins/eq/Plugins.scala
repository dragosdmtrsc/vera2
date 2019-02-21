package org.change.plugins.eq

import org.change.v2.analysis.constraint.Condition
import org.change.v2.analysis.equivalence.MagicTuple
import org.change.v2.analysis.memory.SimpleMemory
import org.change.v2.analysis.processingmodels.Instruction

import scala.collection.Iterable
import scala.collection.Map

trait PortPlugin {
  def apply(src : String) : String
  def apply(src : String, dst : String) : Boolean
  def all() : Iterable[(String, String)]
}

trait InputPortPlugin extends PortPlugin {}

trait OutputPortPlugin extends PortPlugin {}

trait PacketPlugin {
  def apply(x : SimpleMemory, y : SimpleMemory) : Boolean
}

trait TopologyPlugin {
  def apply() : Map[String, Instruction]
  def startNodes() : scala.collection.Set[String]
}

abstract class AbsTopologyPlugin extends TopologyPlugin {
  override def startNodes(): scala.collection.Set[String] = apply().keySet
}

trait SievePlugin {
  def apply(outcomes : List[SimpleMemory]) : Iterable[(Condition, Iterable[SimpleMemory])]
}

trait InputPacketPlugin {
  def apply() : List[SimpleMemory]
}

trait ExecutorConsumer {
  def apply(x : SimpleMemory) : Unit
  def flush() : Unit
}

trait ExecutorPlugin {
  def apply(topology : Map[String, Instruction],
            startNodes : Set[String],
            initials : List[SimpleMemory],
            consumer: ExecutorConsumer)
}

trait OutputPlugin {
  def apply(wrongArity : Iterable[MagicTuple],
            portMismatch : Iterable[MagicTuple],
            outputMismatch : Iterable[MagicTuple]) : Unit
}
trait IntegrationPlugin {
  def topologyLeft() : TopologyPlugin
  def topologyRight() : TopologyPlugin
  def inputMappings() : InputPortPlugin
  def outputMappings() : OutputPortPlugin
  def setOutputMappingParms(p : Map[String, String]) : IntegrationPlugin
  def setInputMappingsParms(p : Map[String, String]) : IntegrationPlugin
  def setTopologyRightParms(p : Map[String, String]) : IntegrationPlugin
  def setTopologyLeftParms(p : Map[String, String]) : IntegrationPlugin
}

trait PluginBuilder[T] {
  def set(parm : String, value : String) : PluginBuilder[T]
  def build() : T
}