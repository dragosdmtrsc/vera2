package org.change.v2.abstractnet.linux

import org.change.v2.analysis.processingmodels.Instruction

trait Translatable {
  def name() : String;
  def generateInstructionsAndLinks() : (Map[String, Instruction], Map[String, String]);
}