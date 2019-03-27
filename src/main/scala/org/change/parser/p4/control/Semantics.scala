package org.change.parser.p4.control

import org.change.plugins.vera.{ParserHelper, ParserToLogics}
import org.change.utils.graph.LabeledGraph
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control.ControlStatement
import org.change.v2.p4.model.control.exp.P4BExpr

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


// T identifies a region data structure
// T maps to a subset of the state space
// merge: T x T -> T represents region union
// fail : T -> T produces a subset of the region which corresponds
// to failed states
// success : T -> T corresponds to success states (i.e.
// states which get propagated along an edge
// translate(edge) : T -> T defines a region transformer
// execute assumes an initial state which will be
// propagated in topo order through the CFG
abstract class Semantics[T](switch: Switch) {
  private val cfgBuilder = new AnalyzeThis(switch)
  private val controlCFGs = mutable.Map.empty[String, LabeledGraph[ControlStatement, Option[P4BExpr]]]
  private val controlSCCs = mutable.Map.empty[String, List[List[ControlStatement]]]

  protected val listeners: ListBuffer[P4EventListener] = ListBuffer.empty[P4EventListener]

  def listen(eventListener : P4EventListener): Semantics[T] = {
    listeners.append(eventListener)
    this
  }

  def getCFG(control : String): LabeledGraph[ControlStatement, Option[P4BExpr]] = {
    controlCFGs.getOrElseUpdate(control, cfgBuilder.build(control))
  }
  def getSCC(control : String) : List[List[ControlStatement]] = {
    controlSCCs.getOrElseUpdate(control, getCFG(control).graphView.scc())
  }
  def getFirst(control : String): ControlStatement = {
    getSCC(control).last.head
  }

  def translate(src : ControlStatement, rho : Option[P4BExpr], dst : ControlStatement)
               (from : T) : T

  def merge(crt : T, merge : T) : T

  def stop(region : T) : T
  def success(region : T) : T
  def finishNode(src : ControlStatement, region : Option[T]) : Unit = {}
  def beforeNode(src : ControlStatement, region : T) : Unit = {}

  def executeParser(current : Map[ControlStatement, T]) : Map[ControlStatement, T] = {
    execute("parser")(current)
  }
  def executeDeparser(current : Map[ControlStatement, T]) : Map[ControlStatement, T] = {
    execute("deparser")(current)
  }
  class V1Model {
    val parser: LabeledGraph[ControlStatement, Option[P4BExpr]] = getCFG("parser")
    val ingress: LabeledGraph[ControlStatement, Option[P4BExpr]] = getCFG("ingress")
    val egress: LabeledGraph[ControlStatement, Option[P4BExpr]] = getCFG("egress")
    val deparser: LabeledGraph[ControlStatement, Option[P4BExpr]] = getCFG("deparser")

    val parserStart: ControlStatement = parser.graphView.scc().last.head
    val ingressStart: ControlStatement = ingress.graphView.scc().last.head
    val egressStart: ControlStatement = egress.graphView.scc().last.head
    val deparserStart: ControlStatement = deparser.graphView.scc().last.head


  }
  def v1model() : V1Model = {
    new V1Model
  }

  def execute(control : String)(current : Map[ControlStatement, T]) : Map[ControlStatement, T] = {
    val lcfg = getCFG(control)
    val cfg = lcfg.graphView
    cfg.scc().reverse.foldLeft(current)((now, scc) => {
      assert(scc.size == 1)
      val node = scc.head
      val neighs = lcfg.edges.getOrElse(node, Nil)
      val my = now.get(node)
      my.map(obj => {
        beforeNode(node, obj)
        val n = neighs
        val partial = n.foldLeft((now, Option.empty[T]))((acc, nxt) => {
          (translate(node, nxt._2, nxt._1) _).andThen(t => {
            val stopped = stop(t)
            val goon = success(t)
            (acc._1.get(nxt._1).map(old => {
              acc._1 + (nxt._1 -> merge(old, goon))
            }).getOrElse(acc._1 + (nxt._1 -> goon)),
            Some(acc._2.map(merge(_, stopped)).getOrElse(stopped)))
          })(obj)
        })
        if (partial._2.isEmpty) {
          finishNode(node, Some(obj))
          partial._1
        } else {
          finishNode(node, partial._2)
          partial._1 - node
        }

      }).getOrElse(now)
    })
  }
}
