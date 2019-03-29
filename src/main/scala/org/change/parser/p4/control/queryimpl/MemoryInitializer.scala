package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.FlowStruct
import org.change.plugins.vera._
import org.change.v2.p4.model.actions.P4ActionType
import org.change.v2.p4.model.table.TableDeclaration
import org.change.v2.p4.model.{ArrayInstance, Header, HeaderInstance, Switch}
import z3.scala.Z3Context

import scala.collection.JavaConverters._
object MemoryInitializer {

  def mkStruct(layout : Header): StructType = {
    StructType(layout.getFields.asScala.map(f => {
      f.getName -> BVType(f.getLength)
    }).toMap ++ Map("IsValid" -> BoolType))
  }

  def helperStuff() : Map[String, P4Type] = {
    Map(
      "packet" -> PacketType,
      "errorCause" -> IntType,
      "packetLength" -> IntType,
      "clone" -> IntType,
      "field_list_ref" -> IntType
    )
  }

  def tableStructures(switch: Switch)
                     (implicit context: Z3Context): Map[String, P4Type] = {
    switch.tables().asScala.map(tableDeclaration => {
      val lastQName = tableDeclaration.getName + "_lastQuery"
      lastQName -> FlowStruct(context, tableDeclaration, switch)
    }).toMap
  }

  def actionStructures(switch: Switch)
                      (implicit context: Z3Context) : Map[String, P4Type] = {
    switch.actions().asScala.filter(_.getActionType == P4ActionType.Complex).flatMap(x => {
      val aname = x.getActionName
      x.getParameterList.asScala.map(ap => {
        aname + "_" + ap.getParamName -> ap.getSort
      })
    }).toMap
  }

  def initialize(switch: Switch)(implicit context : Z3Context) : P4RootMemory = {
    val st = StructType(switch.getInstances.asScala.map(x =>
      x.getName -> (x match {
      case arrayInstance: ArrayInstance =>
        FixedArrayType(mkStruct(arrayInstance.getLayout), arrayInstance.getLength)
      case hi : HeaderInstance =>
        mkStruct(hi.getLayout)
    })).toMap ++ helperStuff ++ tableStructures(switch) ++ actionStructures(switch))
    val tm = new TypeMapper()(context)
    val structObject = tm.fresh(st).asInstanceOf[StructObject]
    P4RootMemory(switch, RootMemory(structObject = structObject, context.mkTrue()))
  }
}
