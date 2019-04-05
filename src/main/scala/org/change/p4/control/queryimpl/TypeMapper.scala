package org.change.p4.control.queryimpl
import org.change.p4.control.FlowStruct
import org.change.p4.control.types._
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.mutable

class TypeMapper()(implicit context: Z3Context) {
  val funMap = mutable.Map.empty[String, (P4Type, P4Type, Z3FuncDecl)]

  def applyFunction(name: String, z3AST: Z3AST): Z3AST = {
    funMap(name)._3(z3AST)
  }
  def applyFunction(name: String, value: Value): Value = {
    new ScalarValue(
      funMap(name)._2,
      applyFunction(name, value.asInstanceOf[ScalarValue].z3AST)
    )
  }
  def mkFunction(name: String, from: P4Type, to: P4Type): Unit = {
    val sortFrom = apply(from)
    val sortTo = apply(to)
    funMap.put(name, (from, to, context.mkFuncDecl(name, sortFrom, sortTo)))
  }

  val packetWrapper: PacketWrapper = PacketWrapper(context)
  def apply(p4Type: P4Type): Z3Sort = p4Type match {
    case BVType(n)      => context.mkBVSort(n)
    case PacketType     => packetWrapper.sort()
    case fs: FlowStruct => fs.sort()
    case IntType        => context.mkIntSort()
    case BoolType       => context.mkBoolSort()
  }
  def zeros(p4Type: P4Type): Value = p4Type match {
    case h: StructType =>
      val f = h.members.mapValues(zeros)
      StructObject(h, f)
    case a: FixedArrayType =>
      val flds = (0 until a.sz).map(_ => zeros(a.inner))
      ArrayObject(a, literal(IntType, 0), flds.toList)
    case PacketType => new ScalarValue(PacketType, packetWrapper.zero())
    case _          => literal(p4Type, 0)
  }
  def fresh(p4Type: P4Type, prefix: String = ""): Value = p4Type match {
    case st: StructType =>
      StructObject(st, st.members.mapValues(fresh(_, prefix)))
    case arr: FixedArrayType =>
      ArrayObject(
        arr,
        literal(IntType, 0),
        (0 until arr.sz).map(_ => fresh(arr.inner, prefix)).toList
      )
    case sv: P4Type =>
      val srt = this(sv)
      new ScalarValue(sv, context.mkFreshConst(prefix, srt))
  }
  def literal(p4Type: P4Type, v: BigInt): ScalarValue = {
    val tp = apply(p4Type)
    if (p4Type == BoolType) {
      if (v == 0)
        new ScalarValue(
          BoolType,
          tp.context.mkFalse(),
          maybeBoolean = Some(false)
        )
      else
        new ScalarValue(
          BoolType,
          tp.context.mkTrue(),
          maybeBoolean = Some(true)
        )
    } else {
      new ScalarValue(
        p4Type,
        tp.context.mkNumeral(v.toString(), tp),
        maybeInt = Some(v)
      )
    }
  }
}

object TypeMapper {
  val contextBoundMapper = mutable.Map.empty[Z3Context, TypeMapper]
  def apply()(implicit context: Z3Context): TypeMapper = {
    contextBoundMapper.getOrElseUpdate(context, new TypeMapper()(context))
  }
}
