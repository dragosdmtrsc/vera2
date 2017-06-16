package org.change.v2.abstractnet.linux

import org.change.v2.analysis.processingmodels.Instruction

trait Translatable {
  def generateInstruction() : Instruction;
}