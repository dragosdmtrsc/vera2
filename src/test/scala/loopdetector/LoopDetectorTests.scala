package loopdetector

import org.change.v2.analysis.executor.loopdetection.LoopDetector
import org.change.v2.analysis.expression.concrete.nonprimitive.{:-:, :@}
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Assign, InstructionBlock}
import org.scalatest.FunSuite

class LoopDetectorTests extends FunSuite {

  val loopDetector = LoopDetector

  test("Empty space is in a loop") {
    assert(loopDetector.loop(State.clean, State.clean))
  }

  test("A state and itself form a loop") {
    assert(loopDetector.loop(State.allSymbolic, State.allSymbolic))
  }

  test("Different constants don't form a loop") {
    val codeA = Assign("a", ConstantValue(2))
    val codeB = Assign("a", ConstantValue(3))

    assert(
      ! loopDetector.loop(
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
      ! loopDetector.loop(
        init(State.clean)._1.head,
        dec(State.clean)._1.head
      )
    )
  }

}
