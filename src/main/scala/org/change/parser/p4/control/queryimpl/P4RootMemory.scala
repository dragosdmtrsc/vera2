package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control._
import org.change.plugins.vera.{BVType, BoolType, IntType, P4Type}
import org.change.v2.p4.model.Switch
import z3.scala.Z3AST
import z3.scala.dsl.BVSort


trait AbsValueWrapper {
  def value : Value
  def maybePath : Option[ChurnedMemPath]
}
case class P4RootMemory(switch : Switch,
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
    //TODO: add proper code here
    override def apply(from: P4Query, to: P4Query): P4Query = this

    override def pop(n: P4Query): PacketQuery = this//super.pop(n)

    override def append(what: P4Query): PacketQuery = this//super.append(what)

    // table query handlers
    override def isDefault: P4Query = super.isDefault

    override def exists(): P4Query = {
      val ast = this.value.asInstanceOf[ScalarValue].z3AST
      val struct = this.value.ofType.asInstanceOf[FlowStruct]
      val finstance = struct.from(ast)
      rv(new ScalarValue(BoolType, finstance.exists()))
    }

    override def isAction(act: String): P4Query = {
      val ast = this.value.asInstanceOf[ScalarValue].z3AST
      val struct = this.value.ofType.asInstanceOf[FlowStruct]
      val finstance = struct.from(ast)
      rv(new ScalarValue(
        BoolType,
        struct.isAction(act, finstance.actionRun())
      ))
    }

    override def getParam(act: String, parmName: String): P4Query = {
      val ast = this.value.asInstanceOf[ScalarValue].z3AST
      val struct = this.value.ofType.asInstanceOf[FlowStruct]
      val finstance = struct.from(ast)
      val ap = finstance.getActionParam(act, parmName)
      val of = finstance.getActionParamType(act, parmName)
      rv(new ScalarValue(
        of, ap
      ))
    }

    // p4 query methods
    override def ok(): P4Query = this

    override def err(): P4Query = ???

    override def errorCause(): P4Query = field("errorCause")

    override def fails(because: String): ValueWrapper = ???

    override def field(name: String): ValueWrapper =
      copy(value = rootMemory.mkSelect(name, value), maybePath = append(name))

    override def len(): LiteralExprValue = value match {
      case sv : ScalarValue if sv.ofType == BoolType =>
        rv(rootMemory.typeMapper.literal(IntType, 1))
      case ArrayObject(_, _, elems) =>
        rv(rootMemory.typeMapper.literal(IntType, elems.size))
      case sv : ScalarValue if sv.ofType.isInstanceOf[BVType] =>
        val ival = sv.ofType.asInstanceOf[BVType].sz
        rv(rootMemory.typeMapper.literal(IntType, ival))
    }

    override def valid(): P4Query = field("IsValid")

    override def next(): P4Query = field("next__")

    override def fresh(): P4Query = ValueWrapper(value =
      rootMemory.typeMapper.fresh(value.ofType, "fresh"), maybePath = None)

    override def zeros(): P4Query = ValueWrapper(value =
      rootMemory.typeMapper.zeros(value.ofType), maybePath = None)

    override def set(src: P4Query, value: P4Query): P4Query = ???

    override def ite(thn: P4Query, els: P4Query): P4Query = rootMemory.unwrapImmutable(value) match {
      case sv : ScalarValue =>
        if (thn.isInstanceOf[P4RootMemory]) {
          assert(els.isInstanceOf[P4RootMemory])
          P4RootMemory.this.copy(rootMemory = rootMemory.where(sv)) ||
            P4RootMemory.this.copy(rootMemory = rootMemory.where(rootMemory.mkNot(sv)))
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

    override def int(value: BigInt, p4Type: P4Type): P4Query = {
      rv(rootMemory.typeMapper.literal(p4Type, value))
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
    case x : AbsValueWrapper => x
    case m : P4RootMemory =>
      m.rootWrap.copy()
  }
  override def update(dst: P4Query, src: P4Query): P4Memory = {
    val d = dst.asInstanceOf[ValueWrapper]
    val s = src.asInstanceOf[ValueWrapper]
    copy(rootMemory = rootMemory.set(d.maybePath.get, s.value))
  }

  override def packet(): ValueWrapper = field("packet")

  override def query(table: String,
                     params : Iterable[P4Query]): ValueWrapper = {
    val fs = lastQuery(table).value.ofType.asInstanceOf[FlowStruct]
    ValueWrapper.rv(
      new ScalarValue(
        fs,
        fs.query(params.map(_.asInstanceOf[ValueWrapper].value.asInstanceOf[ScalarValue])
          .map(r => r.z3AST).toList).z3AST
      ))
  }

  override def int(value: BigInt, p4Type: P4Type): P4Query = {
    rootWrap.int(value, p4Type)
  }

  override def lastQuery(table: String): ValueWrapper = {
    field(table + "_lastQuery")
  }

  override def as[T <: P4Query]: T = super.as

  override def ok(): P4Query =
    where(errorCause() === errorCause().int(0))

  override def err(): P4Query =
    where(errorCause() != errorCause().int(0))

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
