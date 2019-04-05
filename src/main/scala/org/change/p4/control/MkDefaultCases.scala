package org.change.parser.p4.control

import org.change.v2.p4.model.parser.ReturnSelectStatement

import scala.collection.JavaConverters._

class MkDefaultCases extends ASTVisitor {
  override def postorder(returnSelectStatement: ReturnSelectStatement): Unit = {
    val defaultCase = returnSelectStatement.getCaseEntryList.asScala.filter(_.isDefault)
    if (defaultCase.nonEmpty) {
      val nondefault = returnSelectStatement.getCaseEntryList.asScala.filter(!_.isDefault)
      val defrule = defaultCase.head
      defrule.setNegated(nondefault.toList.asJava)
    }
  }
}
