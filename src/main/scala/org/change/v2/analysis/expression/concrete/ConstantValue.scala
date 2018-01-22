package org.change.v2.analysis.expression.concrete

import com.fasterxml.jackson.annotation.JsonIgnore
import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.z3.Z3Util
import org.change.v2.util.conversion.RepresentationConversion._
import z3.scala.{Z3AST, Z3Solver}

/**
  *
  * Created by radu on 3/24/15.
  *
  */

case class ConstantValue(value: Long, isIp: Boolean = false, isMac: Boolean = false)
  extends Expression with FloatingExpression {
  @JsonIgnore
  def ast = Z3Util.z3Context.mkNumeral(value.toString(), Z3Util.defaultSort)
  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)
  override def equals(other: Any): Boolean =
    other match {
      case that: ConstantValue => that.value == this.value
      case that: ConstantStringValue => that.value.hashCode() == this.value
      case _ => false
    }
  /**
    *
    * A floating expression may include unbounded references (e.g. symbol ids)
    *
    *
    *
    * Given a context (the state) it can produce an evaluable expression.
    *
    * @param s
    * @return
    *
    */
  override def instantiate(s: State): Either[Expression, String] = Left(this)

  override def toString = {
    if (isMac)
      "[" + numberToMac(value) + "]"
    else if (!isIp)
      s"[$value]"
    else
      "[" + numberToIp(value) + "]"
  }
}


case class ConstantBValue(value : String, size : Int) extends Expression with FloatingExpression {
  /**
    * A floating expression may include unbounded references (e.g. symbol ids)
    *
    * Given a context (the state) it can produce an evaluable expression.
    *
    * @param s
    * @return
    */
  override def instantiate(s: State) = Left(this)

  override def toZ3(solver: Option[Z3Solver]) = ???
}

/**
  *
  * For testing purposes only!!!! Note that this is just for better visualization
  *
  * No string logic, just hashcode as a quick hack
  *
  */
case class ConstantStringValue(value: String) extends Expression with FloatingExpression {

  @JsonIgnore
  def ast = Z3Util.z3Context.mkNumeral(value.hashCode.toString(), Z3Util.defaultSort)
  def getNumber = value.hashCode()
  override def toZ3(solver: Option[Z3Solver] = None): (Z3AST, Option[Z3Solver]) = (ast, solver)
  /**
    *
    * A floating expression may include unbounded references (e.g. symbol ids)
    *
    *
    *
    * Given a context (the state) it can produce an evaluable expression.
    *
    * @param s
    * @return
    *
    */
  override def instantiate(s: State): Either[Expression, String] = Left(this)
  override def toString = {
    value
  }
}
