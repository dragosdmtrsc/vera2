package parser.p4

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock

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
