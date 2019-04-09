package org.change.p4.control

import com.microsoft.z3.Context
import org.change.p4.control.types.BVType
import org.change.p4.model.Switch
import org.change.p4.model.actions.P4ActionCall.ParamExpression
import org.change.p4.model.actions.{P4ActionCall, P4ActionParameter, P4ComplexAction}
import org.change.p4.model.control.exp.LiteralExpr
import org.change.p4.model.parser.{FieldRef, ParmRef}

import scala.collection.JavaConverters._

class ParmTypeInference(switch: Switch) extends ASTVisitor {
  val typeSolver = new TypeSolver(new Context())

  def handleTriple(actCall: P4ActionCall): Unit = {
    assert(actCall.params().size() == 3)
    val kinds = actCall
      .params()
      .asScala
      .map(_.asInstanceOf[ParamExpression])
      .zip(actCall.getP4Action.getParameterList.asScala)
      .map(p => {
        lookAt(p._1)
        p
      })
    typeSolver.paramsEqual(kinds(0)._1, kinds(1)._1, kinds.head._2.isRightValue)
    typeSolver.paramsEqual(kinds(1)._1, kinds(2)._1)
  }

  def lookAt(p: ParamExpression): Unit = p.getExpression match {
    case hr: FieldRef =>
      val inst = hr.getHeaderRef.getInstance().getLayout.getField(hr.getField)
      typeSolver.equal(p, BVType(inst.getLength))
    case pr: ParmRef =>
      typeSolver.paramsEqual(pr.getParameter, p)
    case literalExpr: LiteralExpr =>
      if (literalExpr.getWidth > 0)
        typeSolver.equal(p, BVType(literalExpr.getWidth))
    case _ => ;
  }

  override def postorder(complex: P4ComplexAction): Unit = {
    complex.getParameterList.asScala.foreach(typeSolver.getIdx)
  }

  override def postorder(actCall: P4ActionCall): Unit = {
    val params =
      actCall.params().asScala.map(_.asInstanceOf[ParamExpression]).collect {
        case pe: ParamExpression if pe.getExpression.isInstanceOf[ParmRef] => pe
        case pe: ParamExpression
            if pe.getExpression.isInstanceOf[LiteralExpr] =>
          pe
      }
    params.foreach(typeSolver.getIdx)
    val all = actCall.params().asScala.map(_.asInstanceOf[ParamExpression])
    all.foreach(lookAt)
    actCall.getP4Action match {
      case ax: org.change.p4.model.actions.primitives.Add =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.AddToField =>
        handleDouble(actCall)
      case ax: org.change.p4.model.actions.primitives.BitAnd =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.BitOr =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.BitXor =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.CloneEgressPktToEgress =>
        assert(all.size == 2)
        typeSolver.equal(all.head, BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.CloneEgressPktToIngress =>
        assert(all.size == 2)
        typeSolver.equal(all.head, BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.CloneIngressPktToEgress =>
        assert(all.size == 2)
        typeSolver.equal(all.head, BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.CloneIngressPktToIngress =>
        assert(all.size == 2)
        typeSolver.equal(all.head, BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.Count =>
        assert(all.size == 2)
        typeSolver.equal(all(1), BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.ExecuteMeter =>
        assert(all.size == 3)
        typeSolver.equal(all(1), BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.GenerateDigest =>
        assert(all.size == 2)
        typeSolver.equal(all.head, BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.ModifyField =>
        if (actCall.params().size() == 3) {
          handleTriple(actCall)
        } else {
          handleDouble(actCall)
        }
      case ax: org.change.p4.model.actions.primitives.ModifyFieldRngUniform =>
        assert(all.size == 3)
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset =>
        assert(all.size == 4)
        val dest = all.head
        val base = all(1)
        val sz = all(3)
        typeSolver.paramsEqual(base, sz)
        typeSolver.paramsEqual(dest, base, mandatory = false)
        lookAt(dest)
        lookAt(base)
        lookAt(sz)
      case ax: org.change.p4.model.actions.primitives.Pop =>
        if (actCall.params().size() > 1)
          typeSolver.equal(all(1), BVType(8), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.Push =>
        if (actCall.params().size() > 1)
          typeSolver.equal(all(1), BVType(8), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.RegisterRead =>
        if (actCall.params().size() > 2)
          typeSolver.equal(all(2), BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.RegisterWrite =>
        if (actCall.params().size() > 2)
          typeSolver.equal(all(2), BVType(16), mandatory = false)
      case ax: org.change.p4.model.actions.primitives.ShiftLeft =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.ShiftRight =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.Subtract =>
        handleTriple(actCall)
      case ax: org.change.p4.model.actions.primitives.SubtractFromField =>
        handleDouble(actCall)
      case ax: org.change.p4.model.actions.primitives.Truncate =>
        assert(actCall.params().size() == 1)
        typeSolver.equal(
          actCall.params().get(0).asInstanceOf[ParamExpression],
          BVType(16)
        )
      case ax: org.change.p4.model.actions.P4ComplexAction =>
        assert(all.size == ax.getParameterList.size())
        all
          .zip(ax.getParameterList.asScala)
          .foreach(p => {
            typeSolver.paramsEqual(p._1, p._2)
          })
      case _ => ;
    }
  }

  private def handleDouble(actCall: P4ActionCall): Unit = {
    assert(actCall.params.size() == 2)
    val kinds = actCall
      .params()
      .asScala
      .map(_.asInstanceOf[ParamExpression])
      .zip(actCall.getP4Action.getParameterList.asScala)
      .map(p => {
        lookAt(p._1)
        p
      })
    typeSolver.paramsEqual(kinds(0)._1, kinds(1)._1, kinds.head._2.isRightValue)
  }

  override def postorder(switch: Switch): Unit = {
    typeSolver
      .solve {
        case ap: P4ActionParameter => true
        case paramExpression: ParamExpression
            if paramExpression.getExpression.isInstanceOf[LiteralExpr] =>
          true
        case _ => false
      }
      .foreach(x => {
        x._1 match {
          case a: P4ActionParameter =>
            if (x._2.nonEmpty) {
              a.setSort(x._2.get)
            } else {
              throw new IllegalStateException(s"can't infer type for $a")
            }
          case pe: ParamExpression
              if pe.getExpression.isInstanceOf[LiteralExpr] =>
            if (x._2.nonEmpty) {
              x._2.get match {
                case BVType(w) =>
                  pe.getExpression.asInstanceOf[LiteralExpr].setWidth(w)
                case _ => ;
              }
            } else {
              throw new IllegalStateException(s"can't infer type for $pe")
            }
        }
      })
    typeSolver
      .casts()
      .foreach(cast => {
        cast._1 match {
          case paramExpression: ParamExpression =>
            paramExpression.setCastTo(cast._2)
        }
      })
  }
}
