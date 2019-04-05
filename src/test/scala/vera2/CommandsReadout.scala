package vera2

import org.change.p4.control.{FlowStruct, P4Commands}
import org.change.p4.control.queryimpl.ConstraintBuilder
import org.change.p4.model.Switch
import org.scalatest.FunSuite
import z3.scala.Z3Context
import org.change.p4.tools._

class CommandsReadout extends FunSuite {
  test("commands correctly read") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Z3Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    assert(inst.tables.values.flatten.size == 6)
    assert(inst.defaults.size == 3)
  }
  test("commands axioms") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Z3Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    assert(ConstraintBuilder(sw, ctx, inst).toList.size == 3)
  }

  test("commands axioms instances") {
    val against = "inputs/test-cases/simple-router/simple_router-ppc.p4"
    val cmds = "inputs/test-cases/simple-router/commands.txt"
    val ctx = new Z3Context()
    val sw = Switch.fromFile(against).init(ctx)
    val inst = P4Commands.fromFile(sw, cmds)
    val axioms = ConstraintBuilder(sw, ctx, inst).toList
    val solver = ctx.mkSolver()
    axioms.foreach(solver.assertCnstr)
    val flowStruct = FlowStruct(ctx, sw.table("ipv4_lpm"), sw)
    val ast = flowStruct.query(List(ctx.mkInt(326683, ctx.mkBVSort(32))))
    solver.assertCnstr(ast.hits())
    solver.assertCnstr(
      flowStruct.isAction("_drop", ast.actionRun()))
    assert(!solver.check().get)
  }

}
