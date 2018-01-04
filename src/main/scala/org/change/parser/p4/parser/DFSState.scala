package org.change.parser.p4.parser

import java.io.PrintWriter
import java.util.UUID

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :@, Address}
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Constrain, _}
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.parser.Statement

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

  def incrementHeaderCount(hInstance : String): DFSState = {
    val pair = (hInstance -> (currentDepth.getOrElse(hInstance, -1) + 1))
    this.copy(currentDepth = currentDepth + pair)
  }
  def setLatest(latest : String): DFSState = copy(latest = latest)
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
      case rss: ReturnSelectStatement => {
        // non-default cases first
        val cases = rss.getCaseEntryList.filter(x => {
          !x.getValues.isEmpty
        }).map(x => {
          val ces = x.getValues.foldLeft(x.getExpressions.foldLeft(new CaseEntry())((acc, y) => {
            acc.addExpression(y match {
              case v : DataRef => new DataRef(nfs.crt + v.getStart, nfs.crt + v.getEnd)
              case v : LatestRef => new StringRef(nfs.latest + "." + v.getFieldName)
              case _ => y
            })
          }))((acc, y) => {
            acc.addValue(y)
          })
          (nfs.addInstr(ces).addInstr(x.getReturnStatement), ces)
        }).toList
        //default case now => always add constrain not
        val defaultCase = rss.getCaseEntryList.filter(x => {
          x.getValues.isEmpty
        }).map(x => {
          nfs.addInstr(cases.unzip._2.foldLeft(new CaseNotEntry())((acc, r) => {
            acc.addCaseEntry(r)
          })).addInstr(x.getReturnStatement)
        })
        cases.unzip._1 ++ defaultCase
      }
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
    fork.println(JsonUtil.toJson(parseStateMachine(expd, sw)))
    fork.close()

    val explosion = new PrintWriter("inputs/simple-nat/parser-explosion.json")
    explosion.println(JsonUtil.toJson(generateAllPossiblePackets(expd, sw)))
    explosion.close()
  }

  def getWidth(expression: String, switch: Switch) : Int = {
    val split = expression.split("\\.")
    val (hdr, fld) = (split(0), split(1))
    switch.getInstance(hdr).getLayout.getField(fld).getLength
  }

  def generateAllPossiblePackets(expd: List[DFSState],sw : Switch): InstructionBlock = {
    import org.change.v2.analysis.memory.TagExp.IntImprovements
    InstructionBlock(
      CreateTag("START", 0),
      Fork(
        expd.map(x => {
          InstructionBlock(x.history.filter(x => x.isInstanceOf[ExtractStatement]).reverse.flatMap(r => {
              val extractStatement = r.asInstanceOf[ExtractStatement]
              val sref = extractStatement.getExpression.asInstanceOf[StringRef].getRef
              sw.getInstance(sref).getLayout.getFields.foldLeft((Nil : List[Instruction],
                extractStatement.getCrt))((acc, y) => {
                val width = y.getLength
                (acc._1 ++ List[Instruction](
                  Allocate(Tag("START") + acc._2, width),
                  Assign(Tag("START") + acc._2, SymbolicValue())
                ), acc._2 + width)
              })._1
            })
          )
        })
      )
    )
  }

  def parseStateMachine(expd: List[DFSState], sw : Switch): Fork = {
    Fork(expd.map(x => {
      InstructionBlock(
        x.history.tail.filter(!_.isInstanceOf[ReturnStatement]).reverse.map {
          case v: ExtractStatement => {
            val sref = v.getExpression.asInstanceOf[StringRef].getRef
            InstructionBlock(
              Assign(sref + ".IsValid", ConstantValue(1)) ::
                sw.getInstance(sref).getLayout.getFields.foldLeft((Nil: List[Instruction], 0))((acc, r) => {
                  (acc._1 ++ List[Instruction](
                    Allocate(sref + s".${r.getName}", r.getLength),
                    Assign(sref + s".${r.getName}", :@(Tag("START") + v.getCrt + acc._2)),
                    Allocate(s"Original.$sref.${r.getName}", r.getLength),
                    Assign(s"Original.$sref.${r.getName}", :@(sref + s".${r.getName}"))
                  ), acc._2 + r.getLength)
                })._1.toList
            )
          }
          case v: SetStatement => {
            val dstref = v.getLeft.asInstanceOf[StringRef].getRef
            val srcref = v.getRight.asInstanceOf[StringRef].getRef
            Assign(dstref, :@(srcref))
          }
          case v: CaseNotEntry => {
            val cases = v.getCaseEntryList.map(translateCaseEntry(_, sw))
            def gatherAllocate(i: Instruction): Iterable[Instruction] = i match {
              case InstructionBlock(instructions) => instructions.flatMap(gatherAllocate)
              case v: AllocateSymbol => List[Instruction](v)
              case _ => Nil
            }
            def gatherAssign(i: Instruction): Iterable[Instruction] = i match {
              case InstructionBlock(instructions) => instructions.flatMap(gatherAssign)
              case v: AssignNamedSymbol => List[Instruction](v)
              case v: AssignNamedSymbolWithLength => List[Instruction](v)
              case _ => Nil
            }

            def gatherConstrain(i: Instruction): Iterable[Instruction] = i match {
              case InstructionBlock(instructions) => instructions.flatMap(gatherConstrain)
              case v: ConstrainNamedSymbol => List[Instruction](v.not())
              case _ => Nil
            }
            InstructionBlock(
              cases.flatMap(gatherAllocate) ++ cases.flatMap(gatherAssign) :+
                Fork(
                  cases.flatMap(gatherConstrain)
                )
            )
          }
          case v: CaseEntry => {
            translateCaseEntry(v, sw)
          }
        } :+ (x.history.head match {
          case v : ReturnStatement  => if (!v.isError)
            Forward(s"control.${v.getWhere}")
            else Fail(s"Parser failure because ${v.getMessage}")
          case _ => throw new UnsupportedOperationException(s"History head must be ReturnStatement, but ${x.history.head} found")
        })
      )
    })
    )
  }

  def translateCaseEntry(v: CaseEntry, sw : Switch): InstructionBlock = {
    InstructionBlock(
      v.getValues.zip(v.getExpressions).map(x => {
        x._2 match {
          case u: StringRef =>
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
          case u: DataRef =>
            if (x._1.getMask == -1) {
              val tmp = "tmp" + UUID.randomUUID().toString
              val width = u.getEnd.toInt - u.getStart.toInt
              InstructionBlock(
                Allocate(tmp, width),
                Assign(tmp, :@(Tag("START") + u.getStart.toInt).asInstanceOf[Address], width),
                Constrain(tmp, :==:(ConstantValue(x._1.getValue)))
              )
            } else {
              val width = u.getEnd.toInt - u.getStart.toInt
              val tmp = "tmp" + UUID.randomUUID().toString
              InstructionBlock(
                Allocate(tmp, width),
                Assign(tmp, :@(Tag("START") + u.getStart.toInt).asInstanceOf[Address], width),
                Assign(tmp, :&&:(ConstantValue(x._1.getMask), :@(tmp))),
                Constrain(tmp, :==:(ConstantValue(x._1.getValue)))
              )
            }
        }
      })
    )
  }
}
