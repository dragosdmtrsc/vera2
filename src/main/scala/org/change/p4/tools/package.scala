package org.change.p4

import com.microsoft.z3.Context
import org.change.p4.control.SolveTables
import org.change.p4.control.queryimpl.{EnumKind, ExternFunction, StructureInitializer}
import org.change.p4.control.vera.ParserHelper
import org.change.p4.model.Switch

package object tools {
  class Initializer(switch: Switch) {
    def init(context: Context): Switch = {
      val s = SolveTables(switch)
      ExternFunction.clear()
      EnumKind.clear()
      StructureInitializer(switch)(context)
      val helper = ParserHelper(switch)
      switch
    }
  }
  implicit def apply(switch: Switch): Initializer = new Initializer(switch)
}
