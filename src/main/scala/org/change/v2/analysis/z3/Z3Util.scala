package org.change.v2.analysis.z3

import z3.scala.Z3Config
import z3.scala.Z3Context

/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
object Z3Util {

  
  private var threadLocalContext = new ThreadLocal[Z3Context]() {
    override def initialValue() : Z3Context = {
      new Z3Context(new Z3Config("MODEL" -> true))
    }
  }
  
	def z3Context = threadLocalContext.get

  private def intSort = z3Context.mkIntSort()

  /**
   * OPTIMIZE
   * @return
   */
  def solver = z3Context.mkSolver()

  def defaultSort = intSort

}
