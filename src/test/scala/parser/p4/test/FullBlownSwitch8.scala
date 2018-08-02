package parser.p4.test

import java.io.{BufferedOutputStream, FileOutputStream}
import java.util.UUID

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{
  CodeAwareInstructionExecutor,
  TrivialTripleInstructionExecutor
}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.{ControlFlowGraph, Topology}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class FullBlownSwitch8 extends FunSuite {
  test("SWITCH - L3VxlanTunnelTest full symbolic 3") {

    val dir = "inputs/simple-nat"
    val p4 = s"$dir/simple_nat-ppc.p4"
    val port = 1
    val ifaces = Map[Int, String](1 -> "veth0", 2 -> "veth1", 11 -> "cpu")
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
    val allIfaces = Fork(symbolicSwitchInstance.ifaces.map(x => {
      Constrain("standard_metadata.egress_port",
                :==:(ConstantValue(x._1.longValue())))
    }))

    val prog = CodeAwareInstructionExecutor.flattenProgram(
      res.instructions() +
        (s"${symbolicSwitchInstance.getName}.output.in" -> If(
          allIfaces,
          Forward(s"${symbolicSwitchInstance.getName}.output.out"),
          Fail("Cannot find egress_port match for current interfaces"))),
      res.links()
    )

    import org.change.v2.analysis.memory.TagExp._

    val init = TrivialTripleInstructionExecutor
      .execute(
        InstructionBlock(
          res.allParserStatesInline(),
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

    val failed = ListBuffer[SimpleMemory]()
    val success = ListBuffer[SimpleMemory]()
    val statEnd = System.currentTimeMillis()
    System.out.println(s"static analysis done in ${statEnd - start}ms")
    while (waitingQueue.nonEmpty) {
      val crt = waitingQueue.dequeue()
      val lst = mutable.ListBuffer(crt)
      val loc = location(crt)

      while (waitingQueue.nonEmpty && waitingQueue.head.isRight == crt.isRight && loc == location(
               waitingQueue.head)) {
        lst += waitingQueue.dequeue()
      }
      println(
        s"now executing at $loc (${cfg.levels(loc)} / ${cfg.levels.size})")
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
              failed ++= f.map(SimpleMemory.apply)
            })
            val execEnd = System.currentTimeMillis()
            println(s"left executing at $loc time ${execEnd - execStart}")
          }
        } else {
          if (lst.size > mergeThreshhold) {
            val lvl = cfg.levels(loc)
            val id = UUID.randomUUID().toString
            val grpStart = System.currentTimeMillis()
            val grouped = SimpleMemory.hitMe(lst.map(_.right.get))
            mergeThreshhold = grouped.size + grouped.size / 2
            val grpEnd = System.currentTimeMillis()
            println(
              s"merging at $loc time ${grpEnd - grpStart}ms ${grouped.size} vs ${lst.size}")

            val execStart = System.currentTimeMillis()
//            if (grouped.size >= 18) {
//              val merged = group(lst.map(_.right.get))(naturalGroup).toList
//              val ps = new PrintStream("ceva.txt")
//              val ps2 = new PrintStream("ceva2.txt")
//              for (m <- merged.zipWithIndex)
//                for (m2 <- merged.zipWithIndex.drop(m._2 + 1))
//                  ps2.println(m._2, m2._2, m2._1._1._3.diff(m._1._1._3), m._1._1._3.diff(m2._1._1._3))
//              for (m <- merged.zipWithIndex)
//                ps.println(m._2, m._1._1._3)
//              ps.close()
//              ps2.close()
//              System.exit(0)
//            }

            if (grouped.size == lst.size) {
              lst.foreach(st => {
                val trp = TrivialSimpleMemoryInterpreter.execute(prog(loc),
                                                                 st.right.get,
                                                                 false)
                waitingQueue.enqueue(trp.success.map(Right(_)): _*)
                failed ++= trp.failed
                success ++= trp.continue
              })
            } else {
              grouped.foreach(st => {
                val trp =
                  TrivialSimpleMemoryInterpreter.execute(prog(loc), st, false)
                waitingQueue.enqueue(trp.success.map(Right(_)): _*)
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
              waitingQueue.enqueue(trp.success.map(Right(_)): _*)
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
    System.err.println(
      s"Done, ${failed.size}, ${success.size} in ${execStop - statEnd}ms")
    val br = new BufferedOutputStream(new FileOutputStream("failed.json"))

    val satStart = System.currentTimeMillis()
    val achievableFailures = failed
      .groupBy(h => (h.errorCause.getOrElse(""), h.location))
      .filter(r => {
        val sth = r._2.exists(SimpleMemory.isSat)
        if (!sth) {
          System.err.println("Hypothesis globally false. Why?")
        }
        sth
//      SimpleMemory.isSat(SimpleMemory(pathConditions = FOR(r._2.map(h => FAND(h.pathConditions)).toList) :: Nil))
      })
      .keys
    val satEnd = System.currentTimeMillis()
    System.err.println(s"Done SAT solving in ${satEnd - satStart}ms")

    JsonUtil.toJson(achievableFailures, br)
    br.close()
    val bok = new BufferedOutputStream(new FileOutputStream("ok.json"))
    JsonUtil.toJson(success.map(h => h.location), bok)
    bok.close()
  }
}
