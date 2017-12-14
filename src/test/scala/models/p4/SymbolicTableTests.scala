package models.p4

import org.change.v2.models.p4.SymbolicTable
import org.change.v2.p4.model.Switch
import org.scalatest.FunSuite

class SymbolicTableTests extends FunSuite {
  val p4File = "inputs/simple-nat/simple_nat-ppc.p4"

  test("No exceptions when building the set of symbolic tables from the P4 nat.") {
    val switch = Switch.fromFile(p4File)

    val symbolicTables = SymbolicTable.fromSwitch(switch)

    assert(symbolicTables.nonEmpty)
  }

}
