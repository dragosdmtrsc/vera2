package org.change.v2.runners.experiments

import java.io.{File, FileOutputStream, FilenameFilter, PrintStream}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.startpoints.StartPointParser
import org.change.symbolicexec.verification.RuleSetBuilder
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.analysis.executor.SpeculativeExecutor
import org.change.v2.analysis.executor.InstructionExecutor

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
object MultipleVmsFacultatea_new {

  
  def main(args: Array[String]) = {
    val clicksFolder = new File("src/main/resources/facultatea")

    val clicks = clicksFolder.list(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".click")
    }).sorted.map(clicksFolder.getPath + File.separatorChar + _)
    execClicks(clicks)
    execClicks(clicks, 
        outFile = "outputs/speculative_log.log", 
        executor = new SpeculativeExecutor())
    
  }

  def execClicks(clicks: Array[String], 
      outFile : String = "outputs/regular_log.log",
      executor : InstructionExecutor = InstructionExecutor()) = {
    
    println(clicks.size)
    val startOfBuild = System.currentTimeMillis()
    val fos = new FileOutputStream(outFile)

    val ctx = ClickExecutionContext.buildAggregated(
      clicks.map(ClickToAbstractNetwork.buildConfig(_, prefixedElements = true)),
      InterClickLinksParser.parseLinks("src/main/resources/facultatea/links.links"),
      verificationConditions = Nil, //RuleSetBuilder.buildRuleSetFromFile("src/main/resources/click_test_files/facultatea/rules.rules"),
      startElems = Some(StartPointParser.parseStarts(
        "src/main/resources/facultatea/start.start"
      ))
    ).setExecutor(executor).setLogger(new JsonLogger(fos))
    val startOfExec = System.currentTimeMillis()

    var crtExecutor = ctx
    var steps = 0
    while(! crtExecutor.isDone && steps < 1000) {
      steps += 1
      crtExecutor = crtExecutor.execute(verbose=false)
    }
    val doneExec = System.currentTimeMillis()
    fos.close()
    
    println(s"Done, we spent ${startOfExec - startOfBuild} of code generation and ${doneExec - startOfExec} of execution.")
    println(s"Check out the output file $outFile")
  }
}
