package org.change.p4.control.queryimpl
import org.change.p4.control.types._
import com.microsoft.z3._
import org.change.utils.Z3Helper._
import scala.collection.mutable

class TypeMapper()(implicit context: Context) {
  val funMap = mutable.Map.empty[String, (P4Type, P4Type, FuncDecl)]
  val boundMap = mutable.Map.empty[BoundedInt, Set[Expr]]

  def boundAxioms(boundedInt: BoundedInt, vals : Set[Expr]) :
      List[BoolExpr] = {
    boundMap.getOrElse(boundedInt, Set.empty).map(e => {
      context.mkOr(vals.map(v => context.mkEq(e, v)).toSeq:_*)
    }).toList
  }
  def boundAxioms(boundedInt: BoundedInt, min : Int, max : Int) :
      List[BoolExpr] = {
    boundMap.getOrElse(boundedInt, Set.empty).flatMap(e => {
      context.mkLt(e.asInt, context.mkInt(max)) ::
        context.mkGe(e.asInt, context.mkInt(min)) :: Nil
    }).toList
  }

  def applyFunction(name: String, AST: Expr): Expr = {
    funMap(name)._3(AST)
  }
  def applyFunction(name: String, value: Value): Value = {
    new ScalarValue(
      funMap(name)._2,
      applyFunction(name, value.asInstanceOf[ScalarValue].AST)
    )
  }
  def mkFunction(name: String, from: P4Type, to: P4Type): Unit = {
    val sortFrom = apply(from)
    val sortTo = apply(to)
    funMap.put(name, (from, to, context.mkFuncDecl(name, sortFrom, sortTo)))
  }

  val packetWrapper: PacketWrapper = PacketWrapper(context)
  def apply(p4Type: P4Type): Sort = p4Type match {
    case BVType(n)      => context.mkBitVecSort(n)
    case PacketType     => packetWrapper.sort()
    case _ : IntType        => context.mkBVSort(16)
    case BoolType       => context.mkBoolSort()
  }

  def zeros(p4Type: P4Type): Value = p4Type match {
    case h: StructType =>
      val f = h.members.mapValues(zeros)
      StructObject(h, f)
    case a: FixedArrayType =>
      val max = 8
      val flds = (0 until a.sz).map(_ => zeros(a.inner))
      ArrayObject(a, literal(BVType(max), 0), flds.toList)
    case PacketType => new ScalarValue(PacketType, packetWrapper.zero())
    case _          => literal(p4Type, 0)
  }
  def fresh(p4Type: P4Type, prefix: String = ""): Value = p4Type match {
    case st: StructType =>
      StructObject(st, st.members.map(x => x._1 -> fresh(x._2, prefix + "." + x._1)))
    case arr: FixedArrayType =>
      val max = 8
      ArrayObject(
        arr,
        literal(BVType(max), 0),
        (0 until arr.sz).map(idx => fresh(arr.inner, prefix + s"[$idx]")).toList
      )
    case sv: P4Type =>
      val srt = this(sv)
      val res = new ScalarValue(sv, context.mkFreshConst(prefix, srt))
      sv match {
        case x: BoundedInt =>
          val crt = boundMap.getOrElse(x, Set.empty)
          boundMap.put(x, crt + res.AST)
        case _ =>
      }
      res
  }
  def literal(p4Type: P4Type, v: BigInt): ScalarValue = {
    val tp = apply(p4Type)
    if (p4Type == BoolType) {
      if (v == 0)
        new ScalarValue(
          BoolType,
          context.mkFalse(),
          maybeBoolean = Some(false)
        )
      else
        new ScalarValue(
          BoolType,
          context.mkTrue(),
          maybeBoolean = Some(true)
        )
    } else {
      new ScalarValue(
        p4Type,
        context.mkNumeral(v.toString(), tp),
        maybeInt = Some(v)
      )
    }
  }
}

object TypeMapper {
  val contextBoundMapper = mutable.Map.empty[Context, TypeMapper]
  def apply()(implicit context: Context): TypeMapper = {
    contextBoundMapper.getOrElseUpdate(context, new TypeMapper()(context))
  }
}
