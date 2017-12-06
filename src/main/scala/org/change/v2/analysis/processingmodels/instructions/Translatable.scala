package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.processingmodels.Instruction

/**
  * Created by dragos on 25.08.2017.
  */
trait Translatable {
  def generateInstruction(): Instruction;
}
