package org.change.p4.control.queryimpl

import com.microsoft.z3.Context
import org.change.p4.control.types.P4Type

import scala.collection.mutable

case class ExternFunction(name : String,
                     args : List[P4Type],
                     retType : P4Type,
                     context : Context) {
  val callsToRets = mutable.Map.empty[List[Value], Value]
  private val typeMapper = TypeMapper.apply()(context)
  def apply(args : List[Value]) : Value = {
    val ret = typeMapper.fresh(retType, s"call_$name")
    callsToRets.put(args, ret)
    ret
  }
}

object ExternFunction {
  val functionMap = mutable.Map.empty[String, ExternFunction]
  def declareFunction(externFunction: ExternFunction) : ExternFunction = {
    functionMap.getOrElseUpdate(externFunction.name, externFunction)
  }
  def clear() : Unit = functionMap.clear()
  def getFunction(name : String) : ExternFunction = functionMap(name)
  def hasFunction(name : String) : Boolean = functionMap.contains(name)
}

class EnumKind(val name : String, val members : List[String]) extends P4Type {
  private lazy val backMap = members.zipWithIndex.toMap
  def getId(mem : String): Int = backMap(mem)
}

object EnumKind {
  val enumKinds = mutable.Map.empty[String, EnumKind]
  def clear() : Unit = enumKinds.clear()
  def declareEnum(name : String, members : List[String]): EnumKind = {
    enumKinds.getOrElseUpdate(name, new EnumKind(name, members))
  }
}