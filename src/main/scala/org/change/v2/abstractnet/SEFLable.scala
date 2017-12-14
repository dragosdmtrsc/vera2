package org.change.v2.abstractnet

import org.change.v2.analysis.processingmodels.Instruction

trait SEFLable {

  def toSEFL: Instruction

}
