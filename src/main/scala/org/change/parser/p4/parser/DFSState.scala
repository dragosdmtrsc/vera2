package org.change.parser.p4.parser

import java.io.PrintWriter
import java.util.UUID

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :@}
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Constrain, _}
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.parser._

import scala.collection.JavaConversions._

/**
  * Created by dragos on 13.09.2017.
  */
case class DFSState(crt : Int,
                    depth : Int = 0,
                    history : List[Statement] = Nil,
                    currentDepth : Map[String, Int] = Map[String, Int](),
                    latest : String = "") {
  def addInstr(statement: Statement) = DFSState(crt = crt,
    depth = depth,
    history = statement :: history)
  def increment(by : Int) = DFSState(crt = crt + by,
    depth = depth,
    history = history)

  def incrementDepth() = DFSState(crt = crt, history = history, depth = depth + 1)

  def incrementHeaderCount(hInstance : String) = {
    val pair = (hInstance -> (currentDepth.getOrElse(hInstance, -1) + 1))
    this.copy(currentDepth = currentDepth + pair)
  }
  def setLatest(latest : String) = copy(latest = latest)
}

class StateExpander (switch: Switch, startAt : String){

  def doDFS(dFSState: DFSState) : List[DFSState] = {
    val state = switch.getParserState(startAt)
    val newStates = doDFS(dFSState, state)
    val erred = newStates.partition(x => x.history.head.asInstanceOf[ReturnStatement].isError)
    val stopped = erred._2.partition(x =>
      switch.hasParserState(x.history.head.asInstanceOf[ReturnStatement].getWhere)
    )
    stopped._1.flatMap(x => {
      new StateExpander(switch, x.history.head.asInstanceOf[ReturnStatement].getWhere).doDFS(x.incrementDepth)
    }) ++ erred._1 ++ stopped._2
  }

  def doDFS(dFSState: DFSState, state: State): List[DFSState] = {
    val instrs = state.getStatements.take(state.getStatements.size() - 1)
    val returnStatement = state.getStatements.last
    val nfs = instrs.foldLeft(
      dFSState
    )((acc, x) => {
      val newdfs = acc.addInstr(x)
      x match {
        case v : SetStatement => newdfs
        case v : ExtractStatement => {
          val ref = v.getExpression.asInstanceOf[StringRef].getRef
          val reg = switch.getInstance(ref).getLayout.getLength
          val sth = v.setLocation(acc.crt, reg)
          acc.addInstr(sth).increment(reg).incrementHeaderCount(v.getExpression.asInstanceOf[StringRef].getRef).
            setLatest(v.getExpression.asInstanceOf[StringRef].getRef)
        }
        case _ => newdfs
      }
    })
    returnStatement match {
      case rs: ReturnStatement => List[DFSState](nfs.addInstr(rs))
      case rss: ReturnSelectStatement => rss.getCaseEntryList.map(x => {
        val ces = x.getValues.foldLeft(x.getExpressions.foldLeft(new CaseEntry())((acc, y) => {
          acc.addExpression(y match {
            case v : DataRef => new DataRef(nfs.crt + v.getStart, nfs.crt + v.getEnd)
            case v : LatestRef => new StringRef(nfs.latest + "." + v.getFieldName)
            case _ => y
          })
        }))((acc, y) => {
          acc.addValue(y)
        })
        nfs.addInstr(ces).addInstr(x.getReturnStatement)
      }).toList
    }
  }
}

object StateExpander {
  def main(args: Array[String]): Unit = {
    val sw = Switch.fromFile("inputs/simple-nat/simple_nat-ppc.p4")
    val pw = new PrintWriter("inputs/simple-nat/parser-walk.json")
    val expd = new StateExpander(sw, "start").doDFS(DFSState(0))
    pw.println(JsonUtil.toJson(expd))
    pw.close()
    println(expd.size)
    val fork = new PrintWriter("inputs/simple-nat/parser-fork.json")
    fork.println(JsonUtil.toJson(Fork(expd.map(x => {
        InstructionBlock(
          x.history.tail.filter(!_.isInstanceOf[ReturnStatement]).reverse.map(y => {
            y match {
              case v : ExtractStatement => {
                val sref = v.getExpression.asInstanceOf[StringRef].getRef
                InstructionBlock(
                  Assign(sref + ".IsValid", ConstantValue(1)) ::
                  sw.getInstance(sref).getLayout.getFields.foldLeft((Nil : List[Instruction], 0))( (acc, r) => {
                    (acc._1 ++ List[Instruction](
                      Allocate(sref + s".${r.getName}", r.getLength),
                      Assign(sref + s".${r.getName}", :@(Tag("START") + v.getCrt + acc._2))
                    ), acc._2 + r.getLength)
                  })._1.toList
                )
              }
              case v : SetStatement => {
                val dstref = v.getLeft.asInstanceOf[StringRef].getRef
                val srcref = v.getRight.asInstanceOf[StringRef].getRef
                Assign(dstref, :@(srcref))
              }
              case v : CaseEntry => {
                InstructionBlock(
                  v.getValues.zip(v.getExpressions).map(x => {
                    x._2 match {
                      case u : StringRef => {
                        if (x._1.getMask == -1) {
                          Constrain(u.getRef, :==:(ConstantValue(x._1.getValue)))
                        } else {
                          val width = sw.getInstance(u.getRef.split("\\.")(0)).getLayout
                            .getFields.find(_.getName == u.getRef.split("\\.")(1)).get.getLength
                          val tmp = "tmp" + UUID.randomUUID().toString
                          InstructionBlock(
                            Allocate(tmp, width),
                            Assign(tmp, :&&:(ConstantValue(x._1.getMask), :@(u.getRef))),
                            Constrain(tmp, :==:(ConstantValue(x._1.getValue)))
                          )
                        }
                      }
                      case u : DataRef => {
                        if (x._1.getMask == -1) {
                          val tmp = "tmp" + UUID.randomUUID().toString
                          val width = u.getEnd.toInt - u.getStart.toInt
                          InstructionBlock(
                            Allocate(tmp, width),
                            Assign(tmp, :@(Tag("START") + u.getStart.toInt)),
                            Constrain(tmp, :==:(ConstantValue(x._1.getValue)))
                          )
                        } else {
                          val width = u.getEnd.toInt - u.getStart.toInt
                          val tmp = "tmp" + UUID.randomUUID().toString
                          InstructionBlock(
                            Allocate(tmp, width),
                            Assign(tmp, :&&:(ConstantValue(x._1.getMask), :@(Tag("START") + u.getStart.toInt))),
                            Constrain(tmp, :==:(ConstantValue(x._1.getValue)))
                          )
                        }
                      }
                    }
                  })
                )
              }
            }
          })
        )
      })
    )))
    fork.close()
  }
}
