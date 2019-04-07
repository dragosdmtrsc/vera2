package org.change.utils

import com.microsoft.z3._
import com.microsoft.z3.enumerations.Z3_lbool
import Z3Helper._
class Z3Helper(context: Context) {
  def mkFreshBoolConst(prefix : String) : BoolExpr =
    context.mkFreshConst(prefix, context.mkBoolSort()).asInstanceOf[BoolExpr]
  def mkBVSort(sz : Int) : Sort = {
    context.mkBitVecSort(sz)
  }

  def mkTupleSort(name : String, sorts : Iterable[Sort]) :
    (Sort, FuncDecl, List[FuncDecl]) = {
    val tupleSort = context.mkTupleSort(context.mkSymbol(name),
      (0 until sorts.size).map(s => context.mkSymbol(s"field_$s")).toArray,
      sorts.toArray
    )
    (tupleSort, tupleSort.mkDecl(), tupleSort.getFieldDecls.toList)
  }
}

class Help(expr : Expr) {
  def asBV : BitVecExpr = expr.asInstanceOf[BitVecExpr]
  def asInt : ArithExpr = expr.asInstanceOf[ArithExpr]
  def asBool : BoolExpr = expr.asInstanceOf[BoolExpr]
  def getBool : Option[Boolean] = expr.getBoolValue match {
    case Z3_lbool.Z3_L_TRUE => Some(true)
    case Z3_lbool.Z3_L_FALSE => Some(false)
    case _ => None
  }
  def getInt: Option[BigInt] = {
    if (expr.isBV) {
      Some(BigInt(expr.asInstanceOf[BitVecNum].getBigInteger))
    } else if (expr.isInt) {
      Some(BigInt(expr.asInstanceOf[IntNum].getBigInteger))
    } else {
      None
    }
  }
}
class SolverHelper(solver : Solver) {
  def assertCnstr(e : Expr) : Unit = solver.add(e.asBool)
  def docheck() : Option[Boolean] = {
    solver.check() match {
      case Status.SATISFIABLE => Some(true)
      case Status.UNSATISFIABLE => Some(false)
      case Status.UNKNOWN => None
    }
  }
  def docheck(assumptions : List[Expr]) : Option[Boolean] = {
    solver.check(assumptions:_*) match {
      case Status.SATISFIABLE => Some(true)
      case Status.UNSATISFIABLE => Some(false)
      case Status.UNKNOWN => None
    }
  }
}
object Z3Helper {
  implicit def rich(context: Context): Z3Helper = new Z3Helper(context)
  implicit def rich(expr : Expr) : Help = new Help(expr)
  implicit def rich(solver : Solver) : SolverHelper = new SolverHelper(solver)
}