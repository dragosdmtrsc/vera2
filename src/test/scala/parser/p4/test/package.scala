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
import org.change.v2.analysis.processingmodels.instructions.{Call, CreateTag, Forward, InstructionBlock}
import org.change.v2.p4.model.{ISwitchInstance, Switch}
import org.change.v2.analysis.memory.TagExp.IntImprovements

package object test {
 def executeAndPrintStats(ib: Instruction, initial: List[State], codeAwareInstructionExecutor : CodeAwareInstructionExecutor): (List[State], List[State]) = {
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


  def anonymizeAndForward(port : String): Instruction = {
    InstructionBlock(
      Instruction(anonymize),
      Forward(port)
    )
  }

  def anonymize(state: State): (List[State], List[State]) = {
    (state.copy(memory = state.memory.copy(symbols = state.memory.symbols.map(r => s"${r._1}_anon${UUID.randomUUID().toString}" -> r._2))) :: Nil,
      Nil : List[State])
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


  def createConsumer(dir: String): (PrintStream, PrintStream, State => Unit) = {
    val failIndex = new PrintStream(s"$dir/index-fail.html")
    val successIndex = new PrintStream(s"$dir/index-success.html")
    successIndex.println("<ul>")
    failIndex.println("<ul>")
    val file = new File(dir).getAbsolutePath
    val outDir = new File(dir + "/outputs")
    if (!outDir.exists())
      outDir.mkdir()
    val printer = (s: State) => {
      val tmp = UUID.randomUUID().toString
      if (s.errorCause.isEmpty) {
        val ps = new PrintStream(s"$dir/outputs/success-$tmp.json")
        ps.println(s)
        ps.close()
        successIndex.println(s"""<li><a href=\"file://$file/outputs/success-$tmp.json\">${s.history.head}</a></li>""")
        successIndex.flush()
      } else {
        if (s.location.startsWith("switch.parser") && (s.errorCause.get.startsWith("Cannot resolve") ||
          s.errorCause.get.startsWith("Wrong choice"))) {
          // nothing here
        } else {
          val ps = new PrintStream(s"$dir/outputs/fail-$tmp.json")
          ps.println(s)
          ps.close()
          failIndex.println(s"""<li><a href=\"file://$file/outputs/fail-$tmp.json\">${s.errorCause.get} - ${s.history.head}</a></li>""")
          failIndex.flush()
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
              port: Int, genFactory: (Switch, ISwitchInstance) => ParserGenerator): Unit = {
    val sw = Switch.fromFile(p4)
    val switchInstance = SymbolicSwitchInstance.fromFileWithSyms("switch",
      ifaces,
      Map.empty,
      sw,
      dataplane)


    val parserGenerator: ParserGenerator = genFactory(sw, switchInstance)
    //-i 0@veth0 -i 1@veth2 -i 2@veth4 -i 3@veth6 -i 4@veth8 -i 5@veth10 -i 6@veth12 -i 7@veth14 -i 8@veth16 -i 64@veth250
    val res = new ControlFlowInterpreter(switchInstance, switch = sw,
      optParserGenerator = Some(
        parserGenerator
      )
    )
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
        Call(switchInstance.getName + ".generator." + packetLayout)
      ), State.clean, verbose = true)

    println(s"initial states gathered ${initial.size}")
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
