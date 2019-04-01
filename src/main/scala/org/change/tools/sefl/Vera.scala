package org.change.tools.sefl

import org.change.parser.p4.{DisjQuery, IndexOutOfBounds, IsValidQuery}
import org.change.parser.p4.control.{BufferResult, P4Commands, SemaWithEvents, SolveTables}
import org.change.parser.p4.control.queryimpl.{ConstraintBuilder, MemoryInitializer, P4RootMemory}
import org.change.v2.p4.model.Switch
import org.change.v3.semantics.context

case class VeraArgs(p4File : String = "",
                    pipelines : Set[String] = Set.empty,
                    commands : Set[String] = Set.empty,
                    maxBugs : Int = 50) {
  def addPipeline(pipeline : String): VeraArgs =
    copy(pipelines = pipelines + pipeline)

  def addCommandsFile(commandsFile : String): VeraArgs =
    copy(commands = commands + commandsFile)
}

object Vera {
  val usage = """
    Usage: java -jar <JAR> org.change.tools.sefl.Vera
            [--commands <file>]*
            [--pipeline <pipeline-name=ingress|egress>]*
            p4file
  """

  def main(args: Array[String]) {
    def parse(argList : List[String], vera: VeraArgs) : (List[String], VeraArgs) = argList.head match {
      case "--pipeline" =>
        parse(argList.tail.tail, vera.addPipeline(argList.tail.head))
      case "--commands" =>
        parse(argList.tail.tail, vera.addCommandsFile(argList.tail.head))
      case "--help" =>
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
    val switch = SolveTables(Switch.fromFile(veraArgs.p4File))
    val input = MemoryInitializer.initialize(switch)(context)
    val qb = DisjQuery(switch, context)(
      new IsValidQuery(switch, context),
      new IndexOutOfBounds(switch, context)
    )
    val sema = new SemaWithEvents[P4RootMemory](switch).addListener(qb)
    val parsed = sema.parse(input)
    val postingress = sema.runControl("ingress", parsed)
    val BufferResult(cloned, goesOn, recirculated, dropped) =
      sema.buffer(postingress, input)
    val egressInput = goesOn.as[P4RootMemory]
    val egressOutcome = sema.runControl("egress", egressInput)
    val BufferResult(ecloned, egoesOn, erecirculated, edropped) =
      sema.buffer(egressOutcome, egressInput, ingress = false)
    val limit = veraArgs.maxBugs
    if (veraArgs.commands.nonEmpty) {
      for (x <- veraArgs.commands) {
        val instance = P4Commands.fromFile(switch, x)
        val constraints = ConstraintBuilder(switch, context, instance).toList
        qb.solver.push()
        for (c <- constraints) {
          qb.solver.assertCnstr(c)
        }
        System.err.println(s"now checking dataplane $x")
        for (x <- qb.possibleLocations().take(limit)) {
          System.err.println("at: ")
          System.err.println(x._1)
          System.err.println("because: ")
          System.err.println(x._2)
        }
        qb.solver.pop()
      }
    } else {
      for (x <- qb.possibleLocations().take(limit)) {
        System.err.println("at: ")
        System.err.println(x._1)
        System.err.println("because: ")
        System.err.println(x._2)
      }
    }
  }
}
