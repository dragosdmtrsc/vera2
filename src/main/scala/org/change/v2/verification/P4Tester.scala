package org.change.v2.verification

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.utils.prettifier.JsonUtil

/**
  * Created by mateipopovici on 14/12/17.
  */
object P4Tester {

  def main(args: Array[String]): Unit = {
    // println("=== Stuck states ===")


    val p4 = "inputs/simple-nat/simple_nat-ppc.p4"
    val dataplane = "inputs/simple-nat/commands.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), "router")


    //res.instructions() foreach {case (port, i) => println (port + "-->" + Policy.show(i))}

    Printer.vizPrinter((res.instructions(),res.links()),"big.html")


    //print(res.toDot())



    /*
    val fin = "inputs/simple-nat/graph.dot"
    val ps = new PrintStream(fin)
    ps.println(res.toDot())
    ps.close()
    import sys.process._
    s"dot -Tpng $fin -O" !
*/
    //println(JsonUtil.toJson(res.instructions()))

    /*
    val ps = new PrintStream("inputs/simple-nat/p4nat.json")
    ps.println(JsonUtil.toJson(res.instructions()))
    ps.println(JsonUtil.toJson(res.links))

    ps.close()
*/

  }
}