package org.change.p4.control

import org.change.p4.control.types.BVType
import org.change.p4.model.Switch
import org.change.p4.model.control.exp._
import org.change.p4.model.parser.SetStatement
import z3.scala.Z3Context

class LiteralTypeInference(switch: Switch) extends ASTVisitor {
  val typeSolver = new TypeSolver(new Z3Context())

  override def postorder(validRef: ValidRef): Unit = {
    typeSolver.equal(validRef, BVType(1))
  }

  override def postorder(negBExpr: NegBExpr): Unit = {
    typeSolver.equal(negBExpr, BVType(1))
  }

  override def postorder(binBExpr: BinBExpr): Unit = {
    typeSolver.equal(binBExpr, BVType(1))
  }

  override def postorder(literalBool: LiteralBool): Unit = {
    typeSolver.equal(literalBool, BVType(1))
  }

  override def postorder(relOp: RelOp): Unit = {
    typeSolver.paramsEqual(relOp.getLeft, relOp.getRight)
    typeSolver.equal(relOp, BVType(1))
  }

  override def postorder(literalExpr: LiteralExpr): Unit = {
    if (literalExpr.getWidth > 0)
      typeSolver.equal(literalExpr, BVType(literalExpr.getWidth))
  }

  override def postorder(setStatement: SetStatement): Unit = {
    val left = setStatement.getLeft.getFieldReference
    val right = setStatement.getRightE
    if (right.isInstanceOf[LiteralExpr]) {
      typeSolver.equal(right, BVType(left.getLength))
    }
  }

  override def postorder(binExpr: BinExpr): Unit = {
    typeSolver.paramsEqual(binExpr, binExpr.getLeft)
    typeSolver.paramsEqual(binExpr.getRight, binExpr.getLeft)
  }

  override def postorder(unOpExpr: UnOpExpr): Unit = {
    typeSolver.paramsEqual(unOpExpr, unOpExpr.getExpr)
  }

  override def postorder(fieldRefExpr: FieldRefExpr): Unit = {
    assert(fieldRefExpr.getFieldRef.getField != null)
    typeSolver.equal(
      fieldRefExpr,
      BVType(fieldRefExpr.getFieldRef.getFieldReference.getLength)
    )
  }

  override def postorder(switch: Switch): Unit = {
    typeSolver
      .solve(x => x.isInstanceOf[LiteralExpr])
      .foreach(lit => {
        val tp = lit._2
        val litr = lit._1.asInstanceOf[LiteralExpr]
        if (tp.nonEmpty) {
          val BVType(wid) = tp.get
          if (wid <= 0) {
            throw new IllegalArgumentException(
              wid + " not allowed as a valid bv width"
            )
          }
          litr.setWidth(wid)
        }
      })
  }
}

object LiteralTypeInference {
  def apply(switch: Switch): Unit = {
    Traverse(new LiteralTypeInference(switch))(switch)
  }
}
