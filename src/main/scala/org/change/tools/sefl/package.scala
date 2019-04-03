package org.change.tools

import org.change.parser.p4.control.SolveTables
import org.change.parser.p4.control.queryimpl.StructureInitializer
import org.change.v2.p4.model.Switch
import z3.scala.Z3Context
import z3.scala.Z3Context.AstPrintMode

package object sefl {
  class Initializer(switch: Switch) {
    def init(context : Z3Context) : Switch = {
      val s = SolveTables(switch)
      context.setAstPrintMode(AstPrintMode.Z3_PRINT_SMTLIB2_COMPLIANT)
      StructureInitializer(switch)(context)
    }
  }
  implicit def apply(switch : Switch) : Initializer = new Initializer(switch)
}
