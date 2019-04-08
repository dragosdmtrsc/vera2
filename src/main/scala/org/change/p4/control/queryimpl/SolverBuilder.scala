package org.change.p4.control.queryimpl
import com.microsoft.z3.{Context, Solver}

object SolverBuilder {
  def build(context : Context) : Solver = {
    val ackermanized = context.mkTactic("ackermannize_bv")
    val simplify = context.mkTactic("simplify")
    val qfbv = context.mkTactic("qfbv")
    val eq2bv = context.mkTactic("eq2bv")
    val dt2bv = context.mkTactic("dt2bv")
    val macros = context.mkTactic("macro-finder")
    val t1 = context.andThen(
      simplify,
      macros,
      dt2bv,
      eq2bv,
      ackermanized,
      simplify,
      qfbv)
    val slv = context.mkSolver(t1)
    val parms = context.mkParams()
    parms.add("mbqi", false)
    slv.setParameters(parms)
    slv
  }
}
