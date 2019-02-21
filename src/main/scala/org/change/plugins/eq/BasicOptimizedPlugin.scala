package org.change.plugins.eq

import java.io.File

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.processingmodels.Instruction

import scala.collection.mutable

class BasicOptimizedPlugin extends AbsIntegrationPlugin {
  private lazy val rright =
    OptimizedRouter.makeOptimizedRouterForBV_d(new File(topologyLeftParms("file")), "OPT_")
  private lazy val rleft =
    OptimizedRouter.makeOptimizedRouterForBV_d(new File(topologyRightParms("file")), "")

  private lazy val left : Map[String, Instruction] = rleft.instructions
  private lazy val right : Map[String, Instruction] = rright.instructions

  override def topologyLeft(): TopologyPlugin = new AbsTopologyPlugin {
    override def apply(): collection.Map[String, Instruction] = left
  }

  override def topologyRight(): TopologyPlugin = new AbsTopologyPlugin {
    override def apply(): collection.Map[String, Instruction] = right
  }

  override def inputMappings(): InputPortPlugin = {
    val inputDict = Map[String, String]("0" -> "OPT_0")
    new PortMapper(inputDict)
  }

  override def outputMappings(): OutputPortPlugin = {
    val outputDict = rleft.instructions.map(i => i._1 -> ("OPT_" + i._1))
    new PortMapper(outputDict)
  }
}
object BasicOptimizedPlugin {
  class Builder extends PluginBuilder[BasicOptimizedPlugin] {
    override def set(parm: String, value: String): PluginBuilder[BasicOptimizedPlugin] = this

    override def build(): BasicOptimizedPlugin = new BasicOptimizedPlugin()
  }
}

class SimpleRouterPlugin(f : String) extends AbsTopologyPlugin {
  lazy val opt = OptimizedRouter.makeOptimizedRouterForBV_d(new File(f), "")
  override def apply(): collection.Map[String, Instruction] = opt.instructions
}
object SimpleRouterPlugin {
  class Builder extends PluginBuilder[SimpleRouterPlugin] {
    var file = ""
    override def set(parm: String, value: String): PluginBuilder[SimpleRouterPlugin] = parm match {
      case "file" => file = value
        this
      case _ => throw new IllegalArgumentException(parm + " not recognized, expecting file")
    }
    override def build(): SimpleRouterPlugin = new SimpleRouterPlugin(file)
  }
}
