package org.change.utils

import java.io.{File, FileOutputStream, OutputStream, PrintStream}

import org.change.v2.p4.model.Switch

import scala.collection.JavaConversions._

object P4Helpers {
  def dumpCommands(switch: Switch, file : String): Unit = {
    val ps = new PrintStream(file)
    dumpCommands(switch, ps)
    ps.close()
  }
  def dumpCommands(switch: Switch): Unit = {
    dumpCommands(switch, System.out)
  }
  def dumpCommands(switch: Switch, file : PrintStream): Unit = {
    for (t <- switch.getDeclaredTables)
      file.print(s"table_dump $t")
  }

  def main(args: Array[String]): Unit = {
    val dir = "inputs/simple-nat-testing/"
    val p4 = s"$dir/simple_nat-ppc.p4"
    P4Helpers.dumpCommands(Switch.fromFile(p4))
  }

}
