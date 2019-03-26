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
      new LabeledGraph(edges.toMap)
    } else {
      if (controlName == "parser") {
        parserHelper.unrolledCFG.mapNodes(stat => {
          (stat._1 match {
            case ss : SetStatement =>
              new SetStatement(ss.getLeft, ss.getRight)
            case cne : CaseNotEntry =>
              new CaseNotEntry(cne)
            case ce : CaseEntry =>
              new CaseEntry(ce)
            case rs : ReturnStatement =>
              new ReturnStatement(rs)
            case rss : ReturnSelectStatement =>
              new ReturnSelectStatement(rss)
            case es : ExtractStatement =>
              new ExtractStatement(es)
          }) : ControlStatement
        }).label((_, _) => Option.empty[P4BExpr])
      } else if (controlName == "deparser") {
        new Graph(parserHelper.cfg.scc().flatMap(r => {
          r.collect {
            case es : ExtractStatement =>
              new EmitStatement(es.getExpression)
          }
        }).foldLeft((Option.empty[ControlStatement], Map.empty[ControlStatement, List[ControlStatement]]))((acc, x) => {
          (Some(x), if (acc._1.nonEmpty) {
            acc._2 + (acc._1.get -> (x :: Nil))
          } else {
            acc._2
          })
        })._2).label((_, _) => Option.empty[P4BExpr])
      } else {
        throw new IllegalArgumentException(s"can't find $controlName object")
      }
    }
  }
}
