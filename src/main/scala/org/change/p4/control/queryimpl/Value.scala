package org.change.p4.control.queryimpl
import org.change.p4.control.types.{FixedArrayType, P4Type, StructType}
import com.microsoft.z3.{AST, Expr}

trait Value {
  def ofType: P4Type
}

case class ImmutableValue(value: Value) extends Value {
  override def ofType: P4Type = value.ofType
}

trait MemoryObject extends Value
case class StructObject(ofType: StructType, fieldRefs: Map[String, Value])
    extends MemoryObject
case class ArrayObject(ofType: FixedArrayType,
                       next: ScalarValue,
                       elems: List[Value])
    extends MemoryObject
class ScalarValue(val ofType: P4Type,
                  val AST: Expr,
                  val maybeBoolean: Option[Boolean] = None,
                  val maybeInt: Option[BigInt] = None)
    extends Value {
  override def toString: String = {
    AST.toString
  }
}
