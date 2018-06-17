package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, CodeAwareInstructionExecutorWithListeners, CodeAwareInstructionExecutorWithListeners2, StateConsumer}
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{MemoryObject, State, Value, ValueStack}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer
import scala.util.Random

class FullBlownSwitch extends FunSuite {
  test("SWITCH - L3VxlanTunnelTest full symbolic") {

    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val port = 2
    val ifaces = Map[Int, String](
      0 -> "veth0", 1 -> "veth2",
      2 -> "veth4", 3 -> "veth6",
      4 -> "veth8", 5 -> "veth10",
      6 -> "veth12", 7 -> "veth14",
      8 -> "veth16", 64 -> "veth250"
    )
    val switch = Switch.fromFile(p4)
    val symbolicSwitchInstance = SymbolicSwitchInstance.fullSymbolic("switch", ifaces, Map.empty, switch)
    val pg = new SkipParserAndDeparser(switch,
      switchInstance = symbolicSwitchInstance,
      codeFilter = None
    )
    val res = ControlFlowInterpreter.buildSymbolicInterpreter(symbolicSwitchInstance, switch, Some(pg))
    import org.change.v2.analysis.executor.StateConsumer.fromFunction
    val printer = createConsumer("/home/dragos/extended/vera-outputs/")

    val allIfaces = Fork(symbolicSwitchInstance.ifaces.map(x => {
      Constrain("standard_metadata.egress_port", :==:(ConstantValue(x._1.longValue())))
    }))
    val q = scala.collection.mutable.Queue[String]()
    val map = scala.collection.mutable.Map[String, ListBuffer[State]]()


    def caieConsumer(s : State) : Unit = {
      if (!map.contains(s.location)) {
        q.enqueue(s.location)
        map.put(s.location, ListBuffer[State](s))
      } else {
        map(s.location) += s
      }
    }
    val codeAwareInstructionExecutorWithListeners = new CodeAwareInstructionExecutorWithListeners2(
      CodeAwareInstructionExecutor(res.instructions() +
        (s"${symbolicSwitchInstance.getName}.output.in" -> If (allIfaces,
          Forward(s"${symbolicSwitchInstance.getName}.output.out"),
          Fail("Cannot find egress_port match for current interfaces")
        )),
        res.links(), new Z3BVSolver),
      successStateConsumers = printer._3 :: Nil,
      failedStateConsumers =  printer._3 :: Nil,
      unfinishedStateConsumers = StateConsumer.fromFunction(caieConsumer) :: Nil
    )
    import org.change.v2.analysis.memory.TagExp._

    val init  = codeAwareInstructionExecutorWithListeners.caie.execute(
      InstructionBlock(
        CreateTag("START", 0),
        Call("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally()
      ), State.clean, false
    )._1.head
    val ib = Forward(s"${symbolicSwitchInstance.getName}.input.$port")

    codeAwareInstructionExecutorWithListeners.execute(InstructionBlock(
      ib
    ), init, false)

    val ps = new PrintStream("diagnostics.txt")

    val allEmptyDict = scala.collection.mutable.Map[(String, Set[String]), (List[State], List[State])]()

    val z3BVSolver = new Z3BVSolver()
    while (q.nonEmpty) {
      val initqsize = q.size

      val start = System.currentTimeMillis()
      val port = q.dequeue()
      val states = map(port)
      map.remove(port)
      System.err.println(s"now at $port executing ${states.size}")
      val instr = codeAwareInstructionExecutorWithListeners.caie.program(port)
      instr match {
        case Forward(place) => if (!map.contains(place)) {
          q.enqueue(place)
          map.put(place, states.map(_.forwardTo(place)))
        } else {
          map(place) ++= states.map(_.forwardTo(place))
        }
        case _ =>
          states.foreach(codeAwareInstructionExecutorWithListeners.execute(
            instr,
            _,
            verbose = false
          ))
      }
      val total = System.currentTimeMillis() - start
      System.err.println(s"time $total for initial port $port $initqsize final ${q.size}")
    }

    printer._1.close()
    printer._2.close()
  }
}
