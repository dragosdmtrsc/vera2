package org.change.v2.models

import org.change.v2.analysis.processingmodels.Instruction

trait ProducesSEFL {
  def toSEFL[A](extraInfo: Option[A] = None): Instruction
}
