package org.change.v2.runners.experiments

import java.io._

import org.change.utils.prettifier.{JsonUtil, SomeWriter}
import org.change.v2.analysis.memory.State

object JSONTests {

  def main(argv: Array[String]) {
    val (successful, failed) = SEFLRunner.ex2

    val str = SomeWriter(Map[String, List[State]](("successful" -> successful),
      ("failed" -> failed)))

    val pstream = new PrintStream("outputs/json/out.json")
    pstream.println(str)
    pstream.close()


    val f = new File("outputs/json/out.json")
    val reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)))
    var line = reader.readLine()
    val sb = new StringBuilder()
    while (line != null) {
      sb.append(line)
      line = reader.readLine()
    }
    reader.close()

    val backInBusiness = JsonUtil.fromJson[Map[String, List[State]]](sb.toString)
    val str2 = SomeWriter(backInBusiness)
    val pstream2 = new PrintStream("outputs/json/out2.json")
    pstream2.println(str)
    pstream2.close()


  }

}