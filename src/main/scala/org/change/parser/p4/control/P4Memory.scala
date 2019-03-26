package org.change.parser.p4.control

import org.change.v2.p4.model.Switch
import org.change.v3.semantics.SimpleMemory

case class P4Memory(switch : Switch) {

  def fails(because : String) : P4Memory = ???
  def header(headerName : String) : P4Query[HeaderValue] = ???
  def scalar(name : String) : P4Query[ScalarValue] = ???
}

trait P4Query[PV]
trait P4Value[T] extends P4Query[P4Value[T]]
class HeaderValue extends P4Value[HeaderValue]
class ArrayValue extends P4Value[ArrayValue]
class ScalarValue extends P4Value[ScalarValue]