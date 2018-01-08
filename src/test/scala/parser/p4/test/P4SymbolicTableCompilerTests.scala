//package parser.p4.test
//
//import org.change.parser.p4.parser.{DFSState, StateExpander}
//import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
//import org.change.v2.analysis.executor.solvers.Z3Solver
//import org.change.v2.analysis.expression.concrete.SymbolicValue
//import org.change.v2.analysis.memory.State
//import org.change.v2.analysis.processingmodels.instructions.{AllocateSymbol, Forward}
//import org.change.v2.analysis.z3.Z3Util
//import org.change.v2.p4.model.Switch
//import org.change.v2.p4.model.updated.program.P4Program
//import org.change.v2.p4.model.updated.sefl_compiler.SymbolicTableCompiler
//import org.scalatest.FunSuite
//
//class P4SymbolicTableCompilerTests extends FunSuite {
//  val simpleNat = "inputs/simple-nat/simple_nat-ppc.p4"
//
//  test("Action parsing.") {
//    val p4 = simpleNat
//    P4Program.fromP4File(p4) match {
//      case Right(p4Program) => SymbolicTableCompiler.compile(p4Program)
//      case Left(errMsg) => fail(errMsg)
//    }
//
//  }
//
//  test("Run SEFL.") {
//    val startState = State.clean
//
//    val code = AllocateSymbol("A", 14)
//
//    val executor = new CodeAwareInstructionExecutor(
//      Map(
//        "portA" -> code
//      ),
//      new Z3Solver
//    )
//
//    val res = executor.executeForward(Forward("portA"),startState)
//  }
//
//  test("Generate all packet layouts.") {
//    val switch : Switch = Switch.fromFile(simpleNat)
//    val expd = new StateExpander(switch, "start").doDFS(DFSState(0))
//    val code = StateExpander.generateAllPossiblePackets(expd, switch)
//
//    val startState = State.clean
//
//    val executor = new CodeAwareInstructionExecutor(
//      Map(
//        "portA" -> code
//      ),
//      new Z3Solver
//    )
//
//    val res = executor.executeForward(Forward("portA"),startState)
//
//    println("Hello")
//  }
//
//}
