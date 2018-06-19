package parser.p4.test

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, CodeAwareInstructionExecutorWithListeners2, StateConsumer, TripleInstructionExecutor}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer

class FullBlownSwitch2 extends FunSuite {
  test("SWITCH - L3VxlanTunnelTest full symbolic 2") {

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

    val prog = CodeAwareInstructionExecutor.flattenProgram(res.instructions() +
      (s"${symbolicSwitchInstance.getName}.output.in" -> If (allIfaces,
        Forward(s"${symbolicSwitchInstance.getName}.output.out"),
        Fail("Cannot find egress_port match for current interfaces")
      )),
      res.links())

    def caieConsumer(s : State) : Unit = {
      if (prog.contains(s.location)) {
        if (!map.contains(s.location)) {
          q.enqueue(s.location)
          map.put(s.location, ListBuffer[State](s))
        } else {
          map(s.location) += s
        }
      } else {
        printer._3(s)
      }
    }

    val iexe = new TripleInstructionExecutor(new Z3BVSolver)
    import org.change.v2.analysis.memory.TagExp._

    val init  = iexe.execute(
      InstructionBlock(
        CreateTag("START", 0),
        prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally(),
        Forward(s"${symbolicSwitchInstance.getName}.input.$port")
      ), State.clean, false
    )._1.head

    caieConsumer(init)

    val allEmptyDict = scala.collection.mutable.Map[(String, Set[String]), (List[State], List[State])]()

    while (q.nonEmpty) {
      val initqsize = q.size

      val start = System.currentTimeMillis()
      val port = q.dequeue()
      val states = map(port)
      map.remove(port)
      System.err.println(s"now at $port executing ${states.size}")
      val instr = prog(port)
      instr match {
        case Forward(place) => if (!map.contains(place)) {
          q.enqueue(place)
          map.put(place, states.map(_.forwardTo(place)))
        } else {
          map(place) ++= states.map(_.forwardTo(place))
        }
        case _ =>
          states.foreach(st => {
            val (s, f, u) = iexe.execute(
              instr,
              st,
              verbose = false
            )
            u.foreach(printer._3)
            f.foreach(printer._3)
            s.foreach(caieConsumer)
          })
      }
      val total = System.currentTimeMillis() - start
      System.err.println(s"time $total for initial port $port $initqsize final ${q.size}")
    }

    printer._1.close()
    printer._2.close()
  }
}
