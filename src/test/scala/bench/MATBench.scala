package bench

import org.change.v2.abstractnet.mat.tree.Node
import org.change.v2.abstractnet.mat.condition.{Range => CRange}
import org.change.v2.abstractnet.optimizedrouter.ParseForwardingTable
import org.scalameter.api._
import org.scalameter.picklers.noPickler._

object MATBench extends Bench.LocalTime {
  val path = "src/main/resources/routing_tables/"
  val files = Gen.enumeration("file")("small.txt", "medium.txt")

  val masks = for {
    file <- files
    completePath = path + file
    masks = ParseForwardingTable.getRoutingEntries(completePath)
    sortedMasks = masks.map(_._1).map(interval => CRange(interval._1, interval._2)).sortBy(- _.generality)
  } yield sortedMasks

  performance of "MAT Tree" in {
    measure method "construct" in {
      using(masks) config(exec.benchRuns -> 3) in {
        maskSet => Node.makeForest(maskSet)
          println("done")
      }
    }
  }
}