package org.change.v2.tools.sefl

import java.io.{BufferedOutputStream, FileOutputStream, PrintWriter}

import org.change.v2.plugins.eq.{PluginHolder, TopologyPlugin}
import org.change.v2.util.ToDot

case class ToSEFLArgs(topoClass : String = "",
                   topoParms : Map[String, String] = Map.empty,
                   startFrom : String = ".*", outFile : String = "")

object ToSEFL {
  val usage =
    """--topology-plugin <plugin_class>
[--topology-parm <parm_name> <parm_value>]*
[--start-from <regex>]?
--out-file <file_name>
    """.stripMargin

  def main(args: Array[String]): Unit = {
    val arglist = args.toList
    if (args.length < 4) {
      System.err.println(usage)
      System.exit(1)
    }
    def nextOption(eqParams : ToSEFLArgs, list: List[String]) : ToSEFLArgs = {
      list match {
        case Nil => eqParams
        case "--topology-plugin" :: value :: tail => nextOption(eqParams.copy(topoClass = value), tail)
        case "--topology-parm" :: name :: value :: tail =>
          nextOption(eqParams.copy(topoParms = eqParams.topoParms + (name -> value)), tail)
        case "--start-from" :: regex :: tail =>
          nextOption(eqParams.copy(startFrom = regex), tail)
        case "--out-file" :: file :: tail =>
          nextOption(eqParams.copy(outFile = file), tail)
      }
    }
    val options = nextOption(ToSEFLArgs(), args.toList)
    if (options.outFile.isEmpty || options.topoClass.isEmpty) {
      System.err.println(usage)
      System.exit(1)
    }
    val topo = PluginHolder.instantiate[TopologyPlugin](options.topoClass, options.topoParms)
    val g = topo()
    val os = new PrintWriter(new BufferedOutputStream(new FileOutputStream(options.outFile)))
    ToDot.apply("topology", g.toMap, topo.startNodes().filter(_.matches(options.startFrom)).toSet, os)
    os.close()
  }

}
