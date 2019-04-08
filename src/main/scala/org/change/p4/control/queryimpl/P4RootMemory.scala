package org.change.p4.control.queryimpl

import com.microsoft.z3.enumerations.{Z3_ast_kind, Z3_decl_kind, Z3_lbool}
import com.microsoft.z3.{AST, BitVecNum, IntNum}
import org.change.p4.control._
import org.change.p4.control.types._
import org.change.p4.model.Switch

trait AbsValueWrapper extends P4Query {
  def value: Value
  def maybePath: Option[ChurnedMemPath]

  override def toString: String = value.toString

  override def toBool: Option[Boolean] = {
    value.ofType match {
      case BoolType =>
        val sv = value.asInstanceOf[ScalarValue]
        if (sv.maybeBoolean.nonEmpty)
          sv.maybeBoolean
        else {
          sv.AST.simplify().getBoolValue match {
            case Z3_lbool.Z3_L_FALSE => Some(false)
            case Z3_lbool.Z3_L_TRUE => Some(true)
            case _ => None
          }
        }
      case _ => None
    }
  }

  override def toInt: Option[BigInt] = {
    value match {
      case as: ScalarValue =>
        if (as.maybeInt.nonEmpty) Some(as.maybeInt.get.toInt)
        else {
          as.AST match {
            case num: IntNum => Some(BigInt(num.getBigInteger))
            case bvnum : BitVecNum =>
              Some(BigInt(bvnum.getBigInteger))
            case _ => None
          }
        }
      case _ => None
    }
  }

}
case class PlainValue(value: Value) extends AbsValueWrapper {
  override def maybePath: Option[ChurnedMemPath] = None
}
object PlainValue {
  def rv(value: Value): AbsValueWrapper = PlainValue(value)
}

case class P4RootMemory(override val switch: Switch, rootMemory: RootMemory)
    extends P4Memory(switch) {
  case class ValueWrapper(value: Value, maybePath: Option[ChurnedMemPath])
      extends PacketQuery
      with P4TableQuery
      with LiteralExprValue
      with AbsValueWrapper {
    import ValueWrapper._
    lazy val ast: AST = value.asInstanceOf[ScalarValue].AST

    def freshPath(from: String): MemPath = {
      val l = Left(from): Either[String, Int]
      val mp: MemPath = l :: Nil
      mp
    }

    def append(cp: String): Option[ChurnedMemPath] =
      maybePath.map(r => {
        if (r.nonEmpty)
          r.map(x => (x._1, Left(cp) :: x._2))
        else {
          val mpc = (rootMemory.mkBool(true), freshPath(cp))
          mpc :: Nil
        }
      })
    def append(churn: Iterable[(ScalarValue, Int)]): Option[ChurnedMemPath] = {
      maybePath.map(r => {
        churn.flatMap(idxCond => {
          r.map(path => {
            (
              rootMemory.mkAnd2(path._1, idxCond._1),
              Right(idxCond._2) :: path._2
            )
          })
        })
      })
    }
    override def fields(): Iterable[String] = value match {
      case StructObject(_, fieldRefs) => fieldRefs.keys
    }

    // packet mapper
    override def apply(from: P4Query, to: P4Query): P4Query = {
      field("pack").extract(from, from + to - to.int(1))
    }

    override def contents(): P4Query = {
      field("pack")
    }

    override def packetLength(): P4Query = {
      field("len")
    }

    def extract(from : P4Query, to : P4Query) : P4Query = {
      val f = from.as[AbsValueWrapper].toInt.get.toInt
      val t = to.as[AbsValueWrapper].toInt.get.toInt
      rv(rootMemory.mkExtract(value, f, t))
    }

    override def pop(what: P4Query): PacketQuery = {
      val asint = what.as[AbsValueWrapper].toInt.get
      val pack = value.ofType.asInstanceOf[PacketKind].maxLen
      val extended = int(asint, BVType(pack))
      rv(StructObject(
        PacketKind(switch),
        Map(
          "pack" -> (field("pack") >> extended).as[AbsValueWrapper].value,
          "len" -> (field("len") - field("len").int(asint)).as[AbsValueWrapper].value
        )
      ))
    }
    override def prepend(what: P4Query): PacketQuery = {
      val n = what.len().toInt.get
      val pack = value.ofType.asInstanceOf[PacketKind].maxLen
      val extended = int(n, BVType(pack))
      rv(StructObject(
        PacketKind(switch),
        Map(
          "pack" -> ((field("pack") << extended) |
            rv(rootMemory.mkExtend(what.as[AbsValueWrapper].value, pack))).as[AbsValueWrapper].value,
          "len" -> (field("len") + field("len").int(n)).as[AbsValueWrapper].value
        )
      ))
    }
    // table query handlers
    override def isDefault: P4Query = {
      !field("hits")
    }
    override def isAction(act: String): P4Query = {
      val fld = field("action")
      fld === fld.int(fld.value.ofType.asInstanceOf[EnumKind].getId(act))
    }
    override def getParam(act: String, parmName: String): P4Query = {
      field(act + "_" + parmName)
    }
    // p4 query methods
    override def ok(): P4Query = this

    override def errorCause(): P4Query = field("errorCause")

    override def field(name: String): ValueWrapper =
      copy(value = rootMemory.mkSelect(name, value), maybePath = append(name))

    override def len(): LiteralExprValue = value match {
      case sv: ScalarValue if sv.ofType == BoolType =>
        rv(rootMemory.typeMapper.literal(UnboundedInt, 1))
      case ArrayObject(_, _, elems) =>
        rv(rootMemory.typeMapper.literal(BVType(8), elems.size))
      case sv: ScalarValue if sv.ofType.isInstanceOf[BVType] =>
        val ival = sv.ofType.asInstanceOf[BVType].sz
        rv(rootMemory.typeMapper.literal(UnboundedInt, ival))
    }

    override def valid(): P4Query = field("IsValid")

    override def next(): P4Query = field("next__")

    override def fresh(prefix : String = ""): P4Query =
      ValueWrapper(
        value = rootMemory.typeMapper.fresh(value.ofType, prefix),
        maybePath = None
      )

    override def zeros(): P4Query =
      ValueWrapper(
        value = rootMemory.typeMapper.zeros(value.ofType),
        maybePath = None
      )
    override def ite(thn: P4Query, els: P4Query): P4Query =
      rootMemory.unwrapImmutable(value) match {
        case sv: ScalarValue =>
          if (thn.isInstanceOf[P4RootMemory]) {
            assert(els.isInstanceOf[P4RootMemory])
            val rmt = thn.as[P4RootMemory]
            val rme = els.as[P4RootMemory]
            rmt.copy(rootMemory = rmt.rootMemory.where(sv)) ||
            rme.copy(
              rootMemory = rme.rootMemory.where(rme.rootMemory.mkNot(sv))
            )
          } else {
            copy(
              value = rootMemory
                .mkITE(sv, asWrapper(thn).value, asWrapper(els).value),
              maybePath = None
            )
          }
      }

    override def int(value: BigInt): P4Query =
      copy(rootMemory.typeMapper.literal(this.value.ofType, value), None)

    override def isArray: Boolean = value match {
      case _: ArrayObject => true
      case _              => false
    }
    override def ===(other: P4Query): P4Query =
      rv(rootMemory.mkEQ(value, asWrapper(other).value))

    override def <(other: P4Query): P4Query =
      rv(rootMemory.mkLT(value, asWrapper(other).value))

    override def >(other: P4Query): P4Query =
      rv(rootMemory.mkGT(value, asWrapper(other).value))

    override def >=(other: P4Query): P4Query =
      rv(rootMemory.mkGE(value, asWrapper(other).value))

    override def <=(other: P4Query): P4Query =
      rv(rootMemory.mkLE(value, asWrapper(other).value))

    override def apply(idx: P4Query): P4Query = {
      val (nv, churn) = rootMemory.mkIndex(asWrapper(idx).value, value)
      copy(value = nv, maybePath = append(churn))
    }

    override def int(value: BigInt, p4Type: P4Type): P4Query = {
      rv(rootMemory.typeMapper.literal(p4Type, value))
    }

    override def &&(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory
            .mkAnd2(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }
    override def ||(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkOr2(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def &(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory
            .mkAnd2(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def |(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkOr2(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def ^(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory
            .mkXor2(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def +(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkAdd(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def <<(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkSHL(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }
    override def >>(other : P4Query) : P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkSHR(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def -(other: P4Query): P4Query = value match {
      case sv: ScalarValue =>
        rv(
          rootMemory.mkSub(sv, asWrapper(other).value.asInstanceOf[ScalarValue])
        )
    }

    override def unary_~(): P4Query = value match {
      case sv: ScalarValue =>
        rv(rootMemory.mkNot(sv))
    }

    override def unary_-(): P4Query = value match {
      case sv: ScalarValue =>
        rv(rootMemory.mkNeg(sv))
    }

    override def unary_!(): P4Query = value match {
      case sv: ScalarValue =>
        rv(rootMemory.mkNot(sv))
    }

    override def boolVal(bv: Boolean): ValueWrapper = rv(rootMemory.mkBool(bv))

    override def nr: BigInt = value match {
      case s: ScalarValue => s.maybeInt.get
    }
  }
  object ValueWrapper {
    def rv(value: Value): ValueWrapper = ValueWrapper(value, None)
  }
  lazy val rootWrap = ValueWrapper(rootMemory.structObject, Some(Nil))

  override def where(p4Query: P4Query): P4Memory = {
    copy(
      rootMemory = rootMemory.where(
        p4Query.asInstanceOf[ValueWrapper].value.asInstanceOf[ScalarValue]
      )
    )
  }

  override def or(qries : Iterable[P4Query]) : P4Query = {
    if (qries.isEmpty) {
      boolVal(false)
    } else {
      if (qries.forall(_.isInstanceOf[P4RootMemory])) {
        copy(rootMemory = RootMemory.merge(qries.map(_.as[P4RootMemory].rootMemory)))
      } else {
        super.or(qries)
      }
    }
  }

  def asWrapper(p4Query: P4Query): AbsValueWrapper = p4Query match {
    case x: AbsValueWrapper => x
    case m: P4RootMemory =>
      m.rootWrap.copy()
  }
  override def update(dst: P4Query, src: P4Query): P4Memory = {
    val d = dst.asInstanceOf[ValueWrapper]
    val s = src.asInstanceOf[ValueWrapper]
    copy(rootMemory = rootMemory.set(d.maybePath.get, s.value))
  }

  override def cloneSession(cloneSpec: P4Query): P4Query = {
    ValueWrapper.rv(
      rootMemory.typeMapper
        .applyFunction("clone_session", cloneSpec.as[AbsValueWrapper].value)
    )
  }

  override def multicastSession(mgid: P4Query): P4Query = {
    ValueWrapper.rv(
      rootMemory.typeMapper
        .applyFunction("mgid_session", mgid.as[AbsValueWrapper].value)
    )
  }

  override def egressAllowed(p4Query: P4Query): P4Query = {
    ValueWrapper.rv(
      rootMemory.typeMapper.applyFunction(
        "constrain_egress_port",
        p4Query.as[AbsValueWrapper].value
      )
    )
  }

  override def ingressAllowed(p4Query: P4Query): P4Query = {
    ValueWrapper.rv(
      rootMemory.typeMapper.applyFunction(
        "constrain_ingress_port",
        p4Query.as[AbsValueWrapper].value
      )
    )
  }

  override def root(): P4Query = rootWrap

  override def packet(): ValueWrapper = field("packet")

  override def query(table: String, params: Iterable[P4Query]): ValueWrapper = {
    val fs = lastQuery(table).value.ofType.asInstanceOf[FlowStruct]
    ValueWrapper.rv(
      fs.query(
          params
            .map(_.asInstanceOf[ValueWrapper].value.asInstanceOf[ScalarValue])
            .toList
        )
    )
  }

  override def int(value: BigInt, p4Type: P4Type): P4Query = {
    rootWrap.int(value, p4Type)
  }

  override def lastQuery(table: String): ValueWrapper = {
    field(table + "_lastQuery")
  }

  override def as[T <: P4Query]: T = super.as

  override def ok(): P4Query = {
    val n = where(errorCause() === errorCause().int(0))
      .update(errorCause(), errorCause().zeros())
    n
  }

  override def err(): P4Query =
    where(errorCause() != errorCause().int(0))

  override def errorCause(): ValueWrapper = field("errorCause")

  override def fails(because: String): P4Query =
    update(errorCause(), int(ErrorLedger.errorIndex(because)))

  override def int(v: BigInt): ValueWrapper =
    ValueWrapper.rv(rootMemory.typeMapper.literal(UnboundedInt, v))

  override def field(name: String): ValueWrapper = rootWrap.field(name)

  override def fresh(prefix : String = "fresh"): P4Query =
    copy(
      rootMemory = RootMemory(
        ocondition = rootMemory.mkBool(true),
        structObject = rootMemory.typeMapper
          .fresh(rootMemory.structObject.ofType, prefix)
          .asInstanceOf[StructObject]
      )(rootMemory.context)
    )

  override def ||(other: P4Query): P4Query = {
    val rootMem = other.as[P4RootMemory]
    copy(
      switch = switch,
      rootMemory = this.rootMemory.merge(rootMem.rootMemory)
    )
  }

  override def zeros(): P4RootMemory =
    copy(
      rootMemory = RootMemory(
        ocondition = rootMemory.mkBool(true),
        structObject = rootMemory.typeMapper
          .zeros(rootMemory.structObject.ofType)
          .asInstanceOf[StructObject]
      )(rootMemory.context)
    )

  override def boolVal(bv: Boolean): ValueWrapper = rootWrap.boolVal(bv)
}
