package org.change.v2.analysis.constraint

import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.MemoryObject

trait Condition
case class OP(expression : Expression, constraint: Constraint, size : Int) extends Condition
case class FAND(conditions : List[Condition]) extends Condition
case class FOR(conditions : List[Condition]) extends Condition
case class FNOT(condition : Condition) extends Condition
object TRUE extends Condition
object FALSE extends Condition

object FOR {
  def makeFOR(cds : Iterable[Condition]): Condition =
    if (cds.isEmpty)
      FALSE
    else if (cds.exists(_ == TRUE))
      TRUE
    else if (cds.size == 1)
      cds.head
    else {
      val flatm = cds.collect {
        case FOR(cs) => cs
        case FALSE => Nil
        case v : Condition => v :: Nil
      }.flatten
      if (cds.size == flatm.size) {
        new FOR(flatm.toList)
      } else {
        FOR.apply(flatm.toList)
      }
    }
}

object FAND {
  def makeFAND(cds : Iterable[Condition]) : Condition = if (cds.isEmpty)
    TRUE
  else if (cds.exists(_ == FALSE))
    FALSE
  else if (cds.size == 1)
    cds.head
  else {
    val flatm = cds.collect {
      case TRUE => Nil
      case FAND(cs) => cs
      case v : Condition => v :: Nil
    }.flatten
    if (cds.size == flatm.size) {
      new FAND(flatm.toList)
    } else {
      FAND.apply(flatm.toList)
    }
  }
}

object FNOT {

  def makeFNOT(condition: Condition) : Condition = condition match {
    case o : OP => new FNOT(o)
    case FAND(conditions) => FOR.apply(conditions.map(makeFNOT))
    case FOR(conditions) => FAND.apply(conditions.map(makeFNOT))
    case FNOT(condition) => condition
    case TRUE => FALSE
    case FALSE => TRUE
    case _ => ???
  }
}