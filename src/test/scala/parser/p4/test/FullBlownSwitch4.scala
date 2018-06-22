package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.ControlFlowGraph
import org.change.v2.analysis.equivalence.{MultiSuperState, SimpleSuperState, SuperState}
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, InstantiateAndRun, TripleInstructionExecutor, TrivialTripleInstructionExecutor}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.{InstructionCrawler, State}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

import scala.collection.mutable.ListBuffer

class FullBlownSwitch4 extends FunSuite {
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

    val prog = CodeAwareInstructionExecutor.flattenProgram(res.instructions() +
      (s"${symbolicSwitchInstance.getName}.output.in" -> If (allIfaces,
        Forward(s"${symbolicSwitchInstance.getName}.output.out"),
        Fail("Cannot find egress_port match for current interfaces")
      )) + (s"${symbolicSwitchInstance.getName}.parser" -> Forward("switch.parser.parse_ethernet.parse_ipv4.parse_tcp")),
      res.links()
    )

    val cfg = new ControlFlowGraph(name = "switch", program = prog)
    System.err.println(s"starting topo sort")
    cfg.topoSort(s"${symbolicSwitchInstance.getName}.input.2" :: Nil)

    val mapper = cfg.sorted.filter(prog.contains).
      map(x => x -> InstructionCrawler.crawlInstruction(prog(x))).toMap

    val dependencies = scala.collection.mutable.Map[(String, String), Set[String]]()

    val ps = new PrintStream("deps.txt")
    for (x <- cfg.sorted.zipWithIndex) {
      if (!x._1.startsWith(s"${symbolicSwitchInstance.getName}.input.") && mapper.contains(x._1)) {
        val rwx = mapper(x._1)
        for (y <- cfg.sorted.drop(x._2 + 1)) {
          if (mapper.contains(y)) {
            val rwy = mapper(y)
            val intersection = rwx._2.intersect(rwy._1).filter(x=> x != "IsClone")
            if (intersection.nonEmpty) {
              dependencies.put((x._1, y), intersection)
              ps.println(s"$y depends on $x because of $intersection")
            }
          }
        }
      }
    }

  }
}
