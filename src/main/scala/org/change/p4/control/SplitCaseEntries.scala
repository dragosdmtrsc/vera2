package org.change.p4.control

import org.change.p4.model.Switch
import org.change.p4.model.control.exp.{DataRefExpr, FieldRefExpr, LiteralExpr, P4Expr}
import org.change.p4.model.parser.CaseEntry

import scala.collection.JavaConverters._

class SplitCaseEntries(switch: Switch) extends ASTVisitor {

  def splitANumber(caseEntry: CaseEntry, v: BigInt): List[LiteralExpr] = {
    var currentNumber = v
    var listOfNumbers = List.empty[LiteralExpr]
    caseEntry.getBVExpressions.asScala.reverse.foreach {
      case fieldRefExpr: FieldRefExpr =>
        val len = fieldRefExpr.getFieldRef.getFieldReference.getLength
        val bigLen = BigInt(len)
        val nr = ((BigInt(1) << len) - 1) & currentNumber
        val newlit = new LiteralExpr(nr, len)
        currentNumber = currentNumber >> len
        listOfNumbers = newlit :: listOfNumbers
      case dataRefExpr: DataRefExpr =>
        val len = dataRefExpr.getDataRef.getEnd.toInt
        val bigLen = BigInt(len)
        val nr = ((BigInt(1) << len) - 1) & currentNumber
        val newlit = new LiteralExpr(nr, len)
        currentNumber = currentNumber >> len
        listOfNumbers = newlit :: listOfNumbers
      case x: P4Expr =>
        throw new IllegalArgumentException(
          s"can't handle anything but field ref exprs, got $x"
        )
    }
    if (currentNumber != 0 && currentNumber != -1) {
      throw new AssertionError(
        s"unconsumed number $v -> only got to $currentNumber"
      )
    }
    listOfNumbers
  }

  override def postorder(caseEntry: CaseEntry): Unit = {
    if (!caseEntry.isDefault && caseEntry.getBVExpressions
          .size() != caseEntry.getBvValues.size()) {
      if (caseEntry.getBvValues.size() != 1)
        throw new AssertionError(
          "values size must be 1, got " + caseEntry.getBvValues
        )
      var currentNumber = caseEntry.getBvValues.get(0).getValue
      val vals = splitANumber(caseEntry, caseEntry.getBvValues.get(0).getValue)
      val masks = splitANumber(caseEntry, caseEntry.getBvMasks.get(0).getValue)
      if (vals.size != caseEntry.getBVExpressions.size())
        throw new AssertionError(
          vals +
            " must be of equal size to " + caseEntry.getBVExpressions
        )
      if (masks.size != caseEntry.getBVExpressions.size()) {
        throw new AssertionError(
          masks +
            " must be of equal size to " + caseEntry.getBVExpressions
        )
      }
      caseEntry.setBvValues(vals.asJava)
      caseEntry.setBvMasks(masks.asJava)
    }
  }
}
