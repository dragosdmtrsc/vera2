package org.change.v2.analysis.memory

import org.change.v2.analysis.processingmodels.State
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.CLASS, 
  include = JsonTypeInfo.As.PROPERTY, 
  property = "type")
trait Intable {
  def apply(s: State): Option[Int]
}