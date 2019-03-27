package org.change.parser.p4.control

import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.control._
import org.change.v2.p4.model.control.exp.{P4BExpr, RelOp, ValidRef}
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.table.TableDeclaration
import org.change.v3.semantics.{IntraExecutor, SimpleMemory, context}

import scala.collection.JavaConverters._
import scala.collection.mutable

class SEFLSemantics(switch: Switch) extends
    Semantics[List[SimpleMemory]](switch) {
  private val intra = new IntraExecutor
  private val translator = new SEFLTranslator(switch)
  private val traverse = new Traverse(translator)
  private val tableHandlers = mutable.Map.empty[String, TableHandler]

  private def getTable(table : TableDeclaration): TableHandler = {
    tableHandlers.getOrElseUpdate(table.getName,
      new TableHandler(switch, table, context))
  }
  import SMInstantiator._
  val ctx = P4Memory(switch)
  private val evaluator = new EvalWrapper(switch, context)
  override def translate(src: ControlStatement,
                         rho: Option[P4BExpr],
                         dst: ControlStatement)
                        (from: List[SimpleMemory]): List[SimpleMemory] = {
    val p = from.partition(_.finished())
    assert(p._1.isEmpty)
    val ret = if (rho.nonEmpty) {
      val what = ctx(rho.get)
      val validFail = ctx.validityFailure(rho.get)
      p._2/*.instantiateAs[P4MemoryValue[List[SimpleMemory]]](validFail.ite(
        ctx.fails(s"validity failure reading ${rho.get}"),
        ctx.where(what)
      )).value*/
//      from.flatMap(sm => {
//        val (ok, err) = evaluator.evaluate(rho.get, sm, zeroOnInvalid = false)
//        val simpd = context.simplifyAst(err)
//        val oksimpd = context.simplifyAst(ok)
//        if (simpd != context.mkFalse()) {
//          sm.addCondition(simpd).fail("segfault at " + src + " when evaluating " + rho.get) :: Nil
//        } else {
//          Nil
//        } ++ (if (oksimpd != context.mkFalse()) {
//          sm.addCondition(oksimpd) :: Nil
//        } else {
//          Nil
//        })
//      })
    } else {
      src match {
        case ss : SetStatement =>
          val validFail = ctx.validityFailure(ss.getLeft)
          val validFailSrc = ctx.validityFailure(ss.getRightE)
          p._2
//          validFail.ite(ctx.fails(s"cannot write to invalid header ${ss.getLeft}"),
//            validFailSrc.ite(
//              ctx.fails(s"cannot read from an invalid header ${ss.getRightE}"),
//              ctx.update(ctx(ss.getLeft), ctx(ss.getRightE))
//            )
//          ).instantiate(p._2).asInstanceOf[P4MemoryValue[List[SimpleMemory]]].value
        case es : ExtractStatement =>
          val hdr1 = ctx(es.getExpression).asInstanceOf[HeaderQuery]
          val packet = ctx.packet()
          val validRef = hdr1.valid()
          val dostuff = hdr1.fields().foldLeft(ctx.update(validRef, validRef.int(1)))((crtQuery, fld) => {
            crtQuery.update(fld, packet(0, fld.len())).update(packet, packet.pop(fld.len()))
          })
//          (if (ctx.field(es.getExpression.getPath).isArray()) {
//            dostuff.update(ctx.field(es.getExpression.getPath).next(),
//              ctx.field(es.getExpression.getPath).next())
//          } else {
//            dostuff
//          }).instantiate(p._2).asInstanceOf[P4MemoryValue[List[SimpleMemory]]].value
          p._2
        case es : EmitStatement =>
          val packet = ctx.packet()
          val hdr1 = ctx(es.getHeaderRef).asInstanceOf[HeaderQuery]
          val valid  = ctx(es.getHeaderRef).asInstanceOf[HeaderQuery]
//          val dostuff = valid.ite(hdr1.fields().foldLeft(ctx)((crtQuery, fld) => {
//            crtQuery.update(packet, packet.append(fld))
//          }), ctx).asInstanceOf[P4Memory]
//          (if (ctx.field(es.getHeaderRef.getPath).isArray()) {
//            dostuff.update(ctx.field(es.getHeaderRef.getPath).next(),
//              ctx.field(es.getHeaderRef.getPath).next() + ctx.field(es.getHeaderRef.getPath).next().int(1))
//          } else {
//            dostuff
//          }).instantiate(p._2).asInstanceOf[P4MemoryValue[List[SimpleMemory]]].value
          p._2
        case ce : CaseEntry =>
          val valfail = ctx.validityFailure(ce.getBExpr)
//          valfail.ite(
//            ctx.fails(s"validity failure reading ${ce.getBExpr}"),
//            ctx.where(ctx(ce.getBExpr))
//          ).instantiate(p._2).asInstanceOf[P4MemoryValue[List[SimpleMemory]]].value
          p._2
        case rs : ReturnStatement => p._2
        case rs : ReturnSelectStatement => p._2
        case at : ApplyTableStatement =>
          p._2.flatMap(f => getTable(at.getTable).handle(f)).map(f => {
            // dirty hack goes here =>
            // need to reset last_flow to be able to merge stuff around
            f.assignNewValue("last_flow", context.mkInt(0, context.mkBVSort(1))).get
          })
        case ast : ApplyAndSelectTableStatement =>
          ast.getCaseEntries.asScala.foreach(ce => {
            ce.setTableDeclaration(ast.getTable)
          })
          p._2.flatMap(f => getTable(ast.getTable).handle(f))
        case tableCaseEntry: TableCaseEntry =>
          val tableHandler = getTable(tableCaseEntry.getTableDeclaration)
          p._2.map(f => tableHandler.handleTableEntry(tableCaseEntry, f))
        case ac : ApplyControlStatement => p._2
      }
    }
    ret
  }

  override def merge(crt: List[SimpleMemory], merge: List[SimpleMemory]): List[SimpleMemory] = {
    crt ++ merge
  }
  override def stop(region: List[SimpleMemory]): List[SimpleMemory] =
    region.filter(_.finished())
  override def success(region: List[SimpleMemory]): List[SimpleMemory] =
    region.filter(!_.finished())

  override def finishNode(src: ControlStatement, region: Option[List[SimpleMemory]]): Unit = {
    val stoppedHere = region.getOrElse(Nil)
    val failedHere = stoppedHere.filter(_.errorCause.nonEmpty)
    System.err.println("finished " + src + " : " + failedHere.size + "/" + stoppedHere.size)
    if (failedHere.nonEmpty) {
      System.err.println("example failure :" + failedHere.head.error)
    }
  }
}
