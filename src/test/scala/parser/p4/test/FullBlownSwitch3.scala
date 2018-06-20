package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.parser.p4.parser.{NewParserStrategy, SkipParserAndDeparser}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.executor.solvers.{Solver, Z3BVSolver}
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, TripleInstructionExecutor}
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantStringValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.{MemorySpace, State}
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

    val iexe = new TripleInstructionExecutor(new Z3BVSolver)
    import org.change.v2.analysis.memory.TagExp._

    val init  = iexe.execute(
      InstructionBlock(
        CreateTag("START", 0),
        Assign("egress_pipeline", ConstantValue(1)),
        prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally(),
        prog(s"${symbolicSwitchInstance.getName}.input.$port")
      ), State.clean, false
    )._1.head

    def crawlConstraint(floatingConstraint: FloatingConstraint) : (Set[String], Set[String]) =
      floatingConstraint match {
      case :|:(a, b) => val as = crawlConstraint(a)
        val bs = crawlConstraint(b)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :&:(a, b) => val as = crawlConstraint(a)
        val bs = crawlConstraint(b)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :~:(a) => crawlConstraint(a)
      case :==:(exp) => crawlExpression(exp)
      case :<:(exp) => crawlExpression(exp)
      case :<=:(exp) => crawlExpression(exp)
      case :>:(exp) => crawlExpression(exp)
      case :>=:(exp) => crawlExpression(exp)
      case _ => (Set.empty, Set.empty)
    }

    def crawlExpression(floatingExpression : FloatingExpression): (Set[String], Set[String])  =
      floatingExpression match {
      case :<<:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :!:(left) => crawlExpression(left)
      case Symbol(id) => (Set(id), Set.empty)
      case :||:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :^:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :&&:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :+:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case :-:(left, right) => val as = crawlExpression(left)
        val bs = crawlExpression(right)
        (as._1 ++ bs._1, as._2 ++ bs._2)
      case _ => (Set.empty, Set.empty)
    }

    def crawlInstruction(instruction : Instruction): (Set[String], Set[String]) = instruction match {
      case Fork(forkBlocks) => val a = forkBlocks.map(r => crawlInstruction(r)).unzip
        (a._1.flatten.toSet, a._2.flatten.toSet)
      case InstructionBlock(instructions) => val a = instructions.map(r => crawlInstruction(r)).unzip
        (a._1.flatten.toSet, a._2.flatten.toSet)
      case AllocateSymbol(id, size) => (Set.empty, Set(id))
      case DeallocateNamedSymbol(id) => (Set.empty, Set(id))
      case If(testInstr, thenWhat, elseWhat) => val a = List(testInstr, thenWhat, elseWhat).map(r => crawlInstruction(r)).unzip
        (a._1.flatten.toSet, a._2.flatten.toSet)
      case ConstrainNamedSymbol(id, dc, c) => val d = crawlConstraint(dc)
        (d._1 + id, d._2)
      case AssignNamedSymbol(id, exp, t) => val d = crawlExpression(exp)
        (d._1, d._2 + id)
      case ExistsNamedSymbol(symbol) => (Set(symbol), Set.empty)
      case ConstrainFloatingExpression(floatingExpression, dc) =>
        val e = crawlExpression(floatingExpression)
        val d = crawlConstraint(dc)
        (e._1 ++ d._1, e._2 ++ d._2)
      case AssignNamedSymbolWithLength(id, exp, width) => val d = crawlExpression(exp)
        (d._1, d._2 + id)
      case NotExistsNamedSymbol(symbol) => (Set(symbol), Set.empty)
      case _ => (Set.empty, Set.empty)
    }

    caieConsumer(init)

    val allEmptyDict = scala.collection.mutable.Map[(String, Set[String]), (List[State], List[State])]()
//    val regex = "switch.table.[^\\.]+\\.in".r
    val regex = "switch.table.egress_system_acl\\.in".r
    val alwaysTrue = new Solver() {
      override def solve(memory: MemorySpace): Boolean = true
    }
    val tables = prog.filter(pm => regex.findFirstMatchIn(pm._1).nonEmpty).toList.sortBy(f => f._1)
    val alltrueexec = new TripleInstructionExecutor(alwaysTrue)
    val ps = new PrintStream("distinct_vars.csv")
    for {
      pm <- tables
    } {
      val cc = crawlInstruction(pm._2)
      ps.println(s"${pm._1},input," + cc._1.mkString(","))
      ps.println(s"${pm._1},output," + cc._2.mkString(","))
      val start = System.currentTimeMillis()
      val ib = InstructionBlock((cc._1 ++ cc._2).intersect(init.memory.symbols.keySet).toList.flatMap(r => {
        val sz = init.memory.evalToObject(r).map(_.size).getOrElse(64)
        List(
          Allocate(r, sz),
          Assign(r, SymbolicValue())
        )
      }) :+ Forward(pm._1))
      val st = alltrueexec.execute(ib, State.clean, false)._1.head
      System.err.println(s"executing ${pm._1} in ")
      val (stuck, failed, outstanding) = alltrueexec.execute(pm._2, st, true)
      failed.foreach(printer._3)
      outstanding.foreach(printer._3)
      stuck.foreach(printer._3)
      System.err.println(s"${System.currentTimeMillis() - start}ms for " +
        s"#fails ${failed.size} #stuck ${stuck.size} #outstanding ${outstanding.size}")
      if (outstanding.nonEmpty) {
        System.err.println(s"curious behavior stuck at ${pm._1}")
      }

    }
    ps.close()
    printer._1.close()
    printer._2.close()
  }
}
