package org.change.v2.analysis.equivalence

import java.io.PrintStream

import org.change.v2.analysis
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.helpers.GFG
import org.change.v2.util.ToDot
import z3.scala.{Z3AST, Z3Context, Z3Solver}

import scala.collection.mutable
import collection.JavaConverters._


object EqPrinter {
  def printme(res1 : List[SimpleMemory], ps1 : PrintStream): Unit = {
    val alreadyKnown = mutable.Set[(String, String)]()
    def print(bh : List[BranchC]): Unit = {
      if (bh.size == 1) {
        if (!alreadyKnown.contains(("ROOT", bh.head.toString))) {
          ps1.println("ROOT -> " + ToDot.normalize(bh.head.toString) + ";")
          alreadyKnown.add(("ROOT", bh.head.toString))
        }
      } else if (bh.nonEmpty) {
        if (!alreadyKnown.contains((bh.tail.head.toString, bh.head.toString))) {
          alreadyKnown.add((bh.tail.head.toString, bh.head.toString))
          ps1.println(ToDot.normalize(bh.tail.head.toString) + " -> " + ToDot.normalize(bh.head.toString) + ";")
        }
        print(bh.tail)
      }
    }
    ps1.println("digraph g {")
    res1.zipWithIndex.foreach(r => {
      val last = r._1.branchHistory.headOption.map(_.toString).getOrElse("ROOT")
      val x = "state" + r._2
      ps1.println(ToDot.normalize(last) + " -> " + ToDot.normalize(x) + ";")
      print(r._1.branchHistory)
    })
    ps1.println("}")
    ps1.close()
  }
}

class Equivalence(val instructions1 : Map[String, Instruction],
                  val instructions2 : Map[String, Instruction]) {

    import EqPrinter._
    def show(input : List[SimpleMemory],
             initialLocations : Iterable[(String, String)],
             outputPortCorrespondence : ((String, String) => Boolean),
             outputEquivalenceHook : ((Z3Solver, SimpleMemory, SimpleMemory) => Boolean)
            ) : (List[SimpleMemory], List[SimpleMemory]) = {

      def simpleSatStrategy(condition: Condition, newCondition : Condition) : Boolean =
        SimpleMemory.isSatS(FAND.makeFAND(condition :: newCondition :: Nil))
      val simpleMemoryInterpreter = new SimpleMemoryInterpreter(simpleSatStrategy)
      val toTheEndExecutor = new ToTheEndExecutor(simpleMemoryInterpreter, instructions1)
      val toTheEndExecutor2 = new ToTheEndExecutor(simpleMemoryInterpreter, instructions2)
      var i = 0
      val wrongArity = mutable.Buffer[(Condition, String, String)]()
      val portMismatch = mutable.Buffer[(Condition, String, String)]()

      for ((l1, l2) <- initialLocations) {
        val res1 = toTheEndExecutor.executeFrom(l1, input.head)
        i = i + 1
        val all = toTheEndExecutor.noSieve(res1.flat())
        println(s"${all.size} total number of res1 pcs vs ${res1.success.size + res1.failed.size}")
        all.foreach(h => {
          val res2 = toTheEndExecutor2.executeFrom(l2, input.head.copy(pathCondition = SimplePathCondition(h._1)))
          i = i + 1
          // compare h._2 to res2
          // 1: arity
          val all2 = toTheEndExecutor2.noSieve(res2.flat())
          val succ1 = h._2.filter(_.error == "")
          all2.foreach(r => {
            val succ2 = r._2.filter(_.error == "")
            if (succ1.size != succ2.size) {
              wrongArity += ((r._1, l1, l2))
            } else {
              if (succ1.size == 1) {
                if (!outputPortCorrespondence(succ1.head.location, succ2.head.location)) {
                  portMismatch += ((r._1, l1, l2))
                } else {
                  val ctx = new Z3Context()
                  val slv = ctx.mkSolver()
                  val trans = new analysis.memory.SimpleMemory.Translator(ctx, slv)
                  slv.assertCnstr(trans.createAST(succ2.head.pathCondition.cd))
                  if (!outputEquivalenceHook(slv, succ1.head, succ2.head)) {
                    portMismatch += ((r._1, l1, l2))
                  }
                }
              } else {
                val o1o2Rel = succ1.map(u => {
                  succ2.map(v => {
                    val ctx = new Z3Context()
                    val slv = ctx.mkSolver()
                    val trans = new analysis.memory.SimpleMemory.Translator(ctx, slv)
                    slv.assertCnstr(trans.createAST(v.pathCondition.cd))
                    if (outputPortCorrespondence(u.location, v.location) && outputEquivalenceHook(slv, u, v)) new Integer(1) else new Integer(0)
                  }).asJava
                }).toArray
                val gfg = new GFG(succ1.size)
                val bpm = gfg.maxBPM(o1o2Rel)
                if (bpm != succ1.size) {
                  portMismatch += ((r._1, l1, l2))
                }
              }
            }
          })
        })
      }
      (Nil, wrongArity.map(r => input.head.copy(errorCause = Some("Wrong arity"),
        pathCondition = SimplePathCondition(r._1),
        history = r._2 :: r._3 :: input.head.history)).toList ++ portMismatch.map(r => input.head.copy(errorCause = Some("Port mismatching"),
        pathCondition = SimplePathCondition(r._1),
        history = r._2 :: r._3 :: input.head.history)))
    }


//  def show(input : List[State],
//           initialLocations : Iterable[(String, String)],
//           outputPortCorrespondence : ((String, String) => Boolean),
//           outputEquivalenceHook : ((Z3Solver, State, State) => Boolean)
//          ) : (List[State], List[State]) = {
//    val solver = new Z3BVSolver()
//    val caie1 = CodeAwareInstructionExecutor(instructions1, solver)
//    val caie2 = CodeAwareInstructionExecutor(instructions2, solver)
//
//    initialLocations.foldLeft((Nil, Nil) : (List[State], List[State]))((acc, r) => {
//      val out1 = input.flatMap(s => {
//        caie1.execute(Let("out1", Forward(r._1)), s, true)._1
//      })
//      val out2 = out1.flatMap(s => {
//        val (ss, f) = caie2.execute(Forward(r._2), s, true)
//        ss ++ f
//      })
//      out2.foldLeft(acc)((acc, outP2) => {
//        val outP1 = outP2.memory.saved("out1").head
//        def stateSize(state: State): Int = {
//          state.memory.intersections.map(s => stateSize(s)).sum +
//            (if (state.errorCause.isEmpty) 1 else 0)
//        }
//        def success(state : State): List[State] = {
//          if (state.errorCause.isEmpty) state :: state.memory.intersections.flatMap(success)
//          else state.memory.intersections.flatMap(success)
//        }
//
//        val successfulP1 = stateSize(outP1)
//        val successfulP2 = stateSize(outP2)
//        if (successfulP1 != successfulP2)
//          (acc._1, outP2.copy(errorCause = Some(s"Arity difference: $successfulP1 vs $successfulP2")) :: acc._2)
//        else {
//          val allSuccess1 = success(outP1)
//          val allSuccess2 = success(outP2)
//
//          var found = true
//          var lastIndexFound = -1
//          def locationPredicate(c1 : State, c2 : State) : Boolean =
//            outputPortCorrespondence(c1.location, c2.location)
//          def layoutPredicate(c1 : State, c2 : State) : Boolean =
//            c1.memory.rawObjects.keySet == c2.memory.rawObjects.keySet
//          def outputPredicate(c1 : State, c2 : State) : Boolean = {
//            val translator = new Z3BVTranslator(context = new Z3Context())
//            val slv = translator.translate(outP2.memory)
//            outputEquivalenceHook(slv, c1, c2)
//          }
//          def fullPredicate(c1 : State, c2 : State): Boolean = {
//            val crt = outputPortCorrespondence(c1.location, c2.location) &&
//              c1.memory.rawObjects.keySet == c2.memory.rawObjects.keySet
//            if (!crt)
//              false
//            else {
//              val translator = new Z3BVTranslator(context = new Z3Context())
//              val slv = translator.translate(outP2.memory)
//              outputEquivalenceHook(slv, c1, c2)
//            }
//          }
//
//          def checkEquivalence[T](candidates : Array[Set[Int]])
//                                 (predicate : (State, State) => Boolean)
//                                 (onP1NotFound : (State, Int) => T)
//                                 (onP2NotFound : (State, Iterable[Int]) => T)
//                                 (onNoMatch    : (State) => T)
//                                 (onEquiv      : (State, Array[Set[Int]]) => T) : T = {
//            val edges = Array.fill(candidates.length)(mutable.Set[Integer]())
//            val redges = Array.fill(candidates.length)(mutable.Set[Integer]())
//            val newCandidates = Array.fill(candidates.length)(Set[Int]())
//            for (c1 <- allSuccess1.zipWithIndex
//                 if found
//            ) {
//              found = false
//              for (i2 <- candidates(c1._2)) {
//                val c2 = allSuccess2(i2)
//                if (predicate(c1._1, c2)) {
//                  newCandidates(c1._2) = newCandidates(c1._2) + i2
//                  edges(c1._2) += i2
//                  redges(i2) += c1._2
//                  found = true
//                  lastIndexFound = c1._2
//                }
//              }
//            }
//            if (!found) {
//              // there is someone in the first set not matched
//              onP1NotFound(outP2, lastIndexFound)
//            } else if (redges.size != allSuccess2.size) {
//              // there is someone in the second set not matched
//              val difference = allSuccess1.indices.filter(r => {
//                redges(r).nonEmpty
//              })
//              onP2NotFound(outP2, difference)
//            } else {
//              // sets have equal size + there is at least a mapping in
//              // both directions => use max flow algo to compute if
//              // there is any such thing as a bijection
//              val gfg = new GFG(successfulP1)
//              val maxBpm = gfg.maxBPM(edges.map(_.toIterable.asJava))
//              if (maxBpm != successfulP1) {
//                onNoMatch(outP2)
//              } else {
//                onEquiv(outP2, newCandidates)
//              }
//            }
//          }
//          def genericP1Hook(step : String): ((State, Int) => (List[State], List[State])) = {
//            (s, lastIndexFound) => (acc._1, s.copy(errorCause = Some(
//              s"No $step match found for all program 1 states ${lastIndexFound + 1}")) :: acc._2)
//          }
//          def genericP2Hook(step : String): ((State, Iterable[Int]) => (List[State], List[State])) = {
//            (s, difference) =>
//              (acc._1, s.copy(errorCause = Some(
//                s"No $step match found for all program 2 states: ${difference.mkString(",")} missing")) :: acc._2)
//          }
//
//          def genericNoMatchHook(step : String) : ((State) => (List[State], List[State])) = {
//            s => (acc._1, s.copy(errorCause = Some(
//              s"No $step match found between outcomes of program 1 and program 2")) :: acc._2)
//          }
//
//          checkEquivalence(Array.fill(successfulP1)((0 until successfulP1).toSet))(
//            locationPredicate)(genericP1Hook("output port"))(genericP2Hook("output port"))(
//            genericNoMatchHook("output port")
//          )((s, candidates) => {
//            checkEquivalence(candidates)(layoutPredicate)(genericP1Hook("layout"))(
//              genericP2Hook("layout"))(genericNoMatchHook("layout"))((s, candidates) =>
//              checkEquivalence(candidates)(
//                outputPredicate
//              )((s, lastIndexFound) => {
//                genericP1Hook("output")(s, lastIndexFound)
//              })((s, difference) => {
//                genericP2Hook("output")(s, difference)
//              })((s) => {
//                genericNoMatchHook("output")(s)
//              })((s, candidates) => {
//                (outP2 :: acc._1, acc._2)
//              })
//            )
//          })
//        }
//      })
//    })
//  }
//
//  def show(input : List[State],
//           inputPortCorrespondence : ((String, String) => Boolean),
//           outputPortCorrespondence : ((String, String) => Boolean),
//           outputEquivalenceHook : ((Z3Solver, State, State) => Boolean)
//          ): (List[State], List[State]) = {
//    val initialLocations = for {
//      x <- instructions1.keys
//      y <- instructions2.keys
//      if inputPortCorrespondence(x, y)
//    }
//      yield (x, y)
//    show(input, initialLocations, outputPortCorrespondence, outputEquivalenceHook)
//  }
//
//  def outputEquiv(slv : Z3Solver, c1 : State, c2 : State) = {
//    val startTag = Tag("START")
//    val z3BVTranslator = new Z3BVTranslator(slv.context)
//    def memoryObjectsToAST(v1 : Option[MemoryObject],
//                           v2 : Option[MemoryObject],
//                           negate : Boolean = true) = {
//      v1.zip(v2).flatMap(r => {
//        r._1.value.zip(r._2.value).map(x => {
//          if (negate)
//            slv.context.mkNot(slv.context.mkEq(
//              z3BVTranslator.translate(slv, x._1.e, r._1.size)._1,
//              z3BVTranslator.translate(slv, x._2.e, r._2.size)._1
//            ))
//          else
//            slv.context.mkEq(
//              z3BVTranslator.translate(slv, x._1.e, r._1.size)._1,
//              z3BVTranslator.translate(slv, x._2.e, r._2.size)._1
//            )
//        })
//      })
//    }
//    def translateToAST(etherTypeTag : TagExp, negate : Boolean = true) : Either[Boolean, Iterable[Z3AST]] = {
//      val v1 = etherTypeTag(c1).flatMap(c1.memory.evalToObject)
//      val v2 = etherTypeTag(c2).flatMap(c2.memory.evalToObject)
//      if (v1.isEmpty != v2.isEmpty ||
//        v1.zip(v2).exists(x => x._1.value.isEmpty != x._2.value.isEmpty))
//        Left(false)
//      else {
//        if (v1.zip(v2).exists(x => x._1.value.isEmpty != x._2.value.isEmpty)) {
//          Left(false)
//        } else {
//          Right(memoryObjectsToAST(v1, v2, negate))
//        }
//      }
//    }
//
//    val simpleFirst =
//      List[TagExp](EtherType, EtherSrc, EtherDst).foldLeft(Right(List[Z3AST]()): Either[Boolean, Iterable[Z3AST]])(
//        (acc, x) => acc match {
//          case Left(verdict) => Left(verdict)
//          case Right(add) => translateToAST(x) match {
//            case Right(a2) =>  Right(add ++ a2)
//            case Left(verdict2) => Left(verdict2)
//          }
//        }
//      )
//
//    simpleFirst match {
//      case Left(verdict) => verdict
//      case Right(zasts) =>
//        val etherTypeC = translateToAST(EtherType, false)
//        val ipConds = List[TagExp](IPSrc, IPDst, Proto).foldLeft(Right(List[Z3AST]()): Either[Boolean, Iterable[Z3AST]])(
//          (acc, x) => acc match {
//            case Left(verdict) => Left(verdict)
//            case Right(add) => translateToAST(x, false) match {
//              case Right(a2) =>  Right(add ++ a2)
//              case Left(verdict2) => Left(verdict2)
//            }
//          }
//        ) match {
//          case Right(all) => Right(
//            slv.context.mkNot(slv.context.mkImplies(
//              slv.context.mkAnd(etherTypeC.right.get.toSeq:_*),
//              slv.context.mkAnd(all.toSeq:_*)
//            ))
//          )
//          case Left(verdict) => Left(verdict)
//        }
//        ipConds match {
//          case Right(what) =>
//            slv.assertCnstr(slv.context.mkOr(what :: zasts.toList:_*))
//            !slv.check().getOrElse(true)
//          case Left(verdict) => verdict
//        }
//    }
//  }
//
//  def show(input : Instruction,
//           inputPortCorrespondence : ((String, String) => Boolean),
//           outputPortCorrespondence : ((String, String) => Boolean),
//           outputEquivalenceHook : ((Z3Solver, State, State) => Boolean)
//          ) : (List[State], List[State]) = {
//    val codeAwareInstructionExecutor = CodeAwareInstructionExecutor(Map.empty, new Z3BVSolver())
//    val initStates = codeAwareInstructionExecutor.execute(input, State.clean, true)._1
//    show(initStates, inputPortCorrespondence, outputPortCorrespondence, outputEquivalenceHook)
//  }

}