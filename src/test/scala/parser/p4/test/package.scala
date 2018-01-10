package parser.p4

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.util.UUID

import org.change.parser.p4.ControlFlowInterpreter
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Forward, InstructionBlock}

package object test {
 def executeAndPrintStats(ib: InstructionBlock, initial: List[State], codeAwareInstructionExecutor : CodeAwareInstructionExecutor): (List[State], List[State]) = {
    val init = System.currentTimeMillis()
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

}
