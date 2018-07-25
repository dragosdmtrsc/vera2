package parser.p4.test

import java.io.PrintStream
import java.util.UUID

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.SkipParserAndDeparser
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, TripleInstructionExecutor, TrivialTripleInstructionExecutor}
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.{ControlFlowGraph, Topology}
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite
import scodec.bits.BitVector

import scala.collection.mutable

class FullBlownSwitch6 extends FunSuite {
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
      (s"${symbolicSwitchInstance.getName}.output.in" -> If(allIfaces,
        Forward(s"${symbolicSwitchInstance.getName}.output.out"),
        Fail("Cannot find egress_port match for current interfaces")
      )) + (s"${symbolicSwitchInstance.getName}.parser" -> Forward("switch.parser.parse_ethernet.parse_ipv4.parse_tcp")),
      res.links()
    )

    import org.change.v2.analysis.memory.TagExp._

    val init = TrivialTripleInstructionExecutor.execute(
      InstructionBlock(
        CreateTag("START", 0),
        Assign("egress_pipeline", ConstantValue(1)),
        prog("switch.generator.parse_ethernet.parse_ipv4.parse_tcp"),
        res.initializeGlobally(),
        prog(s"${symbolicSwitchInstance.getName}.input.$port")
      ), State.clean, false
    )._1.head


    val cfg = new ControlFlowGraph(symbolicSwitchInstance.getName, program = prog)
    cfg.topoSort(s"${symbolicSwitchInstance.getName}.input.$port" :: Nil)
    val mapper = cfg.sorted.filter(prog.contains)
    val waitingQueue = mutable.PriorityQueue.empty[Either[State, SimpleMemory]](
      Ordering.by[Either[State, SimpleMemory], Int] {
        case Left(s) => cfg.levels(s.location)
        case Right(s) => cfg.levels(s.location)
      }.reverse
    )
    val parsed = mutable.PriorityQueue.empty[State](
      Ordering.by[State, Int](s => cfg.levels(s.location)).reverse
    )
    val solver = new Z3BVSolver()
    waitingQueue.enqueue(Left(init))
    val psfail = new PrintStream("fail.txt")
    val psOk = new PrintStream("done.txt")

//    val topo = Topology(prog)
//    val all = topo.allVars()
//    val rnw = all.read.diff(all.write)
//    val wnr = all.write.diff(all.read)
//    val rw = all.write.intersect(all.read)

    def location(e : Either[State, SimpleMemory]) = e match {
      case Left(s) => s.location
      case Right(s) => s.location
    }

    while (waitingQueue.nonEmpty) {
      val crt = waitingQueue.dequeue()
      val lst = mutable.ListBuffer(crt)
      while (waitingQueue.nonEmpty && waitingQueue.head.isRight == crt.isRight && location(crt) == location(waitingQueue.head)) {
        lst += waitingQueue.dequeue()
      }
      val loc = location(crt)
      print(s"reduction for ${lst.size} @$loc = ${cfg.levels(loc)} / ${cfg.levels.size}")
      val redStart = System.currentTimeMillis()
      if (crt == s"${symbolicSwitchInstance.getName}.control.ingress" && crt.isLeft) {
        lst.map(h => {

        })
      } else {
        if (crt.isLeft) {
          lst.foreach {
            case Left(s) => val (s, f, c) = TrivialTripleInstructionExecutor.execute(prog(loc), s, false)
              waitingQueue.enqueue(s.map(Left(_)):_*)
          }
        } else {
          if (lst.size > 100) {
            val id = UUID.randomUUID().toString
            lst.map(_.right.get).groupBy(h => (h.memTags,
              h.rawObjects.keySet,
              h.symbols.keySet.filter(!_.endsWith("IsValid")),
              h.symbols.filter(r => r._1.endsWith("IsValid")))).map(chi => {
              val ms = SimpleMemory(
                memTags = chi._1._1,
                symbols = chi._1._4 ++ chi._1._3.map(meta => {
                  meta -> SimpleMemoryObject(SymbolicValue(s"meta$meta$id"), chi._2.head.symbols(meta).size)
                }).toMap,
                rawObjects = chi._1._2.map(offset => {
                  offset -> SimpleMemoryObject(SymbolicValue(s"offset$offset$id"), chi._2.head.rawObjects(offset).size)
                }).toMap
              )
              val cdLst = FOR(chi._2.foldLeft(List.empty[Condition])((acc, st) => {
                val mms = st
                val pcForSyms = chi._1._3.foldLeft(mms.pathConditions)((acc, meta) => {
                  val symName = s"meta$meta$id"
                  val toBeAdded = OP(SymbolicValue(symName), EQ_E(mms.symbols(meta).expression), mms.symbols(meta).size)
                  toBeAdded :: acc
                })
                val pcForRaws = chi._1._2.foldLeft(pcForSyms)((acc, offset) => {
                  val symName = s"offset$offset$id"
                  val toBeAdded = OP(SymbolicValue(symName), EQ_E(mms.rawObjects(offset).expression), mms.rawObjects(offset).size)
                  toBeAdded :: acc
                })
                FAND(pcForRaws) :: acc
              }))
              ms.addCondition(cdLst)
            })
          }
        }
      }

//      val buf = if (lst.size > 1000) {
//        val id = UUID.randomUUID()
//        lst.groupBy(h => (h.memory.memTags,
//          h.memory.rawObjects.keySet,
//          h.memory.symbols.keySet.filter(!_.endsWith("IsValid")),
//          h.memory.symbols.filter(r => r._1.endsWith("IsValid")))).map(chi => {
//          val ms = MemorySpace(
//            memTags = chi._1._1,
//            symbols = chi._1._4 ++ chi._1._3.map(meta => {
//              meta -> MemoryObject(SymbolicValue(s"meta$meta$id"), chi._2.head.memory.symbols(meta).size)
//            }).toMap,
//            rawObjects = chi._1._2.map(offset => {
//              offset -> MemoryObject(SymbolicValue(s"offset$offset$id"), chi._2.head.memory.rawObjects(offset).size)
//            }).toMap
//          )
//          val cdLst = FOR(chi._2.foldLeft(List.empty[Condition])((acc, st) => {
//            val mms = st.memory
//            val pcForSyms = chi._1._3.foldLeft(mms.pathConditions)((acc, meta) => {
//              val symName = s"meta$meta$id"
//              val toBeAdded = OP(SymbolicValue(symName), EQ_E(mms.symbols(meta).expression), mms.symbols(meta).size)
//              toBeAdded :: acc
//            })
//            val pcForRaws = chi._1._2.foldLeft(pcForSyms)((acc, offset) => {
//              val symName = s"offset$offset$id"
//              val toBeAdded = OP(SymbolicValue(symName), EQ_E(mms.rawObjects(offset).expression), mms.rawObjects(offset).size)
//              toBeAdded :: acc
//            })
//            FAND(pcForRaws) :: acc
//          }))
//          State(memory = ms.addCondition(cdLst), history = crt.location :: Nil)
//        })
//      } else {
//        lst
//      }
//      val redTime  = System.currentTimeMillis() - redStart
//      println(s" in ${redTime}ms ${buf.size}")
//      buf.foreach(crt => {
//        val (a, b, c) = TrivialTripleInstructionExecutor.execute(prog(crt.location), crt, false)
//        waitingQueue.enqueue(a:_*)
//      })
    }

//    println(s"rnw ${rnw.size}, wnr ${wnr.size}, rw ${rw.size}")
//    val allvars = rw
//    println(parsed.size, parsed.map(_.location).mkString(","))
//    val absExec = mutable.PriorityQueue.empty[AbsState](
//      Ordering.by[AbsState, Int](s => cfg.levels(s.location)).reverse
//    )
//    parsed.foreach(r => {
//      absExec.enqueue(AbsState.fromState(r, rw, rnw, wnr))
//    })
//    val absexecutor = new AbsInterpreter()
//    while (absExec.nonEmpty) {
//      val crt = absExec.dequeue()
//      val lst = mutable.ListBuffer(crt)
//      while (absExec.nonEmpty && absExec.head.location == crt.location) {
//        lst += absExec.dequeue()
//      }
//      val groups = lst.groupBy(r => (r.valid, r.allocated)).values.map(h => h.head)
//      val rd = topo.m(crt.location).read
//      val mask = rd.filter(crt.mapping.contains).map(crt.mapping(_)).foldLeft(BitVector.low(crt.mapping.size))((acc, x) => acc.set(x))
//      val vmask = rd.filter(crt.valMapping.contains).map(crt.valMapping(_)).foldLeft(BitVector.low(crt.valMapping.size))((acc, n) => acc.set(n))
//      val startSameBeh = System.currentTimeMillis()
//      val sameBehavior = groups.groupBy(h => {
//        val startAnd = System.currentTimeMillis()
//        val masked = ((h.valid & vmask).toByteVector, (h.allocated & mask).toByteVector)
//        val total = System.currentTimeMillis() - startSameBeh
//        masked
//      })
//      val behEq = System.currentTimeMillis() - startSameBeh
//      println(s"at location ${crt.location} L${cfg.levels(crt.location)} of ${cfg.levels.size} ${lst.size} vs ${groups.size} vs ${sameBehavior.size} took $behEq ms")
//      sameBehavior.foreach(h => {
//        val as = crt.copy(allocated = BitVector(h._1._2), valid = BitVector(h._1._1))
//        val at = absexecutor.execute(prog(crt.location), as, false)
//
//        val fw = at.success.flatMap(r => {
//          h._2.map(chi => {
//            chi.copy(valid = chi.valid | (as.valid ^ r.valid & r.valid) & ~(as.valid ^ r.valid & as.valid),
//              allocated = chi.allocated | (as.allocated ^ r.allocated & r.allocated) & ~(as.allocated ^ r.allocated & as.allocated),
//              history = r.location :: chi.history)
//          })
//        })
//        absExec.enqueue(fw:_*)
//      })
//    }
  }
}
