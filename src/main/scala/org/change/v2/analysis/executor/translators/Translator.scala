package org.change.v2.analysis.executor.translators

import org.change.v2.analysis.memory.MemorySpace

// thread-bound class!! Always beware
abstract class Translator[T] {
  def translate(mem: MemorySpace): T;
}