package org.change.plugins.eq

import org.change.v2.analysis.constraint.Condition
import org.change.v2.analysis.equivalence.MagicTuple
import org.change.v2.analysis.memory.SimpleMemory
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2tov3.V2Translator

import scala.collection.Iterable
import scala.collection.Map

class V3NotImplemented extends Exception
class V2NotImplemented extends Exception

trait SupportMixin {
  def supports() : Set[Int] = Set.empty
}

trait PortPlugin extends SupportMixin {
  def apply(src : String) : String
  def apply(src : String, dst : String) : Boolean
  def all() : Iterable[(String, String)]
}

trait InputPortPlugin extends PortPlugin {}

trait OutputPortPlugin extends PortPlugin {}

trait PacketPlugin extends SupportMixin {
  def apply(x : SimpleMemory, y : SimpleMemory) : Boolean
  def apply3(x : org.change.v3.semantics.SimpleMemory,
             y : org.change.v3.semantics.SimpleMemory) : Boolean =
    throw new V3NotImplemented
}

trait TopologyPlugin extends SupportMixin {
  def apply() : Map[String, Instruction]
  def apply3() : Map[String, org.change.v3.syntax.Instruction] =
    V2Translator.v2v3(apply())
  override def supports(): Set[Int] = Set(2, 3)
  def defaultInputPlugin() : InputPacketPlugin =
    throw new UnsupportedOperationException("default input plugin is not " +
      "implemented for this topo plugin. Explicitly specify it.")
  def startNodes() : scala.collection.Set[String]
}

abstract class AbsTopologyPlugin extends TopologyPlugin {
  override def startNodes(): scala.collection.Set[String] = apply().keySet
}

trait SievePlugin extends SupportMixin {
  def apply(outcomes : List[SimpleMemory]) : Iterable[(Condition, Iterable[SimpleMemory])]
  def apply3(outcomes : List[org.change.v3.semantics.SimpleMemory]) :
    Iterable[(org.change.v3.semantics.Condition, Iterable[org.change.v3.semantics.SimpleMemory])] =
    throw new V3NotImplemented
}

trait InputPacketPlugin extends SupportMixin {
  def apply() : List[SimpleMemory]
  def apply3() : List[org.change.v3.semantics.SimpleMemory] =
    throw new V3NotImplemented
}

trait ExecutorConsumer extends SupportMixin {
  def apply(x : SimpleMemory) : Unit
  def apply3(x : org.change.v3.semantics.SimpleMemory) : Unit =
    apply(V2Translator.mem3mem2(x))
  override def supports(): Set[Int] = Set(2, 3)
  def flush() : Unit
}

trait ExecutorPlugin extends SupportMixin {
  def apply(topology : Map[String, Instruction],
            startNodes : Set[String],
            initials : List[SimpleMemory],
            consumer: ExecutorConsumer)
  def apply3(topology : Map[String, org.change.v3.syntax.Instruction],
            startNodes : Set[String],
            initials : List[org.change.v3.semantics.SimpleMemory],
            consumer: ExecutorConsumer) : Unit = {
    throw new V3NotImplemented
  }
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