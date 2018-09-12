package org.change.v2.analysis.equivalence

import java.io.PrintStream

import org.change.v2.analysis
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.helpers.GFG
import org.change.v2.util.ToDot
import z3.scala.{Z3AST, Z3Context, Z3Solver}

import scala.collection.mutable
import collection.JavaConverters._

object EqPrinter {
  def printme(res1: List[SimpleMemory], ps1: PrintStream): Unit = {
    val alreadyKnown = mutable.Set[(String, String)]()
    def print(bh: List[BranchC]): Unit = {
      if (bh.size == 1) {
        if (!alreadyKnown.contains(("ROOT", bh.head.toString))) {
          ps1.println("ROOT -> " + ToDot.normalize(bh.head.toString) + ";")
          alreadyKnown.add(("ROOT", bh.head.toString))
        }
      } else if (bh.nonEmpty) {
        if (!alreadyKnown.contains((bh.tail.head.toString, bh.head.toString))) {
          alreadyKnown.add((bh.tail.head.toString, bh.head.toString))
          ps1.println(
            ToDot.normalize(bh.tail.head.toString) + " -> " + ToDot.normalize(
              bh.head.toString) + ";")
        }
        print(bh.tail)
      }
    }
    ps1.println("digraph g {")
    res1.zipWithIndex.foreach(r => {
      val last = r._1.branchHistory.headOption.map(_.toString).getOrElse("ROOT")
      val x = "state" + r._2
      ps1.println(ToDot.normalize(last) + " -> " + ToDot.normalize(x) + ";")
      print(r._1.branchHistory)
    })
    ps1.println("}")
    ps1.close()
  }
}

class Equivalence(val instructions1: Map[String, Instruction],
                  val instructions2: Map[String, Instruction]) {

  type MagicTuple =
    (Condition, (Iterable[SimpleMemory], Iterable[SimpleMemory]))
  def simpleSatStrategy(condition: Condition,
                        newCondition: Condition): Boolean =
    newCondition match {
      case TRUE  => true
      case FALSE => false
      case _ =>
        SimpleMemory.isSatS(FAND.makeFAND(condition :: newCondition :: Nil))
    }

  val simpleMemoryInterpreter = new SimpleMemoryInterpreter(simpleSatStrategy)
  val toTheEndExecutor =
    new ToTheEndExecutor(simpleMemoryInterpreter, instructions1)
  val toTheEndExecutor2 =
    new ToTheEndExecutor(simpleMemoryInterpreter, instructions2)
  def show(input: List[SimpleMemory],
           initialLocations: Iterable[(String, String)],
           outputPortCorrespondence: ((String, String) => Boolean),
           outputEquivalenceHook: ((Z3Solver,
                                    SimpleMemory,
                                    SimpleMemory) => Boolean))
    : (Iterable[MagicTuple], Iterable[MagicTuple], Iterable[MagicTuple]) = {
    var i = 0
    val wrongArity = mutable.Buffer.empty[MagicTuple]
    val portMismatch = mutable.Buffer[MagicTuple]()
    val outputMismatch = mutable.Buffer[MagicTuple]()

    for (in <- input) {
      for ((l1, l2) <- initialLocations) {
        val start = java.lang.System.currentTimeMillis()
        val res1 = toTheEndExecutor.executeFrom(l1, in)
        val end = java.lang.System.currentTimeMillis()
        println(s"executed $l1 for ${res1.success.size} in ${end - start}ms")
        i = i + 1
        val all = toTheEndExecutor.sieve(res1.flat())
        println(
          s"${all.size} total number of res1 pcs vs ${res1.success.size + res1.failed.size}")
        all.foreach(h => {
          val res2 = toTheEndExecutor2
            .executeFrom(l2, in.copy(pathCondition = SimplePathCondition(h._1)))
          i = i + 1
          // compare h._2 to res2
          // 1: arity
          val all2 = toTheEndExecutor2.sieve(res2.flat())
          val succ1 = h._2.filter(_.error == "")
          all2.foreach(r => {
            val succ2 = r._2.filter(_.error == "")
            if (succ1.size != succ2.size) {
              wrongArity += ((r._1,
                              (succ1.map(_.copy(
                                 pathCondition = SimplePathCondition.apply())),
                               succ2.map(
                                 _.copy(pathCondition =
                                   SimplePathCondition.apply())))))
            } else {
              if (succ1.size == 1) {
                if (!outputPortCorrespondence(succ1.head.location,
                                              succ2.head.location)) {
                  portMismatch += ((r._1,
                                    (succ1.map(
                                       _.copy(pathCondition =
                                         SimplePathCondition.apply())),
                                     succ2.map(
                                       _.copy(pathCondition =
                                         SimplePathCondition.apply())))))
                } else {
                  val ctx = new Z3Context()
                  val slv = ctx.mkSolver()
                  if (!outputEquivalenceHook(slv, succ1.head, succ2.head)) {
                    outputMismatch += ((r._1,
                                        (succ1.map(
                                           _.copy(pathCondition =
                                             SimplePathCondition.apply())),
                                         succ2.map(
                                           _.copy(pathCondition =
                                             SimplePathCondition.apply())))))
                  }
                }
              } else {
                val firstGraph = succ1
                  .map(u => {
                    succ2.zipWithIndex.filter(p => outputPortCorrespondence(u.location, p._1.location))
                      .map(v => new Integer(v._2))
                      .asJava
                  })
                  .toArray

                val gfg = new GFG(succ1.size)
                val bpm1 = gfg.maxBPMWithExplanation(firstGraph)
                if (bpm1.bpm != succ1.size) {
                  val aslist = succ1.toList
                  val nonMatched = bpm1.notMatched.asScala
                    .map(h => {
                      aslist(h.intValue()).location
                    })
                    .mkString(",")
                  java.lang.System.err.println(
                    s"PORT mismatch because the following were not matched $nonMatched")
                  portMismatch += ((r._1,
                                    (succ1.map(
                                       _.copy(pathCondition =
                                         SimplePathCondition.apply())),
                                     succ2.map(
                                       _.copy(pathCondition =
                                         SimplePathCondition.apply())))))
                } else {
                  val o1o2Rel = succ1
                    .map(u => {
                      succ2.zipWithIndex.filter(p => {
                        val ctx = new Z3Context()
                        val slv = ctx.mkSolver()
                        outputPortCorrespondence(u.location, p._1.location) && outputEquivalenceHook(
                          slv,
                          u,
                          p._1)
                      }).map(r => new Integer(r._2))
                        .asJava
                    })
                    .toArray
                  val bpm2 = gfg.maxBPM(o1o2Rel)
                  if (bpm2 != succ1.size) {
                    outputMismatch += ((r._1,
                                        (succ1.map(
                                           _.copy(pathCondition =
                                             SimplePathCondition.apply())),
                                         succ2.map(
                                           _.copy(pathCondition =
                                             SimplePathCondition.apply())))))
                  }
                }
              }
            }
          })
        })
      }
    }
    (wrongArity, portMismatch, outputMismatch)
  }
}
