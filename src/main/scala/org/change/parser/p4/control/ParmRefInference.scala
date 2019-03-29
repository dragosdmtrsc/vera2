package org.change.parser.p4.control

import org.change.v2.p4.model.{RegisterSpecification, Switch}
import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.actions.{P4Action, P4ActionCall, P4ActionParameter, P4ComplexAction}
import org.change.v2.p4.model.control.exp.{FieldRefExpr, ValidRef}
import org.change.v2.p4.model.fieldlists.FieldList
import org.change.v2.p4.model.parser._

import scala.collection.JavaConverters._

class ParmRefInference(switch: Switch) extends ASTVisitor {
  var lastAction : P4Action = null
  var stack = List.empty[Map[String, P4ActionParameter]]
  override def postorder(parmInstance : ParamExpression) : Unit = {
    val expression = parmInstance.getExpression
    expression match {
      case stringRef : StringRef =>
        val maybeParam = stack.headOption.flatMap(_.get(stringRef.getRef))
        if (maybeParam.nonEmpty) {
          parmInstance.setExpression(new ParmRef(lastAction, maybeParam.get))
        } else {
          val hdr = switch.getInstance(stringRef.getRef)
          if (hdr != null) {
            parmInstance.setExpression(new HeaderRef()
              .setInstance(hdr)
              .setPath(hdr.getName)
            )
          } else {
            val fl : FieldList = switch.fieldList(stringRef.getRef)
            if (fl != null) {
              parmInstance.setExpression(new FieldListRef().setFieldList(fl).setPath(stringRef.getRef))
            } else {
              val reg : RegisterSpecification = switch.register(stringRef.getRef)
              if (reg != null) {
                parmInstance.setExpression(new RegisterRef().setRegister(reg).setPath(stringRef.getRef))
              } else {
                val calc = switch.calculation(stringRef.getRef)
                if (calc != null) {
                  //TODO: mk calculation ref + populate calculcations thoroughly
                  parmInstance.setExpression(new CalculationRef(calc))
                } else {
                  throw new NoSuchFieldError(s"${stringRef.getRef} cannot be resolved to a reference")
                }
              }
            }
          }
        }
      case f : FieldRef =>
        solveFieldRef(f)
      case h : HeaderRef =>
        solveHeader(h)
      case _ => ;
    }
  }

  private def solveFieldRef(f: FieldRef): FieldRef = {
    val hdrref = f.getHeaderRef
    solveHeader(hdrref)
    f.setFieldReference(f.getHeaderRef.getInstance().getLayout.getField(f.getField))
  }

  private def solveHeader(hdrref: HeaderRef): HeaderRef = {
    val hdr = switch.getInstance(hdrref.getPath)
    assert(hdr != null)
    hdrref.setInstance(hdr)
  }

  override def postorder(action: P4ComplexAction): Unit = stack = stack.tail
  override def preorder(action: P4ComplexAction): Boolean = {
    stack = action.getParameterList.asScala.map(x => {
      x.getParamName -> x
    }).toMap :: stack
    lastAction = action
    true
  }

  override def postorder(fieldRefExpr: FieldRefExpr): Unit =
    solveFieldRef(fieldRefExpr.getFieldRef)
  override def postorder(validRef: ValidRef) : Unit =
    solveHeader(validRef.getHref)
}
