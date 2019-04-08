package org.change.p4.control.queryimpl

import com.microsoft.z3._
import com.microsoft.z3.enumerations.Z3_decl_kind
import org.change.p4.control.GlobalParms
import org.change.p4.control.types._
import org.change.utils.Z3Helper._

case class RootMemory(structObject: StructObject, ocondition: ScalarValue)(
  implicit val context: Context
) {

  lazy val condition : BoolExpr = ocondition.AST.asBool

  def mkExtend(v : Value, to : Int): ScalarValue = v.ofType match {
    case BVType(n) if n < to =>
      val delta = to - n
      new ScalarValue(
        AST = context.mkZeroExt(delta, v.asInstanceOf[ScalarValue].AST.asBV),
        ofType = BVType(to)
      )
    case BVType(n) if n == to => v.asInstanceOf[ScalarValue]
    case _ => throw new IllegalArgumentException(
      s"trying to call extend to [$to] from $v of type ${v.ofType}"
    )
  }
  def mkExtract(v : Value, from : Int, to : Int): ScalarValue = v.ofType match {
    case BVType(n) if n > to =>
      new ScalarValue(
        AST = context.mkExtract(to, from, v.asInstanceOf[ScalarValue].AST.asBV),
        ofType = BVType(to - from + 1)
      )
    case _ => throw new IllegalArgumentException(
      s"trying to call extract [$from, $to] from $v of type ${v.ofType}"
    )
  }

  val typeMapper: TypeMapper = TypeMapper()
  def merge(other: RootMemory): RootMemory = {
    if (this.isEmpty()) {
      other
    } else if (other.isEmpty()) {
      this
    } else {
      RootMemory.merge(List(this, other))
    }
  }
  def unwrapImmutable(v: Value): Value = v match {
    case ImmutableValue(v) => v
    case _                 => v
  }
  def newV(old: Value, memPath: MemPath, v: Value): Value = {
    old match {
      case ImmutableValue(x) =>
        x
      case _ =>
        if (memPath.isEmpty) {
          v
        } else {
          memPath.head match {
            case Left(a) if a != "next__" =>
              val so = old.asInstanceOf[StructObject]
              so.copy(
                fieldRefs = so.fieldRefs +
                  (a -> newV(so.fieldRefs(a), memPath.tail, v))
              )
            case Left(a) if a == "next__" =>
              val so = old.asInstanceOf[ArrayObject]
              so.copy(next = v.asInstanceOf[ScalarValue])
            case Right(b) =>
              val so = old.asInstanceOf[ArrayObject]
              if (b < 0 || b >= so.elems.size) {
                so
              } else {
                so.copy(
                  elems = so.elems.updated(
                    b,
                    newV(
                      old.asInstanceOf[ArrayObject].elems(b),
                      memPath.tail,
                      v
                    )
                  )
                )
              }
          }
        }
    }
  }
  def set(memPath: MemPath, v: Value): RootMemory = {
    val t1 = typeOf(memPath)
    val t2 = v.ofType
    val setTo = if (t1 != v.ofType) {
      (t1, t2) match {
        case (BoundedInt(), UnboundedInt) =>
          val sv = v.asInstanceOf[ScalarValue]
          new ScalarValue(t1, sv.AST, maybeInt = sv.maybeInt,
            maybeBoolean = sv.maybeBoolean)
        case (BVType(n), BVType(m)) =>
          val scalarV = v.asInstanceOf[ScalarValue]
          if (n < m) {
            new ScalarValue(
              BVType(n),
              context.mkExtract(n - 1, 0, scalarV.AST.asBV),
              maybeInt = scalarV.maybeInt.map(f => {
                f & ((BigInt(1) << n) - 1)
              })
            )
          } else {
            new ScalarValue(
              BVType(n),
              context.mkZeroExt(n - m, scalarV.AST.asBV),
              maybeInt = scalarV.maybeInt
            )
          }
        case _ =>
          throw new IllegalArgumentException(
            s"cannot assign to " +
              s"$memPath of type $t1 the value $v of type $t2"
          )
      }
    } else {
      v
    }
    val newstruct = newV(structObject, memPath.reverse, setTo)
    this.copy(structObject = newstruct.asInstanceOf[StructObject])
  }
  def typeOf(memPath: MemPath): P4Type =
    memPath.foldRight(structObject.ofType: P4Type)((x, acc) => {
      x match {
        case Left(s) if s != "next__" => acc.asInstanceOf[StructType].members(s)
        case Left(s) if s == "next__" => BVType(8)
        case Right(i)                 => acc.asInstanceOf[FixedArrayType].inner
      }
    })

  def andArgs(ast : Expr) : Seq[Expr] =
    if (ast.isAnd) ast.getArgs.toSeq else Seq(ast)

  def mkAnd2(a: ScalarValue, b: ScalarValue): ScalarValue = a.ofType match {
    case BoolType =>
      (a.maybeBoolean, b.maybeBoolean) match {
        case (Some(false), _) | (_, Some(false)) =>
          new ScalarValue(
            BoolType,
            context.mkFalse(),
            maybeBoolean = Some(false)
          )
        case (Some(true), _) => b
        case (_, Some(true)) => a
        case _               =>
          val args = andArgs(a.AST) ++ andArgs(b.AST)
          new ScalarValue(BoolType,
            context.mkAnd(args.map(_.asBool):_*))
      }
    case bvType: BVType =>
      (a.maybeInt, b.maybeInt) match {
        case (Some(x), _) if x == 0  => typeMapper.literal(bvType, 0)
        case (_, Some(x)) if x == 0  => typeMapper.literal(bvType, 0)
        case (Some(x), _) if x == -1 => b
        case (_, Some(x)) if x == -1 => a
        case _                       =>
          new ScalarValue(bvType, context.mkBVAND(a.AST.asBV, b.AST.asBV))
      }
    case _ => throw new IllegalArgumentException(s"can't and between $a and $b")
  }
  def mkAnd(a: Iterable[ScalarValue]): ScalarValue = {
    if (a.isEmpty)
      throw new IllegalArgumentException("and accepts at least one argument")
    if (a.size == 1)
      a.head
    else a.tail.foldLeft(a.head)((acc, x) => mkAnd2(x, acc))
  }
  def mkOr2(a: ScalarValue, b: ScalarValue): ScalarValue = a.ofType match {
    case BoolType =>
      (a.maybeBoolean, b.maybeBoolean) match {
        case (Some(true), _) | (_, Some(true)) =>
          typeMapper.literal(BoolType, 1)
        case (Some(false), _) => b
        case (_, Some(false)) => a
        case (_, _) =>
          new ScalarValue(BoolType, context.mkOr(a.AST.asBool, b.AST.asBool))
      }
    case bvType: BVType =>
      (a.maybeInt, b.maybeInt) match {
        case (Some(x), _) if x == 0  => b
        case (_, Some(x)) if x == 0  => a
        case (Some(x), _) if x == -1 => typeMapper.literal(bvType, -1)
        case (_, Some(x)) if x == -1 => typeMapper.literal(bvType, -1)
        case _                       =>
          new ScalarValue(bvType, context.mkBVOR(a.AST.asBV, b.AST.asBV))
      }
    case _ => throw new IllegalArgumentException(s"can't and between $a and $b")
  }
  def mkXor2(a: ScalarValue, b: ScalarValue): ScalarValue = a.ofType match {
    case BoolType =>
      (a.maybeBoolean, b.maybeBoolean) match {
        case (Some(x), Some(y)) => mkBool(x.^(y))
        case (_, _) =>
          new ScalarValue(BoolType, context.mkXor(a.AST.asBool, b.AST.asBool))
      }
    case bvType: BVType =>
      (a.maybeInt, b.maybeInt) match {
        case (Some(x), Some(y)) => typeMapper.literal(bvType, x ^ y)
        case (_, _) =>
          new ScalarValue(bvType, context.mkBVXOR(a.AST.asBV, b.AST.asBV))
      }
    case _ => throw new IllegalArgumentException(s"can't xor between $a and $b")
  }
  def mkOr(a: Iterable[ScalarValue]): ScalarValue = {
    if (a.isEmpty)
      throw new IllegalArgumentException("or accepts at least one argument")
    if (a.size == 1)
      a.head
    else a.tail.foldLeft(a.head)((acc, x) => mkOr2(x, acc))
  }
  def mkBool(v: Boolean): ScalarValue = {
    if (v)
      new ScalarValue(BoolType, context.mkTrue(), maybeBoolean = Some(true))
    else
      new ScalarValue(BoolType, context.mkFalse(), maybeBoolean = Some(false))
  }

  def getNext(value: ArrayObject): ScalarValue = {
    value.next
  }
  def mkSelect(field: String, crt: Value): Value = {
    if (field == "next__") {
      crt.asInstanceOf[ArrayObject].next
    } else {
      crt match {
        case StructObject(_, flds) => flds(field)
        case _ =>
          throw new IllegalArgumentException(
            s"$crt must be a struct, can't access $field"
          )
      }
    }
  }
  def mkITE(condition: ScalarValue, thn: Value, els: Value): Value = {
    condition.maybeBoolean match {
      case Some(true)  => thn
      case Some(false) => els
      case _ =>
        thn match {
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
              context.mkITE(condition.AST.asBool, scalarValue.AST, s2.AST)
            )
        }
    }
  }
  def mkIndex(idx: Value, on: Value): (Value, Iterable[(ScalarValue, Int)]) =
    on match {
      case ArrayObject(t, _, flds) =>
        idx match {
          case sv: ScalarValue =>
            sv.maybeInt
              .map(idx => {
                if (idx >= 0 && idx < t.sz) {
                  (flds(idx.toInt), (mkBool(true), idx.toInt) :: Nil)
                } else {
                  (typeMapper.zeros(t.inner), (mkBool(true), idx.toInt) :: Nil)
                }
              })
              .getOrElse({
                val churn = flds.indices.map(u => {
                  (mkEQ(typeMapper.literal(idx.ofType, u), idx), u)
                })
                (flds.indices.foldLeft(typeMapper.zeros(t.inner))((acc, u) => {
                  mkITE(mkEQ(typeMapper.literal(idx.ofType, u), idx), flds(u), acc)
                }), churn)
              })
          case _ =>
            throw new IllegalArgumentException(s"$idx must be of int type")
        }
      case _ => throw new IllegalArgumentException(s"$on must be an array")
    }

  def set(churnedMemPath: ChurnedMemPath, v: Value): RootMemory = {
    val kind = if (churnedMemPath.nonEmpty) {
      typeOf(churnedMemPath.head._2)
    } else {
      structObject.ofType
    }
    if (churnedMemPath.isEmpty) {
      this.copy(structObject = v.asInstanceOf[StructObject])
    } else if (churnedMemPath.size == 1) {
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
  def get(memPath: MemPath): Value = {
    memPath.foldRight(structObject: Value)((x, acc) => {
      x match {
        case Left(s) if s != "next__" =>
          acc.asInstanceOf[StructObject].fieldRefs(s)
        case Left(s) if s == "next__" =>
          acc.asInstanceOf[ArrayObject].next
        case Right(i) => acc.asInstanceOf[ArrayObject].elems(i)
      }
    })
  }
  def isEmpty(): Boolean = {
    ocondition.maybeBoolean match {
      case Some(false) => true
      case _           => false
    }
  }

  def where(condition: ScalarValue): RootMemory =
    copy(ocondition = mkAnd2(ocondition, condition))

  def mkEQ(l: Value, r: Value): ScalarValue = {
    l match {
      case StructObject(_, flds) =>
        val rs = r.asInstanceOf[StructObject]
        mkAnd(flds.map(fv => mkEQ(rs.fieldRefs(fv._1), fv._2)))
      case ArrayObject(_, _, elems) =>
        val rs = r.asInstanceOf[ArrayObject]
        mkAnd(elems.zipWithIndex.map(fv => mkEQ(rs.elems(fv._2), fv._1)))
      case sv: ScalarValue =>
        val rv = r.asInstanceOf[ScalarValue]
        (sv.maybeBoolean, rv.maybeBoolean) match {
          case (Some(x), Some(y)) => mkBool(x == y)
          case _ =>
            (sv.maybeInt, rv.maybeInt) match {
              case (Some(x), Some(y)) => mkBool(x == y)
              case _ =>
                new ScalarValue(
                  BoolType,
                  context.mkEq(sv.AST, r.asInstanceOf[ScalarValue].AST)
                )
            }
        }
    }
  }
  def mkNot(l: Value): ScalarValue = l.ofType match {
    case b: BVType =>
      val ls = l.asInstanceOf[ScalarValue]
      ls.maybeInt match {
        case Some(x) =>
          typeMapper.literal(b, ((BigInt(1) << b.sz) - 1) ^ x)
        case _ => new ScalarValue(b, context.mkBVNot(ls.AST.asBV))
      }
    case BoolType =>
      val ls = l.asInstanceOf[ScalarValue]
      ls.maybeBoolean match {
        case Some(x) => mkBool(!x)
        case _       => new ScalarValue(BoolType, context.mkNot(ls.AST.asBool))
      }
  }
  def mkNeg(l: Value): ScalarValue = l.ofType match {
    case b: BVType =>
      val ls = l.asInstanceOf[ScalarValue]
      ls.maybeInt match {
        case Some(x) =>
          val ones = ((BigInt(1) << b.sz) - 1) ^ x
          typeMapper.literal(b, ones - 1)
        case _ => new ScalarValue(b, context.mkBVNeg(ls.AST.asBV))
      }
    case _ : IntType =>
      val ls = l.asInstanceOf[ScalarValue]
      ls.maybeInt match {
        case Some(x) => typeMapper.literal(UnboundedInt, -x)
        case _       =>
          new ScalarValue(UnboundedInt, context.mkUnaryMinus(ls.AST.asInt))
      }
  }
  def intResolve[T](l: Value, r: Value, c: (BigInt, BigInt) => T): Option[T] = {
    val lsc = l.asInstanceOf[ScalarValue]
    val rsc = r.asInstanceOf[ScalarValue]
    (lsc.maybeInt, rsc.maybeInt) match {
      case (Some(x), Some(y)) => Some(c(x, y))
      case _                  => None
    }
  }

  def mkLT(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ < _)
      .map(mkBool)
      .getOrElse({
        l match {
          case sv: ScalarValue if sv.ofType.isInstanceOf[IntType] =>
            intResolve(l, r, _ < _)
              .map(mkBool)
              .getOrElse(
                new ScalarValue(
                  BoolType,
                  context.mkLt(sv.AST.asInt, r.asInstanceOf[ScalarValue].AST.asInt)
                )
              )
          case sv: ScalarValue =>
            new ScalarValue(
              BoolType,
              context.mkBVULT(sv.AST.asBV, r.asInstanceOf[ScalarValue].AST.asBV)
            )
        }
      })
  def mkLE(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ <= _)
      .map(mkBool)
      .getOrElse({
        l match {
          case sv: ScalarValue if sv.ofType.isInstanceOf[IntType] =>
            new ScalarValue(
              BoolType,
              context.mkLe(sv.AST.asInt, r.asInstanceOf[ScalarValue].AST.asInt)
            )
          case sv: ScalarValue =>
            new ScalarValue(
              BoolType,
              context.mkBVULE(sv.AST.asBV, r.asInstanceOf[ScalarValue].AST.asBV)
            )
        }
      })
  def mkGE(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ >= _)
      .map(mkBool)
      .getOrElse({
        l match {
          case sv: ScalarValue if sv.ofType.isInstanceOf[IntType] =>
            new ScalarValue(
              BoolType,
              context.mkGe(sv.AST.asInt, r.asInstanceOf[ScalarValue].AST.asInt)
            )
          case sv: ScalarValue =>
            new ScalarValue(
              BoolType,
              context.mkBVUGE(sv.AST.asBV, r.asInstanceOf[ScalarValue].AST.asBV)
            )
        }
      })
  def mkGT(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ > _)
      .map(mkBool)
      .getOrElse({
        l match {
          case sv: ScalarValue if sv.ofType.isInstanceOf[IntType] =>
            new ScalarValue(
              BoolType,
              context.mkGt(sv.AST.asInt,
                r.asInstanceOf[ScalarValue].AST.asInt)
            )
          case sv: ScalarValue =>
            new ScalarValue(
              BoolType,
              context.mkBVUGT(sv.AST.asBV,
                r.asInstanceOf[ScalarValue].AST.asBV)
            )
        }
      })
  def mkAdd(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ + _)
      .map(typeMapper.literal(l.ofType, _))
      .getOrElse({
        l.ofType match {
          case _ : IntType =>
            new ScalarValue(
              UnboundedInt,
              context.mkAdd(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[ArithExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[ArithExpr]
              )
            )
          case b: BVType =>
            new ScalarValue(
              b,
              context.mkBVAdd(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr]
              )
            )
          case _ =>
            throw new IllegalArgumentException(
              s"can't add these two values $l + $r"
            )
        }
      })
  def mkSub(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ - _)
      .map(typeMapper.literal(l.ofType, _))
      .getOrElse({
        l.ofType match {
          case _ : IntType =>
            new ScalarValue(
              UnboundedInt,
              context.mkSub(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[ArithExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[ArithExpr]
              )
            )
          case b: BVType =>
            new ScalarValue(
              b,
              context.mkBVSub(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr]
              )
            )
          case _ =>
            throw new IllegalArgumentException(
              s"can't add these two values $l + $r"
            )
        }
      })
  def mkSHL(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ << _.toInt)
      .map(typeMapper.literal(l.ofType, _))
      .getOrElse({
        l.ofType match {
          case b: BVType =>
            new ScalarValue(
              b,
              context.mkBVSHL(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr]
              )
            )
          case _ =>
            throw new IllegalArgumentException(
              s"can't add these two values $l + $r"
            )
        }
      })

  def mkSHR(l: Value, r: Value): ScalarValue =
    intResolve(l, r, _ >> _.toInt)
      .map(typeMapper.literal(l.ofType, _))
      .getOrElse({
        l.ofType match {
          case b: BVType =>
            new ScalarValue(
              b,
              context.mkBVLSHR(
                l.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr],
                r.asInstanceOf[ScalarValue].AST.asInstanceOf[BitVecExpr]
              )
            )
          case _ =>
            throw new IllegalArgumentException(
              s"can't add these two values $l + $r"
            )
        }
      })
}


object RootMemory {
  def changeset(values : Iterable[Value],
                current: MemPath,
                ofType : P4Type)
      : Iterable[(MemPath, Iterable[ScalarValue])] = ofType match {
    case StructType(flds) =>
      flds.toList.flatMap(fieldDef => {
        val projection =
          values.map(_.asInstanceOf[StructObject].fieldRefs(fieldDef._1))
        val memPath = Left(fieldDef._1) :: current
        val newtype = fieldDef._2
        changeset(projection, memPath, newtype)
      })
    case FixedArrayType(inner, sz) =>
      (0 until sz).flatMap(idx => {
        val projection =
          values.map(_.asInstanceOf[ArrayObject].elems(idx))
        val memPath = Right(idx) :: current
        val newtype = inner
        changeset(projection, memPath, newtype)
      }) ++ changeset(
        values.map(_.asInstanceOf[ArrayObject].next),
        Left("next__") :: current,
        values.head.asInstanceOf[ArrayObject].next.ofType)
    case _ =>
      if (values.head.isInstanceOf[ImmutableValue]) {
        Nil
      } else {
        val hval = values.head.asInstanceOf[ScalarValue]
        val allTheSame = values.tail.forall(p => {
          val svr = p.asInstanceOf[ScalarValue]
          hval == svr
        })
        if (!allTheSame) {
          (current -> values.map(v => v.asInstanceOf[ScalarValue])) :: Nil
        } else {
          Nil
        }
      }
  }
  val debug = true
  def merge(rootMemories : Iterable[RootMemory]) : RootMemory = {
    val changed = changeset(rootMemories.map(_.structObject),
      emptyPath(),
      rootMemories.head.structObject.ofType)
    val template = rootMemories.head
    val newstruct = changed.foldLeft(template)((acc, ch) => {
      val ofType = ch._2.head.ofType
      val pref = if (debug) {
        ch._1.map({
          case Left(a) => a
          case Right(b) => s"[$b]"
        }).reverse.mkString(".") + ".merge"
      } else {
        "merge"
      }
      acc.set(ch._1, template.typeMapper.fresh(ofType, pref))
    })
    val conditions = changed.map(ch => {
      val newstuff = newstruct.get(ch._1).asInstanceOf[ScalarValue]
      ch._2.map(template.mkEQ(newstuff, _))
    })
    val initial = rootMemories.map(_.ocondition)
    val flat = conditions.foldLeft(initial)((acc, cd) => {
      acc.zip(cd).map(f => template.mkAnd2(f._1, f._2))
    })

    val context = template.context
    val iterators = flat.map(f => {
      if (f.AST.isApp) {
        val exp = f.AST
        val decl = exp.getFuncDecl
        if (decl.getDeclKind == Z3_decl_kind.Z3_OP_AND) {
          Some(exp.getArgs.toSeq.map(_.asInstanceOf[BoolExpr]))
        } else {
          None
        }
      } else None
    })
    val newcond = if (iterators.forall(_.nonEmpty)) {
      var actuals = iterators.map(_.get)
      var break = false
      var crt = 0
      var base = context.mkTrue()
      while (!break) {
        if (actuals.exists(x => x.size >= crt)) {
          break = true
        } else {
          val nowAt = actuals.map(_(crt))
          val h = nowAt.head
          if (!nowAt.tail.forall(_.equals(h))) {
            break = true
          } else {
            if (base == context.mkTrue())
              base = h
            else
              base = context.mkAnd(base, h)
            crt = crt + 1
          }
        }
      }
      val filtered = actuals.filter(f => f.size > crt)
      new ScalarValue(BoolType,
        if (filtered.nonEmpty) {
          context.mkAnd(base, context.mkOr(filtered.map(f => {
            context.mkAnd(f.drop(crt):_*)
          }).toList:_*))
        } else {
          base
        })
    } else {
      template.mkOr(flat)
    }

    // just enable this stupid check
    if (GlobalParms.checkMerging) {
      val oldone = template.mkOr(flat).AST
      val newast = newcond.AST
      val slv = context.mkSolver()
      slv.add(context.mkNot(context.mkEq(oldone, newast)))
      if (slv.check() != Status.UNSATISFIABLE) {
        System.err.println("should be")
        System.err.println(oldone.simplify())
        System.err.println("actually")
        System.err.println(newast.simplify())
        throw new AssertionError("wrong merging strategy")
      } else {
        System.err.println("merge OK")
      }
    }
    newstruct.copy(ocondition = newcond)(template.context)
  }
}