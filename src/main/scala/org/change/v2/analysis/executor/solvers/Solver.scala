package org.change.v2.analysis.executor.solvers

import org.change.v2.analysis.memory.MemorySpace


trait Solver {
  def solve(memory: MemorySpace): Boolean;
}
