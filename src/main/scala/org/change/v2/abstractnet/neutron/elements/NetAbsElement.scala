package org.change.v2.abstractnet.neutron.elements

import org.change.v2.analysis.processingmodels.Instruction

trait NetAbsElement {
  def symnetCode() : Instruction;
}