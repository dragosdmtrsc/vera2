package org.change.v2.plugins.eq

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
}

trait SievePlugin {
  def apply(outcomes : List[SimpleMemory]) : Iterable[(Condition, Iterable[SimpleMemory])]
}

trait InputPacketPlugin {
  def apply() : List[SimpleMemory]
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