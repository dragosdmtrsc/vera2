import java.io.{File, PrintStream}

import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.equivalence.Equivalence
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State._
import org.change.v2.analysis.processingmodels.instructions.{Assign, Forward, InstructionBlock}
import org.scalatest.FunSuite
import org.change.v2.analysis.memory._
import spray.json.JsArray
import spray.json._
import z3.scala.Z3Solver

class EquivalenceTester extends FunSuite{

  test("basic vs optimized take 1") {
    var links  = Map[String, String]()
    val routerFile = "routing_tables/tiny.txt"

    var optimizedRouter = OptimizedRouter.makeOptimizedRouterForBV_d(new File(routerFile), "OPT_")
    var basicRouter = OptimizedRouter.makeOptimizedRouterForBV(new File(routerFile), "")

    val outputDict = basicRouter.instructions.map(i => i._1 -> ("OPT_" + i._1))
    def portOutput(x1 : String, y1 : String) = {
      outputDict.contains(x1) && outputDict(x1) == y1
    }
    def portInput(x : String, y : String) = {
      x == "0" && y == "OPT_0"
    }
    val equiv = new Equivalence(basicRouter.instructions, optimizedRouter.instructions)
    val init = new OVSExecutor(new Z3BVSolver()).execute(
      InstructionBlock(
        Assign("Pid", ConstantValue(0)),
        start,
        ehervlan,
        ipSymb,
        transport,
        end,
        tcpOptions), State.clean, true
    )
    import org.change.v2.util.canonicalnames._
    var time = System.currentTimeMillis()
    def outputEquivalence(slv : Z3Solver, s1 : SimpleMemory, s2 : SimpleMemory) : Boolean = {
      val layoutEquiv = s1.memTags == s2.memTags && s1.rawObjects.keySet == s2.rawObjects.keySet
      if (layoutEquiv) {
        val mustBeEqual = List(
          IPDst, IPSrc, Proto, EtherDst, EtherSrc, EtherType
        )
        val or = FOR.makeFOR(mustBeEqual.map(r => {
          s1.eval(r).map(h => {
            FNOT.makeFNOT(OP(s1.rawObjects(h).expression, EQ_E(s2.rawObjects(h).expression), s1.rawObjects(h).size))
          }).getOrElse(FALSE)
        }))
        if (or == FALSE) {
          // sounds stupid, but it means that no expression was correctly evaluated
          true
        } else {
          val cd = FAND.makeFAND(s2.pathCondition.cd :: or :: Nil)
          SimpleMemory.isSatS(cd)
        }
      } else {
        false
      }
    }

    val (a, b, c) = equiv.show(SimpleMemory.apply(init._1.head) :: Nil, List[(String, String)](("0", "OPT_0")), portOutput, outputEquivalence)
    println(s"Equivalence testing took ${System.currentTimeMillis()-time}ms")

//    val buf = new PrintStream("bad-states.json")
//    buf.println(
//      JsArray(f.map(r => r.toJson).toVector).prettyPrint
//    )
//    buf.close()
//    val buf2 = new PrintStream("ok-states.json")
//    buf2.println(
//      JsArray(s.map(r => r.toJson).toVector).prettyPrint
//    )
//    buf2.close()

  }

}
