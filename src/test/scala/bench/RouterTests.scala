package bench

import java.io.File

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Forward, NoOp}
import org.change.v2.runners.experiments.routerexperiments.{RoutingEntries, RoutingModelFactory}
import org.scalatest.FunSuite

class RouterTests extends FunSuite {
  test("How many ports are defined in huge") {
    val huge = "src/main/resources/routing_tables/huge.txt"

    val fibEntries = OptimizedRouter.getRoutingEntries(new File(huge))

    println(fibEntries.groupBy(_._2).mapValues(_.size).mkString("\n"))
    println(fibEntries.groupBy(_._2).mapValues(_.size).values.sum)
  }

  test("Stupid if/else chain") {
    val huge = "src/main/resources/routing_tables/huge.txt"

    import org.change.v2.runners.experiments.routerexperiments._

    val entries = selectNRandomRoutingEntries(huge, Some(1000))

    val ifElse = buildIfElseChainModel(entries)

    val executor = CodeAwareInstructionExecutor.singleInstructionExecutor(ifElse)

    println("Constructed")

    val r = executor.executeForward(Forward(CodeAwareInstructionExecutor.NULL_PORT), State.symbolicIpState, false)

    println(r._1.size)
  }

  test("Model comparison") {
    import org.change.v2.runners.experiments.routerexperiments._
    val file: String = "src/main/resources/routing_tables/huge.txt"
    for {
      count <- Seq(100000)
    } {
      val entries = selectNRandomRoutingEntries(file, Some(count))
      println(s"Size is $count, unique ports is ${entries.unzip._2.toSet.size}")
//      println("Stupid if/else")
//      buildAndMeasure(buildIfElseChainModel _, entries)
//      println("Port-grouped if/else")
//      buildAndMeasure(buildPerPortIfElse _, entries)
      println("Basic fork model")
      buildAndMeasure(buildBasicForkModel _, entries)
      println("Improved fork model")
      buildAndMeasure(buildImprovedFork _, entries)
    }
  }

  def buildAndMeasure(
                       factory: RoutingModelFactory,
                       count: Option[Int],
                       file: String = "src/main/resources/routing_tables/huge.txt"): Unit = {

    import org.change.v2.runners.experiments.routerexperiments._

    val entries = selectNRandomRoutingEntries(file, count)

    buildAndMeasure(factory, entries)
  }

  def buildAndMeasure(
                      factory: RoutingModelFactory,
                      entries: RoutingEntries
                     ): Unit = {
    var model: Instruction = NoOp
    import org.scalameter._
    val timeToBuild = measure {
      model = factory(entries)
    }
    println(s"Time to build: $timeToBuild")

    val executor = CodeAwareInstructionExecutor.singleInstructionExecutor(model)

    var r: (List[State], List[State]) = (Nil, Nil)

    import org.scalameter._
    val timeToExecute = measure {
      r = executor.executeForward(Forward(CodeAwareInstructionExecutor.NULL_PORT), State.symbolicIpState, false)
    }
    println(s"Time to execute: $timeToExecute")

    println(s"Successful paths: ${r._1.size}, failed is ${r._2.size}")
  }
}
