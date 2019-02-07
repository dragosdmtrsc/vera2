package org.change.v2.analysis

import org.change.v2.analysis.constraint.Condition
import org.change.v2.analysis.memory.SimpleMemory

package object equivalence {
  type MagicTuple =
    (Condition, (Iterable[SimpleMemory], Iterable[SimpleMemory]), SimpleMemory, (String, String))
}
