package org.change.parser.p4.control

import org.change.utils.FreshnessManager
import org.change.v2.p4.model.{ArrayInstance, HeaderInstance, Switch}
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser.IndexedHeaderRef
import org.change.v3.syntax._

import scala.collection.mutable

class SEFLTranslator(switch: Switch,
                     val bmap : mutable.Map[P4BExpr, (Instruction, String)] = mutable.Map.empty,
                     val emap : mutable.Map[P4Expr, (Instruction, String)] = mutable.Map.empty) extends ASTVisitor {
  def solveHeader(path : String): HeaderInstance = switch.getInstance(path)
  override def postorder(validRef : ValidRef) : Unit = {
    val path = validRef.getHref.getPath
    bmap.put(validRef, {
      val bexpr = if (validRef.getHref.isArray) {
        val ihr = validRef.getHref.asInstanceOf[IndexedHeaderRef]
        val hdr = solveHeader(path).asInstanceOf[ArrayInstance]
        if (ihr.isLast) {
          val all = (0 until hdr.getLength).map(x => {
            LAnd(EQ(Literal(x + 1), Symbol(path + ".next")),
              EQ(Symbol(path + s"[$x].IsValid"), Literal(1))) : BExpr
          })
          if (all.size == 1)
            all.head
          else all.tail.foldLeft(all.head)((acc, x) => {
            LOr(acc, x)
          })
        } else {
          EQ(Symbol(path + s"[${ihr.getIndex}].IsValid"), Literal(1))
        }
      } else {
        EQ(Symbol(path + ".IsValid"), Literal(1))
      }
      val ret = FreshnessManager.next("ret")
      (InstructionBlock(
        Allocate(Symbol(ret), 1),
        If (bexpr,
          Assign(Symbol(ret), Literal(1)),
          Assign(Symbol(ret), Literal(0)))
      ), ret)
    })
  }
  override def postorder(negBExpr: NegBExpr): Unit = {
    val last = bmap(negBExpr.getExpr)
    val ret = last._2
    bmap.put(negBExpr, (InstructionBlock(
      last._1,
      Assign(Symbol(ret), BVNot(Symbol(ret)))
    ), ret))
  }
  override def postorder(literalBool:  LiteralBool): Unit = {
    val ret = FreshnessManager.next("ret")
    bmap.put(literalBool, (
      InstructionBlock(
        Allocate(Symbol(ret), 1),
        Assign(Symbol(ret), Literal(if (literalBool.value()) 1 else 0))
      ),
      ret
    ))
  }

  override def postorder(binBExpr : BinBExpr) : Unit = {
    val r = bmap(binBExpr.getRight)
    val l = bmap(binBExpr.getLeft)
    val ret = l._2
    val eleft = Symbol(l._2)
    val eright = Symbol(r._2)
    val instr = Assign(eleft, (binBExpr.getType match {
      case BinBExpr.OpType.AND => BVAnd
      case BinBExpr.OpType.OR => BVOr
    })(eleft, eright))
    bmap.put(binBExpr, (
      InstructionBlock(
        l._1,
        r._1,
        instr,
        Deallocate(eright)
      ), ret))
  }

  override def postorder(relOp: RelOp) : Unit = {
    val l = emap(relOp.getLeft)
    val r = emap(relOp.getRight)
    val eleft = Symbol(l._2)
    val eright = Symbol(r._2)
    val ret = FreshnessManager.next("ret")
    val bexp = (relOp.getType match {
      case RelOp.OpType.EQ => EQ
      case RelOp.OpType.GT => GT
      case RelOp.OpType.GTE => GTE
      case RelOp.OpType.LTE => LTE
      case RelOp.OpType.LT => LT
      case RelOp.OpType.NE => (x1 : BVExpr, x2 : BVExpr) => LNot(EQ(x1, x2))
    })(eleft, eright)
    val instr = If (bexp, Assign(Symbol(ret), Literal(1)), Assign(Symbol(ret), Literal(0)))
    bmap.put(relOp, (InstructionBlock(
      Allocate(Symbol(ret), 1),
      l._1,
      r._1,
      instr,
      Deallocate(eright),
      Deallocate(eleft)
    ), ret))
  }

  override def postorder(literalExpr: LiteralExpr) : Unit = {
    val ret = FreshnessManager.next("ret")
    assert(literalExpr.getWidth > 0)
    val instr = InstructionBlock(
      Allocate(Symbol(ret), literalExpr.getWidth),
      Assign(Symbol(ret), Literal(literalExpr.getValue))
    )
    emap.put(literalExpr, (instr, ret))
  }

  override def postorder(fieldRefExpr: FieldRefExpr) : Unit = {
    val hdr = solveHeader(fieldRefExpr.getFieldRef.getHeaderRef.getPath)
    val instr1 = if (!hdr.isMetadata) {
      if (fieldRefExpr.getFieldRef.getHeaderRef.isArray) {
        val ihr = fieldRefExpr.getFieldRef.getHeaderRef.asInstanceOf[IndexedHeaderRef]
        val ai = hdr.asInstanceOf[ArrayInstance]
        if (ihr.isLast) {
          (0 until ai.getLength).foldRight(
            Fail(s"array ${ihr.getPath} index out of bounds") : Instruction)((x, acc) => {
            mkArrayBound(ai, x, acc)
          })
        } else {
          isIndexValid(ai, ihr.getIndex)
        }
      } else {
        If(LNot(EQ(Symbol(hdr.getName + s".IsValid"), Literal(1))),
          Fail(s"reading from an invalid header ${hdr.getName}"),
          NoOp
        )
      }
    } else NoOp
    val ret = FreshnessManager.next("ret")
    val fld = fieldRefExpr.getFieldRef.getField
    val instr2 = if (fieldRefExpr.getFieldRef.getHeaderRef.isArray) {
      val ihr = fieldRefExpr.getFieldRef.getHeaderRef.asInstanceOf[IndexedHeaderRef]
      val ai = hdr.asInstanceOf[ArrayInstance]
      if (ihr.isLast) {
        (0 until ai.getLength).foldRight(Fail(s"array ${ai.getName} index out of bounds") : Instruction)((x, acc) => {
          val path = ai.getName + s"[$x]"
          If (EQ(Symbol(ai.getName + ".next"), Literal(x + 1)),
            Assign(Symbol(ret), Symbol(path + "." + fld)),
            acc
          )
        })
      } else {
        val path = ai.getName + s"[${ihr.getIndex}]"
        Assign(Symbol(ret), Symbol(path + "." + fld))
      }
    } else {
      val path = fieldRefExpr.getFieldRef.getHeaderRef.getPath
      Assign(Symbol(ret), Symbol(path + "." + fld))
    }
    val fieldWidth = hdr.getLayout.getField(fieldRefExpr.getFieldRef.getField).getLength
    emap.put(fieldRefExpr, (InstructionBlock(
      instr1,
      Allocate(Symbol(ret), fieldWidth),
      instr2
    ), ret))
  }


  override def postorder(binExpr: BinExpr): Unit = {
    val operator = binExpr.getType match {
      case BinExpr.OpType.BVAND => BVAnd
      case BinExpr.OpType.BVOR => BVOr
      case BinExpr.OpType.BVXOR => BVXor.apply _
      case BinExpr.OpType.MINUS => BVSub
      case BinExpr.OpType.PLUS => BVAdd
      case BinExpr.OpType.SHL => BVShl
      case BinExpr.OpType.SHR =>
        throw new UnsupportedOperationException(binExpr + " >> not supported")
    }
    val l = emap(binExpr.getLeft)
    val r = emap(binExpr.getRight)

    val ret = l._2
    val actual = Assign(Symbol(ret), operator(Symbol(l._2), Symbol(r._2)))
    emap.put(binExpr, (InstructionBlock(
      l._1,
      r._1,
      actual,
      Deallocate(Symbol(r._2))
    ), ret))
  }

  override def postorder(unOpExpr: UnOpExpr): Unit = {
    val l = emap(unOpExpr.getExpr)
    val ret = l._2
    val operator = unOpExpr.getType match {
      case UnOpExpr.OpType.NEG => BVNeg
      case UnOpExpr.OpType.NOT => BVNot
    }
    val actual = Assign(Symbol(ret), operator(Symbol(l._2)))
    emap.put(unOpExpr, (InstructionBlock(
      l._1,
      actual
    ), ret))
  }

  private def mkArrayBound(ai: ArrayInstance, x: Int, acc: Instruction): Instruction = {
    If(EQ(Symbol(ai.getName + ".next"), Literal(x + 1)),
      isIndexValid(ai, x),
      acc
    )
  }

  private def isIndexValid(ai: ArrayInstance, x: Int): Instruction = {
    If(LNot(EQ(Symbol(ai.getName + s"[$x].IsValid"), Literal(1))),
      Fail(s"reading from an invalid header ${ai.getName}[$x]"),
      NoOp
    )
  }
}
