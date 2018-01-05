import org.change.v2.analysis.executor.loopdetection.LoopDetector
import org.change.v2.analysis.expression.concrete.nonprimitive.{:-:, :@}
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Assign, InstructionBlock}
import org.scalatest.FunSuite
import z3.scala.Z3Context

class LoopDetectorTests extends FunSuite {

  test("Empty space is in a loop") {
    assert(LoopDetector.loop(State.clean, State.clean))
  }

  test("A state and itself form a loop") {
    assert(LoopDetector.loop(State.allSymbolic, State.allSymbolic))
  }

  test("Different constants don't form a loop") {
    val codeA = Assign("a", ConstantValue(2))
    val codeB = Assign("a", ConstantValue(3))

    assert(
      ! LoopDetector.loop(
          codeA(State.clean)._1.head,
          codeB(State.clean)._1.head
      )
    )
  }

  test("TTL decrementing won't cause loops") {
    val init = Assign("ttl", SymbolicValue())
    val dec = InstructionBlock(
      init,
      Assign("ttl", :-:(:@("ttl"),ConstantValue(1)))
    )

    assert(
      ! LoopDetector.loop(
        init(State.clean)._1.head,
        dec(State.clean)._1.head
      )
    )
  }

  test("") {
    val ctx = Z3Context
  }

}
