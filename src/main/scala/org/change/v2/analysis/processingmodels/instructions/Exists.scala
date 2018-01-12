package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.{Intable, State, TagExp}
import org.change.v2.analysis.processingmodels.Instruction

case class ExistsRaw(a : Intable) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val news = if (verbose) s.addInstructionToHistory(this) else s
    a(news) match {
      case Some(int) => news.memory.eval(int) match {
        case Some(_) => (news :: Nil, Nil)
        case None => Fail(s"Address $a doesn't exist")(news, verbose)
      }
      case None => Fail(TagExp.brokenTagExpErrorMessage)(s, verbose)
    }
  }
}
case class ExistsNamedSymbol(symbol : String) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val news = if (verbose) s.addInstructionToHistory(this) else s
    news.memory.eval(symbol) match {
      case Some(_) => (news :: Nil, Nil)
      case None => Fail(s"Symbol $symbol doesn't exist")(news, verbose)
    }
  }
}
case class NotExistsRaw(a : Intable) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val news = if (verbose) s.addInstructionToHistory(this) else s
    a(news) match {
      case Some(int) => news.memory.eval(int) match {
        case Some(_) => Fail(s"Address $a exists")(news, verbose)
        case None => (news :: Nil, Nil)
      }
      case None => Fail(TagExp.brokenTagExpErrorMessage)(s, verbose)
    }
  }
}
case class NotExistsNamedSymbol(symbol : String) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val news = if (verbose) s.addInstructionToHistory(this) else s
    news.memory.eval(symbol) match {
      case Some(_) => Fail(s"Symbol $symbol exists")(news, verbose)
      case None => (news :: Nil, Nil)
    }
  }
}
object NotExists {
  def apply(symbol: String): Instruction = NotExistsNamedSymbol(symbol)
  def apply(intable: Intable): Instruction = NotExistsRaw(intable)
}

object Exists {
  def apply(symbol: String): Instruction = ExistsNamedSymbol(symbol)
  def apply(intable: Intable): Instruction = ExistsRaw(intable)
}