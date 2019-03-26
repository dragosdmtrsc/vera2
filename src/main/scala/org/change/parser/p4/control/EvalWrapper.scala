package org.change.parser.p4.control

import org.change.parser.p4.HeaderInstance
import org.change.v2.p4.model.{ArrayInstance, Switch}
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser.{FieldRef, HeaderRef, IndexedHeaderRef}
import org.change.v3.semantics.SimpleMemory
import z3.scala.{Z3AST, Z3Context}

import scala.collection.mutable
import scala.collection.JavaConverters._

class EvalWrapper(switch: Switch,
                  context: Z3Context,
                  emap : mutable.Map[P4Expr, SimpleMemory => (Z3AST, Z3AST)] = mutable.Map.empty,
                  bmap : mutable.Map[P4BExpr, SimpleMemory => (Z3AST, Z3AST)] = mutable.Map.empty) {
  def evaluate(p4Expr: P4Expr,
               simpleMemory: SimpleMemory,
               zeroOnInvalid : Boolean) : (Z3AST, Z3AST) = {
    emap.getOrElse(p4Expr, {
      Traverse(new ExpressionEvaluator(switch,
        context,
        emap,
        bmap,
        zeroOnInvalid, asBV = true))(p4Expr)
      emap(p4Expr)
    })(simpleMemory)
  }

  def evaluate(headerRef: ArrayInstance,
               index : Z3AST,
               simpleMemory: SimpleMemory) : (Map[String, Z3AST], Z3AST) = {
    val href = new HeaderRef().setInstance(headerRef).setPath(headerRef.getName)
    val (woval, err) = headerRef.getLayout.getFields.asScala
      .foldLeft((Map.empty[String, Z3AST], context.mkFalse()))((acc, f) => {
        val fieldRef = new FieldRef().setHeaderRef(href)
          .setFieldReference(f)
          .setField(f.getName)
        val (v, err) = evaluate(fieldRef, index, simpleMemory, zeroOnInvalid = true)
        val newerr = context.mkOr(acc._2, err)
        (acc._1 + (f.getName -> v), newerr)
      })
    if (headerRef.isMetadata) {
      (woval, err)
    } else {
      val (vr, nerr) = evaluate(new ValidRef(href), index, simpleMemory)
      (woval + ("IsValid" -> vr), context.mkOr(nerr, err))
    }
  }

  def evaluate(fieldRef : ValidRef,
               index : Z3AST,
               simpleMemory: SimpleMemory) : (Z3AST, Z3AST) = {
    assert(index.getSort.equals(context.mkIntSort()))
    val arrayInstance = fieldRef.getHref.getInstance().asInstanceOf[ArrayInstance]
    val oob = context.mkNot(context.mkAnd(
      context.mkGE(index, context.mkInt(0, context.mkIntSort())),
      context.mkLT(index, context.mkInt(arrayInstance.getLength, context.mkIntSort()))
    ))
    val conditions = (0 until arrayInstance.getLength).map(idx => {
      val idr = new IndexedHeaderRef().setIndex(idx).setInstance(arrayInstance)
      val expr = new ValidRef(idr)
      idx -> evaluate(expr, simpleMemory, zeroOnInvalid = false, asBV = true)
    })
    val error = oob
    val v = if (arrayInstance.getLength == 1) {
      conditions.head._2._1
    } else {
      val start = conditions.head._2._1
      conditions.tail.foldLeft(start)((acc, v) => {
        context.mkITE(context.mkEq(
          context.mkInt(v._1, context.mkIntSort()),
          index
        ), v._2._1, acc)
      })
    }
    (v, error)
  }

  def evaluate(fieldRef : FieldRef,
               index : Z3AST,
               simpleMemory: SimpleMemory,
               zeroOnInvalid : Boolean) : (Z3AST, Z3AST) = {
    assert(index.getSort.equals(context.mkIntSort()))
    val arrayInstance = fieldRef.getHeaderRef.getInstance().asInstanceOf[ArrayInstance]
    val flds = arrayInstance.getLayout.getField(fieldRef.getField)
    val oob = context.mkNot(context.mkAnd(
      context.mkGE(index, context.mkInt(0, context.mkIntSort())),
      context.mkLT(index, context.mkInt(arrayInstance.getLength, context.mkIntSort()))
    ))
    val conditions = (0 until arrayInstance.getLength).map(idx => {
      val idr = new IndexedHeaderRef().setIndex(idx).setInstance(arrayInstance)
      val idxfr = new FieldRef()
        .setField(fieldRef.getField)
        .setFieldReference(fieldRef.getFieldReference)
        .setHeaderRef(idr)
      val expr = new FieldRefExpr(idxfr)
      idx -> evaluate(expr, simpleMemory, zeroOnInvalid)
    })
    val error = context.mkOr(oob :: conditions.map(v => {
      context.mkAnd(v._2._2, context.mkEq(
        context.mkInt(v._1, context.mkIntSort()),
        index
      ))
    }).toList:_*)
    val v = if (arrayInstance.getLength == 1) {
      conditions.head._2._1
    } else {
      val start = conditions.head._2._1
      conditions.tail.foldLeft(start)((acc, v) => {
        context.mkITE(context.mkEq(
          context.mkInt(v._1, context.mkIntSort()),
          index
        ), v._2._1, acc)
      })
    }
    (v, error)
  }
  def evaluate(p4BExpr: P4BExpr,
               simpleMemory: SimpleMemory,
               zeroOnInvalid : Boolean,
               asBV : Boolean = false) : (Z3AST, Z3AST) = {
    bmap.getOrElse(p4BExpr, {
      Traverse(new ExpressionEvaluator(switch, context, emap, bmap,
        zeroOnInvalid, asBV))(p4BExpr)
      bmap(p4BExpr)
    })(simpleMemory)
  }
}

object ExpressionEvaluator {
  def indexedHeaderRef(headerRef: HeaderRef) : Option[IndexedHeaderRef] = {
    if (headerRef.isArray) Some(headerRef.asInstanceOf[IndexedHeaderRef])
    else None
  }
  def index(indexedHeaderRef: IndexedHeaderRef) : Option[Int] = {
    if (indexedHeaderRef.isLast) None
    else Some(indexedHeaderRef.getIndex)
  }
  def arrayInstance(indexedHeaderRef: IndexedHeaderRef) : ArrayInstance = {
    indexedHeaderRef.getInstance().asInstanceOf[ArrayInstance]
  }
}
class ExpressionEvaluator(switch: Switch,
                          context: Z3Context,
                          emap : mutable.Map[P4Expr, SimpleMemory => (Z3AST, Z3AST)],
                          bmap : mutable.Map[P4BExpr, SimpleMemory => (Z3AST, Z3AST)],
                          zeroOnInvalid : Boolean,
                          asBV : Boolean = false
                         ) extends ASTVisitor {
  import ExpressionEvaluator._
  private def queryForField(path : String,
                            simpleMemory: SimpleMemory,
                            f : String,
                            lookForValid : Boolean,
                            zeroOnInvalid : Boolean) :
  (Z3AST, Z3AST) = {
    val err = if (lookForValid) {
      val ast = simpleMemory.symbols(path + ".IsValid").ast.get
      context.mkEq(ast, context.mkInt(0, ast.getSort))
    } else {
      context.mkFalse()
    }
    if (lookForValid && zeroOnInvalid) {
      val ast = simpleMemory.symbols(path + "." + f).ast.get
      (context.mkITE(
        err, context.mkInt(0, ast.getSort), ast
      ), context.mkFalse())
    } else {
      (simpleMemory.symbols(path + "." + f).ast.get, err)
    }
  }

  private def queryMemory(href: HeaderRef,
                          simpleMemory: SimpleMemory,
                          f: String,
                          handleInvalid : Boolean,
                          zeroOnInvalid : Boolean): (Z3AST, Z3AST) = {
    val handleInv = handleInvalid && !href.getInstance().isMetadata
    indexedHeaderRef(href).map(x => {
      val arrinstance = arrayInstance(x)
      index(x).map(idx => {
        if (idx < 0 || idx >= arrinstance.getLength) {
          throw new IllegalArgumentException(s"out of bounds access $idx for array " +
            s"${arrinstance.getName} of" +
            s"length ${arrinstance.getLength}")
          (context.mkFalse(), context.mkTrue())
        } else {
          val location = arrinstance.getName + s"[$idx]"
          queryForField(location, simpleMemory, f, handleInv, zeroOnInvalid)
        }
      }).getOrElse({
        val nxtAst = simpleMemory.symbols(arrinstance.getName + ".next").ast.get
        val fail = context.mkAnd(context.mkLT(
          nxtAst,
          context.mkInt(1, nxtAst.getSort)
        ), context.mkGT(
          nxtAst,
          context.mkInt(arrinstance.getLength, nxtAst.getSort)
        ))
        val indexed = (0 until arrinstance.getLength).map(idx => {
          val location = arrinstance.getName + s"[$idx]"
          idx -> queryForField(location, simpleMemory, f, handleInv, zeroOnInvalid)
        })
        val init = context.mkInt(0, indexed.head._2._1.getSort)
        indexed.foldLeft((init, fail))((acc, crt) => {
          val tobeequal = context.mkInt(crt._1 + 1, context.mkIntSort())
          val crtval = crt._2._1
          val failNow = crt._2._2
          (context.mkITE(
            context.mkEq(tobeequal, nxtAst),
            crtval,
            acc._1
          ), context.mkOr(acc._2, failNow))
        })
      })
    }).getOrElse({
      val location = href.getInstance().getName
      queryForField(location, simpleMemory, f, handleInv, zeroOnInvalid)
    })
  }

  override def postorder(validRef: ValidRef): Unit = {
    val fun = (simpleMemory : SimpleMemory) => {
      val res = queryMemory(validRef.getHref, simpleMemory, "IsValid",
        handleInvalid = false,
        zeroOnInvalid = false)
      if (asBV) {
        res
      } else {
        (context.mkEq(res._1, context.mkInt(0, res._1.getSort)), res._2)
      }
    }
    bmap.put(validRef, fun)
  }

  override def postorder(negBExpr: NegBExpr): Unit = {
    val fun = (simpleMemory : SimpleMemory) => {
      val e = bmap(negBExpr.getExpr)(simpleMemory)
      (context.mkNot(e._1), e._2)
    }
    bmap.put(negBExpr, fun)
  }

  override def postorder(binBExpr: BinBExpr): Unit = {
    val fun = (simpleMemory : SimpleMemory) => {
      val l = bmap(binBExpr.getLeft)(simpleMemory)
      val r = bmap(binBExpr.getRight)(simpleMemory)
      val res = binBExpr.getType match {
        case BinBExpr.OpType.AND =>
          context.mkAnd(l._1, r._1)
        case BinBExpr.OpType.OR =>
          context.mkOr(l._1, r._1)
      }
      (res, context.simplifyAst(context.mkOr(l._2, r._2)))
    }
    bmap.put(binBExpr, fun)
  }

  override def postorder(literalBool: LiteralBool): Unit = {
    bmap.put(literalBool, _ =>
      (if (literalBool.value()) context.mkTrue() else context.mkFalse(), context.mkFalse()))
  }

  override def postorder(relOp: RelOp): Unit = {
    val le = emap(relOp.getLeft)
    val re = emap(relOp.getRight)
    val fun = (simpleMemory : SimpleMemory) => {
      val l = le(simpleMemory)
      val r = re(simpleMemory)
      (relOp.getType match {
        case RelOp.OpType.EQ =>
          context.mkEq(l._1, r._1)
        case RelOp.OpType.GT =>
          context.mkBVSgt(l._1, r._1)
        case RelOp.OpType.GTE =>
          context.mkBVSge(l._1, r._1)
        case RelOp.OpType.LT =>
          context.mkBVSlt(l._1, r._1)
        case RelOp.OpType.LTE =>
          context.mkBVSle(l._1, r._1)
        case RelOp.OpType.NE =>
          context.mkNot(context.mkEq(l._1, r._1))
      }, context.mkOr(l._2, r._2))
    }
    bmap.put(relOp, fun)
  }

  override def postorder(literalExpr: LiteralExpr): Unit = {
    emap.put(literalExpr, _ => (context.mkNumeral(literalExpr.getValue.toString(),
      context.mkBVSort(literalExpr.getWidth)), context.mkFalse()))
  }

  override def postorder(binExpr: BinExpr): Unit = {
    val le = emap(binExpr.getLeft)
    val re = emap(binExpr.getRight)
    val fun = (simpleMemory : SimpleMemory) => {
      val l = le(simpleMemory)
      val r = re(simpleMemory)
      (binExpr.getType match {
        case BinExpr.OpType.BVAND =>
          context.mkBVAnd(l._1, r._1)
        case BinExpr.OpType.BVOR =>
          context.mkBVOr(l._1, r._1)
        case BinExpr.OpType.BVXOR =>
          context.mkBVXor(l._1, r._1)
        case BinExpr.OpType.MINUS =>
          context.mkBVSub(l._1, r._1)
        case BinExpr.OpType.PLUS =>
          context.mkBVAdd(l._1, r._1)
        case BinExpr.OpType.SHL =>
          context.mkBVShl(l._1, r._1)
        case BinExpr.OpType.SHR =>
          throw new UnsupportedOperationException(binExpr + " can't handle >>")
      }, context.mkOr(l._2, r._2))
    }
    emap.put(binExpr, fun)
  }

  override def postorder(unOpExpr: UnOpExpr): Unit = {
    val ee = emap(unOpExpr.getExpr)
    val fun = (simpleMemory : SimpleMemory) => {
      val e = ee(simpleMemory)
      (unOpExpr.getType match {
        case UnOpExpr.OpType.NEG =>
          context.mkBVNeg(e._1)
        case UnOpExpr.OpType.NOT =>
          context.mkBVNot(e._1)
      }, e._2)
    }
    emap.put(unOpExpr, fun)
  }

  override def postorder(fieldRefExpr: FieldRefExpr): Unit = {
    val fr = fieldRefExpr.getFieldRef
    val fun = (simpleMemory : SimpleMemory) => {
      queryMemory(fr.getHeaderRef,
        simpleMemory,
        fr.getField,
        handleInvalid = true, zeroOnInvalid = zeroOnInvalid
      )
    }
    emap.put(fieldRefExpr, fun)
  }
}