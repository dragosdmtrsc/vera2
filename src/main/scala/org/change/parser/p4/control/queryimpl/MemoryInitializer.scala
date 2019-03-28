package org.change.parser.p4.control.queryimpl

import org.change.plugins.vera._
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
      "errorCause" -> IntType
    )
  }

  def initialize(switch: Switch)(implicit context : Z3Context) : P4RootMemory = {
    val st = StructType(switch.getInstances.asScala.map(x =>
      x.getName -> (x match {
      case arrayInstance: ArrayInstance =>
        FixedArrayType(mkStruct(arrayInstance.getLayout), arrayInstance.getLength)
      case hi : HeaderInstance =>
        mkStruct(hi.getLayout)
    })).toMap ++ helperStuff)
    val tm = new TypeMapper()(context)
    val structObject = tm.fresh(st).asInstanceOf[StructObject]
    P4RootMemory(switch, context.mkFalse(), RootMemory(structObject = structObject, context.mkTrue()))
  }
}
