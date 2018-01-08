package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{Address, Concat}
import org.change.v2.analysis.memory.{Intable, MemoryObject, State, TagExp}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.types.{IP4Type, LongType, NumericType}
import org.change.v2.util.conversion.RepresentationConversion._

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
    exp instantiate s match {
      case Left(e) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Error during assignment of $id")(s => {
        s.memory.Assign(id, e, t)
      })
      case Right(err) => Fail(err).apply(s, v)
    }
  }

  override def toString = s"$id <- $exp"

}

case class AssignRaw(a: Intable, exp: FloatingExpression,
                     t: NumericType = LongType) extends Instruction {
  /**
    *
    * A state processing block produces a set of new states based on a previous one.
    *
    * @param s
    * @return
    */
  override def apply(s: State, v: Boolean): (List[State], List[State]) = a(s) match {
    case Some(int) => {
      exp instantiate s match {
        case Left(e) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Error during assignment at $a")(s => {
          s.memory.Assign(int, e)
        })
        case Right(err) => Fail(err).apply(s, v)
      }
    }
    case None => Fail(TagExp.brokenTagExpErrorMessage)(s, v)
  }
  override def toString = s"$a <- $exp"
}


case class AssignNamedSymbolWithLength(id: String, exp : Address, width : Int) extends Instruction {
  def concatenate(lst : List[MemoryObject], width : Int, consumed : List[MemoryObject], s : State) : (List[State], List[State]) = {
    if (width == 0) {
      s.memory.assignNewValue(id, Concat(consumed), LongType) match {
        case Some(mem) => (s.copy(memory = mem) :: Nil, Nil)
        case None => Fail(s"Couldn't assign $id with value ${Concat(consumed)}")(s, true)
      }
    } else {
      if (lst.isEmpty)
        Fail("Cannot cover width")(s, true)
      else if (width > 0) {
        concatenate(lst.tail, width - lst.head.size, consumed :+ lst.head, s)
      } else {
        Fail("Cannot cover width")(s, true)
      }
    }
  }

  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = exp.a(s) match {
    case Some(int) =>  val news = s.addInstructionToHistory(this)
      news.memory.evalToObject(int) match {
        case Some(memoryObject) => memoryObject.value match {
          case Some(value) => concatenate(s.memory.rawObjects.filter(r => {
            r._1 >= int
          }).toList.sortBy(f => f._1).map(r => r._2), width, consumed = Nil, s)
          case None => Fail(s"No object at $int")(s, verbose)
        }
        case None => Fail(s"No object at $int")(s, verbose)
      }
    case None => Fail(TagExp.brokenTagExpErrorMessage)(s, verbose)
  }
}

object Assign {

  def apply(a: Either[String, Intable], exp: FloatingExpression): Instruction = {
    apply(a, exp, LongType)
  }

  def apply(a: Either[String, Intable], exp: FloatingExpression, kind: NumericType): Instruction =
    a match {
      case Left(x) => apply(x, exp, kind)
      case Right(x) => apply(x, exp, kind)
    }

  def apply(id: String, exp : Address, width : Int) : Instruction = AssignNamedSymbolWithLength(id, exp, width)


  def apply(a: Intable, exp: FloatingExpression): Instruction =
    apply(a, exp, LongType)

  def apply(a: Intable, exp: FloatingExpression, kind: NumericType): Instruction =
    AssignRaw(a, exp, kind)

  def apply(a: Intable, ip: String): Instruction =
    apply(a, ConstantValue(ipToNumber(ip)), IP4Type)

  def apply(id: String, exp: FloatingExpression, kind: NumericType): Instruction =
    AssignNamedSymbol(id, exp, kind)

  def apply(id: String, exp: FloatingExpression): Instruction =
    apply(id, exp, LongType)

  def apply(id: String, exp: Int): Instruction =
    apply(id, ConstantValue(exp))

  def apply(a: Intable, exp: Int): Instruction = apply(a, ConstantValue(exp))

  def apply(id: String, ip: String): Instruction =
    apply(id, ConstantValue(ipToNumber(ip)), IP4Type)
}