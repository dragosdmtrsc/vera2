package org.change.parser.p4

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:+:, :-:, :@}
import org.change.v2.analysis.memory.{Intable, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{NoOp, _}
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.actions._
import org.change.v2.p4.model.actions.primitives._

import scala.collection.JavaConversions._

object ActionInstance {
  def apply(p4Action: P4Action, argList: List[Any],
            switchInstance: SwitchInstance, dropMessage: String = "Dropped right here"): ActionInstance = new ActionInstance(p4Action, argList, switchInstance, dropMessage)
}

class ActionInstance(p4Action: P4Action, argList : List[Any],
                     switchInstance: SwitchInstance, dropMessage : String = "Dropped right here") {

  private val ctx = switchInstance.getSwitchSpec.getCtx

  def handleComplexAction(complexAction: P4ComplexAction) : Instruction = {
    val arity = complexAction.getParameterList.size()
    if (arity != argList.size)
      throw new IllegalArgumentException(s"Wrong arity got ${argList.size} vs wanted $arity")
    val argNameToIndex = complexAction.getParameterList().zipWithIndex.map { x => x._1.getParamName -> x._2 }.toMap
    InstructionBlock(complexAction.getActionList.map( x => {
      val args = x.parameterInstances().map( y => {
        if ((y.getParameter.getType & P4ActionParameterType.UNKNOWN.x) != 0) {
          argList(argNameToIndex(y.getValue.toString))
        } else {
          y.getValue
        }
      }).toList
      new ActionInstance(x.getP4Action, args, switchInstance, dropMessage).sefl()
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
      case value : Long => {
        dstField match {
          case Left(j) => Assign(j, ConstantValue(value))
          case Right(s) => Assign(s, ConstantValue(value))
        }
      }
      case _: String =>
        try {
          val value = java.lang.Long.decode(argSource.toString).longValue
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
      case value: Int => ConstantValue(value)
      case value: Long => ConstantValue(value)
      case _: String =>
        try {
          val value = java.lang.Long.decode(arg.toString).longValue()
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

  def initialize() = {
    InstructionBlock(

    )
  }

  def setOriginal() : Instruction = {
    InstructionBlock(ctx.headerInstances.flatMap(x => {
        x._2.layout.fields.values.map(_._1).map(y => {
          if (x._2.isInstanceOf[MetadataInstance]) {
            NoOp
          } else {
            Assign("Original." + x._1 + "." + y, :@(Tag(x._1) + x._2.getTagOfField(y)))
          }
        })
      })
    )
  }

  def restore(butFor : List[String]) : Instruction = {
    InstructionBlock(ctx.headerInstances.flatMap(x => {
        if (!butFor.exists(p => p == x._1)) {
          x._2.layout.fields.values.map(_._1).map(y => {
            if (!butFor.exists(_ == x._1 + "." + y)) {
              if (x._2.isInstanceOf[MetadataInstance]) {
                Assign(x._1 + "." + y, :@("Original." + x._1 + "." + y))
              } else {
                Assign(Tag(x._1) + x._2.getTagOfField(y), :@("Original." + x._1 + "." + y))
              }
            }
            else
              NoOp
          })
        } else {
          List[Instruction](NoOp)
        }
      })
    )
  }


  def handleResubmit(resubmit: Resubmit) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    InstructionBlock(
      restore(actualFieldList.getFields.toList),
      Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(5)),
      Forward(switchInstance.getName + ".parser")
    )
  }

  def handleRecirculate(recirculate: Recirculate) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    InstructionBlock(
      setOriginal,
      restore(actualFieldList.getFields.toList),
      Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(6)),
      Forward(switchInstance.getName + ".parser")
    )
  }

  def handleCloneFromIngressToIngress(cloneIngressPktToIngress: CloneIngressPktToIngress) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          restore(actualFieldList.getFields.toList),
          Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(1)),
          Forward(switchInstance.getName + ".parser")
        ),
        Forward(s"${switchInstance.getName}.egress")
      )
    )
  }

  def handleCloneFromIngressToEgress(cloneIngressPktToEgress: CloneIngressPktToEgress) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          restore(actualFieldList.getFields.toList),
          Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(2)),
          Forward(switchInstance.getName + ".out")
        ),
        Forward(s"${switchInstance.getName}.egress")
      )
    )
  }


  def handleCloneFromEgressToIngress(cloneEgressPktToIngress: CloneEgressPktToIngress) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          setOriginal,
          restore(actualFieldList.getFields.toList),
          Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(3)),
          Forward(switchInstance.getName + ".parser")
        ),
        Forward(s"${switchInstance.getName}.out")
      )
    )
  }


  def handleCloneFromEgressToEgress(cloneEgressPktToIngress: CloneEgressPktToEgress) = {
    val fldList = argList(0).toString
    val actualFieldList = switchInstance.getSwitchSpec.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          setOriginal,
          restore(actualFieldList.getFields.toList),
          Assign(ctx.resolveField("standard_metadata.instance_type").right.get, ConstantValue(4)),
          Forward(switchInstance.getName + ".egress")
        ),
        Forward(s"${switchInstance.getName}.out")
      )
    )
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
      val intVal = java.lang.Long.decode(argSource2.toString).longValue()
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
      val intVal = java.lang.Long.decode(argSource2.toString).longValue
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

  def handleAddHeader(addHeader: AddHeader) : Instruction = {
    val headerInstance = argList(0).toString
    val regName = if (headerInstance.contains("[")) {
      val index = headerInstance.indexOf('[')
      headerInstance.substring(0, index)
    } else {
      headerInstance
    }
    val hdrInstance = ctx.headerInstances(regName)
    hdrInstance match {
      case u : ArrayHeader => Assign(u.arrayName, :@(""))
    }
  }



  def handlePrimitiveAction(primitiveAction : P4Action) : Instruction = {
    primitiveAction.getActionType match {
      case P4ActionType.AddToField => handleAddToField(primitiveAction.asInstanceOf[AddToField])
      case P4ActionType.Add => handleAdd(primitiveAction.asInstanceOf[Add])
      case P4ActionType.Subtract => handleSubtract(primitiveAction.asInstanceOf[Subtract])
      case P4ActionType.SubtractFromField => handleSubtractFromField(primitiveAction.asInstanceOf[SubtractFromField])
      case P4ActionType.ModifyField => handleModifyField(primitiveAction.asInstanceOf[ModifyField])
      case P4ActionType.Drop => Fail(dropMessage)
      case P4ActionType.NoOp => NoOp
      case P4ActionType.RegisterRead => handleRegisterRead(primitiveAction.asInstanceOf[RegisterRead])
      case P4ActionType.RegisterWrite => handleRegisterWrite(primitiveAction.asInstanceOf[RegisterWrite])
      case P4ActionType.CloneEgressPktToEgress => handleCloneFromEgressToEgress(primitiveAction.asInstanceOf[CloneEgressPktToEgress])
      case P4ActionType.CloneEgressPktToIngress => handleCloneFromEgressToIngress(primitiveAction.asInstanceOf[CloneEgressPktToIngress])
      case P4ActionType.CloneIngressPktToEgress => handleCloneFromIngressToIngress(primitiveAction.asInstanceOf[CloneIngressPktToIngress])
      case P4ActionType.CloneIngressPktToEgress => handleCloneFromIngressToEgress(primitiveAction.asInstanceOf[CloneIngressPktToEgress])
      case P4ActionType.Resubmit => handleResubmit(primitiveAction.asInstanceOf[Resubmit])
      case P4ActionType.Recirculate => handleRecirculate(primitiveAction.asInstanceOf[Recirculate])
      case _ => throw new UnsupportedOperationException(s"Primitive action of type ${primitiveAction.getActionType} not yet supported")
    }
  }

  def sefl() : Instruction = {
    val actual = if (p4Action.getActionType == P4ActionType.UNKNOWN) {
      switchInstance.getSwitchSpec.getActionRegistrar.getAction(p4Action.getActionName)
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
