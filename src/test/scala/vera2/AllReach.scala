package vera2
import com.microsoft.z3.Context
import org.change.p4.control.{BufferResult, RootEvaluator, SemaWithEvents}
import org.change.p4.control.queryimpl.{MemoryInitializer, P4RootMemory}
import org.change.p4.model.Switch
import org.scalatest.FunSuite
import org.change.p4.tools._

class AllReach extends FunSuite {
  for (tc <- battery) {
    test(s"some input packets are forwarded $tc") {
      val ctx = new Context()
      val switch = Switch.fromFile(tc).init(ctx)
      val input = MemoryInitializer.initialize(switch)(ctx)
      var sema = new SemaWithEvents[P4RootMemory](switch)
      val parsed = sema.parse(input)
      val postingress = sema.runControl("ingress", parsed)
      val BufferResult(cloned, goesOn, recirculated, dropped) =
        sema.buffer(postingress, input)
      val egressInput = goesOn.as[P4RootMemory]
      val egressOutcome = sema.runControl("egress", egressInput)
      val BufferResult(ecloned, egoesOn, erecirculated, edropped) =
        sema.buffer(egressOutcome, egressInput, ingress = false)
      val deparsed = sema.deparse(egoesOn)
      val evaluator = RootEvaluator(deparsed)(ctx)
      assert(evaluator.hasResult)
    }
  }
}
