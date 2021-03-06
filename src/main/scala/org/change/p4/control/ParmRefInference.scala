package org.change.p4.control

import org.change.p4.model.actions.P4ActionCall.ParamExpression
import org.change.p4.model.actions.{P4Action, P4ActionParameter, P4ComplexAction}
import org.change.p4.model.control.exp.{FieldRefExpr, ValidRef}
import org.change.p4.model.fieldlists.FieldList
import org.change.p4.model.parser._
import org.change.p4.model.{RegisterSpecification, Switch}

import scala.collection.JavaConverters._

class ParmRefInference(switch: Switch) extends ASTVisitor {
  var lastAction: P4Action = null
  var stack = List.empty[Map[String, P4ActionParameter]]
  override def postorder(parmInstance: ParamExpression): Unit = {
    val expression = parmInstance.getExpression
    expression match {
      case stringRef: StringRef =>
        val maybeParam = stack.headOption.flatMap(_.get(stringRef.getRef))
        if (maybeParam.nonEmpty) {
          parmInstance.setExpression(new ParmRef(lastAction, maybeParam.get))
        } else {
          val hdr = switch.getInstance(stringRef.getRef)
          if (hdr != null) {
            parmInstance.setExpression(
              new HeaderRef()
                .setInstance(hdr)
                .setPath(hdr.getName)
            )
          } else {
            val fl: FieldList = switch.fieldList(stringRef.getRef)
            if (fl != null) {
              parmInstance.setExpression(
                new FieldListRef().setFieldList(fl).setPath(stringRef.getRef)
              )
            } else {
              val reg: RegisterSpecification = switch.register(stringRef.getRef)
              if (reg != null) {
                parmInstance.setExpression(
                  new RegisterRef().setRegister(reg).setPath(stringRef.getRef)
                )
              } else {
                val calc = switch.calculation(stringRef.getRef)
                if (calc != null) {
                  parmInstance.setExpression(new CalculationRef(calc))
                } else {
                  throw new IllegalArgumentException(
                    s"${stringRef.getRef} cannot be resolved to a reference"
                  )
                }
              }
            }
          }
        }
      case f: FieldRef =>
        solveFieldRef(f)
      case h: HeaderRef =>
        solveHeader(h)
      case _ => ;
    }
  }

  private def solveFieldRef(f: FieldRef): FieldRef = {
    val hdrref = f.getHeaderRef
    solveHeader(hdrref)
    if (f.getHeaderRef.getInstance().getLayout.getField(f.getField) == null) {
      throw new IllegalArgumentException(s"field ${f.getField} is not part of header " +
        s"${hdrref.getInstance().getName} of type ${hdrref.getInstance().getLayout.getName}")
    }
    f.setFieldReference(
      f.getHeaderRef.getInstance().getLayout.getField(f.getField)
    )
  }

  override def postorder(setStatement: SetStatement): Unit = {
    setStatement.setLeft(solveFieldRef(setStatement.getLeft))
  }

  private def solveHeader(hdrref: HeaderRef): HeaderRef = {
    val hdr = switch.getInstance(hdrref.getPath)
    if (hdr == null) {
      throw new IllegalArgumentException(s"header instance ${hdrref.getPath} not found")
    }
    hdrref.setInstance(hdr)
  }

  override def postorder(action: P4ComplexAction): Unit = stack = stack.tail
  override def preorder(action: P4ComplexAction): Boolean = {
    stack = action.getParameterList.asScala
      .map(x => {
        x.getParamName -> x
      })
      .toMap :: stack
    lastAction = action
    true
  }

  override def postorder(fieldRefExpr: FieldRefExpr): Unit =
    solveFieldRef(fieldRefExpr.getFieldRef)
  override def postorder(validRef: ValidRef): Unit =
    solveHeader(validRef.getHref)
}
