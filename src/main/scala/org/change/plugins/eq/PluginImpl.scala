package org.change.plugins.eq
import java.io.{BufferedOutputStream, File, FileOutputStream}
import java.nio.file.{Path, Paths}

import org.change.utils.prettifier.JsonUtil
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.equivalence.MagicTuple
import org.change.v2.analysis.executor.OVSExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.translators.Z3BVTranslator
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.State._
import org.change.v2.analysis.memory.{SimpleMemory, State, ToTheEndExecutor}
import org.change.v2.analysis.processingmodels.instructions.{Assign, CreateTag, InstructionBlock}
import org.change.v2.util.canonicalnames._
import z3.scala.Z3Context
import org.change.v2.analysis.memory.TagExp._

import scala.collection.mutable

class PluginImpl(
                  val fwa : String,
                  val fpm : String,
                  val fom : String) extends OutputPlugin {

  override def apply(wrongArity: Iterable[MagicTuple],
                     portMismatch: Iterable[MagicTuple],
                     outputMismatch: Iterable[MagicTuple]): Unit = {
    val wa = new BufferedOutputStream(new FileOutputStream(fwa))
    JsonUtil.toJson(wrongArity, wa)
    val pm = new BufferedOutputStream(new FileOutputStream(fpm))
    JsonUtil.toJson(portMismatch, pm)
    val om = new BufferedOutputStream(new FileOutputStream(fom))
    JsonUtil.toJson(outputMismatch, om)
    if (wrongArity.isEmpty && portMismatch.isEmpty && outputMismatch.isEmpty) {
      println("result: equivalent")
    } else {
      println("result: non-equivalent")
    }
    if (wrongArity.nonEmpty) {
      for (x <- wrongArity) {
        val h1 = "[" + x._2._1.map(x => {
          x.errorCause.map(f => s"$f@").getOrElse("") + x.history.head
        }).mkString(";") + "]"
        val h2 = "[" + x._2._2.map(x => {
          x.errorCause.map(f => s"$f@").getOrElse("") + x.history.head
        }).mkString(";") + "]"
        println("wrong arity: " + h1 + " vs " + h2)
      }
    }

    if (outputMismatch.nonEmpty) {
      println("output mismatch: " + outputMismatch.size)
    }
    if (portMismatch.nonEmpty) {
      for (x <- portMismatch) {
        println("port mismatch: " + "(" + x._2._1.map(_.history.head).mkString(",") + ")" + " vs " +
          "(" + x._2._2.map(_.history.head).mkString(",") + ")")
      }
    }
  }
}
object PluginImpl {
  class Builder extends PluginBuilder[PluginImpl] {
    var dir = "."
    var prefix = "out"
    override def set(parm: String, value: String): PluginBuilder[PluginImpl] = {
      if (parm != "dir" && parm != "prefix")
        throw new IllegalArgumentException(parm + " is illegal, expected dir or prefix")
      parm match {
        case "dir" => dir = value
        case "prefix" => prefix = value
      }
      this
    }
    override def build(): PluginImpl = {
      new PluginImpl(
        Paths.get(dir, prefix + "." + "arity.json").toString,
        Paths.get(dir, prefix + "." + "portmismatch.json").toString,
        Paths.get(dir, prefix + "." + "output.json").toString
      )
    }
  }
}

class NoPacketPlugin extends InputPacketPlugin {
  override def apply(): List[SimpleMemory] = CreateTag("START", 0)(State.clean)._1.map(SimpleMemory.apply)
}
object NoPacketPlugin {
  class Builder extends PluginBuilder[NoPacketPlugin] {
    override def set(parm: String, value: String): PluginBuilder[NoPacketPlugin] = this
    override def build(): NoPacketPlugin = new NoPacketPlugin()
  }
}

class TCPPacketPluginImpl(symTtl : Boolean) extends InputPacketPlugin {
  override def apply(): List[SimpleMemory] = new OVSExecutor(new Z3BVSolver()).execute(
    InstructionBlock(
      Assign("Pid", ConstantValue(0)),
      start,
      ehervlan,
      ipSymb(symTtl),
      transport,
      end,
      tcpOptions), State.clean, true
  )._1.map(SimpleMemory.apply)
}
object TCPPacketPluginImpl {
  class Builder extends PluginBuilder[TCPPacketPluginImpl] {
    var symTtl : Boolean = false
    override def set(parm: String, value: String): PluginBuilder[TCPPacketPluginImpl] = parm match {
      case "symttl" => symTtl = value.toBoolean; this
      case _ => throw new IllegalArgumentException(s"$parm unknown, only know symttl")
    }
    override def build(): TCPPacketPluginImpl = new TCPPacketPluginImpl(symTtl)
  }
}

class DefaultOutputEquivalence extends PacketPlugin {
  override def apply(s1: SimpleMemory, s2: SimpleMemory): Boolean = {
    val ctx = new Z3Context()
    val slv = ctx.mkSolver()
    val start = System.currentTimeMillis()
    val trans = new memory.SimpleMemory.Translator(slv.context, slv)
    s2.pathCondition.cd match {
      case FAND(cds) =>
        cds.foreach(r => slv.assertCnstr(trans.createAST(r)))
      case _ => slv.assertCnstr(trans.createAST(s2.pathCondition.cd))
    }

    val layoutEquiv = s1.memTags == s2.memTags && s1.rawObjects.keySet == s2.rawObjects.keySet
    val eq = if (layoutEquiv) {
      val mustBeEqual = List(
        IPDst, IPSrc, Proto, EtherDst, EtherSrc, EtherType, TTL
      )
      val or = FOR.makeFOR(mustBeEqual.map(r => {
        s1.eval(r).map(h => {
          FNOT.makeFNOT(OP(s1.rawObjects(h).expression, EQ_E(s2.rawObjects(h).expression), s1.rawObjects(h).size))
        }).getOrElse(FALSE)
      }))
      if (or == FALSE) {
        // sounds stupid, but it means that no expression was correctly evaluated
        true
      } else {
        slv.assertCnstr(trans.createAST(or))
        !slv.check().get
      }
    } else {
      false
    }
    val end = System.currentTimeMillis()
    eq
  }
}
object DefaultOutputEquivalence {
  class Builder extends PluginBuilder[DefaultOutputEquivalence] {
    override def set(parm: String, value: String): PluginBuilder[DefaultOutputEquivalence] = this
    override def build(): DefaultOutputEquivalence = new DefaultOutputEquivalence()
  }
}

class SievePluginImpl(nosieve : Boolean) extends SievePlugin {
  override def apply(outcomes: List[SimpleMemory]): Iterable[(Condition, Iterable[SimpleMemory])] = {
    if (!nosieve) ToTheEndExecutor.sieve(outcomes)
    else ToTheEndExecutor.noSieve(outcomes)
  }
}
object SievePluginImpl {
  class Builder extends PluginBuilder[SievePluginImpl] {
    var nosieve : Boolean = false
    override def set(parm: String, value: String): PluginBuilder[SievePluginImpl] = {
      parm match {
        case "nosieve" => nosieve = value.toBoolean
          this
        case _ => throw new IllegalArgumentException("only nosieve accepted")
      }
    }
    override def build(): SievePluginImpl = new SievePluginImpl(nosieve)
  }
}

abstract class AbsIntegrationPlugin extends IntegrationPlugin {
  val outputMappingsParms: mutable.Map[String, String] = collection.mutable.Map[String, String]()
  val inputMappingsParms: mutable.Map[String, String] = collection.mutable.Map[String, String]()
  val topologyRightParms: mutable.Map[String, String] = collection.mutable.Map[String, String]()
  val topologyLeftParms: mutable.Map[String, String] = collection.mutable.Map[String, String]()
  override def setOutputMappingParms(p: collection.Map[String, String]): IntegrationPlugin = {
    outputMappingsParms ++= p
    this
  }

  override def setInputMappingsParms(p: collection.Map[String, String]): IntegrationPlugin = {
    inputMappingsParms ++= p
    this
  }

  override def setTopologyRightParms(p: collection.Map[String, String]): IntegrationPlugin = {
    topologyRightParms ++= p
    this
  }

  override def setTopologyLeftParms(p: collection.Map[String, String]): IntegrationPlugin = {
    topologyLeftParms ++= p
    this
  }
}

class PortMapper(dict : scala.collection.Map[String, String]) extends InputPortPlugin with OutputPortPlugin {
  override def apply(src: String): String = dict(src)

  override def apply(src: String, dst: String): Boolean = dict.contains(src) && dict(src) == dst

  override def all(): Iterable[(String, String)] = dict
}
object PortMapper {
  class Builder extends PluginBuilder[PortMapper] {
    private val dict = mutable.Map[String, String]()
    override def set(parm: String, value: String): PluginBuilder[PortMapper] = {
      dict.put(parm, value)
      this
    }
    override def build(): PortMapper = new PortMapper(dict)
  }
}