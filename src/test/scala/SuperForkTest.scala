import org.change.v2.analysis.executor.CodeAwareInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.{Instruction, SuperFork}
import org.change.v2.analysis.processingmodels.instructions._
import org.scalatest.FunSuite

class SuperForkTest extends FunSuite {

  test("superfork basic") {
    val instructions = Map("p0" -> InstructionBlock(
      Allocate("x", 64),
      SuperFork(
        List[Instruction](
          Forward("p1"),
          Forward("p2")
        )
      )
    ))
    val caie = CodeAwareInstructionExecutor(instructions, new Z3BVSolver())
    val (succ, fail) = caie.execute(Forward("p0"), State.clean, verbose = true)
    assert(fail.isEmpty)
    assert(succ.size == 1)
    assert(succ.head.memory.intersections.size == 1)
  }

  test("superfork constraints 2") {
    val instructions = Map("p0" -> InstructionBlock(
      Allocate("x", 64),
      Assign("x", SymbolicValue()),
      SuperFork(
        List[Instruction](
          InstructionBlock(
            Assert("x", :<=:(ConstantValue(100))),
            Forward("p1")
          ),
          InstructionBlock(
            Assert("x", :>=:(ConstantValue(90))),
            Forward("p2")
          )
        )
      )
    ))
    val caie = CodeAwareInstructionExecutor(instructions, new Z3BVSolver())
    val (succ, fail) = caie.execute(Forward("p0"), State.clean, verbose = true)
    assert(fail.isEmpty)
    assert(succ.size == 3)
  }

  test("superfork multi constraints") {
    val instructions = Map("p0" -> InstructionBlock(
      Allocate("x", 64),
      Assign("x", SymbolicValue()),
      SuperFork(
        List[Instruction](
          InstructionBlock(
            Assert("x", :<=:(ConstantValue(100))),
            Forward("p1")
          ),
          Forward("p2")
        )
      )
    ))
    val caie = CodeAwareInstructionExecutor(instructions, new Z3BVSolver())
    val (succ, fail) = caie.execute(Forward("p0"), State.clean, verbose = true)
    assert(fail.isEmpty)
    assert(succ.size == 2)
    assert(succ.forall(p => p.memory.intersections.size == 1 && p.memory.differences.isEmpty))
  }

  test("superfork fail success") {
    val instructions = Map("p0" -> InstructionBlock(
      Allocate("x", 64),
      SuperFork(
        List[Instruction](
          Fail("p1"),
          Forward("p2")
        )
      )
    ))
    val caie = CodeAwareInstructionExecutor(instructions, new Z3BVSolver())
    val (succ, fail) = caie.execute(Forward("p0"), State.clean, verbose = true)
    assert(fail.isEmpty)
    assert(succ.size == 1)
    assert(succ.head.memory.intersections.size == 1)
  }

}
