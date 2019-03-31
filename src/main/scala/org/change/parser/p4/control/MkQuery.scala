package org.change.parser.p4.control

import org.change.plugins.vera.BVType
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser._

import scala.collection.mutable

object MkQuery {
  def apply(ctx : P4Memory,
            expression : Expression) : P4Query = expression match {
    case calculationRef: CalculationRef =>
      // TODO: calculation handling
      ctx.int(calculationRef.getPath.hashCode)
    case registerRef: RegisterRef =>
      //TODO: register handling
      ctx.int(registerRef.getPath.hashCode)
    case fr : FieldListRef =>
      ctx.int(fr.getFieldList.getName.hashCode)
    case c : LiteralExpr =>
      if (c.getWidth > 0)
        ctx.int(c.getValue, BVType(c.getWidth))
      else ctx.int(c.getValue)
    case pr : ParmRef =>
      val act = pr.getAction.getActionName + "_" + pr.getParameter.getParamName
      ctx.field(act)
    case ihr : IndexedHeaderRef =>
      if (ihr.isLast) {
        val path = ctx.field(ihr.getPath)
        ctx.field(ihr.getPath)(path.next() - path.next().int(1))
      } else if (ihr.isNext) {
        val path = ctx.field(ihr.getPath)
        ctx.field(ihr.getPath)(path.next())
      } else {
        val fld = ctx.field(ihr.getPath)
        fld(fld.next().int(ihr.getIndex))
      }
    case f : FieldRef => apply(ctx, f.getHeaderRef).field(f.getField)
    case r : HeaderRef => ctx.field(r.getPath)
  }
  def apply(context : P4Memory,
            expr : P4Expr) : P4Query = {
    val where = mutable.Map.empty[Object, P4Query]
    Traverse(QueryMaker(context, where))(expr)
    where(expr)
  }
  def apply(context : P4Memory,
            expr : P4BExpr) : P4Query = {
    val where = mutable.Map.empty[Object, P4Query]
    Traverse(QueryMaker(context, where))(expr)
    where(expr)
  }
  def validityFailure(context : P4Memory, expr: P4Expr) : P4Query = expr match {
    case fieldRefExpr: FieldRefExpr =>
      validityFailure(context, fieldRefExpr.getFieldRef)
    case binExpr: BinExpr =>
      val l = binExpr.getLeft
      val r = binExpr.getRight
      binExpr.getType match {
        case BinExpr.OpType.BVAND =>
          val lv = apply(context, l)
          val rv = apply(context, r)
          val lf = validityFailure(context, l)
          val rf = validityFailure(context, r)
          (lf && (rv != rv.int(0))) ||
            (rf && (lv != lv.int(0))) ||
              (lf && rf)
        case BinExpr.OpType.BVOR =>
          val lv = apply(context, l)
          val rv = apply(context, r)
          val lf = validityFailure(context, l)
          val rf = validityFailure(context, r)
          (lf && (rv != rv.int(-1))) ||
            (rf && (lv != lv.int(-1))) ||
              (rf && lf)
        case _ => validityFailure(context, l) ||
          validityFailure(context, r)
      }
    case unOp : UnOpExpr =>
      validityFailure(context, unOp.getExpr)
    case _ => context.boolVal(false)
  }
  def validityFailure(context : P4Memory, expr: P4BExpr) : P4Query = expr match {
    case binBExpr: BinBExpr =>
      val lf = validityFailure(context, binBExpr.getLeft)
      val rf = validityFailure(context, binBExpr.getRight)
      val lv = apply(context, binBExpr.getLeft)
      val rv = apply(context, binBExpr.getRight)
      binBExpr.getType match {
        case BinBExpr.OpType.AND =>
          (lf && rv) || (rf && lv)
        case BinBExpr.OpType.OR =>
          (lf && !rv) || (rf && !lv)
      }
    case negBExpr: NegBExpr => validityFailure(context, negBExpr.getExpr)
    case relOp: RelOp =>
      validityFailure(context, relOp.getLeft) ||
        validityFailure(context, relOp.getRight)
    case _ => context.boolVal(false)
  }
  def validityFailure(context : P4Memory,
                      expr: Expression) : P4Query = expr match {
    case fr : FieldRef =>
      if (!fr.getHeaderRef.getInstance().isMetadata) {
        !apply(context, fr.getHeaderRef).valid()
      } else {
        context.boolVal(false)
      }
    case _ => context.boolVal(false)
  }

  def indexOutOfBounds(context : P4Memory, expr: P4Expr) : P4Query = expr match {
    case fieldRefExpr: FieldRefExpr =>
      indexOutOfBounds(context, fieldRefExpr.getFieldRef)
    case binExpr: BinExpr =>
      val l = binExpr.getLeft
      val r = binExpr.getRight
      binExpr.getType match {
        case BinExpr.OpType.BVAND =>
          val lv = apply(context, l)
          val rv = apply(context, r)
          val lf = indexOutOfBounds(context, l)
          val rf = indexOutOfBounds(context, r)
          (lf && (rv != rv.int(0))) ||
            (rf && (lv != lv.int(0))) ||
            (lf && rf)
        case BinExpr.OpType.BVOR =>
          val lv = apply(context, l)
          val rv = apply(context, r)
          val lf = indexOutOfBounds(context, l)
          val rf = indexOutOfBounds(context, r)
          (lf && (rv != rv.int(-1))) ||
            (rf && (lv != lv.int(-1))) ||
            (rf && lf)
        case _ => indexOutOfBounds(context, l) ||
          indexOutOfBounds(context, r)
      }
    case unOp : UnOpExpr =>
      indexOutOfBounds(context, unOp.getExpr)
    case _ => context.boolVal(false)
  }
  def indexOutOfBounds(context : P4Memory, expr: P4BExpr) : P4Query = expr match {
    case binBExpr: BinBExpr =>
      val lf = indexOutOfBounds(context, binBExpr.getLeft)
      val rf = indexOutOfBounds(context, binBExpr.getRight)
      val lv = apply(context, binBExpr.getLeft)
      val rv = apply(context, binBExpr.getRight)
      binBExpr.getType match {
        case BinBExpr.OpType.AND =>
          (lf && rv) || (rf && lv)
        case BinBExpr.OpType.OR =>
          (lf && !rv) || (rf && !lv)
      }
    case negBExpr: NegBExpr => indexOutOfBounds(context, negBExpr.getExpr)
    case relOp: RelOp =>
      indexOutOfBounds(context, relOp.getLeft) ||
        indexOutOfBounds(context, relOp.getRight)
    case _ => context.boolVal(false)
  }
  def indexOutOfBounds(context : P4Memory,
                      expr: Expression) : P4Query = expr match {
    case fr : FieldRef =>
      if (fr.getHeaderRef.isArray) {
        indexOutOfBounds(context, fr.getHeaderRef)
      } else {
        context.boolVal(false)
      }
    case ihr : IndexedHeaderRef =>
      val h = context.field(ihr.getPath)
      val nxt = h.next()
      val len = h.len()
      if (ihr.isLast) {
        ((nxt - nxt.int(1)) < len) &&
          ((nxt - nxt.int(1)) >= nxt.int(0))
      } else if (ihr.isNext) {
        (nxt < len) &&
          (nxt >= nxt.int(0))
      } else {
        (nxt.int(ihr.getIndex) < len) &&
          (nxt.int(ihr.getIndex) >= nxt.int(0))
      }
    case _ => context.boolVal(false)
  }
}
