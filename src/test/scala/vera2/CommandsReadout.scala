package vera2

import org.change.p4.control.P4Commands
import org.change.p4.control.queryimpl._
import org.change.p4.model.Switch
import org.scalatest.FunSuite
import com.microsoft.z3.Context
import org.change.p4.control.types.BVType
import org.change.p4.tools._
import org.change.utils.Z3Helper._

class CommandsReadout extends FunSuite {
  test("commands correctly read") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    assert(inst.tables.values.flatten.size == 6)
    assert(inst.defaults.size == 3)
  }
  test("commands axioms") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    assert(ConstraintBuilder(sw, ctx, inst).toList.size == 3)
  }

  test("commands axioms instances") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    val solver = ctx.mkSolver()
    val flowStruct = FlowStruct(sw, sw.table("ipv4_lpm"), ctx)
    val tm = TypeMapper()(ctx)
    val fs = flowStruct.query(List(
      tm.literal(BVType(32), 326683)
    )).asInstanceOf[StructObject]
    val axioms = ConstraintBuilder(sw, ctx, inst).toList
    axioms.foreach(solver.assertCnstr)
    val hits = fs.fieldRefs("hits").asInstanceOf[ScalarValue].AST
    val act = fs.fieldRefs("action").asInstanceOf[ScalarValue]
    val ek = act.ofType.asInstanceOf[EnumKind]
    solver.assertCnstr(hits)
    solver.assertCnstr(
      ctx.mkEq(ctx.mkBV(ek.getId("_drop"), 8), act.AST)
    )
    assert(!solver.docheck().get)
  }

}
