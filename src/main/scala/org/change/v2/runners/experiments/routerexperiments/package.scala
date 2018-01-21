package org.change.v2.runners.experiments

import java.io.File

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.processingmodels.Instruction

import scala.util.Random

/**
  * This package contains the boilerplate code for the benchmark of the various router models.
  */
package object routerexperiments {

  def getRandomRoutingEntries(file: String, howMany: Int): Seq[((Long, Long), String)] = {

    val allEntries = OptimizedRouter.getRoutingEntries(new File(file))
    val r = new Random()
    val chosenIndices = {
      for {
        _ <- 0 until howMany
      } yield r.nextInt(allEntries.length)
    }.toSet

    allEntries.zipWithIndex.filter(chosenIndices contains _._2).unzip._1
  }

  type RoutingModelFactory = (String, Int) => Instruction

  def buildIfElseChainModel: RoutingModelFactory = (f: String, count: Int) => {
    ???
  }

}
