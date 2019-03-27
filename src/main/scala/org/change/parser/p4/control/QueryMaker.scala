package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp._

import scala.collection.mutable

case class QueryMaker(context : P4Memory,
                      exprs : mutable.Map[Object, P4Query]) extends ASTVisitor {
  override def postorder(validRef: ValidRef): Unit = {
    exprs.put(validRef, context.field(validRef.getHref.getPath).valid())
  }

  override def postorder(negBExpr: NegBExpr): Unit = {
    val last = exprs(negBExpr.getExpr)
    exprs.put(negBExpr, !last)
  }

  override def postorder(binBExpr: BinBExpr): Unit = {
    val l = exprs(binBExpr.getLeft)
    val r = exprs(binBExpr.getRight)
    exprs.put(binBExpr, binBExpr.getType match {
      case BinBExpr.OpType.AND => l && r
      case BinBExpr.OpType.OR => l || r
    })
  }

  override def postorder(literalBool: LiteralBool): Unit =
    exprs.put(literalBool, LiteralBoolValue(literalBool.value()))

  override def postorder(relOp: RelOp): Unit = super.postorder(relOp)

  override def postorder(literalExpr: LiteralExpr): Unit =
    exprs.put(literalExpr, LiteralExprValue(literalExpr.getValue, literalExpr.getWidth))

  override def postorder(binExpr: BinExpr): Unit = {
    val l = exprs(binExpr.getLeft)
    val r = exprs(binExpr.getRight)
    exprs.put(binExpr, l)
  }

  override def postorder(unOpExpr: UnOpExpr): Unit = super.postorder(unOpExpr)

  override def postorder(fieldRefExpr: FieldRefExpr): Unit = super.postorder(fieldRefExpr)

  override def postorder(dre: DataRefExpr): Unit = super.postorder(dre)
}
