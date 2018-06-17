package parser.p4

import java.io.{BufferedOutputStream, File, FileOutputStream, PrintStream}
import java.util.UUID

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.ParserGenerator
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, CodeAwareInstructionExecutorWithListeners}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.v2.analysis.memory.TagExp.IntImprovements
import spray.json.{JsArray, JsString}

package object test {

  val PRINTER_OUTPUT_TO_FILE = true


 def executeAndPrintStats(ib: Instruction, initial: List[State],
                          codeAwareInstructionExecutor : CodeAwareInstructionExecutor): (List[State], List[State]) = {
    val init = System.currentTimeMillis()
    println("Ok now " + codeAwareInstructionExecutor.program.size)
    val (ok, failed) = initial.foldLeft((Nil, Nil): (List[State], List[State]))((acc, init) => {
      val (o, f) = codeAwareInstructionExecutor.execute(ib, init, true)
      (acc._1 ++ o, acc._2 ++ f)
    })
    println(s"Failed # ${failed.size}, Ok # ${ok.size}")
    println(s"Time is ${System.currentTimeMillis() - init}ms")
    (ok, failed)
  }

  def createReverse(withName : String): Map[String, Instruction] = {
    val dir = "inputs/reverse-p4/"
    val p4 = s"$dir/reverse.p4"
    val dataplane = s"$dir/commands-rev.txt"
    val res = ControlFlowInterpreter(p4, dataplane, Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu"), withName)
    CodeAwareInstructionExecutor.flattenProgram(res.instructions(), res.links())
  }

  def postParserInject(parserout : Instruction, program : Map[String, Instruction], name : String = "router"): Map[String, Instruction] = {
    val newparserout = InstructionBlock(
      parserout,
      Forward(s"$name.control.ingress.new")
    )
    val oldone = program(s"$name.control.ingress")
    (program + (s"$name.control.ingress" -> newparserout)) + (s"$name.control.ingress.new" -> oldone)
  }
  def postParserInjectCaie(parserout : Instruction, program : Map[String, Instruction], name : String = "router"): CodeAwareInstructionExecutor = {
    CodeAwareInstructionExecutor(postParserInject(parserout, program, name), Map.empty, new Z3BVSolver)
  }

  def printResults(dir: String, port: Int, ok: List[State], failed: List[State], okBase: String): Unit = {
    val psok = new BufferedOutputStream(new FileOutputStream(s"$dir/ok-port$port-$okBase.json"))
    JsonUtil.toJson(ok, psok)
    psok.close()
    val relevant = failed
    val psko = new BufferedOutputStream(new FileOutputStream(s"$dir/fail-port$port-$okBase.json"))
    JsonUtil.toJson(relevant, psko)
    psko.close()

    import org.change.v2.analysis.memory.jsonformatters.StateToJson._
    import spray.json._
    val psokpretty = new PrintStream(s"$dir/ok-port$port-pretty-$okBase.json")
    psokpretty.println(ok.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    psokpretty.close()


    val pskopretty = new PrintStream(s"$dir/fail-port$port-pretty-$okBase.json")
    pskopretty.println(relevant.toJson(JsonWriter.func2Writer[List[State]](u => {
      JsArray(u.map(_.toJson).toVector)
    })).prettyPrint)
    pskopretty.close()
  }

  def runAndLog[T](function0: Function0[T]): (T, Long) = {
    val init = System.currentTimeMillis()
    val funres = function0()
    (funres, System.currentTimeMillis() - init)
  }
  def createConsumer(dir: String): (PrintStream, PrintStream, State => Unit) = {
    val failIndex = new PrintStream(s"$dir/index-fail.html")
    val successIndex = new PrintStream(s"$dir/index-success.html")
    successIndex.println("<ul>")
    failIndex.println("<ul>")
    val file = new File(dir).getAbsolutePath
    val outDir = new File(dir + "/outputs")
    if (!outDir.exists())
      outDir.mkdir()
    var succIdx = 0
    var failIdx = 0
    val pscsv = new PrintStream(s"$dir/outputs/success.csv")
    val pscsvf = new PrintStream(s"$dir/outputs/fail.csv")
    val printer = (s: State) => {
      if (s.errorCause.isEmpty) {
        if (PRINTER_OUTPUT_TO_FILE) {
//          val ps = new PrintStream(s"$dir/outputs/success-$succIdx.json")
//          ps.println(JsArray(s.instructionHistory.reverse.zipWithIndex.map(ip =>
//            JsString(ip._1.toString)).toVector).toString())
//          ps.close()
          pscsv.println(s.history.reverse.mkString(","))
          if (succIdx % 1000 == 1)
            pscsv.flush()
        }
        successIndex.println(s"""<li><a href=\"file://$file/outputs/success-$succIdx.json\">${s.history.head}</a></li>""")
        if (succIdx % 1000 == 1)
          successIndex.flush()
        succIdx = succIdx + 1
      } else {
        if (s.location.startsWith("switch.parser") && (s.errorCause.get.startsWith("Cannot resolve") ||
          s.errorCause.get.startsWith("Wrong choice"))) {
          // nothing here
        } else {
          if (PRINTER_OUTPUT_TO_FILE) {
//            val ps = new PrintStream(s"$dir/outputs/fail-$failIdx.json")
//            ps.println(JsArray(s.instructionHistory.reverse.zipWithIndex.map(ip =>
//              JsString(ip._1.toString)).toVector).toString())
//            ps.close()
            pscsvf.println(s.history.reverse.mkString(",") + ',' + s.errorCause.get)
            if (failIdx % 1000 == 1)
              pscsvf.flush()
          }
          failIndex.println(s"""<li><a href=\"file://$file/outputs/fail-$failIdx.json\">${s.errorCause.get} - ${s.history.head}</a></li>""")
          if (failIdx % 1000 == 1)
            failIndex.flush()
          failIdx = failIdx + 1
        }
      }
    }
    (failIndex, successIndex, printer)
  }


  def setupAndRun(dir: String, p4: String, dataplane: String,
              postParserInjection: Instruction,
              ifaces: Map[Int, String],
              outputDir: String,
              packetLayout : String,
              port: Int, genFactory: (Switch, ISwitchInstance) => ParserGenerator,
              useSyms : Boolean  = false,
              forceSyms : Boolean = false): Unit = {
    assert(!forceSyms || useSyms)
    val sw = Switch.fromFile(p4)
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("switch",
      ifaces,
      Map.empty,
      sw,
      dataplane, forceSymbolic = forceSyms && useSyms)
    setupAndRun(dir, postParserInjection, outputDir, packetLayout, port, genFactory, useSyms, sw, switchInstance)
  }

  def setupAndRun(dir: String,
                          postParserInjection: Instruction,
                          outputDir: String,
                          packetLayout: String,
                          port: Int,
                          genFactory: (Switch, ISwitchInstance) => ParserGenerator,
                          useSyms: Boolean,
                          sw: Switch,
                          switchInstance: SymbolicSwitchInstance): Unit = {
    val parserGenerator: ParserGenerator = genFactory(sw, switchInstance)
    //-i 0@veth0 -i 1@veth2 -i 2@veth4 -i 3@veth6 -i 4@veth8 -i 5@veth10 -i 6@veth12 -i 7@veth14 -i 8@veth16 -i 64@veth250
    val res = if (!useSyms)
      new ControlFlowInterpreter(switchInstance, switch = sw,
        optParserGenerator = Some(
          parserGenerator
        )
      )
    else
      ControlFlowInterpreter.buildSymbolicInterpreter(switchInstance, sw, optParserGenerator = Some(
        parserGenerator
      ))
    val ib = Forward(s"${switchInstance.getName}.input.$port")
    val (failIndex, successIndex, printer) = createConsumer(outputDir)

    val codeAwareInstructionExecutor = new CodeAwareInstructionExecutorWithListeners(
      postParserInjectCaie(
        postParserInjection,
        CodeAwareInstructionExecutor(res.instructions(), res.links(), solver = new Z3BVSolver).program,
        switchInstance.getName
      ),
      successStateConsumers = printer :: Nil,
      failedStateConsumers = printer :: Nil
    )
    val (initial, _) = codeAwareInstructionExecutor.
      execute(InstructionBlock(
        CreateTag("START", 0),
        Call(switchInstance.getName + ".generator." + packetLayout),
        if (useSyms)
          res.initializeGlobally()
        else
          NoOp
      ), State.clean, verbose = true)

    println(s"initial states gathered ${initial.size}")
    println("initial state\n" + initial.head)

    val (ok: List[State], failed: List[State]) = executeAndPrintStats(
      ib,
      initial,
      codeAwareInstructionExecutor
    )
    printResults(dir, port, ok, failed.filter(r => {
      !(r.errorCause.exists(k => k.startsWith("Cannot resolve reference to")) &&
        r.history.head.contains("switch.parser."))
    }), "soso")
    successIndex.close()
    failIndex.close()
  }
}
