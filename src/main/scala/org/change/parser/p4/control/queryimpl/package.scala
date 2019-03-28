package org.change.parser.p4.control

import org.change.plugins.vera._
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3Sort}

package object queryimpl {
  trait Value {
    def ofType : P4Type
  }
  trait MemoryObject extends Value
  case class StructObject(ofType: StructType,
                          fieldRefs : Map[String, Value]) extends MemoryObject
  case class ArrayObject(ofType: FixedArrayType,
                         elems : List[Value]) extends MemoryObject
  class ScalarValue(val ofType : P4Type, val z3AST: Z3AST) extends Value {
    private lazy val ctx = z3AST.context
    def copy(z3AST: Z3AST = this.z3AST) : ScalarValue = {
      new ScalarValue(ofType, z3AST)
    }
    def tryResolve : Option[Int] = {
      ctx.getNumeralInt(ctx.simplifyAst(z3AST)).value
    }
  }
  type ChurnedMemPath  = Iterable[(ScalarValue, MemPath)]

  class PacketWrapper(implicit context: Z3Context) {
    val packetSort = context.mkADTSorts(Seq(
      ("Packet",
        Seq("nil", "take"),
        Seq(
          Seq(),
          Seq(("pin", RecursiveType(0)),
            ("nr", RegularSort(context.mkIntSort()))
          )
        )
      )
    )).head

    def zero() : Z3AST = packetSort._2.head()
    def sort() : Z3Sort = packetSort._1
  }

  class TypeMapper()(implicit context: Z3Context) {
    val packetWrapper = new PacketWrapper()(context)
    def apply(p4Type: P4Type) : Z3Sort = p4Type match {
      case BVType(n) => context.mkBVSort(n)
      case PacketType => packetWrapper.sort()
      case IntType => context.mkIntSort()
      case BoolType => context.mkBoolSort()
    }
    def zeros(p4Type: P4Type) : Value = p4Type match {
      case h : StructType =>
        val f = h.members.mapValues(zeros)
        StructObject(h, f)
      case a : FixedArrayType =>
        val flds = (0 until a.sz).map(_ => zeros(a.inner))
        ArrayObject(a, flds.toList)
      case PacketType => new ScalarValue(PacketType, packetWrapper.zero())
      case _ => literal(p4Type, 0)
    }
    def fresh(p4Type: P4Type, prefix : String = "") : Value = p4Type match {
      case st : StructType =>
        StructObject(st, st.members.mapValues(fresh(_, prefix)))
      case arr : FixedArrayType =>
        ArrayObject(arr, (0 until arr.sz).map(_ => fresh(arr.inner, prefix)).toList)
      case sv : P4Type =>
        val srt = this(sv)
        new ScalarValue(sv, context.mkFreshConst(prefix, srt))
    }
    def literal(p4Type: P4Type, v : BigInt): ScalarValue = {
      val tp = apply(p4Type)
      new ScalarValue(p4Type, tp.context.mkNumeral(v.toString(), tp))
    }
  }

  type MemPath = List[Either[String, Int]]
}
