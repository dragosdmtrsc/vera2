package org.change.parser.p4.control

import org.change.parser.p4.control.SMInstantiator._
import org.change.parser.p4.control.queryimpl.P4RootMemory
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.actions.primitives._
import org.change.v2.p4.model.actions.{P4Action, P4ComplexAction}
import org.change.v2.p4.model.control._
import org.change.v2.p4.model.control.exp.P4BExpr
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.table.{MatchKind, TableDeclaration}

import scala.collection.JavaConverters._

class QueryDrivenSemantics[T<:P4Memory](switch: Switch) extends Semantics[T](switch) {

  def analyze(action: Add, params : List[P4Query], stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) + params(2))
  }

  override def beforeNode(src: ControlStatement, region: T): Unit = {
    if (region.as[P4RootMemory].rootMemory.isEmpty()) {
      System.err.println(s"bad things detected at $src")
    } else {
      System.err.println(s"reach $src")
    }
  }

  def analyze(action: AddHeader,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 1)
//    params.head.valid().ite(
//      ctx.fails(s"loss of information in ${action :: stackTrace}"),
//      ctx.update(params.head, params.head.zeros())
//        .update(params.head.valid(), ctx.boolVal(true))
//    ).asInstanceOf[P4Memory]
    ctx.update(params.head, params.head.zeros())
       .update(params.head.valid(), ctx.boolVal(true))
  }

  def analyze(action: AddToField,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 2)
    ctx.update(params.head, params.head + params(1))
  }
  def analyze(action: BitAnd,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) & params(2))
  }
  def analyze(action: BitOr,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) | params(2))
  }
  def analyze(action: BitXor,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) ^ params(2))
  }
  def analyze(action: CloneEgressPktToEgress,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneEgress = ctx.field("clone")
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field("field_list_ref")
    ctx.update(cloneEgress, ctx.int(2))
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }

  def analyze(action: CloneEgressPktToIngress,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneEgress = ctx.field("clone")
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field("field_list_ref")
    ctx.update(cloneEgress, ctx.int(1))
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }
  def analyze(action: CloneIngressPktToEgress,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneEgress = ctx.field("clone")
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field("field_list_ref")
    ctx.update(cloneEgress, ctx.int(2))
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }
  def analyze(action: CloneIngressPktToIngress,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneEgress = ctx.field("clone")
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field("field_list_ref")
    ctx.update(cloneEgress, ctx.int(1))
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }

  def analyze(action: CopyHeader,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val av = params.head.valid()
    val bv = params(1).valid()
    ctx.update(params.head, params(1))
//    (av && !bv).ite(
//      ctx.fails(s"loss of information in ${action :: stackTrace}"),
//      (!av && !bv).ite(
//        ctx,
//        ctx.update(params.head, params(1))
//      )
//    ).asInstanceOf[P4Memory]
  }

  def analyze(action: Count,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    //TODO: add sema if need be
    ctx
  }

  def analyze(action: Drop,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val fld = ctx.standardMetadata().field("egress_spec")
    ctx.update(fld, fld.int(511))
  }

  def analyze(action: GenerateDigest,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx
  }

  def analyze(action: ModifyField,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx.update(params.head, params(1))
  }
  def analyze(action: ModifyFieldRngUniform,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val v1 = params(1)
    val v2 = params(2)
    val dst = params.head
    val fresh = dst.fresh()
    ctx.where(fresh < v2 && fresh >= v1).update(dst, fresh)
  }

  def analyze(action: ModifyFieldWithHashBasedOffset,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val base = params(1)
    val dst = params.head
    val sz = params(3)
    val fresh = dst.fresh()
    ctx.where(fresh < sz && fresh >= dst.int(0)).update(dst, fresh)
  }

  def analyze(action: NoOp,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = ctx

  def analyze(action: Pop,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val arr = params.head
    val nxtidx = arr.next()
    val count = params(1)
    val fresh = arr.fresh()
    val len = arr.len().nr.toInt
    ctx.where(
      ctx.and((0 until len).map(idx => {
        val i = ctx.int(idx)
        val zeros = fresh(i).zeros()
        fresh(i) === (count < count.int(len - idx)).ite(arr(i - count), zeros)
      }))
    ).update(arr, fresh)
  }

  def analyze(action: Push,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    val arr = params.head
    val nxtidx = arr.next()
    val count = params(1)
    val fresh = arr.fresh()
    val len = arr.len().nr.toInt
    ctx.where(
      ctx.and((0 until len).map(idx => {
        val i = ctx.int(idx)
        val zeros = fresh(i).zeros()
        val valid = zeros.valid()
        val flds = ctx.and(fresh(i).fields().filter(_ != "IsValid").map(f => {
          fresh(i).field(f) === fresh(i).field(f).zeros()
        }) ++ List(zeros.valid() === ctx.boolVal(true)))
        (i < count).ite(flds, fresh(i) === arr(i - count))
      }))
    ).update(arr, fresh)
  }

  def analyze(action: Recirculate,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx.update(ctx.standardMetadata().field("recirculate_flag"), params.head)
      .update(ctx.field("field_list_ref"), params(1))
  }

  def analyze(action: RegisterRead,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx
  }

  def analyze(action: Resubmit,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx.update(ctx.standardMetadata().field("resubmit_flag"), params.head)
      .update(ctx.field("field_list_ref"), params(1))
  }

  def analyze(action: ShiftLeft,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory =
    ctx.update(params.head, params(1) << params(2))

  def analyze(action: ShiftRight,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = ???

  def analyze(action: Subtract,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory =
    ctx.update(params.head, params(1) - params(2))

  def analyze(action: SubtractFromField,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory =
    ctx.update(params.head, params.head - params(1))

  def analyze(action: RemoveHeader,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx.update(params.head.valid(), params.head.valid().int(0))
  }
  def analyze(action: ExecuteMeter,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
     ctx
  }
  def analyze(action: RegisterWrite,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    ctx
  }

  def analyze(action: Truncate,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory =
    ctx.update(ctx.standardMetadata().field("target_packet_length"), params.head)
  def analyze(p4Action: P4Action,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory) : P4Memory = {
    p4Action match {
      case ax : org.change.v2.p4.model.actions.primitives.Add => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.AddHeader => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.AddToField => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.BitAnd => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.BitOr => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.BitXor => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.CloneEgressPktToEgress => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.CloneEgressPktToIngress => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.CloneIngressPktToEgress => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.CloneIngressPktToIngress => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.CopyHeader => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Count => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Drop => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ExecuteMeter => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.GenerateDigest => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ModifyField => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ModifyFieldRngUniform => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.NoOp => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Pop => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Push => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Recirculate => analyze(ax,params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.RegisterRead => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.RegisterWrite => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.RemoveHeader => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Resubmit => analyze(ax,params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ShiftLeft => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.ShiftRight => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Subtract => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.SubtractFromField => analyze(ax,params, stackTrace)
      case ax : org.change.v2.p4.model.actions.primitives.Truncate => analyze(ax, params, stackTrace)
      case ax : org.change.v2.p4.model.actions.P4ComplexAction => analyze(ax, params, stackTrace)
    }
  }

  def analyze(action: P4ComplexAction,
              params : List[P4Query],
              stackTrace : List[P4Action])
             (implicit ctx : P4Memory): P4Memory = {
    assert(action.getParameterList.size() == params.size)
    val mapper = action.getParameterList.asScala.zip(params).map(p => {
      p._1.getParamName -> p._2
    }).toMap
    val resolutionContext = mapper.foldLeft(ctx)((acc, parm) => {
      acc.update(acc.field(action.getActionName + "_" + parm._1), parm._2)
    })
    action.getActionList.asScala.foldLeft(ctx)((acc, actcall) => {
      val parms = actcall.params().asScala.map(_.asInstanceOf[ParamExpression].getExpression).map(exp => {
//        resolutionContext.validityFailure(exp).ite(
//          resolutionContext(exp).fails(s"cannot read $exp, because of a validity failure in ${action :: stackTrace}"),
//          resolutionContext(exp)
//        )
        resolutionContext(exp)
      })
      analyze(actcall.getP4Action, parms.toList, action :: stackTrace)(acc)
    })
  }

  protected def mkActionQuery(p4Action: P4Action,
                              lst : List[P4Query])
                              (implicit ctx :P4Memory) : P4Query = {
    analyze(p4Action, lst, Nil)
  }

  protected def mkTableQuery(tableDeclaration: TableDeclaration)
                            (implicit ctx : P4Memory) : T = {
    val matches = tableDeclaration.getMatches.asScala.map(tm => {
      if (tm.getMatchKind == MatchKind.Valid) {
        val hdr = tm.getReferenceKey.asInstanceOf[HeaderRef]
        ctx(hdr).valid()
      } else {
        tm.getReferenceKey match {
          case fr: FieldRef =>
            ctx(fr.getHeaderRef).valid().ite(
              ctx(fr),
              ctx(fr).zeros()
            )
          case fr: MaskedFieldRef =>
            val href = fr.getReference.getHeaderRef
            //TODO: move this as a pass in SolveTables
            fr.getMask.setWidth(fr.getReference.getFieldReference.getLength)
            val qmask = RichContext.apply(ctx).apply(fr.getMask.asInstanceOf[Expression])
            val r = ctx(fr.getReference)
            ctx(href).valid().ite(
              r & qmask,
              r.zeros()
            )
        }
      }
    })

    val newquery = ctx.update(ctx.lastQuery(tableDeclaration.getName),
                              ctx.query(tableDeclaration.getName, matches))
    val lastFlow = newquery.lastQuery(tableDeclaration.getName)
    val actionSema = newquery.when((if (tableDeclaration.hasProfile) {
      tableDeclaration.actionProfile().getActions.asScala
    } else {
      tableDeclaration.getAllowedActions.asScala.map(_.getActionName)
    }).map(x => {
      (lastFlow.isAction(x), mkActionQuery(switch.action(x),
        switch.action(x).getParameterList.asScala.map(parm => {
          lastFlow.getParam(x, parm.getParamName)
        }).toList
      ))
    }))
    newquery.lastQuery(tableDeclaration.getName).exists().ite(
      actionSema,
      newquery
    ).as[T]
  }

  protected def mkQuery(src : ControlStatement,
                        rho : Option[P4BExpr],
                        dst : ControlStatement)(implicit ctx : P4Memory) : T = {
    val ret = if (rho.nonEmpty) {
      val what = ctx(rho.get)
      ctx.where(what)
    } else {
      src match {
        case _ : EndOfControl => ctx
        case ss : SetStatement =>
          ctx.update(ctx(ss.getLeft), ctx(ss.getRightE))
        case es : ExtractStatement =>
          val hdr1 = ctx(es.getExpression)
          val validRef = hdr1.valid()
          val dostuff = hdr1.fields()
            .filter(_ != "IsValid")
            .foldLeft(ctx.update(validRef, validRef.int(1)))((crtQuery, fname) => {
            val hdr1 = crtQuery(es.getExpression)
            val fld = hdr1.field(fname)
            val packet = crtQuery.packet()
            crtQuery.update(fld, packet(fld.len().int(0), fld.len())).update(packet, packet.pop(fld.len()))
          })
          if (dostuff.field(es.getExpression.getPath).isArray) {
            val nxtfield = dostuff.field(es.getExpression.getPath).next()
            dostuff.update(nxtfield, nxtfield + nxtfield.int(1))
          } else {
            dostuff
          }
        case es : EmitStatement =>
          val packet = ctx.packet()
          val hdr1 = ctx(es.getHeaderRef)
          val valid  = ctx(es.getHeaderRef)
          val dostuff = valid.ite(hdr1.fields().foldLeft(ctx)((crtQuery, fld) => {
            crtQuery.update(packet, packet.append(hdr1.field(fld)))
          }), ctx).asInstanceOf[P4Memory]
          if (ctx.field(es.getHeaderRef.getPath).isArray) {
            dostuff.update(ctx.field(es.getHeaderRef.getPath).next(),
              ctx.field(es.getHeaderRef.getPath).next() + ctx.field(es.getHeaderRef.getPath).next().int(1))
          } else {
            dostuff
          }
        case ce : CaseEntry =>
          ctx.where(ctx(ce.getBExpr))
        case rs : ReturnStatement => ctx
        case rs : ReturnSelectStatement => ctx
        case at : ApplyTableStatement =>
          mkTableQuery(at.getTable)
        case ast : ApplyAndSelectTableStatement =>
          mkTableQuery(ast.getTable)
        case tce: TableCaseEntry =>
          val lastFlow = ctx.lastQuery(tce.getTableDeclaration.getName)
          def toAST(tce : TableCaseEntry) : P4Query = {
            if (tce.isHitMiss) {
              if (tce.hit()) {
                !lastFlow.isDefault
              } else if (tce.miss()) {
                lastFlow.isDefault
              } else {
                ctx.and(tce.getNegated.asScala.map(!toAST(_)))
              }
            } else {
              if (tce.defaultCase()) {
                ctx.and(tce.getNegated.asScala.map(!toAST(_)))
              } else {
                lastFlow.isAction(tce.action())
              }
            }
          }
          ctx.where(toAST(tce))
        case ac : ApplyControlStatement => ctx
      }
    }
    ret.as[T]
  }
  override def translate(src: ControlStatement,
                         rho: Option[P4BExpr],
                         dst: ControlStatement)(from: T): T = {
    mkQuery(src, rho, dst)(from)
  }
  override def merge(crt: T, merge: T): T = {
    (crt || merge).as[T]
  }
  override def stop(region: T): T = region.err().as[T]
  override def success(region: T): T = region.ok().as[T]
}
