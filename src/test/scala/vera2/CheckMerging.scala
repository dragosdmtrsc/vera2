package vera2
import java.io.PrintStream

import com.microsoft.z3.Context
import org.change.p4.control._
import org.change.p4.control.queryimpl.{ConstraintBuilder, MemoryInitializer, P4RootMemory, TypeMapper}
import org.change.p4.model.Switch
import org.scalatest.FunSuite
import org.change.p4.tools._

class CheckMerging extends FunSuite {
  GlobalParms.checkMerging = true

  test("merge check") {
    val against = "inputs/test-cases/big-switch-sliced/switch-ppc.p4"
    val context = new Context()
    val switch: Switch = Switch.fromFile(against).init(context)
    var sema = new SemaWithEvents[P4RootMemory](switch)
    val cfg = sema.getCFG("ingress").graphView
    val ingressDot = new PrintStream("ingress.dot")
    cfg.mkDot(ingressDot, withHeader = true)
    ingressDot.close()
    val input = MemoryInitializer.initialize(switch)(context)
    val parsed = sema.parse(input)
    val postingress = sema.runControl("ingress", parsed)
    val BufferResult(cloned, goesOn, recirculated, dropped) =
      sema.buffer(postingress, input)
    val egressInput = goesOn.as[P4RootMemory]
    val egressOutcome = sema.runControl("egress", egressInput)
    val BufferResult(ecloned, egoesOn, erecirculated, edropped) =
      sema.buffer(egressOutcome, egressInput, ingress = false)
    val deparsed = sema.deparse(egoesOn)
    val evaluator = RootEvaluator(postingress)(context)
    // side effect is good here, we should always populate
    // the enum objects primitives with their implementations
    val enumAxioms = TypeMapper()(context).boundEnums()
    evaluator.constrain(enumAxioms)
    evaluator.constrain(
      parsed.packet().packetLength() ===
        parsed.packet().packetLength().zeros())
  }

}
