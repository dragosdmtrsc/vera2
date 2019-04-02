package org.change.parser.p4.control

import org.change.plugins.vera.ParserHelper
import org.change.utils.graph.{Graph, LabeledGraph}
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control._
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AnalyzeThis(switch: Switch) {
  private val parserHelper = new ParserHelper(switch)
  private val edges = mutable.Map.empty[ControlStatement, List[(ControlStatement, Option[P4BExpr])]]
  private val outstanding = ListBuffer.empty[(ControlStatement, Option[P4BExpr])]
  def mkEdge(controlStatement: ControlStatement): Unit = {
    for (open <- outstanding) {
      val crt = edges.getOrElse(open._1, Nil)
      edges.put(open._1, (controlStatement, open._2) :: crt)
    }
    outstanding.clear()
  }
  def open(statement : ControlStatement) : Unit = {
    outstanding.append((statement, None))
  }
  def open(statement : ControlStatement, p4BExpr: P4BExpr) : Unit = {
    outstanding.append((statement, Some(p4BExpr)))
  }
  def lookAt(statement : ControlStatement): Unit = statement match {
    case b : BlockStatement =>
      for (x <- b.getStatements.asScala) {
        lookAt(x)
      }
    case ifElseStatement: IfElseStatement =>
      mkEdge(ifElseStatement)
      outstanding.append((ifElseStatement, Some(ifElseStatement.getCondition)))
      lookAt(ifElseStatement.getIfbranch)
      val oldOutstanding = outstanding.clone()
      outstanding.clear()
      outstanding.append((ifElseStatement, Some(
        new NegBExpr(ifElseStatement.getCondition))))
      if (ifElseStatement.getElseBranch != null) {
        lookAt(ifElseStatement.getElseBranch)
      }
      outstanding.appendAll(oldOutstanding)
    case applyTableStatement: ApplyTableStatement =>
      mkEdge(applyTableStatement)
      open(applyTableStatement)
    case applyAndSelectTableStatement: ApplyAndSelectTableStatement =>
      mkEdge(applyAndSelectTableStatement)
      //TODO: make this a pass in SolveTables, because it is out of scope here
      if (!applyAndSelectTableStatement.isFullyCovered)
        applyAndSelectTableStatement.createDefault()
      val oldOutstanding = ListBuffer.empty[(ControlStatement, Option[P4BExpr])]
      applyAndSelectTableStatement.getCaseEntries.asScala.foreach(ce => {
        open(applyAndSelectTableStatement)
        lookAt(ce)
        oldOutstanding.appendAll(outstanding)
        outstanding.clear()
      })
      outstanding.appendAll(oldOutstanding)
    case caseEntry: TableCaseEntry =>
      mkEdge(caseEntry)
      open(caseEntry)
      if (caseEntry.getStatement != null)
        lookAt(caseEntry.getStatement)
    case applyControlStatement: ApplyControlStatement =>
      mkEdge(applyControlStatement)
      open(applyControlStatement)
      lookAt(switch.controlBlock(applyControlStatement.getBlock.getName).getStatement)
  }

  def build(controlName : String) : LabeledGraph[ControlStatement, Option[P4BExpr]] = {
    val ctrl = switch.controlBlock(controlName)
    if (ctrl != null) {
      edges.clear()
      outstanding.clear()
      lookAt(ctrl.getStatement)
      if (outstanding.nonEmpty) {
        mkEdge(new EndOfControl(controlName))
      }
      new LabeledGraph(edges.toMap)
    } else {
      if (controlName == "parser") {
        parserHelper.mkUnrolledLabeledGraph
      } else if (controlName == "deparser") {
        val eds = parserHelper.unrolledCFG.scc().flatMap(r => {
          r.collect {
            case (es : ExtractStatement, _) =>
              new EmitStatement(es.getExpression)
          }
        }).foldLeft((Option.empty[ControlStatement], Map.empty[ControlStatement, List[ControlStatement]]))((acc, x) => {
          (Some(x), if (acc._1.nonEmpty) {
            acc._2 + (acc._1.get -> (x :: Nil))
          } else {
            acc._2
          })
        })
        val last = eds._1.get
        new Graph(eds._2 +
          (last -> (new EndOfControl("deparser") :: Nil)))
          .label((_, _) => Option.empty[P4BExpr])
      } else {
        throw new IllegalArgumentException(s"can't find $controlName object")
      }
    }
  }
}
