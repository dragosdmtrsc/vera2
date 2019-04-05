package org.change.parser.p4.control.queryimpl

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ErrorLedger {
  private val errorMap = mutable.Map("" -> 0)
  private val errors = ListBuffer("")

  def errorIndex(err : String) : Int = {
    val idx = errorMap.getOrElseUpdate(err, errors.size)
    if (idx == errors.size)
      errors.append(err)
    idx
  }
  def error(idx : Int): String = {
    if (idx >= 0 && idx < errors.size)
      errors(idx)
    else "unknown error"
  }
}
