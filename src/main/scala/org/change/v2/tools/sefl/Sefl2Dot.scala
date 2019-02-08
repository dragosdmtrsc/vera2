package org.change.v2.tools.sefl

import java.io.{BufferedOutputStream, FileOutputStream, PrintWriter}

import org.change.v2.cmd.EQParams
import org.change.v2.plugins.eq.{PluginHolder, TopologyPlugin}
import org.change.v2.tools.eq.Equivalence.{findField, isScalar}
import org.change.v2.util.ToDot

case class DotArgs(topoClass : String = "",
                   topoParms : Map[String, String] = Map.empty,
                   startFrom : String = ".*",
                   outDot : String = "")

object Sefl2Dot {
  val usage =
    """--topology-plugin <plugin_class>
[--topology-parm <parm_name> <parm_value>]*
[--start-from <regex>]?
--out-dot <file_name>
    """.stripMargin

  def main(args: Array[String]): Unit = {
    if (args.size < 4) {
      System.err.println(usage)
      System.exit(1)
    }
    def nextOption(eqParams : DotArgs, list: List[String]) : DotArgs = {
      list match {
        case Nil => eqParams
        case "--topology-plugin" :: value :: tail => nextOption(eqParams.copy(topoClass = value), tail)
        case "--topology-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(topoParms = eqParams.topoParms + (name -> value)), tail)
        case "--start-from" :: regex :: tail =>
          nextOption(eqParams.copy(startFrom = regex), tail)
        case "--out-dot" :: file :: tail =>
          nextOption(eqParams.copy(outDot = file), tail)
      }
    }
    val options = nextOption(DotArgs(), args.toList)
    if (options.outDot.isEmpty || options.topoClass.isEmpty) {
      System.err.println(usage)
      System.exit(1)
    }
    val topo = PluginHolder.instantiate[TopologyPlugin](options.topoClass, options.topoParms)
    val g = topo()
    val os = new PrintWriter(new BufferedOutputStream(new FileOutputStream(options.outDot)))
    ToDot.apply("topology", g.toMap, topo.startNodes().filter(_.matches(options.startFrom)).toSet, os)
    os.close()
  }
}
