package org.change.parser.p4.control

import org.change.v2.p4.model.control.{ApplyAndSelectTableStatement, ApplyTableStatement, ControlStatement, TableCaseEntry}
import org.change.v2.p4.model.table.TableDeclaration
import org.change.v3.syntax._

import scala.collection.JavaConverters._
import scala.collection.mutable

class ControlStatementToSEFL(imap : mutable.Map[ControlStatement, Instruction] =
                             mutable.Map.empty) extends ASTVisitor {

  def handleTableCall(tableDeclaration: TableDeclaration) : Instruction = {
    ???
  }

  override def postorder(applyTableStatement: ApplyTableStatement) : Unit = {
    imap.put(applyTableStatement, handleTableCall(applyTableStatement.getTable))
  }
  override def postorder(applyTableStatement: ApplyAndSelectTableStatement) : Unit = {
    imap.put(applyTableStatement, handleTableCall(applyTableStatement.getTable))
  }
  def seflize(tableCaseEntry : TableCaseEntry) : BExpr = {
    if (tableCaseEntry.defaultCase()) {
      LNot(LOr(tableCaseEntry.getNegated.asScala.map(seflize)))
    } else {
      if (tableCaseEntry.isHitMiss) {
        if (tableCaseEntry.hit()) {
          EQ(Symbol("hit"), Literal(1))
        } else if (tableCaseEntry.miss()) {
          EQ(Symbol("hit"), Literal(0))
        } else {
          assert(false)
          BoolLiteral(false)
        }
      } else {
        EQ(Symbol("action"), Literal(tableCaseEntry.action().hashCode))
      }
    }
  }
  override def postorder(tableCaseEntry : TableCaseEntry) : Unit = {
    imap.put(tableCaseEntry, Assume(seflize(tableCaseEntry)))
  }
}
