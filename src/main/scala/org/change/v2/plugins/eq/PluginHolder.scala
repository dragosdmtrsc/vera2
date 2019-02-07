package org.change.v2.plugins.eq

import org.change.v2.analysis.equivalence.Equivalence
import org.change.v2.cmd.EQParams

import scala.collection.JavaConverters._
import scala.collection.Map

class PluginHolder(
  val inputMapping : InputPortPlugin,
  val outputMapping : OutputPortPlugin,
  val inputGen : InputPacketPlugin,
  val outPacket : PacketPlugin,
  val topoLeft : TopologyPlugin,
  val topoRight : TopologyPlugin,
  val sievePlugin : SievePlugin,
  val outputPlugin : OutputPlugin) {

  def apply() : Unit = {
    val equivalence = new Equivalence(topoLeft().toMap, topoRight().toMap)
    val (wrongArity, portMismatch, outputMismatch) = equivalence.show(
      inputGen(),
      inputMapping.all(),
      (x, y) => outputMapping.apply(x, y),
      (slv, s1, s2) => outPacket(s1, s2),
      Some(sievePlugin.apply)
    )
    outputPlugin(wrongArity, portMismatch, outputMismatch)
  }
}

object PluginHolder {
  def instantiate(clazz : String) : PluginBuilder[_] = {
    val what = Class.forName(clazz)
    what.getDeclaredClasses.toList.find(x => x.getSimpleName == "Builder").
      get.newInstance().
      asInstanceOf[PluginBuilder[_]]
  }

  def setParams[T](parms : Map[String, String], pb : PluginBuilder[_]) : T = {
    parms.foldLeft(pb)((acc, p) => {
      acc.set(p._1, p._2)
    }).build().asInstanceOf[T]
  }

  def instantiate[T](clazz : String, parms : Map[String, String]) : T =
    setParams[T](parms, instantiate(clazz))

  def apply(eqParams: EQParams) : PluginHolder = {
    val integrationPlugin = instantiate[IntegrationPlugin](
      eqParams.getIntegrationPlugin, Map.empty
    ).setInputMappingsParms(eqParams.getInputPortParms.asScala).
      setOutputMappingParms(eqParams.getOutputPortParms.asScala).
      setTopologyLeftParms(eqParams.getLeftTopoParms.asScala).
      setTopologyRightParms(eqParams.getRightTopoParms.asScala)
    new PluginHolder(
      outputPlugin = instantiate[OutputPlugin](eqParams.getOutputClass, eqParams.getOutputParms.asScala),
      inputGen = instantiate[InputPacketPlugin](eqParams.getInputPacketClass, eqParams.getInputPacketParms.asScala),
      outPacket = instantiate[PacketPlugin](eqParams.getPacketEqClass, eqParams.getPacketEqParms.asScala),
      sievePlugin = instantiate[SievePlugin](eqParams.getSieveClass, eqParams.getSieveParms.asScala),
      inputMapping = integrationPlugin.inputMappings(),
      outputMapping = integrationPlugin.outputMappings(),
      topoRight = integrationPlugin.topologyRight(),
      topoLeft = integrationPlugin.topologyLeft()
    )
  }
}
