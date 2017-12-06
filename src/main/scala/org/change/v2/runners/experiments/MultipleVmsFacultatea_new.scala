package org.change.v2.runners.experiments

import java.io.{File, FileOutputStream, FilenameFilter}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.startpoints.StartPointParser
import org.change.v2.analysis.executor.{DecoratedInstructionExecutor, InstructionExecutor, SpeculativeExecutor}
import org.change.v2.analysis.executor.solvers.{SerialSMTSolver, Solver, Z3Solver}
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger

/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
object MultipleVmsFacultatea_new {


  def main(args: Array[String]) = {
    val clicksFolder = new File("src/main/resources/facultatea")
    var solver: Solver = null
    if (args.length < 1) {
      System.err.println("USAGE: [smt|z3|compare] {path_to_smt_exec}")
      System.exit(1)
    }
    else {
      if (args(0) == "smt") {
        if (args.length < 2) {
          System.err.println("USAGE: [smt|z3|compare] {path_to_smt_exec}")
          System.exit(1)
        }
        solver = new SerialSMTSolver(args(1))
      }
      else if (args(0) == "z3") {
        solver = new Z3Solver()
      }
      else if (args(0) == "compare")
        ???
    }
    val clicks = clicksFolder.list(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".click")
    }).sorted.map(clicksFolder.getPath + File.separatorChar + _)
    execClicks(clicks,
      outFile = String.format("outputs/%sregular_log.log", args(0)),
      executor = new DecoratedInstructionExecutor(solver))
    execClicks(clicks,
      outFile = String.format("outputs/%sspeculative_log.log", args(0)),
      executor = new SpeculativeExecutor(solver))

  }

  def execClicks(clicks: Array[String],
                 outFile: String = "outputs/regular_log.log",
                 executor: InstructionExecutor = InstructionExecutor()) = {

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
    while (!crtExecutor.isDone && steps < 1000) {
      steps += 1
      crtExecutor = crtExecutor.execute(verbose = false)
    }
    val doneExec = System.currentTimeMillis()
    fos.close()

    println(s"Done, we spent ${startOfExec - startOfBuild} of code generation and ${doneExec - startOfExec} of execution.")
    println(s"Check out the output file $outFile")
  }
}
