package org.change.p4.control

import org.change.p4.model.Switch
import org.change.p4.model.actions.{P4ActionCall, P4ActionType}
import org.change.p4.model.control.{ApplyAndSelectTableStatement, ApplyControlStatement, ApplyTableStatement}
import org.change.p4.model.parser._
import org.change.p4.model.table.TableDeclaration

import scala.collection.JavaConverters._

class SolveTables(switch: Switch) extends ASTVisitor {
  override def postorder(applyControlStatement: ApplyControlStatement): Unit = {
    applyControlStatement.setBlock(
      switch.controlBlock(applyControlStatement.getBlock.getName)
    )
  }
  override def postorder(applyTableStatement: ApplyTableStatement): Unit = {
    applyTableStatement.setTable(
      switch.table(applyTableStatement.getTable.getName)
    )
  }

  def solve(expr: Expression): Unit = expr match {
    case hr: HeaderRef =>
      if (hr.getInstance() == null) {
        val inst = switch.getInstance(hr.getPath)
        if (inst == null)
          throw new NoSuchFieldError(s"can't resolve header ${hr.getPath}")
        hr.setInstance(inst)
      }
    case fr: FieldRef =>
      solve(fr.getHeaderRef)
      val fld = fr.getHeaderRef.getInstance().getLayout.getField(fr.getField)
      if (fld == null) {
        throw new NoSuchFieldError(
          s"can't resolve field ${fr.getField} in header ${fr.getHeaderRef.getInstance()}"
        )
      }
      fr.setFieldReference(fld)
    case maskedFieldRef: MaskedFieldRef => solve(maskedFieldRef.getReference)
  }

  override def postorder(tableDeclaration: TableDeclaration): Unit = {
    tableDeclaration.getMatches.asScala.foreach(m => {
      solve(m.getReferenceKey)
    })
    if (tableDeclaration.hasProfile) {
      //bind reference to real object
      tableDeclaration.setProfile(
        switch.getProfile(tableDeclaration.actionProfile().getName)
      )
      if (!tableDeclaration.actionProfile.getActions.contains("no_op")) {
        tableDeclaration.actionProfile.getActions.add("no_op")
      }
    } else {
      if (!tableDeclaration.getAllowedActions.asScala.exists(
            p => p.getActionType == P4ActionType.NoOp
          )) {
        tableDeclaration.addAction(switch.action("no_op"))
      }
    }
  }
  override def postorder(extractStatement: ExtractStatement): Unit =
    solve(extractStatement.getExpression)

  override def postorder(
    applyAndSelectTableStatement: ApplyAndSelectTableStatement
  ): Unit = {
    applyAndSelectTableStatement.setTable(
      switch.table(applyAndSelectTableStatement.getTable.getName)
    )
    applyAndSelectTableStatement.createDefault()
    applyAndSelectTableStatement.getCaseEntries.asScala.foreach(ce => {
      ce.setTableDeclaration(applyAndSelectTableStatement.getTable)
    })
  }

  override def postorder(actCall: P4ActionCall): Unit = {
    val act = actCall.getP4Action
    if (act.getActionType == P4ActionType.UNKNOWN) {
      val theAct = switch.action(act.getActionName)
      if (theAct == null)
        throw new IllegalArgumentException(act.getActionName + " action undeclared")
      actCall.setAction(theAct)
    }
  }
}
object SolveTables {
  def apply(switch: Switch): Switch = {
    Traverse(new ResolveLatest)(switch)
    Traverse(new SolveTables(switch))(switch)
    Traverse(new ParmRefInference(switch))(switch)
    Traverse(new ParmTypeInference(switch))(switch)
    Traverse(new LiteralTypeInference(switch))(switch)
    Traverse(new SplitCaseEntries(switch))(switch)
    Traverse(new MkCaseEntryLiteralWidth(switch))(switch)
    Traverse(new MkDefaultCases)(switch)
    switch
  }
}
