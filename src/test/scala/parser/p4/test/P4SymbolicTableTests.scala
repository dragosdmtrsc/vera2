package parser.p4.test

import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

class P4SymbolicTableTests extends FunSuite {

  test("Action parsing") {
    val sw: Switch = Switch.fromFile("inputs/simple-nat/simple_nat-ppc.p4")
    println(sw.getDeclaredTables)
  }

}
