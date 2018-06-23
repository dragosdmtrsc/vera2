package parser.p4.test

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.parser.p4.parser.{NewParserStrategy, SkipParserAndDeparser}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.v2.analysis.ControlFlowGraph
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

import scala.collection.mutable
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


    val prog = CodeAwareInstructionExecutor.flattenProgram(res.instructions() +
      (s"${symbolicSwitchInstance.getName}.output.in" -> If (allIfaces,
        Forward(s"${symbolicSwitchInstance.getName}.output.out"),
        Fail("Cannot find egress_port match for current interfaces")
      )) + (s"${symbolicSwitchInstance.getName}.parser" -> Forward("switch.parser.parse_ethernet.parse_ipv4.parse_tcp")),
      res.links()
    )

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
    val cfg = new ControlFlowGraph(symbolicSwitchInstance.getName, program = prog)
    cfg.topoSort(s"${symbolicSwitchInstance.getName}.input.$port" :: Nil)
    val mapper = cfg.sorted.filter(prog.contains).
      map(x => x -> InstructionCrawler.crawlInstruction(prog(x))).toMap
    val dependencies = scala.collection.mutable.Map[String, Map[String, Set[String]]]()
    val allDependencies = scala.collection.mutable.Map[String, Set[String]]()

    val ps = new PrintStream("deps.txt")
    for (x <- cfg.sorted.zipWithIndex) {
      if (!x._1.startsWith(s"${symbolicSwitchInstance.getName}.input.") && mapper.contains(x._1)) {
        val rwx = mapper(x._1)
        for (y <- cfg.sorted.drop(x._2 + 1)) {
          if (mapper.contains(y)) {
            val rwy = mapper(y)
            val intersection = rwx._2.intersect(rwy._1).filter(x=> x != "IsClone")
            if (intersection.nonEmpty) {
              if (!dependencies.contains(x._1)) {
                dependencies.put(x._1, Map.empty)
                allDependencies.put(x._1, Set.empty)
              }
              if (!dependencies(x._1).contains(y))
                dependencies.put(x._1, Map.empty + (y -> Set.empty))
              dependencies.put(x._1, dependencies(x._1) + (y -> intersection))
              allDependencies.put(x._1, allDependencies(x._1) ++ intersection)
              ps.println(s"$y depends on $x because of $intersection")
            }
          }
        }
      }
    }

    val waitingQueue = mutable.PriorityQueue.empty[SuperState](
      Ordering.by[SuperState, Int](s => cfg.levels(s.port)).reverse
    )
    val backupQueue  = ListBuffer[SuperState]()
    val solver  = new Z3BVSolver()

    waitingQueue.enqueue(SimpleSuperState(init))
    val psfail = new PrintStream("fail.txt")
    val psOk = new PrintStream("done.txt")
    while (waitingQueue.nonEmpty) {
      val top = ListBuffer(waitingQueue.dequeue())
      while (waitingQueue.nonEmpty && waitingQueue.head.port == top.head.port) {
        top += waitingQueue.dequeue()
      }

      if (top.size < 5 && top.forall(_.isInstanceOf[SimpleSuperState])) {
        for (st <- top) {
          val instr = prog(st.port)
          val (o, f, s) = iexe.execute(instr, st.asInstanceOf[SimpleSuperState].state, verbose = false)
          (s ++ f).foreach(printer._3)
          o.foreach(x => waitingQueue.enqueue(SimpleSuperState(x)))
        }
      } else {
        val start = System.currentTimeMillis()
        val states = top
        val port = top.head.port
        System.err.println(s"running at level ${cfg.levels(port)} = $port")
        val instr = prog(port)

        instr match {
          case Forward(place) =>
            if (waitingQueue.nonEmpty && cfg.levels(place) < cfg.levels(port))
              backupQueue += MultiSuperState(State.clean.forwardTo(place), states)
            else if (prog.contains(place))
              states.foreach(x => waitingQueue.enqueue(x match {
                case SimpleSuperState(s) => SimpleSuperState(s.forwardTo(place))
                case MultiSuperState(s, otherStates, maybeList) => MultiSuperState(
                  s.forwardTo(place), otherStates, maybeList
                )
              }))
            else {
              psOk.println(s"successful at $port for max ${MultiSuperState(State.clean.forwardTo(place), states).max()}")
            }
          case _ =>
            val eqEnd = System.currentTimeMillis()
            val (read, write) = InstructionCrawler.crawlInstruction(instr)
            val dict = states.foldLeft(Map.empty[(Set[(String, Int)], Set[(String, Long)]),
              Iterable[SuperState]])((acc, x) => {
              x.varsAndValids(read).foldLeft(acc)((acc2, y) => {
                if (acc2.contains(y._1))
                  acc2 + (y._1 -> (acc2(y._1) ++ y._2))
                else
                  acc2 + y
              })
            })
//
//            System.err.println(s"equiv on vars and valids took ${System.currentTimeMillis() - eqEnd}ms at port " +
//              s"$port")
            for (((syms, valids), sts) <- dict) {
              val pleaseAdd = InstructionBlock(
                valids.toList.map(i => {
                  Assign(i._1, ConstantValue(i._2))
                })
              )
              val history =  InstantiateAndRun(
                instr, syms, pleaseAdd, instructionExecutor = iexe
              )
              if (history._2.nonEmpty || history._3.nonEmpty) {
                val startMaterializing = System.currentTimeMillis()
                history._2.foreach(r => {
                  psfail.println(s"failing because " +
                    s"${r.errorCause.get} at $port for max ${MultiSuperState(r, sts).max()}")
                })
                history._3.foreach(r => {
                  psOk.println(s"successful at $port for max ${MultiSuperState(r, sts).max()}")
                })
              }
              val nodep = !dependencies.contains(port)
              val historyClasses = if (nodep) {
                history._1.groupBy(x => {
                  x.location
                }).map(r => (State.clean.forwardTo(r._1), r._2))
              } else {
                history._1.groupBy(x => {
                  val ih = x.instructionHistory.filter(x => x match {
                    case a : AllocateSymbol => true
                    case AssignNamedSymbol(id, _, _) if id.endsWith(".IsValid") => true
                    case _ => false
                  })
                  val allocs = ih.collect {
                    case a: AllocateSymbol => a
                  }.map(r => (r.id, r.size)).filter(h => syms.contains(h)).foldLeft(Map[String, Int]())((acc, x) => {
                    acc + x
                  }).filter(y => {
                    !nodep && allDependencies(port).contains(y._1)
                  })
                  val ovalids = ih.collect{
                    case a : AssignNamedSymbol => a.id -> (if (a.exp.isInstanceOf[ConstantValue])
                      a.exp.asInstanceOf[ConstantValue].value
                    else {
                      if (a.exp.isInstanceOf[Symbol]) {
                        val sb = a.exp.asInstanceOf[Symbol]
                        if (sb.id.endsWith(".IsValid")) {
                          valids.find(x => x._1 == sb.id).get._2
                        } else {
                          throw new IllegalArgumentException(x.instructionHistory + "")
                        }
                      } else {
                        throw new IllegalArgumentException("wrong type " + a + " " + x.instructionHistory + "")
                      }
                    })
                  }.foldLeft(Map[String, Long]())((acc, x) => {
                    acc + x
                  }).filter(y => {
                    !nodep && allDependencies(port).contains(y._1)
                  })
                  (x.location, ovalids, allocs)
                }).map(hc => {
                  val allocd = hc._1._3.foldLeft(State.clean)((acc, h) => {
                    acc.addInstructionToHistory(Allocate(h._1, h._2))
                  })

                  (hc._1._2.foldLeft(allocd)((acc, h) => {
                    acc.addInstructionToHistory(Assign(h._1, ConstantValue(h._2)))
                  }).forwardTo(hc._1._1), hc._2)
                })
              }
              System.err.println(s"hist equiv = ${history._1.size} vs ${historyClasses.size}")
              if (history._1.size.toDouble == historyClasses.size.toDouble && history._1.size != 1) {
                System.err.println(s"bad ${history._1.map(x => x.location)} vs ")
                historyClasses.foreach(x => {
                  System.err.println(s"@ ${x._1.location}")
                  System.err.println(x._1.instructionHistory)
                })
              }
              historyClasses.foreach(hc => {
                val st = hc._1
                if (!prog.contains(st.location)) {
                  psOk.println(s"out at ${st.location} maximum ${MultiSuperState(st, sts, Some(hc._2)).max()} states")
                } else {
                  if (hc._1.instructionHistory.isEmpty) {
                    val place = hc._1.location
                    sts.foreach(x => waitingQueue.enqueue(x match {
                      case SimpleSuperState(s) => SimpleSuperState(s.forwardTo(place))
                      case MultiSuperState(s, otherStates, maybeList) => MultiSuperState(
                        s.forwardTo(place), otherStates, maybeList
                      )
                    }))
                  } else {
                    if (waitingQueue.nonEmpty && cfg.levels(st.location) < cfg.levels(port)) {
                      System.err.println(s"o fi facut ceva copilu, o fi injurat ${st.location} " +
                        s"(${cfg.levels(st.location)})< $port(${cfg.levels(port)})")
                      backupQueue += MultiSuperState(st, sts, Some(hc._2))
                    } else {
                      val mss = MultiSuperState(st, sts, Some(hc._2))
                      waitingQueue.enqueue(mss)
                    }
                  }
                }
              })
            }
          }

        System.err.println(s"total step time($port) ${System.currentTimeMillis() - start}ms")
      }
    }
  }
}
