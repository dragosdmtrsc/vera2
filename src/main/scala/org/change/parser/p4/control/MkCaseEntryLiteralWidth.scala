package org.change.parser.p4.control

import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control.exp.{DataRefExpr, FieldRefExpr}
import org.change.v2.p4.model.parser.CaseEntry

import scala.collection.JavaConverters._

class MkCaseEntryLiteralWidth(switch: Switch) extends ASTVisitor {
  override def postorder(caseEntry: CaseEntry): Unit = {
    if (!caseEntry.isDefault) {
      if (caseEntry.getBVExpressions.size() != caseEntry.getBvValues.size()) {
        throw new AssertionError(caseEntry.getBVExpressions + " must be of equal size to " +
          caseEntry.getBvValues)
      }
      caseEntry.getBVExpressions.asScala.zipWithIndex.foreach(idxd => {
        val (e, idx) = idxd
        val v = caseEntry.getBvValues.get(idx)
        val m = caseEntry.getBvMasks.get(idx)
        val flen : Long = e match {
          case dr : DataRefExpr =>
            dr.getDataRef.getEnd - dr.getDataRef.getStart
          case fr : FieldRefExpr =>
            fr.getFieldRef.getFieldReference.getLength
          case _ => throw new IllegalStateException(s"cannot have $e as a match")
        }
        v.setWidth(flen.toInt)
        m.setWidth(flen.toInt)
      })
    }
  }
}
