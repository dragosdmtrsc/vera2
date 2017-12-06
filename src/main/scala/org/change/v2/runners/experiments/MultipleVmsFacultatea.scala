package org.change.v2.runners.experiments

import java.io.{File, FileOutputStream, FilenameFilter}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.startpoints.StartPointParser
import org.change.v2.analysis.executor.{ClickAsyncExecutor, DecoratedInstructionExecutor}
import org.change.v2.analysis.executor.solvers.{SMTSolver, Solver, Z3Solver}
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger

/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
object MultipleVmsFacultatea {

  def main(args: Array[String]) = {
    var solver: Solver = null
    if (args.length < 1) {
      System.err.println("USAGE: [smt|z3] {path_to_smt_exec}")
      System.exit(1)
    }
    else {
      if (args(0) == "smt") {
        if (args.length < 2) {
          System.err.println("USAGE: [smt|z3] {path_to_smt_exec}")
          System.exit(1)
        }
        solver = new SMTSolver(args(1))
      }
      else {
        solver = new Z3Solver()
      }
    }

    val clicksFolder = new File("src/main/resources/facultatea")

    val clicks = clicksFolder.list(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith(".click")
    }).sorted.map(clicksFolder.getPath + File.separatorChar + _)

    val startOfBuild = System.currentTimeMillis()

    val rawLinks = InterClickLinksParser.parseLinks("src/main/resources/facultatea/links.links")
    val startElems: Option[Iterable[(String, String, String)]] = Some(StartPointParser.parseStarts
    ("src/main/resources/facultatea/start.start"))

    val (exec, start) = ClickAsyncExecutor.buildAggregated(
      clicks.map(ClickToAbstractNetwork.buildConfig(_, prefixedElements = true)),
      rawLinks,
      startElems,
      1,
      instrExec = new DecoratedInstructionExecutor(solver))

    val startOfExec = System.currentTimeMillis()

    for (s <- start)
      exec.pushForward(s, true)
    while (!exec.isOver) {}

    val doneExec = System.currentTimeMillis()
    exec.exec.shutdown()

    var jLogger = new JsonLogger(new FileOutputStream("outputs/parallel_exec_ok.log"))
    jLogger.log(exec.getOks)
    jLogger = new JsonLogger(new FileOutputStream("outputs/parallel_exec_fail.log"))
    jLogger.log(exec.getFails)

    println(s"Done, we spent ${startOfExec - startOfBuild} of code generation and ${doneExec - startOfExec} of execution.")
  }

}
