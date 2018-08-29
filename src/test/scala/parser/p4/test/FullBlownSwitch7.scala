package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.util.UUID

import com.microsoft.z3.Context
import jdd.bdd.{BDD, BDDPrinter}
import jdd.util.Dot
import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, TrivialTripleInstructionExecutor}
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.SimpleMemory.{group, naturalGroup}
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.{ControlFlowGraph, Topology}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite
import z3.scala.Z3Context

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class FullBlownSwitch7 extends FunSuite {
  test("SWITCH - L3VxlanTunnelTest full symbolic 3") {
    Dot.setRemoveDotFile(false)

    val dir = "inputs/big-switch"
    val p4 = s"$dir/switch-ppc-orig.p4"
    val port = 2
    val ifaces = Map[Int, String](
      0 -> "veth0",
      1 -> "veth2",
      2 -> "veth4",
      3 -> "veth6",
      4 -> "veth8",
      5 -> "veth10",
      6 -> "veth12",
      7 -> "veth14",
      8 -> "veth16",
      64 -> "veth250"
    )
    val switch = Switch.fromFile(p4)
    val symbolicSwitchInstance =
      SymbolicSwitchInstance.fullSymbolic("switch", ifaces, Map.empty, switch)
    val pg = new SkipParserAndDeparser(switch,
                                       switchInstance = symbolicSwitchInstance,
                                       codeFilter = None)
    val res =
      ControlFlowInterpreter.buildSymbolicInterpreter(symbolicSwitchInstance,
                                                      switch,
                                                      Some(pg))
    val printer = createConsumer("/home/dragos/extended/vera-outputs/")

    val allIfaces = Fork(symbolicSwitchInstance.ifaces.map(x => {
      Constrain("standard_metadata.egress_port",
                :==:(ConstantValue(x._1.longValue())))
    }))

    val prog = CodeAwareInstructionExecutor.flattenProgram(
      res.instructions() +
        (s"${symbolicSwitchInstance.getName}.output.in" -> If(
          allIfaces,
          Forward(s"${symbolicSwitchInstance.getName}.output.out"),
          Fail("Cannot find egress_port match for current interfaces"))) + (s"${symbolicSwitchInstance.getName}.parser" -> Forward(
        "switch.parser.parse_ethernet.parse_ipv4.parse_tcp")),
      res.links()
    )

    import org.change.v2.analysis.memory.TagExp._

    val init = TrivialTripleInstructionExecutor
      .execute(
        InstructionBlock(
          CreateTag("START", 0),
          Assign("egress_pipeline", ConstantValue(1)),
          prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
          res.initializeGlobally(),
          prog(s"${symbolicSwitchInstance.getName}.input.$port")
        ),
        State.clean,
        false
      )
      ._1
      .head

    val cfg =
      new ControlFlowGraph(symbolicSwitchInstance.getName, program = prog)
    cfg.topoSort(s"${symbolicSwitchInstance.getName}.input.$port" :: Nil)
    val mapper = cfg.sorted.filter(prog.contains)

    def location(e: Either[State, SimpleMemory]) = e match {
      case Left(s)  => s.location
      case Right(s) => s.location
    }
    val waitingQueue = mutable.PriorityQueue.empty[Either[State, SimpleMemory]](
      Ordering
        .by[Either[State, SimpleMemory], Int](x => cfg.levels(location(x)))
        .reverse
    )
    val solver = new Z3BVSolver()
    waitingQueue.enqueue(Left(init))
    val start = System.currentTimeMillis()
    val topo = Topology(prog)
    var mergeThreshhold = 100

    val alllevels = cfg.sorted.foldRight(Nil: List[Set[String]])((st, acc) => {
      if (topo.m.contains(st)) {
        val crt = topo.m(st).read
        if (acc.isEmpty)
          crt :: acc
        else
          (crt ++ acc.head) :: acc
      } else {
        Set.empty[String] :: acc
      }
    })

    val walllevels = cfg.sorted.foldRight(Nil: List[Set[String]])((st, acc) => {
      if (topo.m.contains(st)) {
        val crt = topo.m(st).write
        if (acc.isEmpty)
          crt :: acc
        else
          (crt ++ acc.head) :: acc
      } else {
        Set.empty[String] :: acc
      }
    })

    val failed = ListBuffer[SimpleMemory]()
    val success = ListBuffer[SimpleMemory]()
    val statEnd = System.currentTimeMillis()
    val globalRNW = alllevels.head.diff(walllevels.head)
    System.out.println(s"static analysis done in ${statEnd - start}ms")
    while (waitingQueue.nonEmpty) {
      val crt = waitingQueue.dequeue()
      val lst = mutable.ListBuffer(crt)
      val loc = location(crt)

      while (waitingQueue.nonEmpty && waitingQueue.head.isRight == crt.isRight && loc == location(
               waitingQueue.head)) {
        lst += waitingQueue.dequeue()
      }
      println(s"now executing at $loc (${cfg.levels(loc)} / ${cfg.levels.size})")
      if (prog.contains(loc)) {
        if (crt.isLeft) {
          if (loc == s"${symbolicSwitchInstance.getName}.control.ingress") {
            waitingQueue.enqueue(lst.map(h => {
              Right(SimpleMemory(h.left.get))
            }): _*)
          } else {
            val execStart = System.currentTimeMillis()
            lst.foreach(ss => {
              val (s, f, c) =
                TrivialTripleInstructionExecutor.execute(prog(loc),
                  ss.left.get,
                  false)
              waitingQueue.enqueue(s.map(Left(_)): _*)
            })
            val execEnd = System.currentTimeMillis()
            println(s"left executing at $loc time ${execEnd - execStart}")
          }
        } else {
          if (lst.size > mergeThreshhold) {
            val lvl = cfg.levels(loc)
            val rw = alllevels(lvl).diff(globalRNW)
            val id = UUID.randomUUID().toString
            val grpStart = System.currentTimeMillis()
            val grouped = SimpleMemory.hitMe(lst.map(_.right.get))
            mergeThreshhold = grouped.size + grouped.size / 2
            val grpEnd = System.currentTimeMillis()
            println(s"merging at $loc time ${grpEnd - grpStart}ms ${grouped.size} vs ${lst.size}")
            val ctx = new Context()
            new Context()
            val execStart = System.currentTimeMillis()
            if (grouped.size == lst.size) {
              lst.foreach(st => {
                val trp = TrivialSimpleMemoryInterpreter.execute(prog(loc),
                  st.right.get.copy(pathCondition = SimplePathCondition.apply()),
                  false)
                trp.success.groupBy(_.location).foreach(l => {
                  val nv = SimpleMemory.hitMe(l._2).map(_.addCondition(st.right.get.pathCondition.cd))
                  waitingQueue.enqueue(nv.map(Right(_)).toList: _*)
                })
//                waitingQueue.enqueue(trp.success.map(Right(_)):_*)
                failed ++= trp.failed
                success ++= trp.continue
              })
            } else {
              grouped.foreach(st => {
                val trp =
                  TrivialSimpleMemoryInterpreter.execute(prog(loc), st, false)
//                trp.success.groupBy(_.location).foreach(l => {
//                  waitingQueue.enqueue(SimpleMemory.hitMe(l._2).map(_.addCondition(st.pathCondition.cd)).map(Right(_)).toList: _*)
//                })
                waitingQueue.enqueue(trp.success.map(Right(_)):_*)
                failed ++= trp.failed
                success ++= trp.continue
              })
            }
            val execEnd = System.currentTimeMillis()
            println(s"executing at $loc time ${execEnd - execStart}ms")
          } else {
            val execStart = System.currentTimeMillis()
            lst.foreach(st => {
              val trp = TrivialSimpleMemoryInterpreter.execute(prog(loc),
                st.right.get,
                false)
//              trp.success.groupBy(_.location).foreach(l => {
//                val nv = SimpleMemory.hitMe(l._2).map(_.addCondition(st.right.get.pathCondition.cd))
//                waitingQueue.enqueue(nv.map(Right(_)).toList: _*)
//              })
              waitingQueue.enqueue(trp.success.map(Right(_)):_*)
              failed ++= trp.failed
              success ++= trp.continue
            })
            val execEnd = System.currentTimeMillis()
            println(
              s"right executing at $loc ${waitingQueue.size} time ${execEnd - execStart}")
          }
        }
      } else {
        System.err.println(s"Done @$loc, ${lst.size}")
        success ++= lst.map(_.right.get)
      }
    }
    val execStop = System.currentTimeMillis()
    System.err.println(s"Done, ${failed.size}, ${success.size} in ${execStop - statEnd}ms")
    val br = new BufferedOutputStream(new FileOutputStream("failed.json"))

    val satStart = System.currentTimeMillis()
    val achievableFailures = failed.groupBy(h => (h.errorCause.getOrElse(""), h.location)).toList.sortBy(f => {
      (f._1._2, f._1._1)
    }).filter(r => {
      val sth = r._2.exists(SimpleMemory.isSat)
      if (!sth) {
        System.err.println("Hypothesis globally false. Why?")
      }
      sth
//      SimpleMemory.isSat(SimpleMemory(pathConditions = FOR(r._2.map(h => FAND(h.pathConditions)).toList) :: Nil))
    }).map(u => u._1)
    val satEnd = System.currentTimeMillis()
    System.err.println(s"Done SAT solving in ${satEnd - satStart}ms")

    JsonUtil.toJson(
      achievableFailures,
      br)
    br.close()
    val bok = new BufferedOutputStream(new FileOutputStream("ok.json"))
    JsonUtil.toJson(success.map(h => h.location), bok)
    bok.close()
  }
}
