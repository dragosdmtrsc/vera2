package org.change.parser.p4

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:+:, :-:, :@}
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Assign, Fail, InstructionBlock, NoOp}
import org.change.v2.p4.model.actions._
import org.change.v2.p4.model.actions.primitives._

import scala.collection.JavaConversions._
/**
  * Created by dragos on 30.08.2017.
  */
object ActionToSefl {

}

class ActionInstance(p4Action: P4Action, argList : List[Any], ctx : P4GrammarListener) {


  def handleComplexAction(complexAction: P4ComplexAction) : Instruction = {
    val arity = complexAction.getParameterList.size()
    if (arity != argList.size)
      throw new IllegalArgumentException(s"Wrong arity got ${argList.size} vs wanted $arity")
    lazy val argNameToIndex = complexAction.getParameterList().zipWithIndex.map { x => x._1.getParamName -> x._2 }.toMap
    InstructionBlock(complexAction.getActionList.map( x => {
      val args = x.parameterInstances().map( y => {
        if ((y.getParameter.getType & P4ActionParameterType.UNKNOWN.x) != 0) {
          argList(argNameToIndex(y.getParameter.getParamName))
        } else {
          y.getValue
        }
      }).toList
      val rec = new ActionInstance(x.getP4Action, args, ctx)
      rec.sefl()
    })
    )
  }

  def handleModifyField(modifyField: ModifyField) : Instruction = {
    val argDest = argList(0)
    val argSource = argList(1)
    val dstField = ctx.resolveField(argDest.toString)
    argSource match {
      case value: Int =>
        dstField match {
          case Left(j) => Assign(j, ConstantValue(value))
          case Right(s) => Assign(s, ConstantValue(value))
        }
      case _: String =>
        try {
          val value = Integer.decode(argSource.toString).intValue()
          dstField match {
            case Left(j) => Assign(j, ConstantValue(value))
            case Right(s) => Assign(s, ConstantValue(value))
          }
        }
        catch {
          case nfe: NumberFormatException => ctx.resolveField(argSource.toString) match {
            case Left(i) => dstField match {
              case Left(j) => Assign(j, :@(i))
              case Right(s) => Assign(s, :@(i))
            }
            case Right(r) => dstField match {
              case Left(j) => Assign(j, :@(r))
              case Right(s) => Assign(s, :@(r))
            }
          }
        }
      case _ =>
        throw new IllegalArgumentException(s"$argSource is wrong. Must be String or Int")
    }
  }

  def handleAddToField(addToField: AddToField) : Instruction = {
    val argDest = argList(0)
    val argSource = argList(1)
    val dstField = ctx.resolveField(argDest.toString)
    val arg = parseArg(argSource)
    dstField match {
      case Left(i) => Assign(i, :+:(:@(i), arg))
      case Right(s) => Assign(s, :+:(:@(s), arg))
    }
  }


  def parseArg(arg : Any) : FloatingExpression = {
    arg match {
      case value: Int =>
        ConstantValue(value)
      case _: String =>
        try {
          val value = Integer.decode(arg.toString).intValue()
          ConstantValue(value)
        }
        catch {
          case nfe: NumberFormatException => toFexp(ctx.resolveField(arg.toString))
        }
      case _ =>
        throw new IllegalArgumentException(s"$arg is wrong. Must be String or Int")
    }
  }

  def handleSubtractFromField(addToField: SubtractFromField) : Instruction = {
    val argDest = argList(0)
    val argSource = argList(1)
    val dstField = ctx.resolveField(argDest.toString)
    val arg = parseArg(argSource)
    dstField match {
      case Left(i) => Assign(i, :-:(:@(i), arg))
      case Right(s) => Assign(s, :-:(:@(s), arg))
    }
  }

  def toFexp(arg : Either[Intable, String]) : FloatingExpression = arg match {
    case Left(i) => :@(i)
    case Right(s) => :@(s)
  }

  def handleAdd(addToField: Add) : Instruction = {
    val argDest = argList(0)
    val argSource1 = argList(1)
    val argSource2 = argList(2)
    val dstField = ctx.resolveField(argDest.toString)
    val arg1 = parseArg(argSource1)
    val arg2 = parseArg(argSource2)
    dstField match {
      case Left(i) => Assign(i, :+:(arg1, arg2))
      case Right(s) => Assign(s, :+:(arg1, arg2))
    }
  }

  def handleSubtract(subtract: Subtract) : Instruction = {
    val argDest = argList(0)
    val argSource1 = argList(1)
    val argSource2 = argList(2)
    val dstField = ctx.resolveField(argDest.toString)
    val arg1 = parseArg(argSource1)
    val arg2 = parseArg(argSource2)
    dstField match {
      case Left(i) => Assign(i, :-:(arg1, arg2))
      case Right(s) => Assign(s, :-:(arg1, arg2))
    }
  }

  def handleRegisterRead(regRead : RegisterRead) : Instruction = {
    val argDest = argList(0)
    val argSource1 = argList(1)
    if (argList.length > 2) {
      val argSource2 = argList(2)
      // this is a global register
      val intVal = Integer.decode(argSource2.toString).intValue()
      val name = "reg." + argSource1.toString + "." + intVal
      ctx.resolveField(argDest.toString) match {
        case Left(i) => Assign(i, :@(name))
        case Right(s) => Assign(s, :@(name))
      }
    } else {
      // this is a direct register => will be referenced by flow number -> don't forget to allocate when adding a new flow
      throw new UnsupportedOperationException("TODO: Direct registers not yet implemented ")
    }
  }

  def handleRegisterWrite(regRead : RegisterWrite) : Instruction = {
    val argDest = argList(0)
    val argSource1 = argList(1)
    if (argList.length > 2) {
      val argSource2 = argList(2)
      // this is a global register
      val intVal = Integer.decode(argSource2.toString).intValue()
      val name = "reg." + argSource1.toString + "." + intVal
      ctx.resolveField(argDest.toString) match {
        case Left(i) => Assign(name, :@(i))
        case Right(s) => Assign(name, :@(s))
      }
    } else {
      // this is a direct register => will be referenced by flow number -> don't forget to allocate when adding a new flow
      throw new UnsupportedOperationException("TODO: Direct registers not yet implemented ")
    }
  }


  def handlePrimitiveAction(primitiveAction : P4Action) : Instruction = {
    primitiveAction.getActionType match {
      case P4ActionType.AddToField => handleAddToField(primitiveAction.asInstanceOf[AddToField])
      case P4ActionType.Add => handleAdd(primitiveAction.asInstanceOf[Add])
      case P4ActionType.Subtract => handleSubtract(primitiveAction.asInstanceOf[Subtract])
      case P4ActionType.SubtractFromField => handleSubtractFromField(primitiveAction.asInstanceOf[SubtractFromField])
      case P4ActionType.ModifyField => handleModifyField(primitiveAction.asInstanceOf[ModifyField])
      case P4ActionType.Drop => Fail("Dropped right here")
      case P4ActionType.NoOp => NoOp
      case P4ActionType.RegisterRead => handleRegisterRead(primitiveAction.asInstanceOf[RegisterRead])
      case P4ActionType.RegisterWrite => handleRegisterWrite(primitiveAction.asInstanceOf[RegisterWrite])
      case _ => throw new UnsupportedOperationException(s"Primitive action of type ${primitiveAction.getActionType} not yet supported")
    }
  }

  def sefl() : Instruction = {
    val actual = if (p4Action.getActionType == P4ActionType.UNKNOWN) {
      ctx.actionRegistrar.getAction(p4Action.getActionName)
    } else {
      p4Action
    }
    if (actual == null || actual.getActionType == P4ActionType.UNKNOWN)
      throw new IllegalArgumentException(s"P4 Action is not in the registrar: ${p4Action.toString}")
    actual.getActionType match {
      case P4ActionType.Complex => handleComplexAction(actual.asInstanceOf[P4ComplexAction])
      case _ => handlePrimitiveAction(actual)
    }
  }

}
