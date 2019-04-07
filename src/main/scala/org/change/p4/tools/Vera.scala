package org.change.p4.tools

import com.microsoft.z3.{Context, Expr, Global}
import org.change.p4.control.queryimpl._
import org.change.p4.control.querylib.{DisjQuery, IndexOutOfBounds, IsValidQuery}
import org.change.p4.control.types.BoundedInt
import org.change.p4.control.{RootEvaluator, _}
import org.change.p4.model.Switch
import org.change.utils.Z3Helper._

import collection.JavaConverters._

object Vera {
  case class VeraArgs(p4File: String = "",
                      validate: Boolean = false,
                      portFilter: String = "",
                      commands: Set[String] = Set.empty,
                      printSolver: Boolean = false,
                      z3Verbose : Boolean = false,
                      maxBugs: Int = 50) {
    def addCommandsFile(commandsFile: String): VeraArgs =
      copy(commands = commands + commandsFile)
  }
  val usage =
    """
    Usage: java -jar <JAR> org.change.tools.sefl.Vera
            [--print-solver]? - dumps all solvers used throughout the system [default: false]
            [--validate]? - must be accompanied with commands and allowed ports -> produces
                            a packet + input port -> expected outcome + output port
            [--allowed-ports <regex>]? - a regular expression which filters out the allowed
            port numbers for egress_port and ingress_port
            [--commands <file>]* - commands.txt files used to bug check or validate Vera with
            p4file
  """

  def main(args: Array[String]) {
    def parse(argList: List[String], vera: VeraArgs): (List[String], VeraArgs) =
      argList match {
        case "--print-solver" :: tl =>
          parse(tl, vera.copy(printSolver = true))
        case "--validate" :: tl =>
          parse(tl, vera.copy(validate = true))
        case "--allowed-ports" :: rex :: tl =>
          parse(tl, vera.copy(portFilter = rex))
        case "--commands" :: cmds :: tl =>
          parse(tl, vera.addCommandsFile(cmds))
        case "--help" :: _ =>
          System.out.println(usage)
          System.exit(0)
          (argList, vera)
        case _ => (argList, vera)
      }
    var (remaining, veraArgs) = parse(args.toList, VeraArgs())
    if (remaining.size != 1) {
      throw new IllegalArgumentException("unknown option " + remaining.size)
    }
    veraArgs = veraArgs.copy(p4File = remaining.head)
    System.out.println(s"preparing to run vera against ${veraArgs.p4File}")
    if (veraArgs.z3Verbose) {
      Global.setParameter("verbose", "15")
    }
    val context = new Context(Map(
      "trace" -> (if (veraArgs.z3Verbose) "true" else "false"),
      "model_compress" -> "false"
    ).asJava)
    // always call init prior to anything
    // it will gather SMT types, make functions,
    // do reference mapping, type inference and essentially
    // initialize all data structures required to run Vera
    val switch = Switch.fromFile(veraArgs.p4File).init(context)
    val input = MemoryInitializer.initialize(switch)(context)
    val fieldList = input.field(FIELD_LIST_REF)
      .value
      .ofType
      .asInstanceOf[BoundedInt]
    val qb = DisjQuery(switch, context)(
      new IsValidQuery(switch, context),
      new IndexOutOfBounds(switch, context)
    )
    var sema = new SemaWithEvents[P4RootMemory](switch)
    val parsed = sema.parse(input)
    sema = if (veraArgs.validate) sema else sema.addListener(qb)
    val postingress = sema.runControl("ingress", parsed)
    val BufferResult(cloned, goesOn, recirculated, dropped) =
      sema.buffer(postingress, input)
//    var goesOn = postingress
    val egressInput = goesOn.as[P4RootMemory]
    val egressOutcome = sema.runControl("egress", egressInput)
    val BufferResult(ecloned, egoesOn, erecirculated, edropped) =
      sema.buffer(egressOutcome, egressInput, ingress = false)
//    var egoesOn = egressOutcome
    val deparsed = sema.deparse(egoesOn)
    val evaluator = RootEvaluator(qb.buildSolver(), context)
    if (veraArgs.portFilter.nonEmpty) {
      val target = SwitchTarget.fromRegex(veraArgs.portFilter)
      val targetConstraints = ConstraintBuilder(switch, context, target).toList
      // watch out for side effect here
      evaluator.constrain(targetConstraints)
    }
    // side effect is good here, we should always populate
    // the take_{}_{} primitives with their implementations
    val enumAxioms = TypeMapper()(context).boundEnums()
    evaluator.constrain(enumAxioms)
    evaluator.constrain(parsed.packet().packetLength() ===
      parsed.packet().packetLength().zeros()
    )
    if (veraArgs.validate) {
      if (veraArgs.commands.isEmpty || veraArgs.portFilter.isEmpty) {
        throw new IllegalStateException(
          "to generate a Vera validation harness, one needs to provide" +
            " command files"
        )
      }
      val target = SwitchTarget.fromRegex(veraArgs.portFilter)
      evaluator.constrained(deparsed.rootMemory.condition) {
        for (x <- veraArgs.commands) {
          val instance = P4Commands.fromFile(switch, x)
          val constraints = ConstraintBuilder(switch, context, instance).toList
          evaluator.constrained(constraints) {
            if (veraArgs.printSolver) {
              System.out.println(
                evaluator.solver.toString
              )
            }
            val pack = evaluator.eval(input.packet().contents()).get
            val packlen = evaluator.eval(input.packet().packetLength())
              .get.toInt.get.toInt
            val inputPort = evaluator
              .eval(input.standardMetadata().field("ingress_port"))
              .get
              .toInt
              .get
            val expectation = evaluator.eval(deparsed.packet().contents()).get
            val expectationlen = evaluator.eval(deparsed.packet().packetLength())
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
              PacketLinearize.linearize(pack.as[AbsValueWrapper].value, packlen, context)
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
    } else {
      val limit = veraArgs.maxBugs
      val generateTestHarness = veraArgs.commands.nonEmpty && veraArgs.portFilter.nonEmpty
      val enumFunction = () => {
        if (veraArgs.printSolver) {
          System.out.println(
            evaluator.solver.toString
          )
        }
        evaluator.enumerate(limit) {
          evaluator
            .model()
            .flatMap(model => {
              qb.nodeToConstraint
                .find(r => {
                  model.eval(r._2, true).getBool.getOrElse(false)
                })
                .map(r => {
                  val (loc, err) = r
                  val errno = model.eval(qb.errCause(loc), false).getBool
                  val errCode = model
                    .eval(qb.errCause(loc), false).getInt
                    .map(_.toInt)
                    .map(ErrorLedger.error)
                    .getOrElse("can't resolve error")
                  System.out.println("at: ")
                  System.out.println(loc)
                  System.out.println("because: ")
                  System.out.println(errCode)
                  if (generateTestHarness) {
                    val inputPort = evaluator
                      .eval(input.standardMetadata().field("ingress_port"))
                      .get
                      .toInt
                      .get
                    val pack = evaluator.eval(input.packet().contents()).get
                    val packlen = evaluator
                      .eval(input.packet().packetLength())
                      .get.toInt.get
                    System.out.println(s"here's how to trigger it")
                    System.out.println("input on port " + inputPort)
                    System.out.println(s"a packet of length $packlen")
                    System.out.println(
                      PacketLinearize.linearize(pack.as[AbsValueWrapper].value,
                        packlen.toInt,
                        context)
                    )
                  }
                  context.mkNot(qb.getLocationSelector(loc))
                })
            })
        }
      }
      if (veraArgs.commands.nonEmpty) {
        for (x <- veraArgs.commands) {
          val instance = P4Commands.fromFile(switch, x)
          val constraints = ConstraintBuilder(switch, context, instance).toList
          System.out.println(s"now checking dataplane $x")
          evaluator.constrained(constraints) {
            enumFunction()
          }
        }
      } else {
        enumFunction()
      }
    }
  }
}
