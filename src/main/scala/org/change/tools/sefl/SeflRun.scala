package org.change.tools.sefl

import java.lang.reflect.Field
import java.util.logging.Logger

import org.change.v2.cmd.EQParams
import org.change.plugins.eq._

case class SeflRunArgs(topoClass : String = "",
                   topoParms : Map[String, String] = Map.empty,
                   startFrom : String = ".*",
                   consumerPlugin : String = "org.change.plugins.runner.ToFileConsumer",
                   consumerParms : Map[String, String] = Map.empty,
                   inputPacketPlugin : String = "org.change.plugins.eq.TCPPacketPluginImpl",
                   inputPacketParms : Map[String, String] = Map.empty,
                   runnerPlugin : String = "org.change.plugins.runner.DefaultExecutorImplv3",
                   runnerParms : Map[String, String] = Map.empty)

object SeflRun {
  val usage = """
    Usage: java -jar <JAR> org.change.tools.sefl.SeflRun
            --topology-plugin <plugin_class>
            [--topology-parm <parm_name> <parm_value>]*
            [--start-from <regex>]?
            --runner-plugin <plugin_class>
            [--runner-parm <parm_name> <parm_value>]*
            --input-packet-plugin <plugin_class>
            [--input-packet-parm <parm_name> <parm_value>]*
            --consumer-plugin <plugin_class>
            [--consumer-parm <parm_name> <parm_value>]*
  """

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.err.println(usage)
      System.exit(1)
    }
    val arglist = args.toList
    val eqParams = SeflRunArgs()

    def nextOption(eqParams : SeflRunArgs, list: List[String]) : SeflRunArgs =
      list match {
        case Nil => eqParams
        case "--topology-plugin" :: value :: tail => nextOption(eqParams.copy(topoClass = value), tail)
        case "--runner-plugin" :: value :: tail => nextOption(eqParams.copy(runnerPlugin = value), tail)
        case "--input-packet-plugin" :: value :: tail => nextOption(eqParams.copy(inputPacketPlugin = value), tail)
        case "--consumer-plugin" :: value :: tail => nextOption(eqParams.copy(consumerPlugin = value), tail)
        case "--start-from" :: value :: tail => nextOption(eqParams.copy(startFrom = value), tail)
        case "--topology-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(topoParms = eqParams.topoParms + (name -> value)), tail)
        case "--consumer-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(consumerParms = eqParams.consumerParms + (name -> value)), tail)
        case "--input-packet-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(inputPacketParms = eqParams.inputPacketParms + (name -> value)), tail)
        case "--runner-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(runnerParms = eqParams.runnerParms + (name -> value)), tail)
      }
    val options = nextOption(eqParams, arglist)
    val topoPlugin = PluginHolder.instantiate[TopologyPlugin](options.topoClass, options.topoParms)
    val runnerPlugin = PluginHolder.instantiate[ExecutorPlugin](options.runnerPlugin, options.runnerParms)
    val inputPacketPlugin = if (options.inputPacketPlugin.isEmpty) {
        topoPlugin.defaultInputPlugin()
      } else {
        PluginHolder.instantiate[InputPacketPlugin](options.inputPacketPlugin, options.inputPacketParms)
      }
    val consumerPlugin = PluginHolder.instantiate[ExecutorConsumer](options.consumerPlugin, options.consumerParms)

    val compliance = topoPlugin.supports()
      .intersect(runnerPlugin.supports())
      .intersect(inputPacketPlugin.supports())
      .intersect(consumerPlugin.supports())
    if (compliance.isEmpty)
      throw new IllegalArgumentException("can't find a common version")
    val maxver = compliance.max
    Logger.getLogger(this.getClass.getName).info(s"running v$maxver")
    if (maxver == 3) {
      runnerPlugin.apply3(
        topoPlugin.apply3(),
        topoPlugin.startNodes().toSet,
        inputPacketPlugin.apply3(),
        consumerPlugin)
    } else if (maxver == 2) {
      runnerPlugin(topoPlugin(), topoPlugin.startNodes().toSet, inputPacketPlugin(), consumerPlugin)
    }
  }
}
