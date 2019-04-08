package vera2
import com.microsoft.z3.{Context, Global}
import org.change.p4.control._
import org.change.p4.control.queryimpl._
import org.change.p4.model.Switch
import org.change.p4.tools._
import org.scalatest.FunSuite

import scala.collection.JavaConverters._

class SwitchCommands extends FunSuite {
  val commands: List[String] = List(
    /*"inputs/test-cases/big-switch/commands-switch.txt",
    "inputs/test-cases/big-switch/no_entries.txt",
    */"inputs/test-cases/big-switch/pd-L2FloodTest.txt"/*,
    "inputs/test-cases/big-switch/pd-L2LearningTest.txt",
    "inputs/test-cases/big-switch/pd-L2QinQTest.txt",
    "inputs/test-cases/big-switch/pd-L2Test.txt",
    "inputs/test-cases/big-switch/pd-L2VxlanTunnelTest.txt",
    "inputs/test-cases/big-switch/pd-L3Ipv4Test.txt",
    "inputs/test-cases/big-switch/pd-L3Ipv6Test.txt",
    "inputs/test-cases/big-switch/pd-L3VxlanTunnelTest.txt",
    "inputs/test-cases/big-switch/table_dump_full.txt",
    "inputs/test-cases/big-switch/table_dump_simple.txt"*/
  )

  val against = "inputs/test-cases/big-switch/switch-ppc.p4"
  val portFilter = ".*"
  val context = new Context(Map("model_compress" -> "false").asJava)
  Global.setParameter("verbose", "15")
  val switch: Switch = Switch.fromFile(against).init(context)
  for (x <- commands) {
    test(s"$against smoke testing commands $x") {
      var sema = new SemaWithEvents[P4RootMemory](switch)
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
      val evaluator = RootEvaluator(deparsed)(context)
      val target = SwitchTarget.fromRegex(portFilter)
      val targetConstraints = ConstraintBuilder(switch, context, target).toList
      evaluator.constrain(targetConstraints)
      // side effect is good here, we should always populate
      // the enum objects primitives with their implementations
      val enumAxioms = TypeMapper()(context).boundEnums()
      evaluator.constrain(enumAxioms)
      evaluator.constrain(
        parsed.packet().packetLength() ===
          parsed.packet().packetLength().zeros())
      val instance = P4Commands.fromFile(switch, x)
      val constraints = ConstraintBuilder(switch, context, instance)
      evaluator.constrained(constraints) {
        if (!evaluator.hasResult) {
          Vera.printSolver(against, evaluator, Some(x))
          System.err.println(evaluator.reasonUnknown())
          assert(evaluator.hasResult)
        }
        val pack = evaluator.eval(input.packet().contents()).get
        val packlen =
          evaluator.eval(input.packet().packetLength())
            .get
            .toInt
            .get
            .toInt
        val inputPort = evaluator
          .eval(input.standardMetadata().field("ingress_port"))
          .get
          .toInt
          .get
        val expectation = evaluator.eval(deparsed.packet().contents()).get
        val expectationlen = evaluator
          .eval(deparsed.packet().packetLength())
          .get
          .toInt
          .get
          .toInt
        val egressPort = evaluator
          .eval(deparsed.standardMetadata().field("egress_port"))
          .get
          .toInt
          .get
        System.out.println(s"harness for $x")
        System.out.println("when input on port " + inputPort)
        System.out.println("a packet:")
        System.out.println(
          PacketLinearize.linearize(pack.as[AbsValueWrapper].value,
            packlen,
            context)
        )
        System.out.println("should come out on port " + egressPort)
        System.out.println("and look like:")
        System.out.println(
          PacketLinearize.linearize(expectation.as[AbsValueWrapper].value,
            expectationlen,
            context)
        )
      }
    }
  }

}
