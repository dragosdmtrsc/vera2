package org.change.parser.p4.control

import org.change.v2.p4.model.control.exp._
import org.change.parser.p4.control.SMInstantiator._
import org.change.plugins.vera.BVType

import scala.collection.mutable

case class QueryMaker(context : P4Memory,
                      exprs : mutable.Map[Object, P4Query]) extends ASTVisitor {
  override def postorder(validRef: ValidRef): Unit = {
    val ct = context(validRef.getHref)
    exprs.put(validRef, ct.valid())
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
    exprs.put(literalBool, context.boolVal(literalBool.value()))

  override def postorder(relOp: RelOp): Unit = {
    val l = exprs(relOp.getLeft)
    val r = exprs(relOp.getRight)
    exprs.put(relOp, relOp.getType match {
      case RelOp.OpType.EQ => l === r
      case RelOp.OpType.GT => l > r
      case RelOp.OpType.LT => l < r
      case RelOp.OpType.LTE => l <= r
      case RelOp.OpType.GTE => l >= r
      case RelOp.OpType.NE => l != r
    })
  }

  override def postorder(literalExpr: LiteralExpr): Unit = {
    if (literalExpr.getWidth > 0) {
      exprs.put(literalExpr, context.int(literalExpr.getValue, BVType(literalExpr.getWidth)))
    } else {
      exprs.put(literalExpr, context.int(literalExpr.getValue))
    }
  }

  override def postorder(binExpr: BinExpr): Unit = {
    val l = exprs(binExpr.getLeft)
    val r = exprs(binExpr.getRight)
    exprs.put(binExpr, l)
  }

  override def postorder(unOpExpr: UnOpExpr): Unit = {
    exprs.put(unOpExpr,
      unOpExpr.getType match {
        case UnOpExpr.OpType.NEG => -exprs(unOpExpr.getExpr)
        case UnOpExpr.OpType.NOT => ~exprs(unOpExpr.getExpr)
      }
    )
  }

  override def postorder(fieldRefExpr: FieldRefExpr): Unit = {
    val href = fieldRefExpr.getFieldRef.getHeaderRef
    val hquery = context(fieldRefExpr.getFieldRef)
    exprs.put(fieldRefExpr, hquery)
  }

  override def postorder(dre: DataRefExpr): Unit = super.postorder(dre)
}
