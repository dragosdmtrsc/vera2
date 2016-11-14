package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.memory.TagExp
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.State
import org.change.v2.analysis.types.LongType
import org.change.v2.analysis.types.NumericType
import org.change.v2.analysis.types.IP4Type
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.analysis.expression.concrete.ConstantValue


/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 */
case class AssignNamedSymbol(id: String, exp: FloatingExpression, t: NumericType = LongType) extends Instruction {
  /**
   *
   * A state processing block produces a set of new states based on a previous one.
   *
   * @param s
   * @return
   */
  override def apply(s: State, v: Boolean): (List[State], List[State]) = {
    exp instantiate  s match {
      case Left(e) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Error during assignment of $id") (s => {
        s.memory.Assign(id, e, t)
      })
      case Right(err) => Fail(err).apply(s, v)
    }
  }
}

case class AssignRaw(a: Intable, exp: FloatingExpression, t: NumericType = LongType) extends Instruction {
  /**
   *
   * A state processing block produces a set of new states based on a previous one.
   *
   * @param s
   * @return
   */
  override def apply(s: State, v: Boolean): (List[State], List[State]) = a(s) match {
    case Some(int) => { exp instantiate  s match {
      case Left(e) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Error during assignment at $a") (s => {
        s.memory.Assign(int, e)
      })
      case Right(err) => Fail(err).apply(s, v)
    }}
    case None => Fail(TagExp.brokenTagExpErrorMessage)(s,v)
  }
}

object Assign {
  def apply(a: Intable, exp: FloatingExpression): Instruction =
    apply(a, exp, LongType)
  def apply(a : Intable, exp : FloatingExpression, kind : NumericType) : Instruction = 
    AssignRaw(a, exp, kind)
  def apply(a : Intable, ip : String) : Instruction = 
    apply(a, ConstantValue(ipToNumber(ip)), IP4Type)
    
  def apply(id : String, exp : FloatingExpression, kind : NumericType) : Instruction = 
    AssignNamedSymbol(id, exp, kind)
  def apply(id: String, exp: FloatingExpression): Instruction =
    apply(id, exp, LongType)
  def apply(id: String, ip : String) : Instruction = 
    apply(id, ConstantValue(ipToNumber(ip)), IP4Type)
}