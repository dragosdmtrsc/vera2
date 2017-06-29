<<<<<<< HEAD
package org.change.v2.analysis.expression.concrete

import org.change.v2.analysis.expression.abst.{FloatingExpression, Expression}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.z3.Z3Util
import z3.scala.{Z3Solver, Z3AST}
import org.change.utils.RepresentationConversion._
import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Created by radu on 3/24/15.
 */
case class ConstantValue(value: Long, isIp : Boolean  = false, isMac : Boolean = false) 
  extends Expression with FloatingExpression {
  @JsonIgnore
  def ast = Z3Util.z3Context.mkNumeral(value.toString(), Z3Util.defaultSort)
  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)
    override def equals(other: Any): Boolean =    other match {        case that : ConstantValue => that.value == this.value        case that : ConstantStringValue => that.value.hashCode() == this.value        case _ => false   }  /**
   * A floating expression may include unbounded references (e.g. symbol ids)
   *
   * Given a context (the state) it can produce an evaluable expression.
   * @param s
   * @return
   */
  override def instantiate(s: State): Either[Expression, String] = Left(this)

  override def toString = {
    if (isMac)
      "[" + numberToMac(value) +  "]"
    else if (!isIp)
      s"[$value]"
    else
      "[" + numberToIp(value) +  "]"
  }
}

/**
 * For testing purposes only!!!! Note that this is just for better visualization
 * No string logic, just hashcode as a quick hack
 */
case class ConstantStringValue(value: String) 
  extends Expression with FloatingExpression {
  @JsonIgnore
  def ast = Z3Util.z3Context.mkNumeral(value.hashCode.toString(), Z3Util.defaultSort)

  def getNumber = value.hashCode()
  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)

  /**
   * A floating expression may include unbounded references (e.g. symbol ids)
   *
   * Given a context (the state) it can produce an evaluable expression.
   * @param s
   * @return
   */
  override def instantiate(s: State): Either[Expression, String] = Left(this)

  override def toString = {
    value
  }
}


=======
package org.change.v2.analysis.expression.concrete

import org.change.v2.analysis.expression.abst.{FloatingExpression, Expression}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.z3.Z3Util
import z3.scala.{Z3Context, Z3Solver, Z3AST, Z3Sort}

/**
 * Created by radu on 3/24/15.
 */
case class ConstantValue(value: Long) extends Expression with FloatingExpression {
  lazy val ast = Z3Util.z3Context.mkInt(value.asInstanceOf[Int], Z3Util.defaultSort)

  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)


  override def toZ3(context: Z3Context, solver: Z3Solver, sort: Z3Sort): (Z3AST, Z3Solver) =
    (context.mkInt(value.asInstanceOf[Int], sort), solver)

  /**
   * A floating expression may include unbounded references (e.g. symbol ids)
   *
   * Given a context (the state) it can produce an evaluable expression.
   * @param s
   * @return
   */
  override def instantiate(s: State): Either[Expression, String] = Left(this)

  override def toString = s"Const($value)]"
}
>>>>>>> stb
