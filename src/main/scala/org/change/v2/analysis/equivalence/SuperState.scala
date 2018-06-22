package org.change.v2.analysis.equivalence

import org.change.v2.analysis.constraint.Condition
import org.change.v2.analysis.executor.{TripleInstructionExecutor, TrivialTripleInstructionExecutor}
import org.change.v2.analysis.executor.solvers.{AlwaysTrue, Solver}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.{ConditionInstantiator, InstructionCrawler, State}
import org.change.v2.analysis.processingmodels.instructions.{AllocateSymbol, AssignNamedSymbol, InstructionBlock}

trait SuperState {
  def port : String
  def varsAndValids(inputVars : Set[String]) : Map[(Set[(String, Int)], Set[(String, Long)]), Iterable[SuperState]]
  def materialize(iexec : TripleInstructionExecutor = TrivialTripleInstructionExecutor,
                  solver : Solver = AlwaysTrue) : Iterable[State]
  def max() : Int
}

object SimpleSuperState {
  implicit def fromState(state: State): SimpleSuperState = new SimpleSuperState(state)
}

case class SimpleSuperState(state : State) extends SuperState {
  override def port: String = state.location
  lazy val syms = state.memory.symbols.keySet.map(sm => (sm, state.memory.evalToObject(sm).get.size))
  override def varsAndValids(inputVars: Set[String]): Map[(Set[(String, Int)], Set[(String, Long)]), Iterable[SuperState]] = {
    val intersect = syms.filter(h => inputVars.contains(h._1))
    Map((intersect, syms.filter(x => x._1.endsWith(".IsValid")).map(x => {
      x._1 -> state.memory.eval(x._1).get.e.asInstanceOf[ConstantValue].value
    })) -> (this :: Nil))
  }
  override def max : Int = 1
  override def materialize(iexec : TripleInstructionExecutor = TrivialTripleInstructionExecutor,
                           solver : Solver = AlwaysTrue): Iterable[State] = Iterable(state)
}

case class MultiSuperState(state : State,
                           otherStates : Iterable[SuperState],
                           eqClass : Option[List[State]] = None) extends SuperState {
  override def port: String = state.location

  lazy val addedHere = state.instructionHistory.collect {
    case AllocateSymbol(x, sz) => (x, sz)
  }.foldRight(Map.empty[String, Int])((x, acc) => {
    acc + x
  })
  lazy val validatedHere = state.instructionHistory.collect {
    case AssignNamedSymbol(x, ConstantValue(value, _, _), _) if x.endsWith(".IsValid") =>
      x -> value
  }.foldRight(Map.empty[String, Long])((x, acc) => {
    acc + x
  })

  override def max() : Int = {
    (if (eqClass.isEmpty)
      1
    else
      eqClass.get.size) * otherStates.map(r => r.max()).sum
  }

  override def materialize(iexec : TripleInstructionExecutor = TrivialTripleInstructionExecutor,
                  solver : Solver = AlwaysTrue) : Iterable[State] = {
    otherStates.flatMap(_.materialize()).map(r => r -> state.memory.pathConditions.foldLeft(r.memory)((acc, x) => {
      acc.addCondition(ConditionInstantiator(x, r))
    })).filter(f => solver.solve(f._2)).map(h => {
      val (ss, fs, st) = iexec.execute(InstructionBlock(state.instructionHistory.reverse), h._1, false)
      if (ss.nonEmpty)
        ss.head
      else if (fs.nonEmpty)
        fs.head
      else
        st.head
    })
  }

  override def varsAndValids(inputVars: Set[String]): Map[(Set[(String, Int)], Set[(String, Long)]), Iterable[SuperState]] = {
    val flt = addedHere.filter(k => inputVars.contains(k._1))
    val vld = validatedHere.filter(k => inputVars.contains(k._1))
    otherStates.foldLeft(Map.empty[(Set[(String, Int)], Set[(String, Long)]), Iterable[SuperState]])((acc, x) => {
      val sb = x.varsAndValids(inputVars)
      sb.foldLeft(acc)((acc2, y) => {
        val z = (y._1._1 ++ flt, y._1._2 ++ vld)
        if (acc2.contains(z))
          acc2 + (z -> (acc2(z) ++ y._2))
        else
          acc2 + (z -> y._2)
      })
    })

  }
}
