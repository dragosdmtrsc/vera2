package org.change.tools.eq

import org.change.v2.analysis.equivalence.Equivalence
import org.change.plugins.eq._
import System._
import java.lang.reflect.Field
import java.util.Objects

import org.change.v2.cmd.EQParams

object Equivalence {

  val usage = """
    Usage: java -jar <JAR> org.change.tools.eq.Equivalence
            --integration-plugin <plugin_class>
            [--input-port-parm <parm_name> <parm_value>]*
            [--output-port-parm <parm_name> <parm_value>]*
            --input-packet-plugin <plugin_class>
            [--input-packet-parm <parm_name> <parm_value>]*
            --output-packet-eq-plugin <plugin_class>
            [--output-packet-parm <parm_name> <parm_value>]*
            --output-plugin <plugin_class>
            [--output-parm <parm_name> <parm_value>]*
            --sieve-plugin <plugin_class>
            [--sieve-parm <parm_name> <parm_value>]*
            --left-topo-plugin <plugin_class>
            [--left-topo-parm <parm_name> <parm_value>]*
            --right-topo-plugin <plugin_class>
           [--right-topo-parm <parm_name> <parm_value>]
  """

  def findField(name : String, obj : EQParams): Field = {
    val declFields = obj.getClass.getDeclaredFields
    declFields.toList.find(fld => {
      val an = fld.getDeclaredAnnotation(classOf[EQParams.Parm])
      if (an != null) {
        an.value() == name
      } else {
        false
      }
    }).get
  }
  def isScalar(f : Field): Boolean = f.getType.equals(classOf[String])

  def main(args: Array[String]) {
    if (args.length < 10) {
      System.err.println(usage)
      System.exit(1)
    }
    val arglist = args.toList
    val eqParams = new EQParams()

    def nextOption(eqParams : EQParams, list: List[String]) : EQParams = {
      list match {
        case Nil => eqParams
        case nm :: tail =>
          val fld = findField(nm, eqParams)
          fld.setAccessible(true)
          if (isScalar(fld)) {
            fld.set(eqParams, tail.head)
            nextOption(eqParams, tail.tail)
          } else {
            val dict = fld.get(eqParams).asInstanceOf[java.util.Map[String, String]]
            dict.put(tail.head, tail.tail.head)
            nextOption(eqParams, tail.tail.tail)
          }
      }
    }
    val options = nextOption(eqParams, arglist)
    PluginHolder(options)()
  }
}
