package org.change.plugins.vera

import org.change.plugins.eq.{InputPacketPlugin, PluginBuilder, V2NotImplemented}
import org.change.v2.analysis.memory.SimpleMemory
import org.change.v2.p4.model.parser.ReturnStatement
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.v3.semantics
import scala.collection.JavaConverters._
class VeraInputPacketPlugin(switch: Switch,
                            switchInstance: ISwitchInstance) extends InputPacketPlugin {
  lazy val p2l = new ParserToLogics(switch,
    switchInstance.getIfaceSpec.asScala.toMap.map(x => x._1.intValue() -> x._2))
  override def apply(): List[SimpleMemory] = throw new V2NotImplemented
  override def apply3(): List[semantics.SimpleMemory] =
    p2l.produceTerminalState(new ReturnStatement("ingress")) :: Nil
  override def supports(): Set[Int] = Set(3)
}