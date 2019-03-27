package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.{HeaderQuery, P4Memory, P4Query}
import org.change.v2.p4.model.{HeaderInstance, Switch}
import z3.scala.{Z3AST, Z3Context}

class SimpleP4Memory(switch: Switch,
                     query: Z3AST, errQuery : Z3AST,
                     objects : Map[String, P4Query])
                    (implicit context : Z3Context) extends P4Memory(switch) {

  class ScalarQ(z3AST: Z3AST) extends P4Query
  class LAnd(l : P4Query, r : P4Query) extends P4Query
  class Field(f : String, on: P4Query) extends P4Query
  class ArrayIndex(idx : P4Query, on : P4Query) extends P4Query

  override def field(name : String): P4Query = {
    val hdr = switch.getInstance(name)
    if (hdr != null) {
      phv(hdr)
    } else {
      scalars(name)
    }
  }
}
