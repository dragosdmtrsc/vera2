package vera2

import java.io.PrintStream

import org.change.p4.control.queryimpl.{MemoryInitializer, P4RootMemory, TypeMapper}
import org.change.p4.control.{QueryDrivenSemantics, RootEvaluator, SolveTables}
import org.change.p4.model.Switch
import org.scalatest.FunSuite
import com.microsoft.z3.Context
import org.change.p4.tools._

class DeparserTest extends FunSuite {
  //TODO: fix broken unit tests
  for (against <- battery) {
    test(s"parse/deparse consistency $against")(() => {
      val context = new Context()
      val sw = Switch.fromFile(against).init(context)
      val memory = MemoryInitializer.initialize(sw)(context)
      val sema = new QueryDrivenSemantics[P4RootMemory](sw)
      val parserOut = sema.parse(memory)
      val deparserOut = sema.deparse(parserOut)
      val rootEvaluator = RootEvaluator(deparserOut)(context)
      val evald = rootEvaluator.eval(deparserOut.packet()).get
      val initial = rootEvaluator.eval(memory.packet()).get
      val tm = TypeMapper()(context)
      System.err.println(initial)
      System.err.println(evald)
      assert(rootEvaluator.always(memory.packet() === deparserOut.packet()))
    })
    test(s"packet generation capabilities $against") {
      val context = new Context()
      val sw = Switch.fromFile(against).init(context)
      val memory = MemoryInitializer.initialize(sw)(context)
      val sema = new QueryDrivenSemantics[P4RootMemory](sw)
      val parserOut = sema.parse(memory)
      val rootEvaluator = RootEvaluator(
        parserOut.where(parserOut.packet() === parserOut.packet().zeros()).as[P4RootMemory]
      )(context)
      if (against.endsWith("switch-ppc.p4")) {
        val ps2 = new PrintStream("solver.smt")
        ps2.println(rootEvaluator.solver)
        ps2.close()
      }
      assert(rootEvaluator.hasResult)
    }
  }
}
