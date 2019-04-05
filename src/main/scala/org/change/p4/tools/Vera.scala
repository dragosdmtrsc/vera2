package org.change.p4.tools

import org.change.p4.control.queryimpl._
import org.change.p4.control.querylib.{DisjQuery, IndexOutOfBounds, IsValidQuery}
import org.change.p4.control.{RootEvaluator, _}
import org.change.p4.model.Switch
import z3.scala.Z3Context

object Vera {
  case class VeraArgs(p4File: String = "",
                      validate: Boolean = false,
                      portFilter: String = "",
                      commands: Set[String] = Set.empty,
                      printSolver: Boolean = false,
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
    val context = new Z3Context()
    // always call init prior to anything
    // it will gather SMT types, make functions,
    // do reference mapping, type inference and essentially
    // initialize all data structures required to run Vera
    val switch = Switch.fromFile(veraArgs.p4File).init(context)
    val input = MemoryInitializer.initialize(switch)(context)
    val qb = DisjQuery(switch, context)(
      new IsValidQuery(switch, context),
      new IndexOutOfBounds(switch, context)
    )
    var sema = new SemaWithEvents[P4RootMemory](switch)
    sema = if (veraArgs.validate) sema else sema.addListener(qb)
    val parsed = sema.parse(input)
    val postingress = sema.runControl("ingress", parsed)
    val BufferResult(cloned, goesOn, recirculated, dropped) =
      sema.buffer(postingress, input)
    val egressInput = goesOn.as[P4RootMemory]
    val egressOutcome = sema.runControl("egress", egressInput)
    val BufferResult(ecloned, egoesOn, erecirculated, edropped) =
      sema.buffer(egressOutcome, egressInput, ingress = false)
    val deparsed = sema.deparse(egoesOn)
    val evaluator = RootEvaluator(qb.buildSolver())
    if (veraArgs.portFilter.nonEmpty) {
      val target = SwitchTarget.fromRegex(veraArgs.portFilter)
      val targetConstraints = ConstraintBuilder(switch, context, target).toList
      // watch out for side effect here
      evaluator.constrain(targetConstraints)
    }
    // side effect is good here, we should always populate
    // the take_{}_{} primitives with their implementations
    evaluator.constrain(PacketWrapper(context).axioms)
    if (veraArgs.validate) {
      if (veraArgs.commands.isEmpty || veraArgs.portFilter.isEmpty) {
        throw new IllegalStateException(
          "to generate a Vera validation harness, one needs to provide" +
            " command files"
        )
      }
      val target = SwitchTarget.fromRegex(veraArgs.portFilter)
      val targetConstraints = ConstraintBuilder(switch, context, target).toList
      evaluator.constrained(deparsed.rootMemory.condition) {
        for (x <- veraArgs.commands) {
          val instance = P4Commands.fromFile(switch, x)
          val constraints = ConstraintBuilder(switch, context, instance).toList
          evaluator.constrained(constraints) {
            if (veraArgs.printSolver) {
              System.out.println(
                context.benchmarkToSMTLIBString(
                  "solver",
                  "",
                  "",
                  "",
                  evaluator.solver.getAssertions().toSeq,
                  context.mkTrue()
                )
              )
            }

            val pack = evaluator.eval(input.packet()).get
            val inputPort = evaluator
              .eval(input.standardMetadata().field("ingress_port"))
              .get
              .toInt
              .get
            val expectation = evaluator.eval(deparsed.packet()).get
            val egressPort = evaluator
              .eval(deparsed.standardMetadata().field("egress_port"))
              .get
              .toInt
              .get
            System.out.println(s"harness for $x")
            System.out.println("when input on port " + inputPort)
            System.out.println("a packet:")
            System.out.println(
              PacketLinearize.linearize(pack.as[AbsValueWrapper].value)
            )
            System.out.println("should come out on port " + egressPort)
            System.out.println("and look like:")
            System.out.println(
              PacketLinearize.linearize(expectation.as[AbsValueWrapper].value)
            )
          }
        }
      }
    } else {
      val limit = veraArgs.maxBugs
      val generateTestHarness = veraArgs.commands.nonEmpty && veraArgs.portFilter.nonEmpty
      val enumFunction = () => {
        evaluator.enumerate(limit) {
          evaluator
            .model()
            .flatMap(model => {
              qb.nodeToConstraint
                .find(r => {
                  model.evalAs[Boolean](r._2).getOrElse(false)
                })
                .map(r => {
                  val (loc, err) = r
                  val errCode = model
                    .evalAs[Int](qb.errCause(loc))
                    .map(ErrorLedger.error)
                    .getOrElse("can't resolve error")
                  System.out.println("at: ")
                  System.out.println(loc)
                  System.out.println("because: ")
                  System.out.println(errCode)
                  if (veraArgs.printSolver) {
                    System.out.println(
                      context.benchmarkToSMTLIBString(
                        "solver",
                        "",
                        "",
                        "",
                        evaluator.solver.getAssertions().toSeq,
                        context.mkTrue()
                      )
                    )
                  }
                  if (generateTestHarness) {
                    val inputPort = evaluator
                      .eval(input.standardMetadata().field("ingress_port"))
                      .get
                      .toInt
                      .get
                    val pack = evaluator.eval(input.packet()).get
                    System.out.println(s"here's how to trigger it")
                    System.out.println("input on port " + inputPort)
                    System.out.println("a packet:")
                    System.out.println(
                      PacketLinearize.linearize(pack.as[AbsValueWrapper].value)
                    )
                  }
                  context.mkNot(err)
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
