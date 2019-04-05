package org.change.parser.p4.control

import org.change.utils.graph.LabeledGraph
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control.exp.P4BExpr
import org.change.v2.p4.model.control.{ControlStatement, EndOfControl}

import scala.collection.mutable


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
  val cfgBuilder = new AnalyzeThis(switch)
  private val controlCFGs = mutable.Map.empty[String, LabeledGraph[ControlStatement, Option[P4BExpr]]]
  private val controlSCCs = mutable.Map.empty[String, List[List[ControlStatement]]]

  def getCFG(control : String): LabeledGraph[ControlStatement, Option[P4BExpr]] = {
    controlCFGs.getOrElseUpdate(control, cfgBuilder.build(control))
  }
  def getSCC(control : String) : List[List[ControlStatement]] = {
    controlSCCs.getOrElseUpdate(control, getCFG(control).graphView.scc())
  }
  def getFirst(control : String): ControlStatement = {
    getSCC(control).lastOption.map(_.head).getOrElse(new EndOfControl(control))
  }

  def buffer(p4Memory : T,
             initial : T, ingress: Boolean = true) : BufferResult[T] =
    BufferResult(p4Memory, p4Memory, p4Memory, p4Memory)

  def parse(p4Memory: T): T = {
    val first = getFirst("parser")
    val parserCfg = getCFG("parser")
    val mapping = runOnCFG("parser", Map(first -> p4Memory), parserCfg)
    mapping(new EndOfControl("parser"))
  }

  def parseWithInfo(p4Memory: T) : (T, Map[ControlStatement, T]) = {
    val first = getFirst("parser")
    val parserCfg = getCFG("parser")
    val mapping = runOnCFG("parser", Map(first -> p4Memory), parserCfg)
    (mapping(new EndOfControl("parser")), mapping)
  }

  def translate(src : ControlStatement, rho : Option[P4BExpr], dst : ControlStatement)
               (from : T) : T

  def merge(crt : T, merge : T) : T

  def stop(region : T) : T
  def success(region : T) : T
  def finishNode(src : ControlStatement, region : Option[T]) : Unit = {}
  def beforeNode(src : ControlStatement, region : T) : Unit = {}
  def runControl(control : String, startFrom : T) : T = {
    val lcfg = getCFG(control)
    val first = getFirst(control)
    runOnCFG(control, Map(first -> startFrom), lcfg)(new EndOfControl(control))
  }
  def deparse(startFrom : T) : T = {
    val lcfg = getCFG("deparser")
    val first = getFirst("deparser")
    runOnCFG("deparser",
      Map(first -> startFrom), lcfg)(new EndOfControl("deparser"))
  }

  def execute(control : String)(current : Map[ControlStatement, T]) : Map[ControlStatement, T] = {
    val lcfg = getCFG(control)
    runOnCFG(control, current, lcfg)
  }

  private def runOnCFG(control: String,
                       current: Map[ControlStatement, T],
                       lcfg: LabeledGraph[ControlStatement, Option[P4BExpr]]): Map[ControlStatement, T] = {
    val cfg = lcfg.graphView
    cfg.scc().reverse.foldLeft(current)((now, scc) => {
      if (scc.size != 1) {
        throw new AssertionError(s"something is wrong in $control, scc <> 1" + scc)
      }
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
          partial._1
        }
      }).getOrElse(now)
    })
  }
}
