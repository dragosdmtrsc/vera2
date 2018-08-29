package org.change.v2.abstractnet.optimized.router

import org.change.utils
import org.change.v2.abstractnet.click.selfbuildingblocks.EtherMumboJumbo
import org.change.v2.abstractnet.generic._
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames._
import java.io.File
import scala.io.Source
import scala.util.matching.Regex
import org.change.v2.util.regexes._
import org.change.v2.util.conversion.RepresentationConversion

/**
 * A small gift from radu to symnetic.
 */
class OptimizedRouter(name: String,
                      elementType: String,
                      inputPorts: List[Port],
                      outputPorts: List[Port],
                      configParams: List[ConfigParameter])
  extends GenericElement(name,
    elementType,
    inputPorts,
    outputPorts,
    configParams) {

  override def instructions: Map[LocationId, Instruction] = Map(
    inputPortName(0) -> Fail("Unexpected packet dropped @ " + getName)
  )

  override def outputPortName(which: Int = 0): String = s"$name-$which-out"
}

class OptimizedRouterElementBuilder(name: String, elementType: String)
  extends ElementBuilder(name, elementType) {

  override def buildElement: GenericElement = {
    new OptimizedRouter(name, elementType, getInputPorts, getOutputPorts, getConfigParameters)
  }
}

object OptimizedRouter {
  private var unnamedCount = 0

  private val genericElementName = "Router"

  private def increment {
    unnamedCount += 1
  }

  def getBuilder(name: String): OptimizedRouterElementBuilder = {
    increment;
    new OptimizedRouterElementBuilder(name, "Router")
  }

  def getBuilder: OptimizedRouterElementBuilder =
    getBuilder(s"$genericElementName-$unnamedCount")

  def getRoutingEntries(file: File): Seq[((Long, Long), String)] = {
    (for {
      line <- scala.io.Source.fromFile(file).getLines()
      tokens = line.split("\\s+")
      if tokens.length >= 3
      if tokens(0) != ""
      hopType = tokens(1)
      if hopType != "receive" && hopType != "connected"
      matchPattern = tokens(0)
      forwardingPort = tokens(2)
    } yield (
        matchPattern match {
          case ipv4netmaskRegex() => {
            val netMaskTokens = matchPattern.split("/")
            val netAddr = netMaskTokens(0)
            val mask = netMaskTokens(1)
            val (l, u) = RepresentationConversion.ipAndMaskToInterval(netAddr, mask)
            (l,u)
          }
        },
        forwardingPort
        )).toSeq.sortBy(i => i._1._2 - i._1._1)
  }

  def getTrivialRoutingEntries(file: File): Seq[((Long, Long), String)] = {
    (for {
      line <- scala.io.Source.fromFile(file).getLines()
      tokens = line.split("\\s+")
      matchPattern = tokens(0)
      forwardingPort = tokens(1)
    } yield (
      matchPattern match {
        case ipv4netmaskRegex() => {
          val netMaskTokens = matchPattern.split("/")
          val netAddr = netMaskTokens(0)
          val mask = netMaskTokens(1)
          val (l, u) = RepresentationConversion.ipAndMaskToInterval(netAddr, mask)
          (l,u)
        }
      },
      forwardingPort
    )).toSeq.sortBy(i => i._1._2 - i._1._1)
  }

  def getDstMacConstraint(macs: String): Instruction = {
    ConstrainRaw(EtherDst,NOT(OR((for (m <-Source.fromFile(macs).getLines()) yield {
      EQ_E(ConstantValue(RepresentationConversion.macToNumberCiscoFormat(m)))
    }).toList)))
  }

  def makeRouter(f: File): OptimizedRouter = {
    val table = getRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".rt")

    val macsFile = f.getParent + File.separator + name + ".macs"
    val dstMacConstrain = getDstMacConstraint(macsFile)

    new OptimizedRouter(name + "-" + name,"Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(inputPortName("port") ->
        Fork(table.map(i => {
          val ((l,u), port) = i
          (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++
            {
              val conflicts = table.takeWhile(i =>  u-l > i._1._2 - i._1._1) filter (other => {
                val ((otherL, otherU), otherPort) = other
                port != otherPort &&
                  l <= otherL &&
                  u >= otherU
              })

              if (conflicts.nonEmpty)
                Seq(NOT(OR(conflicts.map( conflictual => {
                  AND(List(GTE_E(ConstantValue(conflictual._1._1)), LTE_E(ConstantValue(conflictual._1._2))))
                }).toList)))
              else Nil
            }))
        }).groupBy(_._1).map( kv =>
          InstructionBlock(
            ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)),
            EtherMumboJumbo.stripAllEther,
            if (kv._1.startsWith("Vlan")) {
              val vlan = kv._1.stripPrefix("Vlan").toInt
              InstructionBlock(
                EtherMumboJumbo.constantVlanEncap(vlan),
                if (vlan == 290)
                  Constrain(EtherSrc, :==:(ConstantValue(RepresentationConversion.macToNumberCiscoFormat("0019.e72a.77ff"))))
                else
////                if (vlan == 290)
////                  Constrain(EtherDst, :==:(ConstantValue(RepresentationConversion.macToNumberCiscoFormat("0018.742f.bd80"))))
////               else
                  NoOp
               )
            } else {
              EtherMumboJumbo.symbolicEtherEncap
            },
            dstMacConstrain,
            Forward(outputPortName(kv._1)))
          )))
    }
  }


  def makeOptimizedRouter_Costin(f: File, prefix: String): OptimizedRouter = {
    val table = getRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".rt")

    new OptimizedRouter(prefix,"Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(s"${prefix}0" ->
        Fork(table.map(i => {
          val ((l,u), port) = i
          (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++
            {
              val conflicts = table.takeWhile(i =>  u-l > i._1._2 - i._1._1)filter( other => {
                val ((otherL, otherU), otherPort) = other
                port != otherPort &&
                  l <= otherL &&
                  u >= otherU
              })

              if (conflicts.nonEmpty)
                Seq(NOT(OR((conflicts.map( conflictual => {
                  AND(List(GTE_E(ConstantValue(conflictual._1._1)), LTE_E(ConstantValue(conflictual._1._2))))
                }).toList))))
              else Nil
            }))
        }).groupBy(_._1).map( kv =>
          InstructionBlock(
            Constrain(IPDst, OR(kv._2.map(_._2).toList)),
            Forward(prefix+kv._1))
        ))) ++
        table.map(i => prefix+i._2 -> Forward(prefix+i._2+"_EXIT").asInstanceOf[Instruction]).toMap ++
        table.map(i => prefix+i._2+"_EXIT" -> NoOp.asInstanceOf[Instruction]).toMap
    }
  }

  def makeTrivialRouter(f: File): OptimizedRouter = {
    val table = getTrivialRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".router")

    val macsFile = f.getParent + File.separator + name + ".macs"
    val dstMacConstrain = getDstMacConstraint(macsFile)

    new OptimizedRouter(name + "-" + name,"Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(inputPortName("in") ->
        Fork(table.map(i => {
          val ((l,u), port) = i
          (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++
            {
              val conflicts = table.takeWhile(i =>  u-l > i._1._2 - i._1._1)filter( other => {
                val ((otherL, otherU), otherPort) = other
                port != otherPort &&
                  l <= otherL &&
                  u >= otherU
              })

              if (conflicts.nonEmpty)
                Seq(NOT(OR((conflicts.map( conflictual => {
                  AND(List(GTE_E(ConstantValue(conflictual._1._1)), LTE_E(ConstantValue(conflictual._1._2))))
                }).toList))))
              else
                Nil
            }))
        }).groupBy(_._1).map( kv =>
          InstructionBlock(
            ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)),
            EtherMumboJumbo.stripAllEther,
            EtherMumboJumbo.symbolicEtherEncap,
            dstMacConstrain,
            Forward(outputPortName(kv._1)))
        )))
    }
  }

  def makeNaiveRouter(f: File): OptimizedRouter = {
    val table = getRoutingEntries(f)

    var conflictCount = 0L
    var which = 0

    val i = table.map(i => {
      val ((l,u), port) = i
      (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++
        {
          val conflicts = table.takeWhile(i =>  u-l > i._1._2 - i._1._1)filter( other => {
            val ((otherL, otherU), otherPort) = other
            port != otherPort &&
              l <= otherL &&
              u >= otherU
          })

          conflictCount += conflicts.size
          which += 1

          if (which % 100 == 0) println(which)

          if (conflicts.nonEmpty)
            Seq(NOT(OR((conflicts.map( conflictual => {
              AND(List(GTE_E(ConstantValue(conflictual._1._1)), LTE_E(ConstantValue(conflictual._1._2))))
            }).toList))))
          else Nil
        }))
    }).groupBy(_._1).foldRight(Fail("No route"): Instruction)( (kv, a) =>
      If(ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)), Forward(kv._1), a)
    )

    println("Routing table size " + table.length)
    println("Conflict count " + conflictCount)

    new OptimizedRouter("NAIVE","Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = {
        table.map(i => i._2 -> Forward(i._2+"_EXIT").asInstanceOf[Instruction]).toMap ++
        table.map(i => i._2+"_EXIT" -> NoOp.asInstanceOf[Instruction]).toMap +
        ("0" -> i)
      }
    }
  }

  def optimizedRouterNetworkConfig(f: File): NetworkConfig = {
    val elem = makeRouter(f)

    NetworkConfig(Some(f.getName.trim.stripSuffix(".rt")), Map((elem.getName) -> elem), Nil)
  }

  def trivialRouterNetwrokConfig(f: File): NetworkConfig = {
    val elem = makeTrivialRouter(f)

    NetworkConfig(Some(f.getName.trim.stripSuffix(".router")), Map((elem.getName) -> elem), Nil)
  }
}
