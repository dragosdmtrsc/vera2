package org.change.p4.control

import org.change.p4.model.actions.P4ActionCall
import org.change.p4.model.actions.P4ActionCall.{Param, ParamExpression}
import org.change.p4.model.control._
import org.change.p4.model.control.exp._
import org.change.p4.model.parser._
import org.change.p4.model.table.TableDeclaration
import org.change.p4.model.{ControlBlock, Switch}

import scala.collection.JavaConverters._

class Traverse(visitor: ASTVisitor) {
  def apply(switch: Switch): Unit = {
    if (visitor.preorder(switch)) {
      for (cb <- switch.controlBlocks.asScala) {
        this.apply(cb)
      }
      for (act <- switch.actions.asScala) {
        this.apply(act)
      }
      for (td <- switch.tables().asScala) {
        this.apply(td)
      }
      for (ps <- switch.states().asScala) {
        apply(ps)
      }
      visitor.postorder(switch)
    }
  }
  def apply(ps: State): Unit = {
    if (visitor.preorder(ps)) {
      for (stat <- ps.getStatements.asScala)
        apply(stat)
      visitor.postorder(ps)
    }
  }
  def apply(controlBlock: ControlBlock): Unit = {
    if (visitor.preorder(controlBlock)) {
      apply(controlBlock.getStatement)
      visitor.postorder(controlBlock)
    }
  }
  def apply(tableDeclaration: TableDeclaration): Unit = {
    if (visitor.preorder(tableDeclaration)) {
      visitor.postorder(tableDeclaration)
    }
  }
  def apply(setStatement: SetStatement): Unit = {
    if (visitor.preorder(setStatement)) {
      if (setStatement.getRightE != null)
        apply(setStatement.getRightE)
      visitor.postorder(setStatement)
    }
  }
  def apply(caseNotEntry: CaseNotEntry): Unit = {
    if (visitor.preorder(caseNotEntry)) {
      for (x <- caseNotEntry.getCaseEntryList.asScala)
        apply(x)
      visitor.postorder(caseNotEntry)
    }
  }
  def apply(caseEntry: CaseEntry): Unit = {
    if (visitor.preorder(caseEntry)) {
      caseEntry.getBVExpressions.asScala.foreach(apply)
      apply(caseEntry.getReturnStatement)
      visitor.postorder(caseEntry)
    }
  }
  def apply(returnSelectStatement: ReturnSelectStatement): Unit = {
    if (visitor.preorder(returnSelectStatement)) {
      for (ce <- returnSelectStatement.getCaseEntryList.asScala) {
        apply(ce)
      }
      visitor.postorder(returnSelectStatement)
    }
  }
  def apply(returnStatement: ReturnStatement): Unit = {
    if (visitor.preorder(returnStatement)) {
      visitor.postorder(returnStatement)
    }
  }
  def apply(extractStatement: ExtractStatement): Unit = {
    if (visitor.preorder(extractStatement)) {
      visitor.postorder(extractStatement)
    }
  }
  def apply(emitStatement: EmitStatement): Unit = {
    if (visitor.preorder(emitStatement)) {
      visitor.postorder(emitStatement)
    }
  }

  def apply(statement: ControlStatement): Unit = statement match {
    case b: BlockStatement                  => apply(b)
    case tce: TableCaseEntry                => apply(tce)
    case asel: ApplyAndSelectTableStatement => apply(asel)
    case tabApply: ApplyTableStatement      => apply(tabApply)
    case ifElseStatement: IfElseStatement   => apply(ifElseStatement)
    case applyControlStatement: ApplyControlStatement =>
      apply(applyControlStatement)
    case setStatement: SetStatement   => apply(setStatement)
    case cne: CaseNotEntry            => apply(cne)
    case ce: CaseEntry                => apply(ce)
    case rs: ReturnStatement          => apply(rs)
    case rss: ReturnSelectStatement   => apply(rss)
    case es: ExtractStatement         => apply(es)
    case emitStatement: EmitStatement => apply(emitStatement)
  }

  def apply(blockStatement: BlockStatement): Unit = {
    if (visitor.preorder(blockStatement)) {
      for (stat <- blockStatement.getStatements.asScala) {
        apply(stat)
      }
      visitor.postorder(blockStatement)
    }
  }
  def apply(p4BExpr: P4BExpr): Unit = p4BExpr match {
    case n: NegBExpr      => apply(n)
    case bin: BinBExpr    => apply(bin)
    case lit: LiteralBool => apply(lit)
    case rel: RelOp       => apply(rel)
    case vr: ValidRef     => apply(vr)
  }
  def apply(ifElseStatement: IfElseStatement): Unit = {
    if (visitor.preorder(ifElseStatement)) {
      apply(ifElseStatement.getCondition)
      apply(ifElseStatement.getIfbranch)
      if (ifElseStatement.getElseBranch != null)
        apply(ifElseStatement.getElseBranch)
      visitor.postorder(ifElseStatement)
    }
  }
  def apply(tableCaseEntry: TableCaseEntry): Unit = {
    if (visitor.preorder(tableCaseEntry)) {
      if (tableCaseEntry.getStatement != null)
        apply(tableCaseEntry.getStatement)
      visitor.postorder(tableCaseEntry)
    }
  }
  def apply(applyTableStatement: ApplyTableStatement): Unit = {
    if (visitor.preorder(applyTableStatement)) {
      visitor.postorder(applyTableStatement)
    }
  }
  def apply(
    applyAndSelectTableStatement: ApplyAndSelectTableStatement
  ): Unit = {
    if (visitor.preorder(applyAndSelectTableStatement)) {
      applyAndSelectTableStatement.getCaseEntries.asScala.foreach(apply)
      visitor.postorder(applyAndSelectTableStatement)
    }
  }
  def apply(applyControlStatement: ApplyControlStatement): Unit = {
    if (visitor.preorder(applyControlStatement)) {
      visitor.postorder(applyControlStatement)
    }
  }

  def apply(validRef: ValidRef): Unit = {
    if (visitor.preorder(validRef))
      visitor.postorder(validRef)
  }
  def apply(negBExpr: NegBExpr): Unit = {
    if (visitor.preorder(negBExpr)) {
      apply(negBExpr.getExpr)
      visitor.postorder(negBExpr)
    }
  }
  def apply(binBExpr: BinBExpr): Unit = {
    if (visitor.preorder(binBExpr)) {
      apply(binBExpr.getLeft)
      apply(binBExpr.getRight)
      visitor.postorder(binBExpr)
    }
  }
  def apply(literalBool: LiteralBool): Unit = {
    if (visitor.preorder(literalBool))
      visitor.postorder(literalBool)
  }
  def apply(relOp: RelOp): Unit = {
    if (visitor.preorder(relOp)) {
      apply(relOp.getLeft)
      apply(relOp.getRight)
      visitor.postorder(relOp)
    }
  }
  def apply(p4Expr: P4Expr): Unit = p4Expr match {
    case l: LiteralExpr   => apply(l)
    case be: BinExpr      => apply(be)
    case uo: UnOpExpr     => apply(uo)
    case fr: FieldRefExpr => apply(fr)
    case dre: DataRefExpr => apply(dre)
  }
  def apply(dre: DataRefExpr): Unit = {
    if (visitor.preorder(dre)) {
      visitor.postorder(dre)
    }
  }
  def apply(literalExpr: LiteralExpr): Unit = {
    if (visitor.preorder(literalExpr)) {
      visitor.postorder(literalExpr)
    }
  }
  def apply(binExpr: BinExpr): Unit = {
    if (visitor.preorder(binExpr)) {
      apply(binExpr.getLeft)
      apply(binExpr.getRight)
      visitor.postorder(binExpr)
    }
  }
  def apply(unOpExpr: UnOpExpr): Unit = {
    if (visitor.preorder(unOpExpr)) {
      apply(unOpExpr.getExpr)
      visitor.postorder(unOpExpr)
    }
  }
  def apply(fieldRefExpr: FieldRefExpr): Unit = {
    if (visitor.preorder(fieldRefExpr)) {
      visitor.postorder(fieldRefExpr)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Add): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.AddHeader): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.AddToField): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.BitAnd): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.BitOr): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.BitXor): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.CloneEgressPktToEgress
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.CloneEgressPktToIngress
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.CloneIngressPktToEgress
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.CloneIngressPktToIngress
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.CopyHeader): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Count): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Drop): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.ExecuteMeter
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.GenerateDigest
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.ModifyField
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.ModifyFieldRngUniform
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.NoOp): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Pop): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Push): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.Recirculate
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.RegisterRead
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.RegisterWrite
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.RemoveHeader
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Resubmit): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.ShiftLeft): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.ShiftRight): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Subtract): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(
    action: org.change.p4.model.actions.primitives.SubtractFromField
  ): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.primitives.Truncate): Unit = {
    if (visitor.preorder(action)) {
      visitor.postorder(action)
    }
  }
  def apply(param: Param): Unit = param match {
    case p: ParamExpression => apply(p)
  }
  def apply(paramExpression: ParamExpression): Unit = {
    if (visitor.preorder(paramExpression)) {
      visitor.postorder(paramExpression)
    }
  }
  def apply(call: P4ActionCall): Unit = {
    if (visitor.preorder(call)) {
      for (p <- call.params().asScala) {
        apply(p)
      }
      visitor.postorder(call)
    }
  }
  def apply(action: org.change.p4.model.actions.P4ComplexAction): Unit = {
    if (visitor.preorder(action)) {
      for (call <- action.getActionList.asScala) {
        apply(call)
      }
      visitor.postorder(action)
    }
  }
  def apply(action: org.change.p4.model.actions.P4Action): Unit = action match {
    case ax: org.change.p4.model.actions.primitives.Add        => apply(ax)
    case ax: org.change.p4.model.actions.primitives.AddHeader  => apply(ax)
    case ax: org.change.p4.model.actions.primitives.AddToField => apply(ax)
    case ax: org.change.p4.model.actions.primitives.BitAnd     => apply(ax)
    case ax: org.change.p4.model.actions.primitives.BitOr      => apply(ax)
    case ax: org.change.p4.model.actions.primitives.BitXor     => apply(ax)
    case ax: org.change.p4.model.actions.primitives.CloneEgressPktToEgress =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.CloneEgressPktToIngress =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.CloneIngressPktToEgress =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.CloneIngressPktToIngress =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.CopyHeader     => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Count          => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Drop           => apply(ax)
    case ax: org.change.p4.model.actions.primitives.ExecuteMeter   => apply(ax)
    case ax: org.change.p4.model.actions.primitives.GenerateDigest => apply(ax)
    case ax: org.change.p4.model.actions.primitives.ModifyField    => apply(ax)
    case ax: org.change.p4.model.actions.primitives.ModifyFieldRngUniform =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.NoOp          => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Pop           => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Push          => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Recirculate   => apply(ax)
    case ax: org.change.p4.model.actions.primitives.RegisterRead  => apply(ax)
    case ax: org.change.p4.model.actions.primitives.RegisterWrite => apply(ax)
    case ax: org.change.p4.model.actions.primitives.RemoveHeader  => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Resubmit      => apply(ax)
    case ax: org.change.p4.model.actions.primitives.ShiftLeft     => apply(ax)
    case ax: org.change.p4.model.actions.primitives.ShiftRight    => apply(ax)
    case ax: org.change.p4.model.actions.primitives.Subtract      => apply(ax)
    case ax: org.change.p4.model.actions.primitives.SubtractFromField =>
      apply(ax)
    case ax: org.change.p4.model.actions.primitives.Truncate => apply(ax)
    case ax: org.change.p4.model.actions.P4ComplexAction     => apply(ax)
  }
}

object Traverse {
  def apply(visitor: ASTVisitor): Traverse = new Traverse(visitor)
}
