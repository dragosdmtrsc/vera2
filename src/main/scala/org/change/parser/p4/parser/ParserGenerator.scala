package org.change.parser.p4.parser

import java.util.UUID

import org.change.utils.{FreshnessManager, graph}
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :+:, :-:, :@}
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.{ArrayInstance, Field, ISwitchInstance, Switch}

import scala.collection.JavaConversions._
import scala.collection.mutable

trait ParserGenerator {
  def parserCode() : Instruction
  def generatorCode() : Instruction
  def inlineGeneratorCode() : Instruction
  def deparserCode() : Instruction
  def extraCode() : Map[String, Instruction]
  def generatorStartPoints() : Set[String]
}

class LightParserGenerator(switch: Switch,
                           switchInstance: ISwitchInstance,
                           noInputPackets : Boolean = false,
                           justParser : Boolean = false) extends ParserGenerator {
  private lazy val startState = switch.getParserState("start")
  private val name = switchInstance.getName

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
        case rs : ReturnStatement =>
          if (!rs.isError && switch.getParserState(rs.getWhere) != null)
            propagateExtract(switch.getParserState(rs.getWhere), visited,
            crtBlock._2, crtBlock._1)
          else crtBlock._2
      }
    }
  }

  def translateValue(value : Value) : ConstantValue = ConstantValue(value.getValue & value.getMask)
  def handleArray(headerRef: IndexedHeaderRef, fun : (Int) => Instruction) = {
    if (headerRef.isLast) {
      val ainstance = switch.getInstance(headerRef.getPath).asInstanceOf[ArrayInstance]
      val nxtname = headerRef.getPath + ".next"
      (0 until ainstance.getLength).foldRight(Fail(s"out of bounds for ${headerRef.getPath}") : Instruction)((x, acc) => {
        If (Constrain(nxtname, :==:(ConstantValue(x))),
          fun(x), acc
        )
      })
    } else {
      fun(headerRef.getIndex)
    }
  }

  def nextState(ret : ReturnStatement) : List[org.change.v2.p4.model.parser.State] = {
    if (!ret.isError && switch.getParserState(ret.getWhere) != null)
      switch.getParserState(ret.getWhere) :: Nil
    else Nil
  }
  def nextStates(ret : ReturnSelectStatement) : List[org.change.v2.p4.model.parser.State] =
    ret.getCaseEntryList.flatMap(ce => nextState(ce.getReturnStatement)).toList
  def buildCFG() : org.change.utils.graph.Graph[Statement] = {
    val adjList = mutable.Map.empty[Statement, List[Statement]]
    val visited = mutable.Set.empty[org.change.v2.p4.model.parser.State]
    def doBuild(st : org.change.v2.p4.model.parser.State,
                outstanding : Option[Statement]): Unit = {
      val last = st.getStatements.foldLeft(outstanding)((acc, x) => {
        acc.foreach(o => {
          val crt = adjList.getOrElse(o, Nil)
          adjList.put(o, x :: crt)
        })
        Some(x)
      })
      if (!visited.contains(st)) {
        visited.add(st)
        val nxt = st.getStatements.last match {
          case rss : ReturnSelectStatement => nextStates(rss)
          case rs : ReturnStatement => nextState(rs)
        }
        nxt.foreach(x => doBuild(x, last))
      }
    }
    doBuild(switch.getParserState("start"), None)
    new org.change.utils.graph.Graph(adjList.toMap)
  }
  private lazy val mes : Map[Statement, Option[ExtractStatement]] = propagateExtract(switch.getParserState("start"))
  private lazy val fullCfg: graph.Graph[Statement] = buildCFG()
  private lazy val fullSCC = fullCfg.scc()
  private lazy val toComponent = fullSCC.flatMap(x => {
    x.map(h => h -> x) ++ x.collect {
      case rss : ReturnSelectStatement => rss.getCaseEntryList.map(h => {
        h.getReturnStatement -> x
      })
    }.flatten
  }).toMap

  def translateRv(expression: Expression, statement: Statement): (FloatingExpression, Option[Instruction]) =
    expression match {
      case ct: ConstantExpression => (ConstantValue(ct.getValue), None)
      case ce: CompoundExpression =>
        val lft = translateRv(ce.getLeft, statement)
        val rgt = translateRv(ce.getRight, statement)
        val instr = (lft._2, rgt._2) match {
          case (None, None) => None
          case (Some(i1), Some(i2)) => Some(InstructionBlock(i1, i2))
          case (None, Some(i1)) => Some(i1)
          case (Some(i1), None) => Some(i1)
        }
        (if (ce.isPlus)
          :+:(lft._1, rgt._1)
        else
          :-:(lft._1, rgt._1), instr)
      case expr: StringRef => ???
      case lr : LatestRef =>
        val fieldRef = new FieldRef().setField(lr.getFieldName).
          setHeaderRef(mes(statement).get.getExpression)
        translateRv(fieldRef, statement)
      case fr : FieldRef =>
        val hdr = fr.getHeaderRef
        val instance = switch.getInstance(hdr.getPath)
        if (hdr.isArray) {
          val ainstance = hdr.asInstanceOf[IndexedHeaderRef]
          val tmp = s"${FreshnessManager.next()}.${fr.getField}"
          (:@(tmp), Some(InstructionBlock(
            Allocate(tmp, instance.getLayout.getField(fr.getField).getLength),
            handleArray(ainstance, (x : Int) => {
              Assign(tmp, :@(s"${hdr.getPath}[$x].${fr.getField}"))
            })
          )))
        } else {
          (:@(s"${hdr.getPath}.${fr.getField}"), None)
        }
      case dr : DataRef =>
        if (!noInputPackets)
          (Take(Right(Tag("CRT")), dr.getStart.intValue(), dr.getEnd.intValue()), None)
        else {
          val t = s"${FreshnessManager.next()}"
          (:@(t), Some(InstructionBlock(
            Allocate(t, dr.getEnd.intValue() - dr.getStart.intValue() - 1),
            Assign(t, Havoc(t))
          )))
        }
      case _ => throw new NotImplementedError(expression.toString)
    }

  def handleStatement(statement: Statement,
                      visited : mutable.Set[String],
                      stack : mutable.ListBuffer[String]) : Instruction = {
    statement match {
      case ss : SetStatement =>
        val lv = ss.getLeft
        val rv = translateRv(ss.getRight, statement)
        if (lv.getHeaderRef.isArray) {
          val arr = handleArray(lv.getHeaderRef.asInstanceOf[IndexedHeaderRef], (x : Int) => {
            Assign(s"${lv.getHeaderRef.getPath}[$x].${lv.getField}", rv._1)
          })
          rv._2 match {
            case None => arr
            case Some(i) => InstructionBlock(i, arr)
          }
        } else {
          val h = Assign(s"${lv.getHeaderRef.getPath}.${lv.getField}", rv._1)
          rv._2 match {
            case None => h
            case Some(i) => InstructionBlock(i, h)
          }
        }
      case es : ExtractStatement =>
        val hi = es.getExpression
        val instance = switch.getInstance(hi.getPath)
        def extract(base : String) =
          If (Constrain(base + ".IsValid", :==:(ConstantValue(1))),
            Fail("double extract"),
            InstructionBlock(
              Assign(base + ".IsValid", ConstantValue(1)) ::
                instance.getLayout.getFields.flatMap(fld => {
                  val fname = s"$base.${fld.getName}"
                  List(
                    Allocate(fname, fld.getLength),
                    Assign(fname, if (noInputPackets) Havoc(fname) else :@(Tag("CRT"))),
                    if (noInputPackets) NoOp else CreateTag("CRT", Tag("CRT") + fld.getLength)
                  )
                }).toList
            )
          )
        if (hi.isArray) {
          val ihr = hi.asInstanceOf[IndexedHeaderRef]
          handleArray(ihr, (x : Int) => {
            val sref = new IndexedHeaderRef().setIndex(x).setPath(hi.getPath)
            val extractStatement = new ExtractStatement(sref)
            InstructionBlock(
              extract(instance.getName + s"[$x]"),
              if (ihr.isLast) Increment(instance.getName + ".next") else NoOp
            )
          })
        } else
          extract(instance.getName)
      case rss : ReturnSelectStatement =>
        val exprs = rss.getCaseEntryList.head.getExpressions
        val defaultEntry = rss.getCaseEntryList.find(_.isDefault).map(_.getReturnStatement).
          getOrElse(new ReturnStatement("").setError(true).setMessage("reject"))
        val mapped = exprs.map(translateRv(_, statement))
        val instrs = InstructionBlock(mapped.flatMap(_._2).toList)
        InstructionBlock(
          instrs,
          rss.getCaseEntryList.filter(!_.isDefault).foldRight(
            handleStatement(defaultEntry, visited, stack))((x, acc) => {
            val cd = InstructionBlock(mapped.zip(x.getValues.toList).map(u => {
              if (u._2.getMask != -1)
                ConstrainFloatingExpression(:&&:(ConstantValue(u._2.getMask), u._1._1),
                  :==:(translateValue(u._2)))
              else
                ConstrainFloatingExpression(u._1._1, :==:(translateValue(u._2)))
            }))
            If (cd, handleStatement(x.getReturnStatement, visited, stack), acc)
          })
        )
      case rs : ReturnStatement => if (rs.isError) {
        Fail(rs.getMessage)
      } else {
        val dst = switch.getParserState(rs.getWhere)
        if (dst != null) {
          val srcComponent = toComponent(statement)
          if (!visited.contains(rs.getWhere))
            stack.append(rs.getWhere)
          if (srcComponent.size > 1) {
            // this may wreak havoc
            val dstComponent = toComponent(dst.getStatements.head)
            if (!srcComponent.eq(dstComponent)) {
              // this is an escape transition
              val escapeCode = Assign("escape", ConstantStringValue(rs.getWhere))
              InstructionBlock(
                escapeCode,
                Forward(name + s".parser.${srcComponent.hashCode()}.escape")
              )
            } else {
              // this is an internal transition
              Forward(name + ".parser." + rs.getWhere)
            }
          } else {
            Forward(name + ".parser." + rs.getWhere)
          }
        } else {
          if (!justParser)
            Forward(name + ".control." + rs.getWhere)
          else Forward("parser_success")
        }
      }
    }
  }

  def handleState(state : org.change.v2.p4.model.parser.State,
                  visited : mutable.Set[String],
                  stack : mutable.ListBuffer[String]) :
    Instruction = {
    InstructionBlock(
      state.getStatements.map(handleStatement(_, visited, stack))
    )
  }

  override def parserCode(): Instruction = {
    val instances = switch.getInstances.toList
    val first = Allocate("escape", 64) :: instances.flatMap(x => x match {
      case ai: ArrayInstance => Assign(ai.getName + ".next", ConstantValue(0)) :: Nil
      case _ => Nil
    })
    InstructionBlock(
      first ++ List(CreateTag("CRT", Tag("START")), Forward(name + ".parser." + "start"))
    )
  }

  override def generatorCode(): Instruction = Forward(generatorStartPoints().head)

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

  def maxUnroll(graph : org.change.utils.graph.Graph[Statement],
                 scc : List[Statement]): Int = {
    val extracts = scc.map(_.asInstanceOf[ExtractStatement])
    val nrmax = extracts.map(h => {
      if (h.getExpression.isArray)
        switch.getInstance(h.getExpression.getPath).asInstanceOf[ArrayInstance].getLength
      else 1
    }).sum
    val root = scc.head
    val mindelta = graph.sp(root, root)
    Math.ceil((nrmax + 0.0) / (mindelta + 0.0)).intValue()
  }
  def deparse(base : String, fld : Field) : List[Instruction] =
    List(
      Allocate(Tag("CRT"), fld.getLength),
      Assign(Tag("CRT"), :@(s"$base.${fld.getName}")),
      CreateTag("CRT", Tag("CRT") + fld.getLength)
    )

  override def deparserCode(): Instruction = {
    val topo: List[Instruction] = deparserInstructions
    val first = CreateTag("CRT", Tag("START")) :: switch.getInstances.flatMap {
      case ai: ArrayInstance if !ai.isMetadata =>
        Assign(ai.getName + ".next", ConstantValue(0)) :: Nil
      case _ => Nil
    }.toList
    InstructionBlock(first ++ topo ++ List(Forward(s"${switchInstance.getName}.deparser.out")))
  }

  private def deparserInstructions : List[Instruction] = {
    val cfg = buildCFG().subGraph(x => x.isInstanceOf[ExtractStatement])
    if (true) {
      System.out.println("digraph G {")
      fullSCC.reverse.foreach(x => {
        System.out.println("subgraph { ")
        x.foreach(h => {
          System.out.println(h.hashCode() + " [label=\"" + h + "\"];")
        })
        System.out.println("}")
      })
      fullCfg.edges.foreach(e => {
        e._2.foreach(dst => {
          System.out.println(e._1.hashCode() + " -> " + dst.hashCode() + ";")
        })
      })
      System.out.println("}")
      System.out.flush()
    }

    val sccs = cfg.scc()
    val topo = sccs.reverse.map(scc => {
      val nrunrolls = maxUnroll(cfg, scc)
      val i = scc.map(_.asInstanceOf[ExtractStatement].getExpression).map(f => {
        val inst = switch.getInstance(f.getPath)
        if (f.isArray) {
          val b = s"${f.getPath}"
          handleArray(f.asInstanceOf[IndexedHeaderRef], (x: Int) => {
            val base = b + s"[$x]"
            If(Constrain(s"$base.IsValid", :==:(ConstantValue(1))),
              InstructionBlock(inst.getLayout.getFields.flatMap(fld => {
                deparse(b + s"[$x]", fld)
              }))
            )
          })
        } else {
          If(Constrain(s"${f.getPath}.IsValid", :==:(ConstantValue(1))),
            InstructionBlock(inst.getLayout.getFields.flatMap(fld => {
              deparse(f.getPath, fld)
            }))
          )
        }
      })
      InstructionBlock((0 until nrunrolls).flatMap(_ => i))
    })
    topo
  }

  def generatorName(es : ExtractStatement): String = {
    val path = es.getExpression.getPath
    switchInstance.getName + ".generator." + path + (if (es.getExpression.isArray) {
      val ihr = es.getExpression.asInstanceOf[IndexedHeaderRef]
      if (ihr.isLast) {
        "[next]"
      } else {
        s"[${ihr.getIndex}]"
      }
    } else "")
  }

  lazy val extraGeneratorCode : Map[String, Instruction] = {
    val init = List(
      CreateTag("START", 0),
      CreateTag("CRT", Tag("START"))
    ) ++ switch.getInstances.flatMap {
      case ai: ArrayInstance if !ai.isMetadata =>
        Assign(ai.getName + ".next", ConstantValue(0)) :: Nil
      case _ => Nil
    }.toList
    val cfg = buildCFG().subGraph(x => x.isInstanceOf[ExtractStatement])
    val scc = cfg.scc()
    cfg.edges.map(x => {
      val hdr = x._1.asInstanceOf[ExtractStatement].getExpression
      val instance = switch.getInstance(hdr.getPath)
      val b = hdr.getPath
      val myInstruction = if (hdr.isArray) {
        handleArray(hdr.asInstanceOf[IndexedHeaderRef], (x : Int) => {
          InstructionBlock(instance.getLayout.getFields.flatMap(fld => {
            val base = s"$b[$x]"
            List(
              Allocate(Tag("CRT"), fld.getLength),
              Assign(Tag("CRT"), Havoc(s"$base.${fld.getName}")),
              CreateTag("CRT", Tag("CRT") + fld.getLength)
            )
          }))
        })
      } else {
        InstructionBlock(instance.getLayout.getFields.flatMap(fld => {
          val base = s"$b"
          List(
            Allocate(Tag("CRT"), fld.getLength),
            Assign(Tag("CRT"), Havoc(s"$base.${fld.getName}")),
            CreateTag("CRT", Tag("CRT") + fld.getLength)
          )
        }))
      }
      generatorName(x._1.asInstanceOf[ExtractStatement]) -> InstructionBlock(
        myInstruction,
        Fork(x._2.map(_.asInstanceOf[ExtractStatement]).map(x => Forward(generatorName(x))))
      )
    }) ++ Map(
      switchInstance.getName + ".generator" ->
        Forward(generatorName(scc.last.head.asInstanceOf[ExtractStatement]))
    )
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
    fullSCC.foreach(srcComponent => {
      if (srcComponent.size > 1) {
        val escName = name + s".parser.${srcComponent.hashCode()}.escape"
        val alternatives = srcComponent.foldLeft(Set.empty[String])((acc, stat) => stat match {
          case rss: ReturnSelectStatement => acc ++ nextStates(rss).map(_.getName).toSet
          case rs: ReturnStatement => acc ++ nextState(rs).map(_.getName).toSet
          case _ => acc
        }).filter(alternative => {
          val dst = switch.getParserState(alternative)
          val dstcomponent = toComponent(dst.getStatements.head)
          !dstcomponent.eq(srcComponent)
        })
        if (alternatives.nonEmpty) {
          instrs += (escName -> (if (alternatives.size > 1)
            alternatives.foldRight(Fail("UNREACHABLE"): Instruction)((alternative, acc) => {
              If(Constrain("escape", :==:(ConstantStringValue(alternative))),
                Forward(name + s".parser.$alternative"),
                acc
              )
            }) else Forward(name + s".parser.${alternatives.head}")))
        }
      }
    })

    instrs.toMap ++ extraGeneratorCode
  }

  override def generatorStartPoints(): Set[String] = Set(s"${switchInstance.getName}.generator")
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
