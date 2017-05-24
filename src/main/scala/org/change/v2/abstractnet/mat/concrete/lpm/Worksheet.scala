package org.change.v2.abstractnet.mat.concrete.lpm

import java.io.File

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.abstractnet.mat.condition.Range

/**
  * A small gift from radu to symnetic.
  */
object Worksheet {

  def main(args: Array[String]): Unit = {

    val rt = OptimizedRouter.getRoutingEntries(
      new File("/0/projects/internal/symnet-stuff/Symnetic/src/main/resources/routing_tables/medium.txt")).map( e=>
    Range(e._1._1, e._1._2))

    println(rt.size)

//    val sortedRt = SortedSet(rt: _*)(Range.ordering.reverse)
    val sortedRt = rt.sorted(Range.ordering.reverse)

    println(sortedRt.size)

  }

}
