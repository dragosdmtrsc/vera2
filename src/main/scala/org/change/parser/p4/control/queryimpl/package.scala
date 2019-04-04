package org.change.parser.p4.control

import org.change.plugins.vera._
import org.change.v2.p4.model.Switch
import org.change.v3.semantics.context
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.JavaConverters._
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


    override def toString: String = {
      z3AST.toString()
    }
  }
  type ChurnedMemPath  = Iterable[(ScalarValue, MemPath)]

  class PacketWrapper(implicit context: Z3Context) {

    val takeFuns = mutable.Map.empty[(Int, Int), Z3FuncDecl]
    val popFuns = mutable.Map.empty[Int, Z3FuncDecl]
    private var appends: mutable.TreeSet[Int] = mutable.TreeSet.empty[Int]

    def declareAppend(sz : Int) : Unit = {
      appends = appends + sz
    }

    // assumes that this guy is already resolved and can
    // be simply evald to concrete values
    def takeKind(v : Z3AST) : Option[Int] = {
      appends.find(r => {
        val idx = indexing(r)
        val simpd = context.getBoolValue(
          context.simplifyAst(packetSort._3(idx)(v)))
        if (simpd.nonEmpty) simpd.get
        else false
      })
    }
    def isNil(v : Z3AST) : Boolean = {
      context.getBoolValue(
        context.simplifyAst(packetSort._3.head(v))).getOrElse(false)
    }

    def unwrap(v : Z3AST, of : Int) : (Z3AST, Z3AST) = {
      val idx = indexing(of)
      val oldone = context.simplifyAst(packetSort._4(idx).head(v))
      val appd = context.simplifyAst(packetSort._4(idx)(1)(v))
      (oldone, appd)
    }

    def getPopFun(of : Int) : Z3FuncDecl = {
      popFuns.getOrElseUpdate(of, context.mkFuncDecl(
        s"pop_$of", packetSort._1, packetSort._1
      ))
    }

    def getTakeFun(start : Int, end : Int): Z3FuncDecl = {
      takeFuns.getOrElseUpdate((start, end),
        context.mkFuncDecl(s"take_${start}_$end",
            packetSort._1, context.mkBVSort(end - start))
      )
    }
    lazy val (packetSort, indexing) = {
      val (names, params, mapping) = appends.toList.zipWithIndex.map(xwi => {
        val x = xwi._1
        (s"prepend_$x", Seq(("pin", RecursiveType(0)),
          ("x", RegularSort(context.mkBVSort(x)))
        ), xwi.copy(_2 = xwi._2 + 1))
      }).unzip3
      (context.mkADTSorts(Seq(
        ("Packet",
          "nil" :: names,
          Seq() :: params
        )
      )).head, mapping.toMap)
    }

    def prepend(packet: Z3AST, n : Int, v : Z3AST): Z3AST = {
      if (packet.getSort != sort()) {
        throw new IllegalArgumentException(s"cannot apply append_$n on $packet, " +
          s"expecting something of sort ${sort()}")
      }
      if (v.getSort != context.mkBVSort(n))
        throw new IllegalArgumentException(s"cannot apply append_$n with argument $v of sort ${v.getSort}")
      val consIdx = indexing(n)
      packetSort._2(consIdx)(packet, v)
    }

    def pop(packet: Z3AST, nr : Int): Z3AST = {
      if (packet.getSort != sort()) {
        throw new IllegalArgumentException(s"cannot apply pop on $packet, " +
          s"expecting something of sort ${sort()}")
      }
      getPopFun(nr)(packet)
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

  object PacketWrapper {
    val contextBound = mutable.Map.empty[Z3Context, PacketWrapper]
    def apply(context : Z3Context) : PacketWrapper = {
      contextBound(context)
    }

    def initialize(switch: Switch, context: Z3Context): Unit = {
      val tm = new PacketWrapper()(context)
      switch.getInstances.asScala.foreach(i => {
        i.getLayout.getFields.asScala.foreach(f => {
          if (f.getLength > 0)
            tm.declareAppend(f.getLength)
        })
      })
      contextBound.put(context, tm)
    }
  }

  class TypeMapper()(implicit context: Z3Context) {
    val funMap = mutable.Map.empty[String, (P4Type, P4Type,
      Z3FuncDecl)]

    def applyFunction(name : String, z3AST: Z3AST): Z3AST = {
      funMap(name)._3(z3AST)
    }
    def applyFunction(name : String, value : Value) : Value = {
      new ScalarValue(
        funMap(name)._2,
        applyFunction(name, value.asInstanceOf[ScalarValue].z3AST)
      )
    }
    def mkFunction(name : String, from : P4Type, to : P4Type): Unit = {
      val sortFrom = apply(from)
      val sortTo = apply(to)
      funMap.put(name, (from, to, context.mkFuncDecl(name, sortFrom, sortTo)))
    }

    val packetWrapper: PacketWrapper = PacketWrapper(context)
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

  object TypeMapper {
    val contextBoundMapper = mutable.Map.empty[Z3Context, TypeMapper]
    def apply()(implicit context : Z3Context): TypeMapper = {
      contextBoundMapper.getOrElseUpdate(context, new TypeMapper()(context))
    }
  }
  object StructureInitializer {
    implicit def apply(switch : Switch)(implicit context : Z3Context): Switch = {
      PacketWrapper.initialize(switch, context)
      val tm = TypeMapper.apply()(context)
      val stdMeta = switch.getInstance("standard_metadata")
      val clSpec = stdMeta.getLayout.getField("clone_spec")
      val cloneSpecLen = if (clSpec != null) {
        clSpec.getLength
      } else {
        // default to 16, because this means that clone session
        // will never get called
        16
      }
      tm.mkFunction("clone_session", BVType(cloneSpecLen), BVType(9))
      val w = switch.mcastGrpWidth()
      if (w != 0)
        tm.mkFunction("mgid_session", BVType(w), BVType(9))
      val igPort = switch.getInstance(STANDARD_METADATA).getLayout.getField("ingress_port")
      tm.mkFunction("constrain_ingress_port", BVType(igPort.getLength), BoolType)
      val egPort = switch.getInstance(STANDARD_METADATA).getLayout.getField("egress_port")
      tm.mkFunction("constrain_egress_port", BVType(egPort.getLength), BoolType)
      switch
    }
  }

  type MemPath = List[Either[String, Int]]
}
