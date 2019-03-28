package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control._
import org.change.plugins.vera.IntType
import org.change.v2.p4.model.Switch
import z3.scala.Z3AST


trait AbsValueWrapper {
  def value : Value
  def maybePath : Option[ChurnedMemPath]
}
case class P4RootMemory(switch : Switch,
                        error : Z3AST,
                        rootMemory: RootMemory) extends P4Memory(switch) {
  case class ValueWrapper(value: Value,
                          maybePath : Option[ChurnedMemPath]) extends PacketQuery
                                                       with P4TableQuery with LiteralExprValue
  with AbsValueWrapper {
    import ValueWrapper._
    lazy val ast : Z3AST = value.asInstanceOf[ScalarValue].z3AST

    def freshPath(from : String): MemPath = {
      val l = Left(from) : Either[String, Int]
      val mp : MemPath = l :: Nil
      mp
    }

    def append(cp : String) : Option[ChurnedMemPath] = maybePath.map(r => {
      if (r.nonEmpty)
        r.map(x => (x._1, Left(cp) :: x._2))
      else {
        val mpc = (rootMemory.mkBool(true), freshPath(cp))
        mpc :: Nil
      }
    })
    def append(churn : Iterable[(ScalarValue, Int)]): Option[ChurnedMemPath] = {
      maybePath.map(r => {
        churn.flatMap(idxCond => {
          r.map(path => {
            (rootMemory.mkAnd2(path._1, idxCond._1), Right(idxCond._2) :: path._2)
          })
        })
      })
    }
    override def fields() : Iterable[String] = value match {
      case StructObject(_, fieldRefs) => fieldRefs.keys
    }

    // packet mapper
    override def apply(from: P4Query, to: P4Query): P4Query = super.apply(from, to)

    override def pop(n: P4Query): PacketQuery = super.pop(n)

    override def append(what: P4Query): PacketQuery = super.append(what)

    // table query handlers
    override def isDefault: P4Query = super.isDefault

    override def exists(): P4Query = super.exists()

    override def isAction(act: String): P4Query = super.isAction(act)

    override def getParam(act: String, parmName: String): P4Query = super.getParam(act, parmName)

    // p4 query methods
    override def ok(): P4Query = this

    override def err(): P4Query = ???

    override def errorCause(): P4Query = field("errorCause")

    override def fails(because: String): ValueWrapper = ???

    override def field(name: String): ValueWrapper =
      copy(value = rootMemory.mkSelect(name, value), maybePath = append(name))

    override def len(): LiteralExprValue = value match {
      case ArrayObject(_, elems) =>
        rv(rootMemory.typeMapper.literal(IntType, elems.size))
    }

    override def valid(): P4Query = field("IsValid")

    override def next(): P4Query = field("next")

    override def fresh(): P4Query = ValueWrapper(value =
      rootMemory.typeMapper.fresh(value.ofType, "fresh"), maybePath = None)

    override def zeros(): P4Query = ValueWrapper(value =
      rootMemory.typeMapper.zeros(value.ofType), maybePath = None)

    override def set(src: P4Query, value: P4Query): P4Query = ???

    override def ite(thn: P4Query, els: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        if (thn.isInstanceOf[P4RootMemory]) {
          assert(els.isInstanceOf[P4RootMemory])
          P4RootMemory.this.copy(rootMemory = rootMemory.where(sv)) ||
            P4RootMemory.this.copy(rootMemory = rootMemory.where(sv))
        } else {
          copy(
            value = rootMemory.mkITE(sv, asWrapper(thn).value, asWrapper(els).value),
            maybePath = None
          )
        }
    }

    override def int(value: BigInt): P4Query = copy(rootMemory.typeMapper.literal(
      this.value.ofType, value), None)

    override def isArray: Boolean = value match {
      case _ : ArrayObject => true
      case _ => false
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

    override def &&(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkAnd2(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }
    override def ||(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkOr2(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def &(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkAnd2(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def |(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkOr2(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def ^(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkXor2(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def +(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkAdd(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def <<(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkSHL(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def -(other: P4Query): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkSub(sv, asWrapper(other).value.asInstanceOf[ScalarValue]))
    }

    override def unary_~(): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkNot(sv))
    }

    override def unary_-(): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkNeg(sv))
    }

    override def unary_!(): P4Query = value match {
      case sv : ScalarValue =>
        rv(rootMemory.mkNot(sv))
    }

    override def boolVal(bv: Boolean): ValueWrapper = rv(rootMemory.mkBool(bv))

    override def nr: BigInt = value match {
      case s : ScalarValue => s.tryResolve.get
    }
  }
  object ValueWrapper {
    def rv(value : Value) : ValueWrapper = ValueWrapper(value, None)
  }
  lazy val rootWrap = ValueWrapper(rootMemory.structObject, Some(Nil))

  override def where(p4Query: P4Query): P4Memory = {
    copy(rootMemory = rootMemory.where(p4Query.asInstanceOf[ValueWrapper].ast))
  }
  def asWrapper(p4Query: P4Query) : AbsValueWrapper = p4Query match {
    case x : ValueWrapper => x
    case m : P4RootMemory =>
      m.rootWrap.copy()
  }
  override def update(dst: P4Query, src: P4Query): P4Memory = {
    val d = dst.asInstanceOf[ValueWrapper]
    val s = src.asInstanceOf[ValueWrapper]
    copy(rootMemory = rootMemory.set(d.maybePath.get, s.value))
  }

  override def packet(): ValueWrapper = field("packet")

  override def query(table: String): ValueWrapper = ???

  override def lastQuery(table: String): ValueWrapper = ???

  override def as[T <: P4Query]: T = super.as

  override def ok(): P4Query = this

  override def err(): P4Query =
    copy(rootMemory = rootMemory.where(error))

  override def errorCause(): ValueWrapper = field("errorCause")

  override def fails(because: String): P4Query = update(errorCause(), int(because.hashCode))

  override def int(v : BigInt): ValueWrapper = ValueWrapper.rv(rootMemory.typeMapper.literal(IntType, v))

  override def field(name: String): ValueWrapper = rootWrap.field(name)

  override def fresh(): P4Query =
    copy(rootMemory = RootMemory(
      condition = rootMemory.mkBool(true).z3AST,
      structObject = rootMemory.typeMapper.fresh(rootMemory.structObject.ofType, "fresh").asInstanceOf[StructObject]
    )(rootMemory.context))

  override def ||(other: P4Query): P4Query = {
    val rootMem = other.as[P4RootMemory]
    copy(
      switch = switch,
      rootMemory = this.rootMemory.merge(rootMem.rootMemory)
    )
  }

  override def zeros(): P4RootMemory = copy(rootMemory = RootMemory(
    condition = rootMemory.mkBool(true).z3AST,
    structObject = rootMemory.typeMapper.zeros(rootMemory.structObject.ofType).asInstanceOf[StructObject]
  )(rootMemory.context))

  override def boolVal(bv: Boolean): ValueWrapper = rootWrap.boolVal(bv)
}
