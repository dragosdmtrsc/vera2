package org.change.plugins.vera

import java.io.PrintStream

import org.change.parser.p4.ControlFlowInterpreter
import org.change.parser.p4.control._
import org.change.parser.p4.control.queryimpl.{MemoryInitializer, P4RootMemory}
import org.change.parser.p4.parser.{LightParserGenerator, ParserGenerator}
import org.change.parser.p4.tables.SymbolicSwitchInstance
import org.change.plugins.eq.{InputPacketPlugin, NoPacketPlugin, PluginBuilder, TopologyPlugin}
import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control.ControlStatement
import org.change.v2.p4.model.control.exp.P4BExpr
import org.change.v2.p4.model.parser.ReturnStatement
import org.change.v3.semantics.SimpleMemory
import z3.scala.{Z3AST, Z3Context}
import org.change.v3.semantics._

import scala.collection.mutable
import scala.util.matching.Regex

class VeraTopologyPlugin(switchName : String,
                         sw : Switch,
                         switchInstance: SymbolicSwitchInstance,
                         ifaces : Map[Int, String],
                         cloneSpec : Map[Int, Int],
                         parserGenerator : ParserGenerator,
                         generator : Boolean,
                         noParser : Boolean
                        ) extends TopologyPlugin {
  private lazy val controlFlowInterpreter = ControlFlowInterpreter.buildSymbolicInterpreter(
    switchInstance,
    sw,
    Some(parserGenerator),
    noParser)
  private lazy val instructions = CodeAwareInstructionExecutor.flattenProgram(controlFlowInterpreter.instructions(),
    controlFlowInterpreter.links())
  override def startNodes() : Set[String] =
    if (!generator)
      controlFlowInterpreter.startingPoints()
    else
      parserGenerator.generatorStartPoints()
  override def apply(): collection.Map[String, Instruction] = instructions

  private lazy val inputPacketPlugin = new VeraInputPacketPlugin(sw, switchInstance)
  override def defaultInputPlugin(): InputPacketPlugin = {
    if (noParser) inputPacketPlugin
    else new NoPacketPlugin
  }
}

object VeraTopologyPlugin {
  val pat: Regex = "interface(\\d+)".r
  val clonespec: Regex = "clonespec(\\d+)".r
  class Builder extends PluginBuilder[VeraTopologyPlugin] {
    var commandsTxt = ""
    var p4file = ""
    val ifaces = scala.collection.mutable.Map.empty[Int, String]
    val cloneSpec = scala.collection.mutable.Map.empty[Int, Int]
    var switchName = "switch"
    var ninterfaces = 0
    var generator = false
    var noInputPacket = false
    var justParser = false
    var nodeparser = false
    var noparser = true
    override def set(parm: String, value: String): PluginBuilder[VeraTopologyPlugin] = {
      parm match {
        case "justparser" => justParser = value.toBoolean; this
        case "noparser" => noparser = value.toBoolean; this
        case "generator" => generator = value.toBoolean; this
        case "noinputpacket" => noInputPacket = value.toBoolean; this
        case "commands" => commandsTxt = value; this
        case "p4" => p4file = value; this
        case "name" => switchName = value; this
        case "ninterfaces" => ninterfaces = value.toInt; this
        case "nodeparser" => nodeparser = value.toBoolean; this
        case pat(nr) => ifaces.put(nr.toInt, value); this
        case clonespec(cs) => cloneSpec.put(cs.toInt, value.toInt); this
      }
    }
    override def build(): VeraTopologyPlugin = {
      val start = System.currentTimeMillis()
      val sw = SolveTables(Switch.fromFile(p4file))
      if (ifaces.isEmpty) {
        if (ninterfaces == 0)
          throw new IllegalArgumentException("specify parms: interfaceX NAME or ninterfaces NR")
        (0 until ninterfaces).foreach(h => {
          ifaces.put(h, s"eth$h")
        })
      }
      val solver = context.mkSolver()
      val astToFail = mutable.Map.empty[Z3AST, (ControlStatement, SimpleMemory)]
      val SEFLSemantics = new QueryDrivenSemantics[P4RootMemory](sw) {
        override def finishNode(src: ControlStatement, region: Option[P4RootMemory]): Unit = {
          region.filter(r => {
            !r.rootMemory.isEmpty()
          }).foreach(r => {
            System.err.println(s"possible error in $src")
          })
        }

        override def mkQuery(src: ControlStatement,
                             rho: Option[P4BExpr],
                             dst: ControlStatement)(implicit ctx: P4Memory): P4RootMemory = {
          val out = super.mkQuery(src, rho, dst)(ctx)
          out
        }
      }

      val ph = new ParserHelper(sw)
      val ps = new PrintStream("bla.dot")
      ps.println("digraph G {")
      ph.mkUnrolledLabeledGraph.graphView.toDot(ps)
      ps.println("}")
      ps.close()

      val psi = new PrintStream("ingress.dot")
      psi.println("digraph G {")
      SEFLSemantics.getCFG("ingress").graphView.toDot(psi)
      psi.println("}")
      psi.close()

      val first = SEFLSemantics.getFirst("parser")
      val execd = SEFLSemantics.execute("parser")(Map(first -> MemoryInitializer.initialize(sw)(context)))
      println(execd.head._1)
      val firstIngress = SEFLSemantics.getFirst("ingress")
      val exegress = SEFLSemantics.execute("ingress")(Map(firstIngress -> execd.head._2))
//      val postingress = exegress.head._2
//      val firstEgress = SEFLSemantics.getFirst("egress")
//      val all = SEFLSemantics.execute("egress")(Map(firstEgress -> exegress.head._2))
      val end = System.currentTimeMillis()
      System.err.println(s"propagation took ${end - start}ms")
      System.exit(0)

      val switchInstance = SymbolicSwitchInstance.fromFileWithSyms(switchName,
        ifaces.toMap, cloneSpec.toMap,
        sw, commandsTxt, false)
      val parserGenerator = new LightParserGenerator(sw,
        switchInstance,
        noInputPacket,
        justParser,
        nodeparser)
      new VeraTopologyPlugin(switchName,
        sw,
        switchInstance,
        ifaces.toMap,
        cloneSpec.toMap,
        parserGenerator,
        generator,
        noparser)
    }
  }
}
