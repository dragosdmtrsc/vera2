package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.parser.p4.parser.{NewParserStrategy, SkipParserAndDeparser}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.equivalence.{MultiSuperState, SimpleSuperState, SuperState}
import org.change.v2.analysis.executor.solvers.{AlwaysTrue, Solver, Z3BVSolver}
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, InstantiateAndRun, TripleInstructionExecutor, TrivialTripleInstructionExecutor}
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
      )) + (s"${symbolicSwitchInstance.getName}.parser" -> Forward("switch.parser.parse_ethernet.parse_ipv4.parse_tcp")),
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

    val iexe = new TripleInstructionExecutor(new Z3BVSolver)
    import org.change.v2.analysis.memory.TagExp._

    val init  = TrivialTripleInstructionExecutor.execute(
      InstructionBlock(
        CreateTag("START", 0),
        Assign("egress_pipeline", ConstantValue(1)),
        prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally(),
        prog(s"${symbolicSwitchInstance.getName}.input.$port")
      ), State.clean, false
    )._1.head

    val allEmptyDict = scala.collection.mutable.Map[
      (String,
        Set[(String, Int)],
        Set[(String, Long)]), (List[State], List[State], List[State])]()
    val waitingQueue = ListBuffer[SuperState]()
    val backupQueue  = ListBuffer[SuperState]()
    val solver  = new Z3BVSolver()

    def caieConsumer2(s : SuperState): Unit = {
      if (prog.contains(s.port)) {
        backupQueue += s
      } else {
        s.materialize().filter(st => solver.solve(st.memory)).foreach(printer._3)
      }
    }
    caieConsumer2(SimpleSuperState(init))
    waitingQueue ++= backupQueue
    backupQueue.clear()
    while (waitingQueue.nonEmpty) {
      if (waitingQueue.size < 5 && waitingQueue.forall(_.isInstanceOf[SimpleSuperState])) {
        for (st <- waitingQueue) {
          val instr = prog(st.port)
          System.err.println(s"running simple case at ${st.port}")
          val (o, f, s) = iexe.execute(instr, st.asInstanceOf[SimpleSuperState].state, verbose = false)
          (s ++ f).foreach(printer._3)
          o.foreach(x => caieConsumer2(SimpleSuperState(x)))
        }
      } else {
        val start = System.currentTimeMillis()
        val portClasses = waitingQueue.groupBy(f => f.port)
        for ((port, states) <- portClasses) {
          val instr = prog(port)
          instr match {
            case Forward(place) => caieConsumer2(MultiSuperState(State.clean.forwardTo(place), states))
            case _ =>
              val eqEnd = System.currentTimeMillis()
              val (read, write) = InstructionCrawler.crawlInstruction(instr)
              val dict = states.flatMap(st => {
                st.varsAndValids(read).toList
              }).groupBy(_._1).map(v => v._1 -> v._2.flatMap(h => h._2))
              System.err.println(s"equiv on vars and valids took ${System.currentTimeMillis() - eqEnd}ms at port " +
                s"$port")
              for (((syms, valids), sts) <- dict) {
                val history = if (allEmptyDict.contains((port, syms, valids))) {
                  allEmptyDict(port, syms, valids)
                } else {
                  val pleaseAdd = InstructionBlock(
                    valids.toList.map(i => {
                      Assign(i._1, ConstantValue(i._2))
                    })
                  )
                  val res = InstantiateAndRun(
                    instr, syms, pleaseAdd
                  )
                  allEmptyDict.put((port, syms, valids), res)
                  res
                }
                val startMaterializing = System.currentTimeMillis()
                (history._2 ++ history._3).foreach(r => MultiSuperState(r, sts).materialize().filter(st => solver.solve(st.memory)).foreach(h => {
                  printer._3(h)
                }))
                System.err.println(s"materializing time ${System.currentTimeMillis() - startMaterializing}ms at " +
                  s"$port")
                history._1.foreach(r => caieConsumer2(MultiSuperState(r, sts)))
              }
          }
        }
        System.err.println(s"total step time ${System.currentTimeMillis() - start}ms")
      }
      waitingQueue.clear()
      waitingQueue ++= backupQueue
      backupQueue.clear()
    }
  }
}
