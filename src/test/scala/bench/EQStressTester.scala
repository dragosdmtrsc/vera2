package bench

import java.io.{File, PrintStream}

import com.microsoft.z3._
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.nod.{Router, RouterToNod}
import org.scalatest.FunSuite

import scala.collection.mutable

class EQStressTester extends FunSuite {

  test("stress") {
    val ps = new PrintStream("stdout.txt")
    val ps2 = new PrintStream("stderr.txt")
    System.setOut(ps)
    System.setErr(ps2)
    val table1 = OptimizedRouter.getRoutingEntriesForBV(new File("routing_tables/medium.txt"))

    for (i <- List(100, 500, 1000, 3000, 10000, 20000, 30000, 50000, 60000, 65000)) {
      val r1 = new Router("R0", table1.take(i), false)
      val r2 = new Router("R1", table1.take(i), false)
      RouterToNod.testEquivDatalog(r1, r2, false)
      RouterToNod.testEquivSEFL(r1, r2, false)
    }
  }

}
