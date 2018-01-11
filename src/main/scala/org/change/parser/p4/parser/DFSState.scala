package org.change.parser.p4.parser

import java.io.PrintWriter
import java.util.UUID

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import org.change.parser.p4.parser.StateExpander.deparserConstraints
import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Allocate, Constrain, _}
import org.change.v2.p4.model.{ArrayInstance, Switch}
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

  def getReturnStatement : ReturnStatement = history.head.asInstanceOf[ReturnStatement]

  def addInstr(statement: Statement) = copy(crt = crt,
    depth = depth,
    history = statement :: history)
  def increment(by : Int) = copy(crt = crt + by,
    depth = depth,
    history = history)

  def incrementDepth() = copy(crt = crt, history = history, depth = depth + 1)

  def incrementHeaderCount(hInstance : String): DFSState = {
    val pair = hInstance -> (currentDepth.getOrElse(hInstance, 0) + 1)
    this.copy(currentDepth = currentDepth + pair)
  }
  def setLatest(latest : String): DFSState = copy(latest = latest)

  lazy val seflPortName : String = history.tail.reverse.
    filter(r => r.isInstanceOf[ReturnStatement]).
    filter(r => !r.asInstanceOf[ReturnStatement].isError).
    map(_.asInstanceOf[ReturnStatement]).map(u => {
    u.getWhere
  }).mkString(".")
}

class StateExpander (switch: Switch, startAt : String){

  private def parserEdges(ss : org.change.v2.p4.model.parser.State): Iterable[(String, String)] = {
    ss.getStatements.
      filter(x => x.isInstanceOf[ReturnStatement] || x.isInstanceOf[ReturnSelectStatement]).
      flatMap {
        case rs : ReturnStatement => List[(String, String)]((ss.getName, rs.getWhere))
        case rss : ReturnSelectStatement => rss.getCaseEntryList.map(x => {
          (ss.getName, x.getReturnStatement.getWhere)
        })
      }
  }
  private def parserErrorNodes(ss : org.change.v2.p4.model.parser.State): Iterable[String] = {
    ss.getStatements.
      filter(x => x.isInstanceOf[ReturnStatement] || x.isInstanceOf[ReturnSelectStatement]).
      flatMap {
        case rs : ReturnStatement if rs.isError => rs.getMessage :: Nil
        case rss : ReturnSelectStatement => rss.getCaseEntryList.
          filter(_.getReturnStatement.isError).
          map(x => {
            x.getReturnStatement.getMessage
          })
        case _ => Nil
      }
  }

  private def doTopoSort(): Map[String, Int] = {
    val asList = switch.parserStates().toList
    val errorNodes = asList.flatMap(x => parserErrorNodes(switch.getParserState(x)))
    val ingressNode = "ingress" :: Nil
    val edges = asList.flatMap(x => parserEdges(switch.getParserState(x)))

    val nodes = asList ++ errorNodes ++ ingressNode

    val graph = new Graph(nodes.size)
    val indexedSeq = nodes.zipWithIndex.toMap
    for (e <- edges)
       graph.addEdge(indexedSeq(e._1), indexedSeq(e._2))
    graph.topologicalSort().zipWithIndex.map(r => nodes(r._1.intValue()) -> r._2).toMap
  }
  private lazy val topoSorted = doTopoSort()

  def doDFS(dFSState: DFSState, maybeTopo : Option[Map[String, Int]] = None) : List[DFSState] = {
    val topoSorted = maybeTopo match {
      case None => this.topoSorted
      case Some (x) => x
    }
    val state = switch.getParserState(startAt)
    val newStates = doDFS(dFSState, state).
      sortBy(f => -topoSorted(f.getReturnStatement.getWhere))
    newStates.
      flatMap(x =>
        if (x.getReturnStatement.isError)
          x :: Nil
        else if (!switch.hasParserState(x.getReturnStatement.getWhere))
          x :: Nil
        else
          new StateExpander(switch, x.getReturnStatement.getWhere).doDFS(x.incrementDepth(), Some(topoSorted))
      )
  }

  def doDFS(dFSState: DFSState, state: State): List[DFSState] = {
    val instrs = state.getStatements.take(state.getStatements.size() - 1)
    val returnStatement = state.getStatements.last
    val nfs = instrs.foldLeft(
      (dFSState, false)
    )((acc, x) => {
      if (!acc._2) {
        val newdfs = acc._1.addInstr(x)
        x match {
          case v : SetStatement =>
            val sref = v.getRight match {
              case lref : LatestRef => new StringRef(newdfs.latest + "." + lref.getFieldName)
              case _ => v.getRight
            }
            (acc._1.addInstr(new SetStatement(v.getLeft, sref)), false)
          case v : ExtractStatement =>
            val ref = v.getExpression.asInstanceOf[StringRef].getRef
            val sinstance = switch.getInstance(ref)
            val reg = sinstance.getLayout.getLength
            val pleaseStop = sinstance match {
              case instance: ArrayInstance => acc._1.currentDepth.getOrElse(ref, 0) >= instance.getLength
              case _ => false
            }
            val sth = v.setLocation(acc._1.crt, reg)
            sinstance match {
              case aInstance : ArrayInstance =>
                if (v.getExpression.asInstanceOf[StringRef].getArrayIndex == -1)
                  v.getExpression.asInstanceOf[StringRef].setArrayIndex(acc._1.currentDepth.getOrElse(ref, 0))
              case _ => ;
            }
            (acc._1.addInstr(sth).increment(reg).incrementHeaderCount(ref).
              setLatest(v.getExpression.asInstanceOf[StringRef].getRef), pleaseStop)
          case _ => (newdfs, acc._2)
        }
      } else {
        acc
      }

    })
    if (nfs._2) {
      Nil
    } else {
      returnStatement match {
        case rs: ReturnStatement => List[DFSState](nfs._1.addInstr(rs))
        case rss: ReturnSelectStatement =>
          // non-default cases first
          val cases = rss.getCaseEntryList.filter(x => {
            !x.getValues.isEmpty
          }).map(x => {
            val ces = x.getValues.foldLeft(x.getExpressions.foldLeft(new CaseEntry())((acc, y) => {
              acc.addExpression(y match {
                case v : DataRef => new DataRef(nfs._1.crt + v.getStart, nfs._1.crt + v.getEnd)
                case v : LatestRef => new StringRef(nfs._1.latest + "." + v.getFieldName)
                case _ => y
              })
            }))((acc, y) => {
              acc.addValue(y)
            })
            (nfs._1.addInstr(ces).addInstr(x.getReturnStatement), ces)
          }).toList
          //default case now => always add constrain not
          val defaultCase = rss.getCaseEntryList.filter(x => {
            x.getValues.isEmpty
          }).map(x => {
            nfs._1.addInstr(cases.unzip._2.foldLeft(new CaseNotEntry())((acc, r) => {
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
    explosion.println(JsonUtil.toJson(generateAllPossiblePackets(expd, sw, "")))
    explosion.close()
  }

  def getWidth(expression: String, switch: Switch) : Int = {
    val split = expression.split("\\.")
    val (hdr, fld) = (split(0), split(1))
    switch.getInstance(hdr).getLayout.getField(fld).getLength
  }


  def generateAllPossiblePacketsAsDict(expd: List[DFSState], sw: Switch) : Map[String, Instruction] = {
    expd.map(x => {
      "generator." + x.seflPortName ->
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
    }).toMap
  }

  def generateAllPossiblePackets(expd: List[DFSState],sw : Switch, name : String): InstructionBlock = {
    import org.change.v2.analysis.memory.TagExp.IntImprovements
    InstructionBlock(
      CreateTag("START", 0),
      Fork(
        expd.map(x => Call(name + ".generator." + x.seflPortName))
      )
    )
  }

  private def gatherAllocate(i: Instruction): Iterable[Instruction] = i match {
    case InstructionBlock(instructions) => instructions.flatMap(gatherAllocate)
    case v: AllocateSymbol => List[Instruction](v)
    case _ => Nil
  }

  private def gatherAssign(i: Instruction): Iterable[Instruction] = i match {
    case InstructionBlock(instructions) => instructions.flatMap(gatherAssign)
    case v: AssignNamedSymbol => List[Instruction](v)
    case v: AssignNamedSymbolWithLength => List[Instruction](v)
    case _ => Nil
  }

  private def gatherConstrain(i: Instruction): Iterable[Instruction] = i match {
    case InstructionBlock(instructions) => instructions.flatMap(gatherConstrain)
    case v: ConstrainNamedSymbol => List[Instruction](v.not())
    case _ => Nil
  }

  def deparserCode(expd : List[DFSState], sw : Switch) : Instruction = {
//    Fork(expd.map(x => Forward("deparser." + x.seflPortName)))
    expd.foldRight(Fail("Deparser error: No match") : Instruction)((r, acc) => {
      InstructionBlock(
        deparserAssignments(r, sw),
        deparserConstraints(r, sw)(Forward("deparser." + r.seflPortName), acc)
      )
    })
  }

  private def deparserAssignments(x : DFSState, sw : Switch) : Instruction = {
    val insts = extractDeparserInstructions(x,sw)
    def getAssgns(instruction: Instruction) : List[Instruction] = instruction match {
      case v : AssignNamedSymbol => instruction :: Nil
      case v : AllocateSymbol => instruction :: Nil
      case v : AllocateRaw => instruction :: Nil
      case v : AssignRaw => instruction :: Nil
      case InstructionBlock(instructions) => instructions.flatMap(getAssgns).toList
      case _ => Nil
    }
    InstructionBlock(insts.flatMap(getAssgns))
  }

  private def deparserConstraints(x : DFSState, sw : Switch) : Function2[Instruction, Instruction, Instruction] = {
    val insts = extractDeparserInstructions(x,sw)
    def getConstraints(instruction: Instruction) : List[Instruction] = instruction match {
      case v : ConstrainNamedSymbol => instruction :: Nil
      case v : ConstrainRaw => instruction :: Nil
      case InstructionBlock(instructions) => instructions.flatMap(getConstraints).toList
      case _ => Nil
    }
    (thn, els) => If (InstructionBlock(insts.flatMap(getConstraints)), thn, els)
  }

  private def extractDeparserInstructions(x: DFSState, sw : Switch) = {
    x.history.tail.filter(!_.isInstanceOf[ReturnStatement]).reverse.map {
      case v: ExtractStatement =>
        val sref = v.getExpression.asInstanceOf[StringRef].getRef
        Constrain(sref + ".IsValid", :==:(ConstantValue(1)))
      case v: CaseNotEntry if v.getCaseEntryList.forall(ce => ce.getExpressions.exists(_.isInstanceOf[StringRef])) =>
        val cases = v.getCaseEntryList.map(translateCaseEntry(_, sw))
        InstructionBlock(
          cases.flatMap(gatherAllocate) ++ cases.flatMap(gatherAssign) ++ cases.flatMap(gatherConstrain)
        )
      case v: CaseEntry if v.getExpressions.exists(exp =>
        exp.isInstanceOf[StringRef]) => translateCaseEntry(v, sw, ignoreDataRefs = true)
      case _ => NoOp
    }
  }

  def deparserStateMachineToDict(expd : List[DFSState], sw : Switch) : Map[String, Instruction] = {
    expd.map(x => {
      "deparser." + x.seflPortName ->
        deparserStateToInstruction(x, sw)
    }).toMap
  }

  private def deparserStateToInstruction(x: DFSState, sw : Switch, watchForOthers : Boolean = true) = {
    val mustBeValid = x.history.tail.reverse.collect({
      case v : ExtractStatement => v.getExpression.asInstanceOf[StringRef].getRef
    }).toSet

    val watchForOtherHeaders = if (watchForOthers)
      InstructionBlock(sw.getInstances.filter(i => !i.isMetadata).map(instance => {
        if (!mustBeValid.contains(instance.getName))
          If (Constrain(instance.getName + ".IsValid", :==:(ConstantValue(1))),
            Fail(s"Deparser error: Supplementary valid header ${instance.getName} encountered")
          )
        else
          NoOp
      }).toList)
    else NoOp

    InstructionBlock(
      (watchForOtherHeaders :: (DestroyPacket() ::
        x.history.tail.filter(!_.isInstanceOf[ReturnStatement]).reverse.map {
          case v: ExtractStatement =>
            val sref = v.getExpression.asInstanceOf[StringRef].getRef
            val (i, _) = sw.getInstance(sref).getLayout.getFields.foldLeft((Nil: List[Instruction], 0))((acc, r) => {
              (acc._1 ++ List[Instruction](
                If (Constrain("Truncate", :==:(ConstantValue(1))),
                  If (Constrain("TruncateSize", :<=:(ConstantValue(v.getCrt + acc._2))),
                    InstructionBlock(
                      Allocate(Tag("START") + v.getCrt + acc._2, r.getLength),
                      Assign(Tag("START") + v.getCrt + acc._2, :@(sref + s".${r.getName}"))
                    )
                  ),
                  InstructionBlock(
                    Allocate(Tag("START") + v.getCrt + acc._2, r.getLength),
                    Assign(Tag("START") + v.getCrt + acc._2, :@(sref + s".${r.getName}"))
                  )
                )
              ), acc._2 + r.getLength)
            })
            InstructionBlock(i)
          case _ => NoOp
        })) :+ (x.history.head match {
        case v: ReturnStatement => if (!v.isError)
          Forward(s"deparser.out")
        else Fail(s"Parser failure because ${v.getMessage}")
        case _ => throw new UnsupportedOperationException(s"History head must be ReturnStatement, but ${x.history.head} found")
      })
    )
  }

  def stateMachineToDict(expd : List[DFSState], sw : Switch): Map[String, Instruction] = {
    expd.map(x => {
      "parser." + x.seflPortName ->
      InstructionBlock(
        x.history.tail.filter(!_.isInstanceOf[ReturnStatement]).reverse.map {
          case v: ExtractStatement =>
            val maybeIndex = v.getExpression.asInstanceOf[StringRef].getArrayIndex
            val oldSref = v.getExpression.asInstanceOf[StringRef].getRef
            val sref = oldSref + (if (maybeIndex >= 0)
              "[" + maybeIndex + "]"
            else "")

            InstructionBlock(
              Assign(sref + ".IsValid", ConstantValue(1)) ::
                sw.getInstance(oldSref).getLayout.getFields.foldLeft((Nil: List[Instruction], 0))((acc, r) => {
                  (acc._1 ++ List[Instruction](
                    Allocate(sref + s".${r.getName}", r.getLength),
                    Assign(sref + s".${r.getName}", :@(Tag("START") + v.getCrt + acc._2)),
                    Allocate(s"Original.$sref.${r.getName}", r.getLength),
                    Assign(s"Original.$sref.${r.getName}", :@(sref + s".${r.getName}"))
                  ), acc._2 + r.getLength)
                })._1
            )
          case v: SetStatement =>
            def translateCref(expression: Expression) : FloatingExpression = expression match {
              case ct : ConstantExpression => ConstantValue(ct.getValue)
              case ce : CompoundExpression if ce.isPlus => :+:(translateCref(ce.getLeft), translateCref(ce.getRight))
              case ce : CompoundExpression if !ce.isPlus => :-:(translateCref(ce.getLeft), translateCref(ce.getRight))
              case expr : StringRef => :@(expr.getRef)
              case  _ => throw new NotImplementedError(expression.toString)
            }
            val dstref = v.getLeft.asInstanceOf[StringRef].getRef
            val srcref = translateCref(v.getRight)
            Assign(dstref, srcref)
          case v: CaseNotEntry =>
            val cases = v.getCaseEntryList.map(translateCaseEntry(_, sw))
            InstructionBlock(
              cases.flatMap(gatherAllocate) ++ cases.flatMap(gatherAssign) :+
                InstructionBlock(
                  cases.flatMap(gatherConstrain)
                )
            )
          case v: CaseEntry => translateCaseEntry(v, sw)
        } :+ (x.history.head match {
          case v : ReturnStatement  => if (!v.isError)
            Forward(s"control.${v.getWhere}")
          else Fail(s"Parser failure because ${v.getMessage}")
          case _ => throw new UnsupportedOperationException(s"History head must be ReturnStatement, but ${x.history.head} found")
        })
      )
    }).toMap
  }

  def parseStateMachine(expd: List[DFSState], sw : Switch): Fork =
    Fork(expd.map(x => Forward("parser." + x.seflPortName)))

  def translateCaseEntry(v: CaseEntry,
                         sw : Switch, ignoreDataRefs : Boolean = false): InstructionBlock = {
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
            if (ignoreDataRefs)
              NoOp
            else
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
