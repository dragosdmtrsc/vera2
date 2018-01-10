package org.change.parser.p4

import java.util.UUID

import org.change.v2.analysis.expression.abst.{Expression, FloatingExpression}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{Intable, Tag}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Assign, InstructionBlock, NoOp, _}
import org.change.v2.p4.model._
import org.change.v2.p4.model.actions._
import org.change.v2.p4.model.actions.primitives._
import org.change.v2.p4.model.InstanceType._

import scala.collection.JavaConversions._

object ActionInstance {
  private val readOnlyFields = Set[String](
    "standard_metadata.egress_port",
    "standard_metadata.ingress_port",
    "standard_metadata.instance_type",
    "standard_metadata.egress_instance"
  )
}

class ActionInstance(p4Action: P4Action,
                     argList : List[FloatingExpression],
                     switchInstance: ISwitchInstance,
                     switch : Switch,
                     table : String,
                     flowNumber : Int = -1,
                     dropMessage : String = "Dropped right here") {

  override def toString: String = s"${switchInstance.getName}.action.$table.$flowNumber"

  private def handleValidDestination(dst : String, continueWith : Instruction) = {
    val hdr = dst.split("\\.")(0)
    val cd = if (hdr.contains("["))
      hdr.split("\\[")(0)
    else
      hdr
    if (switch.getInstance(cd).isMetadata)
      continueWith
    else
      If (Constrain(hdr + ".IsValid", :==:(ConstantValue(1))),
        continueWith,
        Fail(s"Attempt to write to $dst, which is part of an invalid header")
      )
  }

  private def handleReadonlyDestination(dst : String, continueWith : Instruction) = {
    if (ActionInstance.readOnlyFields contains dst)
      Fail(s"Attempt to write to $dst, a read-only field")
    else
      continueWith
  }

  def handleComplexAction(complexAction: P4ComplexAction) : Instruction = {
    val arity = complexAction.getParameterList.size()
    if (arity != argList.size)
      throw new IllegalArgumentException(s"Wrong arity got ${argList.size} vs wanted $arity")
    val argNameToIndex = complexAction.getParameterList.zipWithIndex.map { x => x._1.getParamName -> x._2 }.toMap
    InstructionBlock(complexAction.getActionList.map( v => {
      val x = normalize(v)
      val args = x.parameterInstances().zip(x.getP4Action.getParameterList).map( pair => {
        val y = pair._1
        val formal = pair._2
        if ((y.getParameter.getType & P4ActionParameterType.UNKNOWN.x) != 0) {
          if ((formal.getType & P4ActionParameterType.FLDLIST.x) != 0) {
            :@(y.getValue)
          } else if ((formal.getType & P4ActionParameterType.HDR.x) != 0) {
            :@(y.getValue)
          } else if ((formal.getType & P4ActionParameterType.R_REF.x) != 0) {
            :@(y.getValue)
          } else if ((formal.getType & P4ActionParameterType.ARR.x) != 0) {
            :@(y.getValue)
          } else {
            argList(argNameToIndex(y.getValue))
          }
        } else {
          if ((y.getParameter.getType & P4ActionParameterType.VAL.x) != 0)
            ConstantValue(y.getValue.toLong)
          else
            :@(y.getValue)
        }
      }).toList
      new ActionInstance(x.getP4Action, args, switchInstance, switch, table, flowNumber, dropMessage).sefl()
    }).toList
    )
  }

  def handleModifyField(modifyField: ModifyField) : Instruction = {
    val argDest = argList.head.asInstanceOf[Symbol].id
    val argSource = argList(1)
    val dstField = argDest
    handleReadonlyDestination(dstField, handleValidDestination(dstField, Assign(dstField, argSource)))
  }

  def handleAddToField(addToField: AddToField) : Instruction = {
    val argDest = argList.head.asInstanceOf[Symbol].id
    val argSource = argList(1)
    handleReadonlyDestination(argDest, handleValidDestination(argDest, Assign(argDest, :+:(:@(argDest), argSource))))
  }

  def handleSubtractFromField(addToField: SubtractFromField) : Instruction = {
    val argDest = argList.head.asInstanceOf[Symbol].id
    val argSource = argList(1)
    handleReadonlyDestination(argDest, handleValidDestination(argDest, Assign(argDest, :-:(:@(argDest), argSource))))
  }

  def setOriginal() : Instruction = {
    InstructionBlock(switch.getInstances.flatMap(x => {
        x.getLayout.getFields.map(_.getName).map(y => {
          if (x.isMetadata) {
            NoOp
          } else {
            Assign("Original." + x.getName + "." + y, :@(x.getName + "." + y))
          }
        })
      }).toList
    )
  }

  def restore(butFor : List[String]) : Instruction = {
    InstructionBlock(switch.getInstances.flatMap(x => {
        if (!butFor.contains(x.getName)) {
          x.getLayout.getFields.map(y => {
            if (!butFor.contains(x.getName + "." + y.getName)) {
              if (x.isMetadata) {
//                InstructionBlock(
//                  Allocate(x.getName + "." + y.getName, y.getLength),
//                  Assign(x.getName + "." + y.getName, :@("Original." + x.getName + "." + y.getName))
//                )
                NoOp
              } else {
                If (Constrain(s"${x.getName}.IsValid", :==:(ConstantValue(1))),
                  InstructionBlock(
                    Allocate(x.getName + "." + y.getName, y.getLength),
                    Assign(x.getName + "." + y.getName, :@("Original." + x.getName + "." + y.getName))
                  )
                )
              }
            }
            else
              NoOp
          })
        } else {
          List[Instruction](NoOp)
        }
      }).toList
    )
  }


  def handleResubmit(resubmit: Resubmit): InstructionBlock = {
    val fldList = argList.head.asInstanceOf[Symbol].id
    val actualFieldList = switch.getFieldListMap()(fldList)
    InstructionBlock(
      restore(actualFieldList.getFields.toList),
      Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_RESUBMIT.value)),
      Forward(switchInstance.getName + ".parser")
    )
  }

  def handleRecirculate(recirculate: Recirculate): InstructionBlock = {
    if (argList.nonEmpty) {
      val fldList = argList.head.asInstanceOf[Symbol].id
      val actualFieldList = switch.getFieldListMap()(fldList)
      InstructionBlock(
        setOriginal(),
        restore(actualFieldList.getFields.toList),
        Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_RECIRC.value)),
        Forward(switchInstance.getName + ".parser")
      )
    } else {
      InstructionBlock(
        Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_RECIRC.value)),
        Forward(switchInstance.getName + ".parser")
      )
    }
  }

  def handleCloneFromIngressToIngress(cloneIngressPktToIngress: CloneIngressPktToIngress): Fork = {
    val fldList = argList(1).asInstanceOf[Symbol].id
    val actualFieldList = switch.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          handleCloneCookie(argList.head),
          restore(actualFieldList.getFields.toList),
          Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_INGRESS_CLONE.value)),
          Assign("IsClone", ConstantValue(1)),
          Forward(switchInstance.getName + ".parser")
        ),
        NoOp
      )
    )
  }

  def handleCloneFromIngressToEgress(cloneIngressPktToEgress: CloneIngressPktToEgress): Fork = {
    val fldList = argList(1).asInstanceOf[Symbol].id
    val actualFieldList = switch.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          handleCloneCookie(argList.head),
          restore(actualFieldList.getFields.toList),
          Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_EGRESS_CLONE.value)),
          Assign("IsClone", ConstantValue(1)),
          Forward(s"${switchInstance.getName}.buffer.in")
        ),
        NoOp
      )
    )
  }


  def handleCloneFromEgressToIngress(cloneEgressPktToIngress: CloneEgressPktToIngress): Fork = {
    val fldList = argList(1).asInstanceOf[Symbol].id
    val actualFieldList = switch.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          handleCloneCookie(argList.head),
          setOriginal(),
          restore(actualFieldList.getFields.toList),
          Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_INGRESS_CLONE.value)),
          Assign("IsClone", ConstantValue(1)),
          Forward(switchInstance.getName + ".parser")
        ),
        Forward("control.egress.out")
      )
    )
  }


  def handleCloneCookie(cookie : Long) : Instruction = {
    Assign(switchInstance.getName + ".CloneCookie", ConstantValue(cookie))
  }

  def handleCloneCookie(cookie : FloatingExpression) : Instruction = {
    Assign(switchInstance.getName + ".CloneCookie", cookie)
  }

  def handleCloneFromEgressToEgress(cloneEgressPktToIngress: CloneEgressPktToEgress): Fork = {
    val fldList = argList(1).asInstanceOf[Symbol].id
    val actualFieldList = switch.getFieldListMap()(fldList)
    Fork(
      List[Instruction](
        InstructionBlock(
          handleCloneCookie(argList.head),
          setOriginal(),
          restore(actualFieldList.getFields.toList),
          Assign("standard_metadata.instance_type", ConstantValue(PKT_INSTANCE_TYPE_EGRESS_CLONE.value)),
          Assign("IsClone", ConstantValue(1)),
          Forward(switchInstance.getName + ".buffer.in")
        ),
        NoOp
      )
    )
  }
  def handleAdd(addToField: Add) : Instruction = Assign(argList.head.asInstanceOf[Symbol].id, :+:(argList(1), argList(2)))

  def handleSubtract(subtract: Subtract) : Instruction = Assign(argList.head.asInstanceOf[Symbol].id, :-:(argList(1), argList(2)))

  def handleRegisterRead(regRead : RegisterRead) : Instruction = {
    val argDest = argList.head.asInstanceOf[Symbol]
    val argSource1 = argList(1).asInstanceOf[Symbol]
    val regSpec = switch.getRegisterSpecificationMap.get(argSource1.id)
    def readRegister(intVal: Long) = {
      val name = if (!regSpec.isStatic) {
        s"${switchInstance.getName}.reg.${argSource1.id}[$intVal]"
      } else {
        s"${switchInstance.getName}.reg[${regSpec.getStaticTable}].${argSource1.id}[$intVal]"
      }
      handleReadonlyDestination(argDest.id, handleValidDestination(argDest.id, Assign(argDest.id, :@(name))))
    }

    if (argList.length > 2) {
      if (regSpec.isStatic && regSpec.getStaticTable != this.table) {
        Fail(s"register_read encountered in $table, but $argSource1 is a static register belonging to ${regSpec.getStaticTable}")
      } else {
        val argSource2 = argList(2)
        // this is a global register
        argSource2 match {
          case ConstantValue(c, _, _) => readRegister(c)
          case _ => (0 until regSpec.getCount).foldRight(Fail(s"register index for ${argSource1.id} out of bounds") : Instruction)((x, acc) => {
            val tmp = s"tmp${UUID.randomUUID()}"
            InstructionBlock(
              Assign(tmp, argSource2),
              If (Constrain(tmp, :==:(ConstantValue(x))),
                readRegister(x),
                acc)
            )
          })
        }
      }
    } else {
      // this is a direct register => will be referenced by flow number -> don't forget to allocate when adding a new flow
      throw new UnsupportedOperationException("TODO: Direct registers not yet implemented ")
    }
  }

  private def regWriteParse(registerWrite: RegisterWrite) = {
    val argDest = argList.head.asInstanceOf[Symbol].id
    val index = if (argList.length > 2)
      argList(1)
    else
      throw new UnsupportedOperationException("TODO: Direct registers not yet implemented ")
    val source = if (argList.length > 2)
      argList(2)
    else
      argList(1)
    (argDest, index, source)
  }

  def handleRegisterWrite(regRead : RegisterWrite) : Instruction = {
    val (argDest, index, argSource1) = regWriteParse(regRead)

    val regSpec = switch.getRegisterSpecificationMap.get(argDest)
    def assignReg(intVal: Long) : Instruction = {
      val name = if (!regSpec.isStatic) {
        s"${switchInstance.getName}.reg.$argDest[$intVal]"
      } else {
        s"${switchInstance.getName}.reg[${regSpec.getStaticTable}].$argDest[$intVal]"
      }
      Assign(name, argSource1)
    }
    if (regSpec.isStatic &&
      regSpec.getStaticTable != this.table
    ) {
      Fail(s"register_write encountered in $table, but $argDest is a static register belonging to ${regSpec.getStaticTable}")
    } else {
      // this is a global register
      index match {
        case ConstantValue(c, _, _) =>
          if (c >= 0 && c < regSpec.getCount)
            assignReg(c)
          else
            Fail(s"register index $c for $argDest out of bounds")
        case _ => (0 until regSpec.getCount).foldRight(Fail(s"register index for $argDest out of bounds") : Instruction)((x, acc) => {
          val tmp = s"tmp${UUID.randomUUID()}"
          InstructionBlock(
            Assign(tmp, index),
            If (Constrain(tmp, :==:(ConstantValue(x))),
              assignReg(x),
              acc)
          )
        })
      }
    }
  }


  def moveHeader(hname : String, index : Int, newIndex : Int): Instruction = {
    val hInstance = switch.getInstance(hname).asInstanceOf[ArrayInstance]

    if (newIndex >= hInstance.getLength)
      NoOp
    else {
      if (index >= hInstance.getLength)
        NoOp
      else {
        InstructionBlock(hInstance.getLayout.getFields.map(x => {
            Assign(hname + newIndex + "." + x.getName, :@(hname + index + "." + x.getName))
          })
        )
      }
    }
  }

  def allocateHeader(hname : String, index : Int = 0): Instruction = switch.getInstance(hname) match {
    case hInstance : ArrayInstance =>
      if (index >= hInstance.getLength)
        NoOp
      else {
        InstructionBlock(hInstance.getLayout.getFields.flatMap(x => {
          List[Instruction](
            Allocate(hname + index + "." + x.getName, x.getLength),
            Assign(hname + index + "." + x.getName, ConstantValue(0))
          )
        }).toList
        )
      }
    case sInstance : org.change.v2.p4.model.HeaderInstance =>
      val oldinstance = sInstance
      InstructionBlock(sInstance.getLayout.getFields.flatMap(x => {
        List[Instruction](
          Allocate(oldinstance.getName + "." + x.getName, x.getLength),
          Assign(oldinstance.getName + "." + x.getName, ConstantValue(0))
        )
      }).toList
      )
    case _ => throw new UnsupportedOperationException(s"Cannot translate this register $hname")

  }

  def getNameAndIndex(dst : String): (String, String, Int) = {
    if (dst.contains("[")) {
      val index = dst.indexOf('[')
      val hname = dst.substring(0, index)
      val nrString = dst.substring(index + 1, dst.indexOf("]"))
      (hname, hname, Integer.decode(nrString).intValue())
    } else {
      (dst, dst, 0)
    }
  }

  def handleCopyHeader() : Instruction = {
    val dst = argList.head.asInstanceOf[Symbol].id
    val src = argList(1).asInstanceOf[Symbol].id

    val (regNameDst, _, _) = getNameAndIndex(dst)
    val (regNameSrc, _, _) = getNameAndIndex(src)

    val instanceDst = switch.getInstance(regNameDst)
    val instrList = InstructionBlock(instanceDst.getLayout.getFields.flatMap(x => {
        val fldName = x.getName
        List[Instruction](
          Allocate(dst + "." + fldName, x.getLength),
          Assign(dst + "." + fldName, :@(src + "." + fldName))
        )
      }).toList
    )
    If (Constrain(regNameSrc + ".IsValid", :==:(ConstantValue(1))),
      InstructionBlock(
        Assign(regNameDst + ".IsValid", ConstantValue(1)),
        instrList
      ),
      Assign(regNameDst + ".IsValid", ConstantValue(0))
    )
  }

  def handleRemoveHeader() : Instruction = {
    val headerInstance = argList.head.asInstanceOf[Symbol].id
    val (regName, hname, index) = getNameAndIndex(headerInstance)
    val instance = switch.getInstance(regName)
    If (Constrain(regName + ".IsValid", :==:(ConstantValue(1))),
      InstructionBlock(
//        Assign(regName + ".IsValid", ConstantValue(0)),
        if (regName != hname) {
          // if we are at a header array
          val instance = switch.getInstance(hname).asInstanceOf[ArrayInstance]
          val moveUpInstruction = (index + 1 until instance.getLength).map( x => {
            val newIndex = x - 1
            If (Constrain(hname + x + ".IsValid", :==:(ConstantValue(1))),
              if (newIndex < 0 || newIndex >= instance.getLength) {
                NoOp
              } else {
                If (Constrain(hname + newIndex + ".IsValid", :==:(ConstantValue(1))),
                  moveHeader(hname, x, newIndex),
                  InstructionBlock(
                    Assign(hname + newIndex + ".IsValid", ConstantValue(1)),
                    allocateHeader(hname, newIndex),
                    moveHeader(hname, x, newIndex)
                  )
                )
              },
              Assign(hname + newIndex + ".IsValid", ConstantValue(0))
            )
          }
          ).toList
          InstructionBlock(moveUpInstruction)
        } else {
          // when this is a scalar header
          InstructionBlock(
            Assign(hname + ".IsValid", ConstantValue(0))
          )
        },
        InstructionBlock(
          instance.getLayout.getFields.map( x => {
            InstructionBlock(
              Allocate(instance.getName + "." + x, x.getLength),
              Assign(instance.getName + "." + x, ConstantValue(0))
            )
            NoOp
          }).toList
        )
      ),
      Fail("Attempt to remove_header whilst header instance still not valid")
    )
  }

  def handleAddHeader(addHeader: AddHeader) : Instruction = {
    val headerInstance = argList.head.asInstanceOf[Symbol].id
    val (regName, hname, index) = getNameAndIndex(headerInstance)
    val instance = switch.getInstance(headerInstance)
    If (Constrain(regName + ".IsValid", :==:(ConstantValue(1))),
      Fail("Attempt to add_header whilst header instance is already valid"),
      InstructionBlock(
        Assign(regName + ".IsValid", ConstantValue(1)),
        if (regName != hname) {
          // if we are at a header array
          val instance = switch.getInstance(hname).asInstanceOf[ArrayInstance]
          val moveUpInstruction = (instance.getLength - 1).to(index, -1).map(x => {
              val newIndex = x + 1
              If (Constrain(hname + x + ".IsValid", :==:(ConstantValue(1))),
                if (newIndex >= instance.getLength) {
                  NoOp
                } else {
                  If (Constrain(hname + newIndex + ".IsValid", :==:(ConstantValue(1))),
                    moveHeader(hname, x, newIndex),
                    InstructionBlock(
                      Assign(hname + newIndex + ".IsValid", ConstantValue(1)),
                      allocateHeader(hname, newIndex),
                      moveHeader(hname, x, newIndex)
                    )
                  )
                },
                NoOp
              )
            }
          ).toList
          InstructionBlock(moveUpInstruction)
        } else {
          // when this is a scalar header
          InstructionBlock(
            Assign(regName + ".IsValid", ConstantValue(1)),
            allocateHeader(regName)
          )
        },
        InstructionBlock(
          instance.getLayout.getFields.map( x => {
            val fieldName = x
            InstructionBlock(
              Allocate(s"${instance.getName}.$fieldName", x.getLength),
              Assign(s"${instance.getName}.$fieldName", ConstantValue(0))
            )
            NoOp
          }).toList
        )
      )
    )
  }

  def handlePush(): Instruction = {
    val arrName = argList.head.asInstanceOf[Symbol].id
    val hdrArray = switch.getInstance(arrName).asInstanceOf[ArrayInstance]
    argList(1) match {
      case ConstantValue(c, _, _) => popBy(c.toInt, hdrArray, arrName)
      case _ => (0 until hdrArray.getLength).foldRight(NoOp : Instruction)((x, acc) => {
        val tmp = s"tmp${UUID.randomUUID()}"
        InstructionBlock(
          Assign(tmp, argList(1)),
          If (Constrain(tmp, :==:(ConstantValue(x))),
            pushBy(x, hdrArray, arrName),
            acc
          )
        )
      })
    }
  }

  private def pushBy(count: Int, hdrArray: ArrayInstance, arrName : String): Instruction = {
    val pushDown = (hdrArray.getLength - count - 1).to(0, -1).map(x => {
      new ActionInstance(switch.getActionRegistrar.getAction("copy_header"),
        List[FloatingExpression](:@(s"$arrName[${x + count}]"), :@(s"$arrName[$x]")), switchInstance, switch, table, flowNumber, dropMessage).sefl()
    }).toList

    val createNews = (0 until count).map(x => {
      new ActionInstance(switch.getActionRegistrar.getAction("add_header"),
        List[FloatingExpression](:@(s"$arrName[$x]")), switchInstance, switch, table, flowNumber, dropMessage).sefl()
    }).toList
    InstructionBlock(
      pushDown ++ createNews
    )
  }

  def handlePop(): Instruction = {
    val arrName = argList.head.asInstanceOf[Symbol].id
    val hdrArray = switch.getInstance(arrName).asInstanceOf[ArrayInstance]
    argList(1) match {
      case ConstantValue(c, _, _) => popBy(c.toInt, hdrArray, arrName)
      case _ => (0 until hdrArray.getLength).foldRight(NoOp : Instruction)((x, acc) => {
        val tmp = s"tmp${UUID.randomUUID()}"
        InstructionBlock(
          Assign(tmp, argList(1)),
          If (Constrain(tmp, :==:(ConstantValue(x))),
            popBy(x, hdrArray, arrName),
            acc
          )
        )
      })
    }
  }

  private def popBy(count: Int, hdrArray: ArrayInstance, arrName : String): Instruction = {
    val pushUp = (count until hdrArray.getLength).map(x => {
      new ActionInstance(switch.getActionRegistrar.getAction("copy_header"),
        List[FloatingExpression](:@(s"$arrName[${x - count}]"), :@(s"$arrName[$x]")), switchInstance, switch, table, flowNumber, dropMessage).sefl()
    })

    val deleteNews = (0 until count).map(x => {
      new ActionInstance(switch.getActionRegistrar.getAction("remove_header"),
        List[FloatingExpression](:@(s"$arrName[${hdrArray.getLength - x}]")), switchInstance, switch, table, flowNumber, dropMessage).sefl()
    })
    InstructionBlock(
      (pushUp ++ deleteNews).toList
    )
  }

  def handleBitAndOrXor(isAnd : Boolean, isOr : Boolean, isXor : Boolean): Instruction = {
    val argDest = argList.head
    val argSource1 = argList(1)
    val argSource2 = argList(2)
    val dstField = argDest.asInstanceOf[Symbol].id
    val arg1 = argSource1
    val arg2 = argSource2
    val fexp = if (isAnd) {
      :&&:(arg1, arg2)
    } else if (isOr) {
      :||:(arg1, arg2)
    } else if (isXor) {
      :^:(arg1, arg2)
    } else {
      throw new UnsupportedOperationException("AND, OR, XOR supported")
    }
    handleReadonlyDestination(dstField, handleValidDestination(dstField, Assign(dstField,fexp)))
  }

  def handleTruncate(): Instruction = {
    InstructionBlock(
      Assign("Truncate", ConstantValue(1)),
      Assign("TruncateSize", ConstantValue(argList.head.asInstanceOf[ConstantValue].value * 8))
    )
  }

  def handleShiftLeft(): Instruction = {
    val destination = argList.head.asInstanceOf[Symbol]
    handleReadonlyDestination(destination.id, handleValidDestination(destination.id, Assign(destination.id, :<<:(argList(1), argList(2)))))
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
      case P4ActionType.CloneIngressPktToIngress => handleCloneFromIngressToIngress(primitiveAction.asInstanceOf[CloneIngressPktToIngress])
      case P4ActionType.CloneIngressPktToEgress => handleCloneFromIngressToEgress(primitiveAction.asInstanceOf[CloneIngressPktToEgress])
      case P4ActionType.Resubmit => handleResubmit(primitiveAction.asInstanceOf[Resubmit])
      case P4ActionType.Recirculate => handleRecirculate(primitiveAction.asInstanceOf[Recirculate])
      case P4ActionType.AddHeader => handleAddHeader(primitiveAction.asInstanceOf[AddHeader])
      case P4ActionType.CopyHeader => handleCopyHeader()
      case P4ActionType.RemoveHeader => handleRemoveHeader()
      case P4ActionType.Pop => handlePop()
      case P4ActionType.Push => handlePush()
      case P4ActionType.BitAnd => handleBitAndOrXor(isAnd = true, isOr = false, isXor = false)
      case P4ActionType.BitOr => handleBitAndOrXor(isAnd = false, isOr = true, isXor = false)
      case P4ActionType.BitXor => handleBitAndOrXor(isAnd = false, isOr = false, isXor = true)
      case P4ActionType.ShiftLeft => handleShiftLeft() 
//        handleBitAndOrXor(isAnd = false, isOr = false, isXor = true)
      case P4ActionType.Truncate => handleTruncate()
      case _ => throw new UnsupportedOperationException(s"Primitive action of type ${primitiveAction.getActionType} not yet supported")
    }
  }


  def normalize(p4Action : P4Action): P4Action = {
    val actual = if (p4Action.getActionType == P4ActionType.UNKNOWN) {
      switch.getActionRegistrar.getAction(p4Action.getActionName)
    } else {
      p4Action
    }
    actual
  }
  def normalize(p4ActionCall: P4ActionCall) : P4ActionCall = {
    p4ActionCall.parameterInstances().foldLeft(new P4ActionCall(
      normalize(p4ActionCall.getP4Action)
    ))((acc, x) => {
      acc.addParameter(x)
    })
  }

  def sefl() : Instruction = {
    val actual = normalize(p4Action)
    if (actual == null || actual.getActionType == P4ActionType.UNKNOWN)
      throw new IllegalArgumentException(s"P4 Action is not in the registrar: $p4Action")
    InstructionBlock(
      actual.getActionType match {
        case P4ActionType.Complex => handleComplexAction(actual.asInstanceOf[P4ComplexAction])
        case _ => handlePrimitiveAction(actual)
      }
    )
  }

}
