package org.change.v2.runners.experiments

import java.io.{File, FileOutputStream, FilenameFilter, PrintStream}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.startpoints.StartPointParser
import org.change.symbolicexec.verification.RuleSetBuilder
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.analysis.executor.ClickAsyncExecutor
import org.change.v2.analysis.memory.State

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
object MultipleVmsFacultatea {

  def main(args: Array[String]) = {
    val clicksFolder = new File("src/main/resources/facultatea")

    val clicks = clicksFolder.list(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".click")
    }).sorted.map(clicksFolder.getPath + File.separatorChar + _)

    val startOfBuild = System.currentTimeMillis()
    
    val rawLinks = InterClickLinksParser.parseLinks("src/main/resources/facultatea/links.links")    
    val startElems : Option[Iterable[(String, String, Int)]] = Some(StartPointParser.parseStarts
         ("src/main/resources/facultatea/start.start"))
    
    val (exec, start) = ClickAsyncExecutor.buildAggregated(
        clicks.map(ClickToAbstractNetwork.buildConfig(_, prefixedElements = true)), 
        rawLinks, 
        startElems)

    val startOfExec = System.currentTimeMillis()

    for (s <- start)
      exec.pushForward(s, true)

    while (!exec.isOver){}

    val doneExec = System.currentTimeMillis()
    exec.exec.shutdown()

    var jLogger = new JsonLogger(new FileOutputStream("outputs/parallel_exec_ok.log"))
    jLogger.log(exec.getOks)
    jLogger = new JsonLogger(new FileOutputStream("outputs/parallel_exec_fail.log"))
    jLogger.log(exec.getFails)

    println(s"Done, we spent ${startOfExec - startOfBuild} of code generation and ${doneExec - startOfExec} of execution.")
  }

}
