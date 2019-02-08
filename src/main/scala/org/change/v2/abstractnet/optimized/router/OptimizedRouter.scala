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

import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :-:, :@}
import org.change.v2.util.Neg

import scala.io.Source
import scala.util.matching.Regex
import org.change.v2.util.regexes._
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.util.conversion.RepresentationConversion.ipToNumber

import scala.collection.mutable

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
    new OptimizedRouter(name,
                        elementType,
                        getInputPorts,
                        getOutputPorts,
                        getConfigParameters)
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
    } yield
      (
        matchPattern match {
          case ipv4netmaskRegex() => {
            val netMaskTokens = matchPattern.split("/")
            val netAddr = netMaskTokens(0)
            val mask = netMaskTokens(1)
            val (l, u) =
              RepresentationConversion.ipAndMaskToInterval(netAddr, mask)
            (l, u)
          }
        },
        forwardingPort
      )).toSeq.sortBy(i => i._1._2 - i._1._1)
  }

  def getRoutingEntriesForBV(file: File, take : Int = -1): Iterable[((Long, Long), String)] = {
    val it = if (take > 0) {
      scala.io.Source.fromFile(file).getLines().take(take)
    } else {
      scala.io.Source.fromFile(file).getLines()
    }
    (for {
      line <- it
      tokens = line.split("\\s+")
      if tokens.length >= 3
      if tokens(0) != ""
      hopType = tokens(1)
      if hopType != "receive" && hopType != "connected"
      matchPattern = tokens(0)
      forwardingPort = tokens(2)
    } yield
      (
        matchPattern match {
          case ipv4netmaskRegex() => {
            val netMaskTokens = matchPattern.split("/")
            val netAddr = netMaskTokens(0)
            val mask = netMaskTokens(1)
            val binary = ("1" * mask.toInt) + "0" * (32 - mask.toInt)
            val m = java.lang.Long.parseLong(binary, 2)
            val ip = ipToNumber(netAddr)
            (ip, m)
          }
        },
        forwardingPort
      )).toList.sortBy(i => -i._1._2)
  }

  def getTrivialRoutingEntries(file: File): Seq[((Long, Long), String)] = {
    (for {
      line <- scala.io.Source.fromFile(file).getLines()
      tokens = line.split("\\s+")
      matchPattern = tokens(0)
      forwardingPort = tokens(1)
    } yield
      (
        matchPattern match {
          case ipv4netmaskRegex() => {
            val netMaskTokens = matchPattern.split("/")
            val netAddr = netMaskTokens(0)
            val mask = netMaskTokens(1)
            val (l, u) =
              RepresentationConversion.ipAndMaskToInterval(netAddr, mask)
            (l, u)
          }
        },
        forwardingPort
      )).toSeq.sortBy(i => i._1._2 - i._1._1)
  }

  def getDstMacConstraint(macs: String): Instruction = {
    ConstrainRaw(
      EtherDst,
      NOT(OR((for (m <- Source.fromFile(macs).getLines()) yield {
        EQ_E(ConstantValue(RepresentationConversion.macToNumberCiscoFormat(m)))
      }).toList)))
  }

  def makeRouter(f: File): OptimizedRouter = {
    val table = getRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".rt")

    val macsFile = f.getParent + File.separator + name + ".macs"
    val dstMacConstrain = getDstMacConstraint(macsFile)

    new OptimizedRouter(name + "-" + name, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] =
        Map(
          inputPortName("port") ->
            Fork(
              table
                .map(i => {
                  val ((l, u), port) = i
                  (port,
                   AND(
                     List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++ {
                       val conflicts = table.takeWhile(i =>
                         u - l > i._1._2 - i._1._1) filter (other => {
                         val ((otherL, otherU), otherPort) = other
                         port != otherPort &&
                         l <= otherL &&
                         u >= otherU
                       })

                       if (conflicts.nonEmpty)
                         Seq(NOT(OR(conflicts
                           .map(conflictual => {
                             AND(List(GTE_E(ConstantValue(conflictual._1._1)),
                                      LTE_E(ConstantValue(conflictual._1._2))))
                           })
                           .toList)))
                       else Nil
                     }))
                })
                .groupBy(_._1)
                .map(kv =>
                  InstructionBlock(
                    ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)),
                    EtherMumboJumbo.stripAllEther,
                    if (kv._1.startsWith("Vlan")) {
                      val vlan = kv._1.stripPrefix("Vlan").toInt
                      InstructionBlock(
                        EtherMumboJumbo.constantVlanEncap(vlan),
                        if (vlan == 290)
                          Constrain(
                            EtherSrc,
                            :==:(ConstantValue(RepresentationConversion
                              .macToNumberCiscoFormat("0019.e72a.77ff"))))
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
                    Forward(outputPortName(kv._1))
                ))))
    }
  }

  def generateConditionsPerPort(table : Iterable[((Long, Long), String)]) = {
    val portEntries = mutable.Map.empty[String, List[Instruction]]
    val ipList = mutable.Buffer.empty[(Long, Long)]
    for (((ip, mask), port) <- table) {
      val masked = ip & mask
      val pcs = ipList.filter(r => {
        r._2 > mask && (r._1 & mask) == masked
      })

      val asserted = ConstrainFloatingExpression(:&&:(:@(IPDst), ConstantValue(mask)),
        :==:(ConstantValue(ip & mask)))
      val constrain =
        if (pcs.isEmpty)
          asserted
        else
          InstructionBlock(asserted :: pcs.map(v => {
            ConstrainFloatingExpression(:&&:(:@(IPDst), ConstantValue(v._2)),
              :~:(:==:(ConstantValue(v._1 & v._2))))
          }).toList)
      val crt = portEntries.getOrElse(port, Nil)
      portEntries.put(port, constrain :: crt)
      ipList.append((ip, mask))
    }
    portEntries
  }

  def makeOptimizedRouterForBV_d(f : File, prefix : String, withTtl : Boolean = false) : OptimizedRouter = {
    val table = getRoutingEntriesForBV(f)
    val portEntries = generateConditionsPerPort(table)
    val gatherNeg = Neg(Fork(portEntries.flatMap(h => {
      h._2
    })))
    new OptimizedRouter(prefix + "-" + prefix, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(
        inputPortName("table") -> InstructionBlock(
          If (Constrain(TTL, :==:(ConstantValue(0))), Fail("dropping packet with 0 ttl")),
          Fork(portEntries.map(r => {
          InstructionBlock(Assume(Fork(r._2)), Forward(outputPortName(r._1)))
        }) ++ (InstructionBlock(Assume(gatherNeg), Fail("no routing entry")) :: Nil)),
          Assign(TTL, :-:(:@(TTL), ConstantValue(1)))
        )
      )
    }
  }


  def makeOptimizedRouterForBV(f : File, prefix : String) : OptimizedRouter = {
    val table = getRoutingEntriesForBV(f)
    val portEntries = mutable.Map.empty[String, List[Instruction]]
    val ipList = mutable.Buffer.empty[(Long, Long)]
    for (((ip, mask), port) <- table) {
      val masked = ip & mask
      val pcs = ipList.filter(r => {
        r._2 > mask && (r._1 & mask) == masked
      })
      val asserted = ConstrainFloatingExpression(:&&:(:@(IPDst), ConstantValue(mask)),
        :==:(ConstantValue(ip & mask)))
      val constrain =
        if (pcs.isEmpty)
          asserted
        else
          InstructionBlock(asserted :: pcs.map(v => {
            ConstrainFloatingExpression(:&&:(:@(IPDst), ConstantValue(v._2)),
              :~:(:==:(ConstantValue(v._1 & v._2))))
          }).toList)
      val crt = portEntries.getOrElse(port, Nil)
      portEntries.put(port, constrain :: crt)
      ipList.append((ip, mask))
      if (ipList.size % 100 == 0)
        System.out.println(s"reached entry ${ipList.size}")
    }
    System.out.println(s"done ${ipList.size}")
    new OptimizedRouter(prefix, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(
        s"${prefix}0" -> portEntries.foldRight(Fail("No route.") : Instruction)((x, acc) => {
          If (Fork(x._2), Forward(prefix + x._1), acc)
        })
      ) ++ table
        .map(
          i =>
            prefix + i._2 -> Forward(prefix + i._2 + "_EXIT")
              .asInstanceOf[Instruction])
        .toMap ++
        table
          .map(i => prefix + i._2 + "_EXIT" -> NoOp.asInstanceOf[Instruction])
          .toMap
    }
  }

  def makeRouterForBV(f: File, prefix: String): OptimizedRouter = {
    val table = getRoutingEntriesForBV(f)
    val name = f.getName.trim.stripSuffix(".rt")
    val i = table.foldRight(Fail("No route."): Instruction)((x, acc) => {
      If(ConstrainFloatingExpression(:&&:(:@(IPDst), ConstantValue(x._1._2)),
                                     :==:(ConstantValue(x._1._1 & x._1._2))),
         Forward(prefix + x._2),
         acc)
    })
    new OptimizedRouter(prefix, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = Map(
        s"${prefix}0" -> i
      ) ++ table
        .map(
          i =>
            prefix + i._2 -> Forward(prefix + i._2 + "_EXIT")
              .asInstanceOf[Instruction])
        .toMap ++
        table
          .map(i => prefix + i._2 + "_EXIT" -> NoOp.asInstanceOf[Instruction])
          .toMap
    }
  }

  def makeOptimizedRouter_Costin(f: File, prefix: String): OptimizedRouter = {
    val table = getRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".rt")

    new OptimizedRouter(prefix, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] =
        Map(
          s"${prefix}0" ->
            Fork(
              table
                .map(i => {
                  val ((l, u), port) = i
                  (port,
                   AND(
                     List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++ {
                       val conflicts = table.takeWhile(i =>
                         u - l > i._1._2 - i._1._1) filter (other => {
                         val ((otherL, otherU), otherPort) = other
                         port != otherPort &&
                         l <= otherL &&
                         u >= otherU
                       })

                       if (conflicts.nonEmpty)
                         Seq(NOT(OR((conflicts
                           .map(conflictual => {
                             AND(List(GTE_E(ConstantValue(conflictual._1._1)),
                                      LTE_E(ConstantValue(conflictual._1._2))))
                           })
                           .toList))))
                       else Nil
                     }))
                })
                .groupBy(_._1)
                .map(kv =>
                  InstructionBlock(Constrain(IPDst, OR(kv._2.map(_._2).toList)),
                                   Forward(prefix + kv._1))))) ++
          table
            .map(
              i =>
                prefix + i._2 -> Forward(prefix + i._2 + "_EXIT")
                  .asInstanceOf[Instruction])
            .toMap ++
          table
            .map(i => prefix + i._2 + "_EXIT" -> NoOp.asInstanceOf[Instruction])
            .toMap
    }
  }

  def makeTrivialRouter(f: File): OptimizedRouter = {
    val table = getTrivialRoutingEntries(f)
    val name = f.getName.trim.stripSuffix(".router")

    val macsFile = f.getParent + File.separator + name + ".macs"
    val dstMacConstrain = getDstMacConstraint(macsFile)

    new OptimizedRouter(name + "-" + name, "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] =
        Map(
          inputPortName("in") ->
            Fork(
              table
                .map(i => {
                  val ((l, u), port) = i
                  (port,
                   AND(
                     List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++ {
                       val conflicts = table.takeWhile(i =>
                         u - l > i._1._2 - i._1._1) filter (other => {
                         val ((otherL, otherU), otherPort) = other
                         port != otherPort &&
                         l <= otherL &&
                         u >= otherU
                       })

                       if (conflicts.nonEmpty)
                         Seq(NOT(OR((conflicts
                           .map(conflictual => {
                             AND(List(GTE_E(ConstantValue(conflictual._1._1)),
                                      LTE_E(ConstantValue(conflictual._1._2))))
                           })
                           .toList))))
                       else
                         Nil
                     }))
                })
                .groupBy(_._1)
                .map(kv =>
                  InstructionBlock(
                    ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)),
                    EtherMumboJumbo.stripAllEther,
                    EtherMumboJumbo.symbolicEtherEncap,
                    dstMacConstrain,
                    Forward(outputPortName(kv._1))
                ))))
    }
  }

  def makeNaiveRouter(f: File, prefix : String = ""): OptimizedRouter = {
    val table = getRoutingEntries(f)

    var conflictCount = 0L
    var which = 0

    val i = table
      .map(i => {
        val ((l, u), port) = i
        (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++ {
          val conflicts = table
            .takeWhile(i => u - l > i._1._2 - i._1._1) filter (other => {
            val ((otherL, otherU), otherPort) = other
            port != otherPort &&
            l <= otherL &&
            u >= otherU
          })

          conflictCount += conflicts.size
          which += 1

          if (which % 100 == 0) println(which)

          if (conflicts.nonEmpty)
            Seq(NOT(OR((conflicts
              .map(conflictual => {
                AND(List(GTE_E(ConstantValue(conflictual._1._1)),
                         LTE_E(ConstantValue(conflictual._1._2))))
              })
              .toList))))
          else Nil
        }))
      })
      .groupBy(_._1)
      .foldRight(Fail("No route"): Instruction)((kv, a) =>
        If(ConstrainRaw(IPDst, OR(kv._2.map(_._2).toList)), Forward(kv._1), a))

    println("Routing table size " + table.length)
    println("Conflict count " + conflictCount)

    new OptimizedRouter("NAIVE", "Router", Nil, Nil, Nil) {
      override def instructions: Map[LocationId, Instruction] = {
        table
          .map(i => i._2 -> Forward(i._2 + "_EXIT").asInstanceOf[Instruction])
          .toMap ++
          table
            .map(i => i._2 + "_EXIT" -> NoOp.asInstanceOf[Instruction])
            .toMap +
          ("0" -> i)
      }
    }
  }

  def optimizedRouterNetworkConfig(f: File): NetworkConfig = {
    val elem = makeRouter(f)

    NetworkConfig(Some(f.getName.trim.stripSuffix(".rt")),
                  Map((elem.getName) -> elem),
                  Nil)
  }

  def bvRouterNetworkConfig(f : File, withTtl : Boolean = false) : NetworkConfig = {
    val elem = makeOptimizedRouterForBV_d(f, f.getName.trim.stripSuffix(".rt"), withTtl)
    NetworkConfig(Some(f.getName.trim.stripSuffix(".rt")),
      Map((elem.getName) -> elem),
      Nil)
  }

  def trivialRouterNetwrokConfig(f: File): NetworkConfig = {
    val elem = makeTrivialRouter(f)

    NetworkConfig(Some(f.getName.trim.stripSuffix(".router")),
                  Map((elem.getName) -> elem),
                  Nil)
  }
}
