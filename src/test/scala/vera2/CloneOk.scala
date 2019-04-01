package vera2

import org.change.parser.p4.RootEvaluator
import org.change.parser.p4.control.{BufferResult, QueryDrivenSemantics}
import org.change.parser.p4.control.queryimpl.{MemoryInitializer, P4RootMemory}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite
import org.change.v3.semantics.context
import org.change.parser.p4.control._

class CloneOk extends FunSuite {
  test("clone session is correctly set up") {
    val against = "inputs/test-cases/simple-nat/simple_nat-ppc.p4"
    val sw = SolveTables(Switch.fromFile(against))
    val sema = new QueryDrivenSemantics[P4RootMemory](sw)
    val memory = MemoryInitializer.initialize(sw)(context)
    val parserOut = sema.parse(memory)
    val ingressOut = sema.runControl("ingress", parserOut)
    val BufferResult(cloned, normal, recirculated, dropped) = sema.buffer(ingressOut, memory)
    val cd = cloned.rootMemory
    val root = RootEvaluator(cloned)(context)
    assert(root.hasResult)
    assert(root.always(cloned.standardMetadata().field(CLONE_SPEC) ===
      cloned.standardMetadata().field(CLONE_SPEC).int(250)))
  }
  test("no clone session when not needed") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val sw = SolveTables(Switch.fromFile(against))
    val sema = new QueryDrivenSemantics[P4RootMemory](sw)
    val memory = MemoryInitializer.initialize(sw)(context)
    val parserOut = sema.parse(memory)
    val ingressOut = sema.runControl("ingress", parserOut)
    val BufferResult(cloned, normal, recirculated, dropped) = sema.buffer(ingressOut, memory)
    val cd = cloned.rootMemory
    val root = RootEvaluator(cloned)(context)
    assert(!root.hasResult)
  }

  test("egress spec is correctly set up") {
    val against = "inputs/test-cases/simple-nat/simple_nat-ppc.p4"
    val sw = SolveTables(Switch.fromFile(against))
    val sema = new QueryDrivenSemantics[P4RootMemory](sw)
    val memory = MemoryInitializer.initialize(sw)(context)
    val parserOut = sema.parse(memory)
    val ingressOut = sema.runControl("ingress", parserOut)
    val BufferResult(cloned, normal, recirculated, dropped) = sema.buffer(ingressOut, memory)
    val cd = cloned.rootMemory
    val root = RootEvaluator(normal)(context)
    val espec = normal.standardMetadata().field("egress_spec")
    assert(root.never(espec === espec.int(DROP_VALUE)))
  }

}
