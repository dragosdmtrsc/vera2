package org.change.v2.analysis.processingmodels

import org.change.v2.analysis.memory.State

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import org.change.v2.analysis.processingmodels.instructions._
/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */

@JsonTypeInfo(
  use = JsonTypeInfo.Id.CLASS, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
abstract class Instruction(id: String = "", rewriteMappings: Map[String, String]= Map()) {

  /**
   *
   * A state processing block produces a set of new states based on a previous one.
   *
   * @param s
   * @return
   */
  def apply(s: State, verbose: Boolean = false): (List[State], List[State])

  def getIdHash = id.hashCode
}

