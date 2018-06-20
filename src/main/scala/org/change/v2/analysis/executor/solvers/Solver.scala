package org.change.v2.analysis.executor.solvers

import org.change.v2.analysis.memory.MemorySpace


trait Solver {
  def solve(memory: MemorySpace): Boolean;
}
object AlwaysTrue extends Solver {
  override def solve(memory: MemorySpace): Boolean = true
}