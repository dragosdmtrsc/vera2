package org.change.parser.p4.parser

import org.change.parser.p4.HeaderInstance
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :+:, :-:, :@}
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.memory.{State, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.{ArrayInstance, ISwitchInstance, Switch}
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.p4.model.parser._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait ParserGenerator {
  def parserCode() : Instruction
  def generatorCode() : Instruction
  def inlineGeneratorCode() : Instruction
  def deparserCode() : Instruction
  def extraCode() : Map[String, Instruction]
  def generatorStartPoints() : Set[String]
}

class LightParserGenerator(switch: Switch,
                           switchInstance: ISwitchInstance) extends ParserGenerator {
  private lazy val startState = switch.getParserState("start")
  private val name = switchInstance.getName
  def translateExpression(expression: Expression) : FloatingExpression = expression match {
    case dr : DataRef => Take(Right(Tag("CRT")), dr.getStart, dr.getEnd)
    case ce : CompoundExpression => if (ce.isPlus)
      :+:(translateExpression(ce.getLeft), translateExpression(ce.getRight))
      else :-:(translateExpression(ce.getLeft), translateExpression(ce.getRight))
    case constant : ConstantExpression => ConstantValue(constant.getValue)
    case lr : LatestRef => ???
    case sr : StringRef => :@(sr.getRef)
  }

  def refersLatest(e : Expression) : Boolean = e match {
    case ce : CompoundExpression =>
      refersLatest(ce.getLeft) || refersLatest(ce.getRight)
    case _ : LatestRef => true
    case _ => false
  }
  def refersLatest(s : Statement): Boolean = s match {
    case rss : ReturnSelectStatement =>
      rss.getCaseEntryList.head.getExpressions.exists(refersLatest)
    case ss : SetStatement => refersLatest(ss.getLeft) || refersLatest(ss.getRight)
    case _ => false
  }

  def propagateExtract(s : org.change.v2.p4.model.parser.State,
                       visited : mutable.Map[String, Int] = mutable.Map.empty,
                       stat2extract : Map[Statement, Option[ExtractStatement]] = Map.empty,
                       maybe : Option[ExtractStatement] = None) : Map[Statement, Option[ExtractStatement]] = {
    val v = visited.getOrDefault(s.getName, 0)
    if (v >= 32) {
      stat2extract
    } else {
      visited.put(s.getName, v + 1)
      val crtBlock = s.getStatements.foldLeft((maybe, stat2extract))((acc, x) => x match {
        case exs : ExtractStatement => (Some(exs), acc._2)
        case _ => if (!refersLatest(x)) {
          acc
        } else {
          if (acc._2.contains(x) && acc._2(x) != acc._1) {
            throw new IllegalStateException(s"ambiguous reference to latest in $x")
          } else {
            if (acc._1.isEmpty) throw new IllegalStateException(s"no latest reference in $x")
            else (acc._1, acc._2 + (x -> acc._1))
          }
        }
      })
      s.getStatements.last match {
        case rss : ReturnSelectStatement => rss.getCaseEntryList.
          filter(!_.getReturnStatement.isError).
          filter(x => switch.getParserState(x.getReturnStatement.getWhere) != null).
          flatMap(ce => {
            propagateExtract(switch.getParserState(ce.getReturnStatement.getWhere), visited,
              crtBlock._2, crtBlock._1)
          }).toMap
        case rs : ReturnStatement => propagateExtract(switch.getParserState(rs.getWhere), visited,
          crtBlock._2, crtBlock._1)
      }
    }
  }

  def translateValue(value : Value) : ConstantValue = ConstantValue(value.getValue & value.getMask)

  def handleStatement(mes : Map[Statement, Option[ExtractStatement]])
                     (statement: Statement,
                      visited : mutable.Set[String],
                      stack : mutable.ListBuffer[String]) : Instruction = statement match {
    case ss : SetStatement =>
      val lv = ss.getLeft.asInstanceOf[StringRef]
      def translateCref(expression: Expression): FloatingExpression =
        expression match {
          case ct: ConstantExpression => ConstantValue(ct.getValue)
          case ce: CompoundExpression if ce.isPlus => :+:(translateCref(ce
            .getLeft), translateCref(ce.getRight))
          case ce: CompoundExpression if !ce.isPlus => :-:(translateCref(ce
            .getLeft), translateCref(ce.getRight))
          case expr: StringRef => :@(expr.getRef)
          case lr : LatestRef => translateCref(mes(statement).get.getExpression)
          case _ => throw new NotImplementedError(expression.toString)
        }

      val dstref = ss.getLeft.asInstanceOf[StringRef].getRef
      val srcref = translateCref(ss.getRight)
      Assign(dstref, srcref)
    case es : ExtractStatement =>
      val hi = es.getExpression.asInstanceOf[StringRef]
      val instance = switch.getInstance(hi.getRef)
      if (hi.isNext) {
        val ainstance = instance.asInstanceOf[ArrayInstance]
        val len = ainstance.getLength
        (0 until len).foldRight(Fail("out of bounds") : Instruction)((x, acc) => {
          val sref = new StringRef(hi.getRef).setArrayIndex(x)
          val extractStatement = new ExtractStatement(sref)
          If (Constrain(hi.getRef + ".next", :==:(ConstantValue(x))),
            InstructionBlock(
              handleStatement(mes)(extractStatement, visited, stack),
              Increment(hi.getRef + ".next")
            ), acc
          )
        })
      } else {
        val base = if (hi.isArray) hi.getRef + s"[${hi.getArrayIndex}]"
        else hi.getRef
        InstructionBlock(
          Assign(base + ".IsValid", ConstantValue(1)) ::
          instance.getLayout.getFields.flatMap(fld => {
            val fname = s"$base.${fld.getName}"
            List(
              Allocate(fname, fld.getLength),
              Assign(fname, :@(Tag("CRT"))),
              CreateTag("CRT", Tag("CRT") + fld.getLength)
            )
          }).toList
        )
      }
    case rss : ReturnSelectStatement =>
      val exprs = rss.getCaseEntryList.head.getExpressions
      val defaultEntry = rss.getCaseEntryList.find(_.isDefault).map(_.getReturnStatement).
        getOrElse(new ReturnStatement("").setError(true).setMessage("reject"))
      rss.getCaseEntryList.filter(!_.isDefault).foldRight(
          handleStatement(mes)(defaultEntry, visited, stack))((x, acc) => {
        val cd = InstructionBlock(exprs.map(translateExpression).zip(x.getValues.toList).map(u => {
          if (u._2.getMask != -1)
            ConstrainFloatingExpression(:&&:(ConstantValue(u._2.getMask), u._1),
              :==:(translateValue(u._2)))
          else
            ConstrainFloatingExpression(u._1, :==:(translateValue(u._2)))
        }))
        If (cd, handleStatement(mes)(x.getReturnStatement, visited, stack), acc)
      })
    case rs : ReturnStatement => if (rs.isError) {
      Fail(rs.getMessage)
    } else {
      if (switch.getParserState(rs.getWhere) != null) {
        if (!visited.contains(rs.getWhere))
          stack.append(rs.getWhere)
        Forward(name + ".parser." + rs.getWhere)
      } else {
        Forward(name + ".control." + rs.getWhere)
      }
    }
  }

  def handleState(state : org.change.v2.p4.model.parser.State,
                  visited : mutable.Set[String],
                  stack : mutable.ListBuffer[String]) :
    Instruction = {
    val estat = handleStatement(propagateExtract(state)) _
    InstructionBlock(
      state.getStatements.map(estat(_, visited, stack))
    )
  }

  override def parserCode(): Instruction = {
    val instances = switch.getInstances.toList
    val first = instances.flatMap(x => x match {
      case ai: ArrayInstance if !ai.isMetadata =>
        Assign(ai.getName + ".next", ConstantValue(0)) :: (0 until ai.getLength).map(x => {
          Assign(ai.getName + s"[$x].IsValid", ConstantValue(0))
        }).toList
      case _ => if (!x.isMetadata)
        Assign(x.getName + ".IsValid", ConstantValue(0)) :: Nil
        else Nil
    })
    InstructionBlock(
      first ++ List(CreateTag("CRT", Tag("START")), Forward(name + ".parser." + "start"))
    )
  }

  override def generatorCode(): Instruction = ???

  override def inlineGeneratorCode(): Instruction = ???

  private def ret2nxt(rs : ReturnStatement) = if (!rs.isError)
    if (switch.getParserState(rs.getWhere) != null)
      rs.getWhere :: Nil
    else Nil
  else Nil

  private def neighs(nxt : org.change.v2.p4.model.parser.State) = nxt.getStatements.collect {
    case rs : ReturnStatement => ret2nxt(rs)
    case rss : ReturnSelectStatement => rss.getCaseEntryList.flatMap(ce => ret2nxt(ce.getReturnStatement))
  }.flatten

  private def topoRecur(crt : String,
                        visited : mutable.Set[String],
                        res : mutable.ListBuffer[String]) : mutable.ListBuffer[String] = {
    visited.add(crt)
    neighs(switch.getParserState(crt)).foreach(h =>
      if (! visited.contains(h))
        topoRecur(h, visited, res))
    res.prepend(crt)
    res
  }
  private def topoSort() = {
    val stack = new mutable.ListBuffer[String]()
    val visited = mutable.Set.empty[String]
    topoRecur("start", visited, stack)
  }

  private def revExtract(f: StringRef) : Instruction = {
    val hi = switch.getInstance(f.getRef)
    val base = if (f.isArray) f.getRef + s"[${f.getArrayIndex}]" else f.getRef
    If (Constrain(base + ".IsValid", :==:(ConstantValue(1))),
      InstructionBlock(hi.getLayout.getFields.flatMap(fld => {
        val fname = s"$base.${fld.getName}"
        List(
          Allocate(Tag("CRT"), fld.getLength),
          Assign(Tag("CRT"), :@(fname)),
          CreateTag("CRT", Tag("CRT") + fld.getLength)
        )
      }))
    )
  }

  private def deparse(s : String) = {
    val st = switch.getParserState(s)
    InstructionBlock(st.getStatements.collect {
      case es : ExtractStatement => es.getExpression.asInstanceOf[StringRef]
    }.map(f => {
      if (f.isNext) {
        val hi = switch.getInstance(f.getRef).asInstanceOf[ArrayInstance]
        (0 until hi.getLength).foldRight(Fail("array index out of bounds") : Instruction)((x, acc) => {
          If (Constrain(hi.getName + ".next", :==:(ConstantValue(x))),
            InstructionBlock(
              Increment(hi.getName + ".next"),
              revExtract(new StringRef(hi.getName + s"[$x]"))
            ), acc
          )
        })
      } else {
        revExtract(f)
      }
    }))
  }

  override def deparserCode(): Instruction = {
    val first = CreateTag("CRT", Tag("START")) :: switch.getInstances.flatMap {
      case ai: ArrayInstance if !ai.isMetadata =>
        Assign(ai.getName + ".next", ConstantValue(0)) :: Nil
      case _ => Nil
    }.toList
    InstructionBlock(first ++ topoSort().map(deparse))
  }

  override lazy val extraCode: Map[String, Instruction] = {
    val stack = mutable.ListBuffer.empty[String]
    val visited = mutable.Set.empty[String]
    stack.append("start")
    val instrs = mutable.Map.empty[String, Instruction]
    while (stack.nonEmpty) {
      val top = stack.remove(0)
      visited.add(top)
      instrs += (name + ".parser." + top) -> handleState(switch.getParserState(top), visited, stack)
    }
    instrs.toMap
  }

  override def generatorStartPoints(): Set[String] = ???
}

class SwitchBasedParserGenerator(switch : Switch,
                                 switchInstance: ISwitchInstance,
                                 codeFilter : Option[Function1[String, Boolean]] = None
                                ) extends ParserGenerator {
  private lazy val expd = new StateExpander(switch, "start").doDFS(DFSState(0))

  override lazy val generatorStartPoints: Set[String] =
    StateExpander.getGenPorts(expd, switch, switchInstance.getName + ".", codeFilter)

  override lazy val parserCode: Instruction = StateExpander.parseStateMachine(expd, switch, codeFilter, name = switchInstance.getName + ".")

  override lazy val generatorCode : Instruction = StateExpander.generateAllPossiblePackets(expd, switch, name = switchInstance.getName + ".", codeFilter)

  override lazy val deparserCode : Instruction = StateExpander.deparserCode(expd, switch, codeFilter, name = switchInstance.getName + ".")

  protected lazy val extraCodeInternal: Map[String, Instruction] =
    StateExpander.deparserStateMachineToDict(expd, switch, codeFilter, name = switchInstance.getName + ".") ++
    StateExpander.stateMachineToDict(expd, switch, codeFilter, name = switchInstance.getName + ".") ++
    StateExpander.generateAllPossiblePacketsAsDict(expd, switch, codeFilter, name = switchInstance.getName + ".")

  override def extraCode() : Map[String, Instruction] = extraCodeInternal

  override def inlineGeneratorCode() =
    InstructionBlock(
      CreateTag("START", 0),
      Fork(
        expd.filter(s => codeFilter.getOrElse((_ : String) => true)(s.seflPortName)).
          map(x => extraCode()(switchInstance.getName + "." + "generator." + x.seflPortName))
      )
    )
}

class SkipParserAndDeparser(switch : Switch, switchInstance: ISwitchInstance,
                            codeFilter : Option[Function1[String, Boolean]] = None)
  extends SwitchBasedParserGenerator(switch, switchInstance, codeFilter)
{

  override lazy val deparserCode : Instruction = Forward(s"${switchInstance.getName}.deparser.out")

  override val extraCode : Map[String, Instruction] = extraCodeInternal.
    filter(_._1.startsWith(s"${switchInstance.getName}.generator.")).
    map(h =>
      h._1 -> InstructionBlock(
        Assign("parser_fixpoint",
          ConstantStringValue(h._1.substring(s"${switchInstance.getName}.generator.".length))),
        h._2
      )) ++ extraCodeInternal.filter(_._1.startsWith(s"${switchInstance.getName}.parser.")).map(h => {
        h._1 -> InstructionBlock(Constrain("parser_fixpoint",
            :==:(ConstantStringValue(h._1.substring(s"${switchInstance.getName}.parser.".length)))),
          h._2
        )
      })
}

class TrivialDeparserGenerator (switch : Switch,
                        switchInstance: ISwitchInstance,
                        codeFilter : Option[Function1[String, Boolean]] = None
                       ) extends SwitchBasedParserGenerator(switch, switchInstance, codeFilter) {
  override lazy val deparserCode: Instruction = Forward(s"${switchInstance.getName}.deparser.out")
}
