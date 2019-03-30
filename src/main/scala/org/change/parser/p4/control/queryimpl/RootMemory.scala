package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.P4Query
import org.change.plugins.vera.{FixedArrayType, _}
import z3.scala.{Z3AST, Z3Context}

case class RootMemory(structObject: StructObject,
                      condition: Z3AST)
                     (implicit val context : Z3Context) {
  def mkPacketTake(value: Value,
                   from: Value,
                   to: Value): Value = {
    val scalarValue = value.asInstanceOf[ScalarValue]
    val fromI = from.asInstanceOf[ScalarValue].tryResolve.get
    val toI = to.asInstanceOf[ScalarValue].tryResolve.get
    new ScalarValue(
      BVType((toI - fromI).toInt),
      typeMapper.packetWrapper.take(scalarValue.z3AST, fromI.toInt, toI.toInt)
    )
  }

  def mkPacketPop(value : Value, n : Value): ScalarValue = {
    val v = value.asInstanceOf[ScalarValue]
    val nr = n.asInstanceOf[ScalarValue].z3AST
    new ScalarValue(
      PacketType,
      typeMapper.packetWrapper.pop(v.z3AST, nr)
    )
  }

  val typeMapper = new TypeMapper

  def leftMerge(l : Value, r : Value) : (ScalarValue, ScalarValue, Value) = l match {
    case ImmutableValue(v) =>
      r match {
        case ImmutableValue(rv) => assert(rv == v)
          (mkBool(true), mkBool(true), l)
        case _ => throw new IllegalStateException(s"can't merge immutable with mutable $l $r")
      }
    case StructObject(ofType, fieldRefs) =>
      val lm = fieldRefs.foldLeft((Map.empty[String, Value], mkBool(true), mkBool(true)))((acc, lv) => {
        val (c1, c2, v) = leftMerge(lv._2, r.asInstanceOf[StructObject].fieldRefs(lv._1))
        (acc._1 + (lv._1 -> v), mkAnd2(c1, acc._2), mkAnd2(c2, acc._3))
      })
      (lm._2, lm._3, StructObject(ofType, lm._1))
    case ArrayObject(ofType, n, elems) =>
      val lm = elems.zipWithIndex.foldLeft((List.empty[Value], mkBool(true), mkBool(true)))((acc, lv) => {
        val (c1, c2, v) = leftMerge(lv._1, r.asInstanceOf[ArrayObject].elems(lv._2))
        (acc._1 :+ v, mkAnd2(c1, acc._2), mkAnd2(c2, acc._3))
      })
      val oarr = r.asInstanceOf[ArrayObject]
      val (c1, c2, vnext) = leftMerge(n, oarr.next)
      (mkAnd2(lm._2, c1), mkAnd2(lm._3, c2), ArrayObject(ofType,
        vnext.asInstanceOf[ScalarValue], lm._1))
    case sv : ScalarValue =>
      val sd = r.asInstanceOf[ScalarValue]
      if (context.isEqAST(sd.z3AST, sv.z3AST)) {
        (mkBool(true), mkBool(true), sv)
      } else {
        val fresh = typeMapper.fresh(sv.ofType, "merge")
        (mkEQ(fresh, sv), mkEQ(fresh, sd), fresh)
      }
  }

  def merge(other: RootMemory) : RootMemory = {
    if (this.isEmpty()) {
      other
    } else if (other.isEmpty()) {
      this
    } else {
      val (c1, c2, va) = leftMerge(structObject, other.structObject)
      val c1s = context.simplifyAst(c1.z3AST)
      val c2s = context.simplifyAst(c2.z3AST)
      copy(
        structObject = va.asInstanceOf[StructObject],
        condition = context.mkOr(
          other.where(c2s).condition,
          where(c1s).condition
        )
      )
    }
  }

  def unwrapImmutable(v: Value) : Value = v match {
    case ImmutableValue(v) => v
    case _ => v
  }

  def newV(old : Value, memPath: MemPath, v : Value) : Value = {
    old match {
      case ImmutableValue(x) =>
        x
      case _ => if (memPath.isEmpty) {
        v
      } else {
        memPath.head match {
          case Left(a) if a != "next__" =>
            val so = old.asInstanceOf[StructObject]
            so.copy(fieldRefs = so.fieldRefs +
              (a -> newV(so.fieldRefs(a), memPath.tail, v)))
          case Left(a) if a == "next__" =>
            val so = old.asInstanceOf[ArrayObject]
            so.copy(next = v.asInstanceOf[ScalarValue])
          case Right(b) =>
            val so = old.asInstanceOf[ArrayObject]
            if (b < 0 || b >= so.elems.size) {
              so
            } else {
              so.copy(elems = so.elems.updated(b,
                newV(old.asInstanceOf[ArrayObject].elems(b), memPath.tail, v)
              ))
            }
        }
      }
    }
  }
  def set(memPath: MemPath, v : Value) : RootMemory = {
    val t1 = typeOf(memPath)
    val t2 = v.ofType
    val setTo = if (t1 != v.ofType) {
      (t1, t2) match {
        case (BVType(n), BVType(m)) =>
          val scalarV = v.asInstanceOf[ScalarValue]
          if (n < m) {
            new ScalarValue(
              BVType(n), context.mkExtract(n - 1, 0,
                scalarV.z3AST))
          } else {
            new ScalarValue(
              BVType(n), context.mkZeroExt(n - m,
                scalarV.z3AST))
          }
        case _ => throw new IllegalArgumentException(s"cannot assign to " +
          s"$memPath of type $t1 the value $v of type $t2")
      }
    } else {
      v
    }
    val newstruct = newV(structObject, memPath.reverse, setTo)
    this.copy(structObject = newstruct.asInstanceOf[StructObject])
  }
  def typeOf(memPath: MemPath) : P4Type =
    memPath.foldRight(structObject.ofType : P4Type)((x, acc) => {
      x match {
        case Left(s) if s != "next__" => acc.asInstanceOf[StructType].members(s)
        case Left(s) if s == "next__" => IntType
        case Right(i) => acc.asInstanceOf[FixedArrayType].inner
      }
    })
  def mkAnd2(a : ScalarValue, b : ScalarValue) : ScalarValue = a.ofType match {
    case BoolType => new ScalarValue(BoolType, context.mkAnd(a.z3AST, b.z3AST))
    case bvType: BVType => new ScalarValue(bvType, context.mkBVAnd(a.z3AST, b.z3AST))
    case _ => throw new IllegalArgumentException(s"can't and between $a and $b")
  }
  def mkAnd(a : Iterable[ScalarValue]): ScalarValue = {
    if (a.isEmpty)
      throw new IllegalArgumentException("and accepts at least one argument")
    if (a.size == 1)
      a.head
    else a.tail.foldLeft(a.head)((acc, x) => mkAnd2(x, acc))
  }
  def mkOr2(a : ScalarValue, b : ScalarValue) : ScalarValue = a.ofType match {
    case BoolType => new ScalarValue(BoolType, context.mkOr(a.z3AST, b.z3AST))
    case bvType: BVType => new ScalarValue(bvType, context.mkBVOr(a.z3AST, b.z3AST))
    case _ => throw new IllegalArgumentException(s"can't and between $a and $b")
  }
  def mkXor2(a : ScalarValue, b : ScalarValue) : ScalarValue = a.ofType match {
    case BoolType => new ScalarValue(BoolType, context.mkXor(a.z3AST, b.z3AST))
    case bvType: BVType => new ScalarValue(bvType, context.mkBVXor(a.z3AST, b.z3AST))
    case _ => throw new IllegalArgumentException(s"can't and between $a and $b")
  }
  def mkOr(a : Iterable[ScalarValue]): ScalarValue = {
    if (a.isEmpty)
      throw new IllegalArgumentException("or accepts at least one argument")
    if (a.size == 1)
      a.head
    else a.tail.foldLeft(a.head)((acc, x) => mkOr2(x, acc))
  }
  def mkBool(v : Boolean) : ScalarValue = {
    if (v) new ScalarValue(BoolType, context.mkTrue()) else new ScalarValue(BoolType, context.mkFalse())
  }

  def getNext(value : ArrayObject) : ScalarValue = {
    value.next
  }
  def mkSelect(field : String, crt : Value): Value = {
    if (field == "next__") {
      crt.asInstanceOf[ArrayObject].next
    } else {
      crt match {
        case StructObject(_, flds) => flds(field)
        case _ => throw new IllegalArgumentException(s"$crt must be a struct, can't access $field")
      }
    }
  }
  def mkITE(condition : ScalarValue, thn : Value, els : Value) : Value = thn match {
    case StructObject(t, flds) =>
      val e = els.asInstanceOf[StructObject]
      StructObject(t, flds.map(v => {
        v._1 -> mkITE(condition, v._2, e.fieldRefs(v._1))
      }))
    case ArrayObject(t, n, elems) =>
      val a2 = els.asInstanceOf[ArrayObject]
      ArrayObject(t, n, elems.zipWithIndex.map(v => {
        mkITE(condition, v._1, a2.elems(v._2))
      }))
    case scalarValue: ScalarValue =>
      val s2 = els.asInstanceOf[ScalarValue]
      new ScalarValue(
        scalarValue.ofType,
        context.mkITE(condition.z3AST, scalarValue.z3AST, s2.z3AST)
      )
  }
  def mkIndex(idx : Value, on : Value) : (Value, Iterable[(ScalarValue, Int)]) = on match {
    case ArrayObject(t, _, flds) =>
      idx match {
        case sv : ScalarValue if sv.ofType == IntType =>
          sv.tryResolve.map(idx => {
            if (idx >= 0 && idx < t.sz) {
              (flds(idx.toInt), (mkBool(true), idx.toInt) :: Nil)
            } else {
              (typeMapper.zeros(t.inner), (mkBool(true), idx.toInt) :: Nil)
            }
          }).getOrElse({
            val churn = flds.indices.map(u => {
              (mkEQ(typeMapper.literal(IntType, u), idx), u)
            })
            (flds.indices.foldLeft(typeMapper.zeros(t.inner))((acc, u) => {
              mkITE(mkEQ(typeMapper.literal(IntType, u), idx),
                flds(u), acc)
            }), churn)
          })
        case _ => throw new IllegalArgumentException(s"$idx must be of int type")
      }
    case _ => throw new IllegalArgumentException(s"$on must be an array")
  }

  def set(churnedMemPath: ChurnedMemPath, v : Value) : RootMemory = {
    val kind = typeOf(churnedMemPath.head._2)
    if (churnedMemPath.size == 1) {
      set(churnedMemPath.head._2, v)
    } else {
      val assigned = churnedMemPath.foldLeft(this)((acc, f) => {
        set(f._2, typeMapper.fresh(kind, "fresh"))
      })
      assigned.where(mkAnd(churnedMemPath.map(x => {
        val cond = x._1
        val old = get(x._2)
        val novel = assigned.get(x._2)
        mkEQ(novel, mkITE(cond, v, old))
      })))
    }
  }
  def get(memPath: MemPath) : Value = {
    memPath.foldRight(structObject : Value)((x, acc) => {
      x match {
        case Left(s) if s != "next__" => acc.asInstanceOf[StructObject].fieldRefs(s)
        case Left(s) if s == "next__" =>
          acc.asInstanceOf[ArrayObject].next
        case Right(i) => acc.asInstanceOf[ArrayObject].elems(i)
      }
    })
  }

  def getBoolValue(ast : Z3AST) : Either[Boolean, Z3AST] = {
    val simplified = ast.context.simplifyAst(ast)
    context.getBoolValue(simplified).map(f => Left(f)).getOrElse(Right(simplified))
  }

  def isEmpty() : Boolean = {
    val got = getBoolValue(condition)
    got match {
      case Left(false) => true
      case _ => false
    }
  }

  def where(condition : Z3AST): RootMemory = {
    copy(condition = getBoolValue(condition) match {
      case Left(false) => context.mkFalse()
      case Left(true) => this.condition
      case Right(ast) => context.mkAnd(this.condition, ast)
    })
  }
  def where(condition : ScalarValue): RootMemory = where(condition.z3AST)

  def mkEQ(l : Value, r : Value) : ScalarValue = {
    l match {
      case StructObject(_, flds) =>
        val rs = r.asInstanceOf[StructObject]
        new ScalarValue(BoolType, context.mkAnd(flds.map(fv => {
          mkEQ(rs.fieldRefs(fv._1), fv._2)
        }).map(_.z3AST).toList:_*))
      case ArrayObject(_, _, elems) =>
        val rs = r.asInstanceOf[ArrayObject]
        new ScalarValue(BoolType, context.mkAnd(elems.zipWithIndex.map(fv => {
          mkEQ(rs.elems(fv._2), fv._1)
        }).map(_.z3AST):_*))
      case sv : ScalarValue =>
        new ScalarValue(BoolType,
          context.mkEq(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
    }
  }
  def mkNot(l : Value) : ScalarValue = l.ofType match {
    case b : BVType => new ScalarValue(b, context.mkBVNot(l.asInstanceOf[ScalarValue].z3AST))
    case BoolType => new ScalarValue(BoolType, context.mkNot(l.asInstanceOf[ScalarValue].z3AST))
  }
  def mkNeg(l : Value) : ScalarValue = l.ofType match {
    case b : BVType => new ScalarValue(b, context.mkBVNeg(l.asInstanceOf[ScalarValue].z3AST))
    case IntType => new ScalarValue(IntType, context.mkUnaryMinus(l.asInstanceOf[ScalarValue].z3AST))
  }
  def mkLT(l : Value, r : Value) : ScalarValue = {
    l match {
      case sv : ScalarValue if sv.ofType == IntType =>
        new ScalarValue(BoolType,
          context.mkLT(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
      case sv : ScalarValue =>
        new ScalarValue(BoolType,
          context.mkBVSlt(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
    }
  }
  def mkLE(l : Value, r : Value) : ScalarValue = {
    l match {
      case sv : ScalarValue if sv.ofType == IntType =>
        new ScalarValue(BoolType,
          context.mkLE(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
      case sv : ScalarValue =>
        new ScalarValue(BoolType,
          context.mkBVSle(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
    }
  }
  def mkGE(l : Value, r : Value) : ScalarValue = {
    l match {
      case sv : ScalarValue if sv.ofType == IntType =>
        new ScalarValue(BoolType,
          context.mkGE(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
      case sv : ScalarValue =>
        new ScalarValue(BoolType,
          context.mkBVSge(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
    }
  }
  def mkGT(l : Value, r : Value) : ScalarValue = {
    l match {
      case sv : ScalarValue if sv.ofType == IntType =>
        new ScalarValue(BoolType,
          context.mkGT(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
      case sv : ScalarValue =>
        new ScalarValue(BoolType,
          context.mkBVSgt(sv.z3AST, r.asInstanceOf[ScalarValue].z3AST))
    }
  }
  def mkAdd(l : Value, r : Value) : ScalarValue = {
    l.ofType match {
      case IntType => new ScalarValue(
        IntType,
        context.mkAdd(l.asInstanceOf[ScalarValue].z3AST,
                      r.asInstanceOf[ScalarValue].z3AST)
      )
      case b : BVType =>
        new ScalarValue(
          b,
          context.mkBVAdd(l.asInstanceOf[ScalarValue].z3AST,
            r.asInstanceOf[ScalarValue].z3AST)
        )
      case _ => throw new IllegalArgumentException(s"can't add these two values $l + $r")
    }
  }
  def mkSub(l : Value, r : Value) : ScalarValue = {
    l.ofType match {
      case IntType => new ScalarValue(
        IntType,
        context.mkSub(l.asInstanceOf[ScalarValue].z3AST,
          r.asInstanceOf[ScalarValue].z3AST)
      )
      case b : BVType =>
        new ScalarValue(
          b,
          context.mkBVSub(l.asInstanceOf[ScalarValue].z3AST,
            r.asInstanceOf[ScalarValue].z3AST)
        )
      case _ => throw new IllegalArgumentException(s"can't add these two values $l + $r")
    }
  }
  def mkSHL(l : Value, r : Value) : ScalarValue = {
    l.ofType match {
      case b : BVType =>
        new ScalarValue(
          b,
          context.mkBVShl(l.asInstanceOf[ScalarValue].z3AST,
            r.asInstanceOf[ScalarValue].z3AST)
        )
      case _ => throw new IllegalArgumentException(s"can't add these two values $l + $r")
    }
  }

  def condition(cd: Z3AST,
                value : Value,
                els : Option[Value] = None) : Value = value.ofType match {
    case FixedArrayType(inner, sz) =>
      val arrayObject = value.asInstanceOf[ArrayObject]
      arrayObject.copy(elems = arrayObject.elems.map(condition(cd, _)))
    case StructType(flds) =>
      val structObject = value.asInstanceOf[StructObject]
      structObject.copy(fieldRefs = structObject.fieldRefs.mapValues(condition(cd, _)))
    case _ =>
      val scalarValue = value.asInstanceOf[ScalarValue]
      scalarValue.copy(cd.context.mkITE(
        cd,
        scalarValue.z3AST,
        typeMapper.zeros(scalarValue.ofType).asInstanceOf[ScalarValue].z3AST
      ))
  }

}


