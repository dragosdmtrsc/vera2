package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.parser.p4.parser.{NewParserStrategy, SkipParserAndDeparser}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.{AlwaysTrue, Solver, Z3BVSolver}
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, InstantiateAndRun, TripleInstructionExecutor}
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.{ConditionInstantiator, InstructionCrawler, MemorySpace, State}
import org.change.v2.analysis.processingmodels.{Instruction, Let, SuperFork, Unfail}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.networkproc._
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer

class FullBlownSwitch3 extends FunSuite {
  test("SWITCH - L3VxlanTunnelTest full symbolic 3") {

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
    val printer = createConsumer("/home/dragos2/extended/vera-outputs/")

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
      res.links()
    )

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

    val alltrueexec = new TripleInstructionExecutor(AlwaysTrue)
    val iexe = new TripleInstructionExecutor(new Z3BVSolver)
    import org.change.v2.analysis.memory.TagExp._

    val init  = alltrueexec.execute(
      InstructionBlock(
        CreateTag("START", 0),
        Assign("egress_pipeline", ConstantValue(1)),
        prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally(),
        prog(s"${symbolicSwitchInstance.getName}.input.$port")
      ), State.clean, false
    )._1.head

    def instantiateCondition(conditions : List[Condition], against : State): List[Condition] = {
      conditions.map(ConditionInstantiator(_, against))
    }

    caieConsumer(init)

    val allEmptyDict = scala.collection.mutable.Map[(String, Set[(String, Int)]), (List[State], List[State], List[State])]()
//    val regex = "switch.table.[^\\.]+\\.in".r
    val regex = "switch.table.egress_system_acl\\.in".r
    val alwaysTrue = new Solver() {
      override def solve(memory: MemorySpace): Boolean = true
    }

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
          val (read, write) = InstructionCrawler.crawlInstruction(instr)
          val all = read ++ write
          val segEquiv = states.groupBy(r => {
            r.memory.symbols.keySet.intersect(all).map(sm => (sm, r.memory.evalToObject(sm).get.size))
          })
          System.err.println(s"#classes: ${segEquiv.size} vs ${states.size}")
          for ((syms, sts) <- segEquiv) {
            val history = if (allEmptyDict.contains((port, syms))) {
              allEmptyDict(port, syms)
            } else {
              val res = InstantiateAndRun(
                instr, syms
              )
              allEmptyDict.put((port, syms), res)
              res
            }
          }
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
