package org.change.v2.analysis.z3

import z3.scala.{Z3Config, Z3Context, Z3Sort}

/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
object Z3Util {


  private var threadLocalContext = new ThreadLocal[Z3Context]() {
    override def initialValue(): Z3Context = {
      new Z3Context(new Z3Config("MODEL" -> true))
    }
  }

  def refreshCache {
    threadLocalContext.set(new Z3Context(new Z3Config("MODEL" -> true)))
  }

  def z3Context = threadLocalContext.get

  private lazy val intSort: Z3Sort = z3Context.mkIntSort()

  /**
    * OPTIMIZE
    *
    * @return
    */
  def solver = z3Context.mkSolver()

  lazy val defaultSort = intSort

  def newContextAndSort(): (Z3Context, Z3Sort) = {
    val ctx = new Z3Context()
    (ctx, ctx.mkIntSort())
  }

}
