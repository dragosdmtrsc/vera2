package org.change.parser.p4.control

import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.control._
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.table.TableDeclaration
import org.change.v2.p4.model.{ControlBlock, Switch}

trait ASTVisitor {
  def preorder(switch: Switch) : Boolean = true
  def preorder(controlBlock: ControlBlock) : Boolean = true
  def preorder(tableDeclaration: TableDeclaration) : Boolean = true
  def preorder(ps: State) : Boolean = true
  def preorder(actCall : org.change.v2.p4.model.actions.P4ActionCall) : Boolean = true
  def preorder(parm : ParamExpression) : Boolean = true

  def preorder(blockStatement: BlockStatement) : Boolean = true
  def preorder(ifElseStatement: IfElseStatement) : Boolean = true
  def preorder(tableCaseEntry: TableCaseEntry) : Boolean = true
  def preorder(applyTableStatement: ApplyTableStatement) : Boolean = true
  def preorder(applyAndSelectTableStatement: ApplyAndSelectTableStatement) : Boolean = true
  def preorder(applyControlStatement: ApplyControlStatement) : Boolean = true

  def preorder(validRef: ValidRef) : Boolean = true
  def preorder(negBExpr: NegBExpr) : Boolean = true
  def preorder(binBExpr: BinBExpr) : Boolean = true
  def preorder(literalBool : LiteralBool) : Boolean = true
  def preorder(relOp: RelOp) : Boolean = true

  def preorder(literalExpr: LiteralExpr) : Boolean = true
  def preorder(binExpr: BinExpr) : Boolean = true
  def preorder(unOpExpr: UnOpExpr) : Boolean = true
  def preorder(fieldRefExpr: FieldRefExpr) : Boolean = true
  def preorder(dre: DataRefExpr) : Boolean = true

  def postorder(switch: Switch) :Unit = {}
  def postorder(controlBlock: ControlBlock) :Unit= {}
  def postorder(tableDeclaration: TableDeclaration) :Unit= {}
  def postorder(ps: State) :Unit= {}

  def postorder(blockStatement: BlockStatement) :Unit = {}
  def postorder(ifElseStatement: IfElseStatement) :Unit = {}
  def postorder(tableCaseEntry: TableCaseEntry) :Unit = {}
  def postorder(applyTableStatement: ApplyTableStatement) :Unit = {}
  def postorder(applyAndSelectTableStatement: ApplyAndSelectTableStatement) :Unit = {}
  def postorder(applyControlStatement: ApplyControlStatement) :Unit = {}

  def postorder(validRef: ValidRef) :Unit = {}
  def postorder(negBExpr: NegBExpr) :Unit = {}
  def postorder(binBExpr: BinBExpr) :Unit = {}
  def postorder(literalBool : LiteralBool) :Unit = {}
  def postorder(relOp: RelOp) :Unit = {}

  def postorder(literalExpr: LiteralExpr) :Unit = {}
  def postorder(binExpr: BinExpr) :Unit = {}
  def postorder(unOpExpr: UnOpExpr) :Unit = {}
  def postorder(fieldRefExpr: FieldRefExpr) :Unit = {}
  def postorder(dre: DataRefExpr) :Unit = {}
  def postorder(actCall : org.change.v2.p4.model.actions.P4ActionCall) : Unit = {}
  def postorder(paramExpression: ParamExpression) : Unit = {}

  def preorder(action: org.change.v2.p4.model.actions.primitives.Add) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.AddHeader) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.AddToField) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.BitAnd) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.BitOr) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.BitXor) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.CloneEgressPktToEgress) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.CloneEgressPktToIngress) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.CloneIngressPktToEgress) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.CloneIngressPktToIngress) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.CopyHeader) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Count) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Drop) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ExecuteMeter) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.GenerateDigest) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ModifyField) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ModifyFieldRngUniform) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.NoOp) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Pop) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Push) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Recirculate) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.RegisterRead) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.RegisterWrite) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.RemoveHeader) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Resubmit) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ShiftLeft) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.ShiftRight) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Subtract) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.SubtractFromField) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.primitives.Truncate) : Boolean = true
  def preorder(action: org.change.v2.p4.model.actions.P4ComplexAction) : Boolean = true
  def postorder(action: org.change.v2.p4.model.actions.primitives.Add) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.AddHeader) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.AddToField) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.BitAnd) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.BitOr) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.BitXor) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.CloneEgressPktToEgress) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.CloneEgressPktToIngress) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.CloneIngressPktToEgress) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.CloneIngressPktToIngress) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.CopyHeader) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Count) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Drop) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ExecuteMeter) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.GenerateDigest) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ModifyField) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ModifyFieldRngUniform) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.NoOp) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Pop) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Push) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Recirculate) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.RegisterRead) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.RegisterWrite) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.RemoveHeader) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Resubmit) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ShiftLeft) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.ShiftRight) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Subtract) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.SubtractFromField) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.primitives.Truncate) : Unit = {}
  def postorder(action: org.change.v2.p4.model.actions.P4ComplexAction) : Unit = {}

  def postorder(setStatement: SetStatement) : Unit = {}
  def postorder(caseNotEntry: CaseNotEntry) : Unit = {}
  def postorder(caseEntry: CaseEntry) : Unit = {}
  def postorder(returnSelectStatement: ReturnSelectStatement) : Unit = {}
  def postorder(returnStatement: ReturnStatement) : Unit = {}
  def postorder(extractStatement: ExtractStatement) : Unit = {}
  def postorder(emitStatement: EmitStatement) : Unit = {}

  def preorder(setStatement: SetStatement) : Boolean = true
  def preorder(caseNotEntry: CaseNotEntry) : Boolean = true
  def preorder(caseEntry: CaseEntry) : Boolean = true
  def preorder(returnSelectStatement: ReturnSelectStatement) : Boolean = true
  def preorder(returnStatement: ReturnStatement) : Boolean = true
  def preorder(extractStatement: ExtractStatement) : Boolean = true
  def preorder(emitStatement: EmitStatement) : Boolean = true
}
