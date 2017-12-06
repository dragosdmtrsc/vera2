package org.change.v2.runners.experiments

import java.io.{File, FileOutputStream, PrintStream}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext

object TestSymnetBasic {

  def main(args: Array[String]) {
    val clickConfig = "src/main/resources/click_test_files/Template2.click"
    val absNet = ClickToAbstractNetwork.buildConfig(clickConfig)
    val executor = ClickExecutionContext.fromSingle(absNet, initialIsClean = true)

    var crtExecutor = executor
    while (!crtExecutor.isDone) {
      crtExecutor = crtExecutor.execute(verbose = true)
    }

    val outputFileName = "template2.output"
    val output = new PrintStream(new FileOutputStream(new File(outputFileName)))
    //    output.println(verboselyStringifyStates(true, true, true))
    output.close()
    println("Done. Output @ " + outputFileName)
  }
}
