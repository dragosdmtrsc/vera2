package org.change.v2.analysis.executor.solvers

import org.change.v2.analysis.memory.MemorySpace

// Thread-bound code => always use safely
abstract class AbstractSolver[T]
  extends Solver {
  def translate(memory: MemorySpace): T;

  override def solve(memory: MemorySpace): Boolean = {
    decide(translate(memory))
  }

  def decide(what: T): Boolean;
}