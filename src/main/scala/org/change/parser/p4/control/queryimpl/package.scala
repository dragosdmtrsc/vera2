package org.change.parser.p4.control

import org.change.plugins.vera._
import z3.Z3Wrapper
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.mutable

package object queryimpl {
  trait Value {
    def ofType : P4Type
  }

  case class ImmutableValue(value : Value) extends Value {
    override def ofType: P4Type = value.ofType
  }

  trait MemoryObject extends Value
  case class StructObject(ofType: StructType,
                          fieldRefs : Map[String, Value]) extends MemoryObject
  case class ArrayObject(ofType: FixedArrayType,
                         next : ScalarValue,
                         elems : List[Value]) extends MemoryObject
  class ScalarValue(val ofType : P4Type,
                    val z3AST: Z3AST,
                    val maybeBoolean : Option[Boolean] = None,
                    val maybeInt : Option[BigInt] = None) extends Value {
  }
  type ChurnedMemPath  = Iterable[(ScalarValue, MemPath)]

  class PacketWrapper(implicit context: Z3Context) {

    private val takeFuns = mutable.Map.empty[(Int, Int), Z3FuncDecl]

    def getTakeFun(start : Int, end : Int): Z3FuncDecl = {
      takeFuns.getOrElseUpdate((start, end),
        context.mkFuncDecl(s"take_${start}_$end",
            packetSort._1, context.mkBVSort(end - start))
      )
    }

    val packetSort = context.mkADTSorts(Seq(
      ("Packet",
        Seq("nil", "pop"),
        Seq(
          Seq(),
          Seq(("pin", RecursiveType(0)),
            ("nr", RegularSort(context.mkIntSort()))
          )
        )
      )
    )).head

    def pop(packet: Z3AST, nr : Z3AST): Z3AST = {
      if (packet.getSort != sort()) {
        throw new IllegalArgumentException(s"cannot apply take on $packet, " +
          s"expecting something of sort ${sort()}")
      }
      packetSort._2(1)(packet, nr)
    }

    def zero() : Z3AST = packetSort._2.head()
    def sort() : Z3Sort = packetSort._1
    def take(p : Z3AST, start : Int, end : Int) : Z3AST = {
      if (p.getSort != sort()) {
        throw new IllegalArgumentException(s"cannot apply take on $p, " +
          s"expecting something of sort ${sort()}")
      }
      getTakeFun(start, end)(p)
    }
  }

  class TypeMapper()(implicit context: Z3Context) {
    val packetWrapper = new PacketWrapper()(context)
    def apply(p4Type: P4Type) : Z3Sort = p4Type match {
      case BVType(n) => context.mkBVSort(n)
      case PacketType => packetWrapper.sort()
      case fs : FlowStruct => fs.sort()
      case IntType => context.mkIntSort()
      case BoolType => context.mkBoolSort()
    }
    def zeros(p4Type: P4Type) : Value = p4Type match {
      case h : StructType =>
        val f = h.members.mapValues(zeros)
        StructObject(h, f)
      case a : FixedArrayType =>
        val flds = (0 until a.sz).map(_ => zeros(a.inner))
        ArrayObject(a, literal(IntType, 0), flds.toList)
      case PacketType => new ScalarValue(PacketType, packetWrapper.zero())
      case _ => literal(p4Type, 0)
    }
    def fresh(p4Type: P4Type, prefix : String = "") : Value = p4Type match {
      case st : StructType =>
        StructObject(st, st.members.mapValues(fresh(_, prefix)))
      case arr : FixedArrayType =>
        ArrayObject(arr,
          literal(IntType, 0),
          (0 until arr.sz).map(_ => fresh(arr.inner, prefix)).toList)
      case sv : P4Type =>
        val srt = this(sv)
        new ScalarValue(sv, context.mkFreshConst(prefix, srt))
    }
    def literal(p4Type: P4Type, v : BigInt): ScalarValue = {
      val tp = apply(p4Type)
      if (p4Type == BoolType) {
        if (v == 0)
          new ScalarValue(BoolType, tp.context.mkFalse(),
            maybeBoolean = Some(false))
        else new ScalarValue(BoolType, tp.context.mkTrue(),
          maybeBoolean = Some(true))
      } else {
        new ScalarValue(p4Type,
          tp.context.mkNumeral(v.toString(), tp), maybeInt = Some(v))
      }
    }
  }

  type MemPath = List[Either[String, Int]]
}
