package parser.p4.test

import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.updated.program.P4Program
import org.change.v2.p4.model.updated.sefl_compiler.SymbolicTableCompiler
import org.scalatest.FunSuite

class P4SymbolicTableCompilerTests extends FunSuite {
  val simpleNat = "inputs/simple-nat/simple_nat-ppc.p4"

  test("Action parsing") {
    val p4 = simpleNat
    P4Program.fromP4File(p4) match {
      case Right(p4Program) => SymbolicTableCompiler.compile(p4Program)
      case Left(errMsg) => fail(errMsg)
    }

  }

}
