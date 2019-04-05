package org.change.p4.control.vera

import org.change.p4.control.Traverse
import org.change.p4.model.control.exp.{DataRefExpr, P4BExpr}
import org.change.p4.model.control.{ControlStatement, EndOfControl}
import org.change.p4.model.parser._
import org.change.p4.model.{ArrayInstance, Switch}
import org.change.utils.graph.{Graph, LabeledGraph}

import scala.collection.JavaConverters._
import scala.collection.mutable

class ParserHelper(switch: Switch) {
  lazy val cfg: Graph[Statement] = buildCFG()
  lazy val lfcfg: Graph[Statement] = cfg.rmLoops(start)
  lazy val start: Statement =
    switch.getParserState("start").getStatements.iterator().next()
  def nextState(ret: ReturnStatement): List[State] = {
    if (!ret.isError && switch.getParserState(ret.getWhere) != null)
      switch.getParserState(ret.getWhere) :: Nil
    else Nil
  }

  def solveEmptyFieldRefs(): Unit = {
    val lastExtract =
      mutable.Map[Statement, Option[ExtractStatement]](start -> None)
    val noloops = lfcfg
    noloops
      .scc()
      .reverse
      .foreach(nds => {
        assert(nds.size == 1)
        val n = nds.head
        val prop = n match {
          case es: ExtractStatement => Some(es)
          case _                    => lastExtract.get(n).flatten
        }
        for (neigh <- noloops.edges.getOrElse(n, Nil)) {
          if (lastExtract.get(neigh).isEmpty)
            lastExtract.put(neigh, prop)
          else {
            val neighlast = lastExtract(neigh)
            if (neighlast != prop)
              lastExtract.put(neigh, None)
          }
        }
      })
    lastExtract
      .filter(_._2.nonEmpty)
      .foreach(x => {
        Traverse(new SetEmpty(x._2.get.getExpression))(x._1)
      })
  }

  lazy val mkUnrolledLabeledGraph
    : LabeledGraph[ControlStatement, Option[P4BExpr]] = {
    val currentMap = mutable.Map.empty[(Statement, Int), ControlStatement]
    def node(statement: Statement,
             idx: Int,
             maybeNew: () => ControlStatement): ControlStatement = {
      currentMap.getOrElseUpdate((statement, idx), maybeNew())
    }
    unrolledCFG
      .mapNodes(stat => {
        (stat._1 match {
          case endOfControl: EndOfControl => endOfControl
          case ss: SetStatement =>
            node(ss, stat._2, () => new SetStatement(ss))
          case ce: CaseEntry =>
            node(ce, stat._2, () => new CaseEntry(ce))
          case rs: ReturnStatement =>
            node(rs, stat._2, () => {
              new ReturnStatement(rs, "_" + stat._2 + "")
            })
          case rss: ReturnSelectStatement =>
            node(rss, stat._2, () => new ReturnSelectStatement(rss))
          case es: ExtractStatement =>
            node(es, stat._2, () => new ExtractStatement(es))
        }): ControlStatement
      })
      .label((_, _) => Option.empty[P4BExpr])
  }

  def resolveDataRefs(labeledGraph: LabeledGraph[ControlStatement, Option[P4BExpr]]):
  Map[(ControlStatement, DataRef), List[List[(ExtractStatement, Int, Int)]]] = {
    labeledGraph.edges.collect {
      case (rss : ReturnSelectStatement, _) =>
        val exprs =
          rss.getCaseEntryList.asScala.head.getBVExpressions.asScala.filter(_.isInstanceOf[DataRefExpr])
            .map(_.asInstanceOf[DataRefExpr])
        if (exprs.nonEmpty) {
          exprs.map(e => {
            val start = e.getDataRef.getStart
            val end = e.getDataRef.getEnd + start - 1
            var stack = List((rss : ControlStatement, start.toInt, end.toInt) :: Nil)
            var pathset = List.empty[List[(ControlStatement, Int, Int)]]
            while (stack.nonEmpty) {
              val lastList = stack.head
              val (nowat, start, end) = lastList.head
              stack = stack.tail
              val (newStart, newEnd) = nowat match {
                case es : ExtractStatement =>
                  val n = es.getExpression.getInstance().getLayout.getLength
                  if (end < n) {
                    // means: stop right there, no need to carry on
                    // the data ref has been exhausted
                    (0, end - n)
                  } else if (start > n - 1) {
                    (start, end)
                  } else {
                    val extracted = n - start
                    (0, end - extracted)
                  }
                case _ => (start, end)
              }
              if (newEnd > 0) {
                val neighs = labeledGraph.edges.getOrElse(nowat, Nil)
                for (n <- neighs) {
                  stack = ((n._1, newStart, newEnd) :: lastList) :: stack
                }
              } else {
                pathset = lastList :: pathset
              }
            }
            (rss, e.getDataRef) -> pathset.map(_.collect {
              case (e : ExtractStatement, a, b) => (e, a, b)
            })
          })
        } else Nil
    }.flatten.toMap
  }

  lazy val unrolledCFG: Graph[(Statement, Int)] = {
    val loops = cfg.nloops(start)
    val annotated = loops.map(loop => {
      val max = loop._2._1.map {
        case es: ExtractStatement =>
          val hexp = es.getExpression
          if (hexp.isArray) {
            switch
              .getInstance(hexp.getPath)
              .asInstanceOf[ArrayInstance]
              .getLength
          } else {
            1
          }
        case _ => 0
      }.sum
      loop._1 -> (loop._2, max)
    })
    val arities =
      annotated.flatMap(r => r._2._1._1.map(q => q -> (r._2._2 + 1)))
    val mmap = mutable.Map.empty[(Statement, Int), List[(Statement, Int)]]
    cfg.edges.foreach(ed => {
      var src = ed._1
      ed._2
        .toSet[Statement]
        .foreach(dst => {
          val isback = annotated
            .get(dst)
            .exists(x => {
              x._1._2.contains(src)
            })
          if (isback) {
            val nrsrc = arities.getOrElse(src, 1)
            (0 until (nrsrc - 1)).foreach(idx => {
              val vl = mmap.getOrElseUpdate((src, idx), Nil)
              mmap.put((src, idx), (dst, idx + 1) :: vl)
            })
            mmap.put(
              (src, nrsrc - 1),
              (
                new ReturnStatement("")
                  .setError(true)
                  .setMessage("out-of-bounds"),
                0
              ) :: Nil
            )
          } else {
            val belongsto1 = annotated.find(an => {
              an._2._1._1.contains(src)
            })
            val belongsto2 = annotated.find(an => {
              an._2._1._1.contains(dst)
            })
            val nrsrc = arities.getOrElse(src, 1)
            if (belongsto1.equals(belongsto2) && belongsto1.nonEmpty) {
              val nrdst = arities.getOrElse(src, 1)
              assert(nrsrc == nrdst)
              (0 until nrsrc).foreach(idx => {
                val vl = mmap.getOrElse((src, idx), Nil)
                mmap.put((src, idx), (dst, idx) :: vl)
              })
            } else {
              (0 until nrsrc).foreach(idx => {
                val vl = mmap.getOrElse((src, idx), Nil)
                mmap.put((src, idx), (dst, 0) :: vl)
              })
            }
          }
        })
    })
    new Graph[(Statement, Int)](mmap.toMap)
  }

  def nextStates(ret: ReturnSelectStatement): List[State] =
    ret.getCaseEntryList.asScala
      .flatMap(ce => nextState(ce.getReturnStatement))
      .toList
  def buildCFG(): Graph[Statement] = {
    val adjList = mutable.Map.empty[Statement, List[Statement]]
    val visited = mutable.Set.empty[org.change.p4.model.parser.State]
    def doBuild(st: org.change.p4.model.parser.State,
                outstanding: Option[Statement]): Unit = {
      if (!visited.contains(st)) {
        val last = st.getStatements.asScala.foldLeft(outstanding)((acc, x) => {
          acc.foreach(o => {
            val crt = adjList.getOrElse(o, Nil)
            adjList.put(o, x :: crt)
          })
          Some(x)
        })
        visited.add(st)
        last.get match {
          case rss: ReturnSelectStatement =>
            val nondef =
              rss.getCaseEntryList.asScala.filter(!_.isDefault).toList
            assert(
              rss.getCaseEntryList.asScala
                .filter(_.isDefault)
                .forall(x => {
                  !x.getNegated.isEmpty
                })
            )
            val crt = adjList.getOrElse(rss, Nil)
            adjList.put(rss, crt ++ rss.getCaseEntryList.asScala)
            rss.getCaseEntryList.asScala.foreach { ce: CaseEntry =>
              val crt = adjList.getOrElse(ce, Nil)
              adjList.put(ce, ce.getReturnStatement :: crt)
              nextState(ce.getReturnStatement)
                .foreach(doBuild(_, Some(ce.getReturnStatement)))
            }
          case rs: ReturnStatement =>
            nextState(rs).foreach(x => doBuild(x, last))
        }
      }
    }
    doBuild(switch.getParserState("start"), None)
    adjList
      .flatMap(f => {
        f._2.collect {
          case rs: ReturnStatement if !rs.isError && nextState(rs).isEmpty =>
            rs
        }
      })
      .foreach(f => {
        val crt = adjList.getOrElseUpdate(f, Nil)
        adjList.put(f, new EndOfControl("parser") :: crt)
      })
    new Graph(adjList.toMap)
  }
}
