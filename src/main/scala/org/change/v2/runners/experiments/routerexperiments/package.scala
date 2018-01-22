package org.change.v2.runners.experiments

import java.io.File

import org.change.v2.abstractnet.click.selfbuildingblocks.EtherMumboJumbo
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.:||:
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames._
import org.change.v2.util.conversion.RepresentationConversion

import scala.util.Random

/**
  * This package contains the boilerplate code for the benchmark of the various router models.
  */
package object routerexperiments {

  type RoutingEntries = Seq[((Long, Long), String)]

  def getRandomRoutingEntries(allEntries: RoutingEntries, howMany: Int): RoutingEntries = {
    val r = new Random()
    val chosenIndices = {
      for {
        _ <- 0 until howMany
      } yield r.nextInt(allEntries.length)
    }.toSet

    allEntries.zipWithIndex.filter(chosenIndices contains _._2).unzip._1
  }

  type RoutingModelFactory = (RoutingEntries) => Instruction

  def buildIfElseChainModel(entries: RoutingEntries): Instruction = {
    entries.foldRight(NoOp: Instruction)( (i, crtCode) =>
      If(Constrain(IPDst, :&:(:>=:(ConstantValue(i._1._1)), :<=:(ConstantValue(i._1._2)))),
        Forward(i._2),
        crtCode
      )
    )
  }

  def selectNRandomRoutingEntries(f: String, count: Option[Int] = None): RoutingEntries = {
    val entries = count match {
      case None => OptimizedRouter.getRoutingEntries(new File(f))
      case Some(howMany) => getRandomRoutingEntries(OptimizedRouter.getRoutingEntries(new File(f)), howMany)
    }

    assert(!entries.zip(entries.tail).exists(twoEntries => {
      val fstWidth = twoEntries._1._1._2 - twoEntries._1._1._1
      val sndWidth = twoEntries._2._1._2 - twoEntries._2._1._1
      fstWidth > sndWidth
    }))

    entries
  }

  def buildBasicForkModel(entries: RoutingEntries): Instruction = {
    Fork( entries.map(i => {
      val ((l,u), port) = i
      (port, AND(List(GTE_E(ConstantValue(l)), LTE_E(ConstantValue(u))) ++
        {
          val conflicts = entries.takeWhile(i =>  u-l > i._1._2 - i._1._1) filter (other => {
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
        Forward(kv._1)))
    )
  }

  def buildPerPortIfElse(entries: RoutingEntries): Instruction = {
    entries.groupBy(_._2)
      .mapValues(portEntries =>
        :|:(portEntries.map( entry => :&:(:>=:(ConstantValue(entry._1._1)), :<=:(ConstantValue(entry._1._2)))).toList))
      .foldRight(NoOp: Instruction)( (port, crtCode) =>
        If(Constrain(IPDst, port._2),
          Forward(port._1),
          crtCode
        )
      )
  }

}
