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
}

object SimpleSuperState {
  implicit def fromState(state: State): SimpleSuperState = new SimpleSuperState(state)
}

case class SimpleSuperState(state : State) extends SuperState {
  override def port: String = state.location

  override def varsAndValids(inputVars: Set[String]): Map[(Set[(String, Int)], Set[(String, Long)]), Iterable[SuperState]] = {
    val syms = state.memory.symbols.keySet.
      intersect(inputVars).map(sm => (sm, state.memory.evalToObject(sm).get.size))
    Map((syms, syms.filter(x => x._1.endsWith(".IsValid")).map(x => {
      x._1 -> state.memory.eval(x._1).get.e.asInstanceOf[ConstantValue].value
    })) -> (this :: Nil))
  }

  override def materialize(iexec : TripleInstructionExecutor = TrivialTripleInstructionExecutor,
                           solver : Solver = AlwaysTrue): Iterable[State] = Iterable(state)
}

case class MultiSuperState(state : State, otherStates : Iterable[SuperState]) extends SuperState {
  override def port: String = state.location

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
    val addedSyms = state.instructionHistory.collect {
      case AllocateSymbol(x, sz) if inputVars.contains(x) => (x, sz)
    }.foldRight(Map.empty[String, Int])((x, acc) => {
      acc + x
    })

    val addedValids = state.instructionHistory.collect {
      case AssignNamedSymbol(x, ConstantValue(value, _, _), _) if inputVars.contains(x) && x.endsWith(".IsValid") =>
        x -> value
    }.foldRight(Map.empty[String, Long])((x, acc) => {
      acc + x
    })

    otherStates.flatMap(_.varsAndValids(inputVars).toList).groupBy(kv => {
      (kv._1._1 ++ addedSyms, kv._1._2 ++ addedValids)
    }).map(f => {
      val uzp = f._2
      f._1 -> uzp.flatMap(r => r._2)
    })
  }
}
