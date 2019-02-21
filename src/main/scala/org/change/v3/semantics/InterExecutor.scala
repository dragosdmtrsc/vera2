package org.change.v3.semantics

import java.util.logging.Logger

import org.change.v2.analysis.memory.Triple
import org.change.utils.FreshnessManager
import org.change.v2.analysis.constraint.SimplePathCondition
import org.change.v3.syntax.Instruction

import scala.collection.mutable

class InterExecutor(program : Map[String, Instruction],
                    tripleExecutor: Mapper[SimpleMemory, Triple[SimpleMemory]],
                    mergePoints : String => Boolean) {
  private val q: mutable.Queue[SimpleMemory] = mutable.Queue[SimpleMemory]()

  def executeFromWithConsumer(start : String, simpleMemory: SimpleMemory,
                              consumer : SimpleMemory => Unit): Unit = executeFromWithConsumer(Set(start),
    simpleMemory, consumer)

  def mergeConditions(conditions : List[SimplePathCondition]) : SimplePathCondition = {
    val hd = conditions.head
    conditions.tail.foldLeft(hd)((acc, x) => {
      val (eq, neq) = acc.map(Some(_)).zipAll(x.map(Some(_)), None, None).partition(h =>
        h._1.exists(h1 => h._2.exists(h2 => h1.equals(h2))))
      val (f, s) = neq.unzip
      eq.map(_._1.get) :+ context.mkOr(context.mkAnd(f.collect {
        case Some(ex) => ex
      }:_*), context.mkAnd(s.collect {
        case Some(ex) => ex
      }:_*))
    }).map(context.simplifyAst)
  }
  def merge(loc : String, states : List[SimpleMemory]) : Iterable[SimpleMemory] = {
    val bytags = states.groupBy(_.memTags).values
    val byraws = bytags.flatMap(t => {
      t.groupBy(_.rawObjects.map(x => (x._1, x._2.size))).values
    })
    val bysyms = byraws.flatMap(t => {
      t.groupBy(_.symbols.map(x => (x._1, x._2.size))).values
    })
    bysyms.map(h => {
      val rawPlus = h.head.rawObjects.map(k => {
        val rov = h.head.rawObjects(k._1)
        k._1 -> (if (!h.tail.forall(_.rawObjects(k._1) == rov)) {
          val mo = SimpleMemoryObject(FreshnessManager.next("merge"), rov.size)
          Some(mo)
        } else {
          None
        })
      })
      val symPlus = h.head.symbols.map(k => {
        val rov = h.head.symbols(k._1)
        k._1 -> (if (!h.tail.forall(_.symbols(k._1) == rov)) {
          val mo = SimpleMemoryObject(FreshnessManager.next("merge"), rov.size)
          Some(mo)
        } else {
          None
        })
      })
      val anyraw = rawPlus.filter(_._2.nonEmpty)
      val anysym = symPlus.filter(_._2.nonEmpty)
      val newconditions = h.map(state => {
        (anyraw.flatMap(v => {
          state.rawObjects(v._1).ast.map(x => {
            context.mkEq(x, v._2.get.ast.get)
          })
        }) ++ anysym.flatMap(v => {
          val ros = state.symbols(v._1)
          ros.ast.map(x => {
            context.mkEq(x, v._2.get.ast.get)
          })
        })).foldLeft(state.pathCondition)((acc, x) => {
          x :: acc
        })
      })
      val c2 = mergeConditions(newconditions)
      h.head.copy(history = loc :: Nil,
        pathCondition = c2,
        rawObjects = rawPlus.map(rp => {
          rp._1 -> rp._2.getOrElse(h.head.rawObjects(rp._1))
        }),
        symbols = symPlus.map(sp => {
          sp._1 -> sp._2.getOrElse(h.head.symbols(sp._1))
        })
      )
    })
  }

  def executeFromWithConsumer(start: Set[String],
                              simpleMemory: SimpleMemory,
                              consumer : SimpleMemory => Unit): Unit = {
    start.foreach(x => {
      q.enqueue(simpleMemory.forwardTo(x))
    })
    var toBeMerged = Map.empty[String, List[SimpleMemory]]
    var prevMerge = false
    while (q.nonEmpty) {
      val crt = q.dequeue()
      if (program.contains(crt.location)) {
        if (mergePoints(crt.location)) {
          assert(crt.errorCause.isEmpty)
          toBeMerged = toBeMerged + (crt.location -> (crt :: toBeMerged.getOrElse(crt.location, Nil)))
        } else {
          val prog = program(crt.location)
          val trip = tripleExecutor.execute(prog, crt, true)
          val filtered = trip
          filtered.continue.foreach(consumer)
          filtered.failed.foreach(consumer)
          q.enqueue(filtered.success: _*)
        }
      } else {
        consumer(crt)
      }
      if (q.isEmpty && toBeMerged.nonEmpty) {
        for (x <- toBeMerged) {
          var i = 0
          val start = System.currentTimeMillis()
          val ms = merge(x._1, x._2)
          val end = System.currentTimeMillis()
          val totalMerge = end - start
          for (merged <- ms) {
            i = i + 1
            val prog = program(merged.location)
            val trip = tripleExecutor.execute(prog, merged, true)
            val filtered = trip
            filtered.continue.foreach(consumer)
            filtered.failed.foreach(consumer)
            q.enqueue(filtered.success: _*)
          }
          Logger.getLogger(this.getClass.getName).info(
            s"at location ${x._1} -> ${x._2.size}, now $i in $totalMerge ms")
        }
        toBeMerged = Map.empty
      }
    }
  }

}
