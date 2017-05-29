package org.change.v2.abstractnet.mat.concrete.lpm

import java.io.File

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.abstractnet.mat.condition.Range
import org.change.v2.abstractnet.mat.tree.Node

import scala.collection.immutable.SortedSet

/**
  * A small gift from radu to symnetic.
  */
object Worksheet {

  def main(args: Array[String]): Unit = {

    val rt = OptimizedRouter.getRoutingEntries(
      new File("src/main/resources/routing_tables/medium.txt")).map( e =>
    Range(e._1._1, e._1._2))

    println("Initial " + rt.size)

    val sortedSet = Set(rt: _*)//(Range.ordering.reverse)
    val sortedRt: Seq[Range] = rt.sorted(Range.ordering.reverse)
//    println(sortedRt.size)
//    println(sortedSet.size)
//    println(sortedRt.take(15).toList)

    val start = System.currentTimeMillis()
    val frst = Node.makeForest(sortedRt)
    val stop = System.currentTimeMillis() - start

    println("Nodes: " + Node.forestSize(frst))
    println("Top-level: " + frst.length)
    println("Avg-height: " + Node.avgForestHeight(frst))
    println("Height: " + Node.forestHeight(frst))
    println("Time to build: " + stop / 1000)
    println("Constraints: " + Node.totalConstraintNumber(frst))
  }

}
