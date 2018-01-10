package org.change.v2.analysis.processingmodels

import org.change.v2.analysis.memory.State

import org.change.v2.analysis.processingmodels.instructions._

/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */

abstract class Instruction(id: String = "", rewriteMappings: Map[String, String] = Map()) {

  /**
    *
    * A state processing block produces a set of new states based on a previous one.
    *
    * @param s
    * @return
    */
  def apply(s: State, verbose: Boolean = false): (List[State], List[State])

  def getIdHash = id.hashCode

  def isTool = false
}


object Instruction {
  implicit def apply(fun : Function1[State, (List[State], List[State])]) = new Instruction() {
    override def isTool: Boolean = true
    override def apply(s: State, verbose: Boolean): (List[State], List[State]) = fun(s)
  }
}

