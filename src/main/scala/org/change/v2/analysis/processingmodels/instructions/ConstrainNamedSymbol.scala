package org.change.v2.analysis.processingmodels.instructions


import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.memory.{Intable, State, TagExp}
import org.change.v2.analysis.processingmodels.Instruction


/**
 * Author: Radu Stoenescu
 * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
 *
 * TODO: When an already instantiated constrain is provided, one should be able not to provide the
 * floating one (This is also going to affect how the toString method works)
 */
case class ConstrainNamedSymbol (id: String, dc: FloatingConstraint, c: Option[Constraint] = None) extends Instruction {
  override def apply(s: State, v: Boolean): (List[State], List[State]) = c match {
    case None => dc instantiate s match {
      case Left(c) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Symbol $id cannot $dc")(s => {
        s.memory.addConstraint(id, c)
      })
      case Right(err) => Fail(err)(s, v)
    }
    case Some(c) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, s"Symbol $id cannot $dc")(s => {
      s.memory.addConstraint(id, c)
    })
  }
  override def toString = s"Constrain($id, $dc)"
  def not() : ConstrainNamedSymbol = c match {
    case None => ConstrainNamedSymbol(id, :~:(dc))
    case Some(c) => ConstrainNamedSymbol(id, :~:(dc), Some(NOT(c)))
  }
}

case class ConstrainFloatingExpression(floatingExpression: FloatingExpression,
                                       dc : FloatingConstraint) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = ???
}

case class ConstrainRaw (a: Intable, dc: FloatingConstraint, c: Option[Constraint] = None) extends Instruction {
  override def apply(s: State, v: Boolean): (List[State], List[State]) = a(s) match {
    case Some(int) => c match {
        case None => dc instantiate s match {
          case Left(c) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, TagExp.brokenTagExpErrorMessage)(s => {
            if (s.memory.eval(int) != None) Some(s.memory) else None
          }) match {
            case (List(st),Nil) => optionToStatePair(st,s"Memory object @ $a cannot $dc") (s => {
              s.memory.addConstraint(int, c)
            })
            case x => x
          }

          case Right(err) => Fail(err)(s, v)
        }
        case Some(c) => optionToStatePair(if (v) s.addInstructionToHistory(this) else s, TagExp.brokenTagExpErrorMessage)(s => {
          if (s.memory.eval(int) != None) Some(s.memory) else None
        }) match {
          case (List(st),Nil) => optionToStatePair(st,s"Memory object @ $a cannot $dc") (s => {
            s.memory.addConstraint(int, c)
          })
          case x => x
        }
      }
    case None => Fail(TagExp.brokenTagExpErrorMessage)(s,v)
  }

  override def toString = s"Constrain($a, $dc)"


  def not() : ConstrainRaw = c match {
    case None => ConstrainRaw(a, :~:(dc))
    case Some(c) => ConstrainRaw(a, :~:(dc), Some(NOT(c)))
  }
}

object Constrain {
  def apply(id: String, dc: FloatingConstraint): Instruction =
    ConstrainNamedSymbol(id, dc, None)
  def apply (a: Intable, dc: FloatingConstraint): Instruction =
    ConstrainRaw(a, dc, None)

  def apply(e : FloatingExpression, dc : FloatingConstraint): ConstrainFloatingExpression =
    ConstrainFloatingExpression(e, dc)

  def apply(a: Either[String, Intable], dc: FloatingConstraint): Instruction =
    a match {
      case Left(s) => apply(s, dc)
      case Right(s) => apply(s, dc)
    }
}

object Assert {
  def apply(id: String, dc: FloatingConstraint): Instruction =
    If(ConstrainNamedSymbol(id, dc, None), NoOp, Fail(s"Assertion failure:$id $dc.toString"))

  def apply(a: Intable, dc: FloatingConstraint): Instruction =
    If(ConstrainRaw(a, dc, None), NoOp, Fail(s"Assertion failure:$a $dc.toString"))

  def apply(a: Either[String, Intable], dc: FloatingConstraint): Instruction =
    a match {
      case Left(s) => apply(s, dc)
      case Right(s) => apply(s, dc)
    }
}


trait FloatingConstraint {
  def instantiate(s: State): Either[Constraint, String]
}

object :|: {
  def apply(cstrs: List[FloatingConstraint]): FloatingConstraint = {
    cstrs match {
      case head :: Nil => head
      case head :: tail => :|:(head, apply(tail))
      case _ => throw new UnsupportedOperationException("Cannot support empty list of constraints")
    }
  }
}

case class :|:(a: FloatingConstraint, b: FloatingConstraint) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = a instantiate s match {
    case Left(ac) => b instantiate s match {
      case Left(bc) => Left(OR(List(ac, bc)))
      case err @ Right(_) => err
    }
    case err @ Right(_) => err
  }
}

case class :&:(a: FloatingConstraint, b: FloatingConstraint) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = a instantiate s match {
    case Left(ac) => b instantiate s match {
      case Left(bc) => Left(AND(List(ac, bc)))
      case err @ Right(_) => err
    }
    case err @ Right(_) => err
  }
}


// TODO: Please update the new executor model with this class in the big ol' case

case class Yes() extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = {
    Left(new Truth())
  }
}


case class :~:(c: FloatingConstraint) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = c instantiate s match {
    case Left(c) => Left(NOT(c))
    case err @ Right(_) => err
  }
}

case class :==:(exp: FloatingExpression) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = exp instantiate s match {
    case Left(e) => Left(EQ_E(e))
    case Right(err) => Right(err)
  }
}

case class :<:(exp: FloatingExpression) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = exp instantiate s match {
    case Left(e) => Left(LT_E(e))
    case Right(err) => Right(err)
  }
}

case class :<=:(exp: FloatingExpression) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = exp instantiate s match {
    case Left(e) => Left(LTE_E(e))
    case Right(err) => Right(err)
  }
}

case class :>:(exp: FloatingExpression) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = exp instantiate s match {
    case Left(e) => Left(GT_E(e))
    case Right(err) => Right(err)
  }
}

case class :>=:(exp: FloatingExpression) extends FloatingConstraint {
  override def instantiate(s: State): Either[Constraint, String] = exp instantiate s match {
    case Left(e) => Left(GTE_E(e))
    case Right(err) => Right(err)
  }
}
