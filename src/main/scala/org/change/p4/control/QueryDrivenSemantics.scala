package org.change.p4.control

import org.change.p4.control.SMInstantiator._
import org.change.p4.model.actions.P4ActionCall.ParamExpression
import org.change.p4.model.actions.primitives._
import org.change.p4.model.actions.{P4Action, P4ComplexAction}
import org.change.p4.model.control._
import org.change.p4.model.control.exp.P4BExpr
import org.change.p4.model.parser._
import org.change.p4.model.table.{MatchKind, TableDeclaration}
import org.change.p4.model.{ArrayInstance, HeaderInstance, Switch}

import scala.collection.JavaConverters._

class QueryDrivenSemantics[T <: P4Memory](switch: Switch)
    extends Semantics[T](switch) {

  def restoreFieldList(dst: T, src: T, omit: FieldRef => Boolean): T = {
    // src has the field_list_ref number set to the field list reference
    // under which it was cloned
    val fieldRef = src.field(FIELD_LIST_REF)
    dst
      .or(
        switch.getFieldListMap
          .keySet()
          .asScala
          .map(fr => {
            val idx = switch.getFieldListIndex(fr)
            val expanded = switch.expandFieldList(fr)
            val cd = fieldRef === fieldRef.int(idx)
            expanded.asScala
              .filter(!omit(_))
              .foldLeft(dst: P4Memory)((acc, x) => {
                acc.update(MkQuery.apply(acc, x), MkQuery.apply(src, x))
              })
              .where(cd)
              .update(fieldRef, fieldRef.zeros())
              .as[T]
          })
      )
      .as[T]
  }

  override def deparse(startFrom: T): T = {
    val resetNextIdx =
      switch.getInstances.asScala.foldLeft(startFrom)((acc, inst) => {
        inst match {
          case ai: ArrayInstance =>
            acc
              .update(
                acc.field(ai.getName).next(),
                acc.field(ai.getName).next().zeros()
              )
              .as[T]
          case hi: HeaderInstance =>
            acc
        }
      })
    super.deparse(startFrom)
  }

  override def buffer(p4Memory: T,
                      initial: T,
                      ingress: Boolean = true): BufferResult[T] = {
    val cloneInstanceType = if (ingress) INGRESS_CLONE else EGRESS_CLONE
    val recircType = if (ingress) RESUBMITED else RECIRCULATED
    val resubField = if (ingress) RESUBMIT_FLAG else RECIRCULATE_FLAG
    // what does the clone look like
    val cloneSpec = p4Memory.standardMetadata().field("clone_spec")
    val cl = p4Memory.where(cloneSpec != cloneSpec.int(0))
    val noClone = p4Memory.where(cloneSpec === cloneSpec.int(0))
    val espec = cl.cloneSession(cloneSpec)
    val copied =
      cl.update(cl.root(), initial.root()).update(cloneSpec, cloneSpec.zeros())
    //means: set instance type to clone_ingress and then re-run
    val toBeCloned = copied
      .update(copied.standardMetadata().field("egress_spec"), espec)
      .update(
        copied.standardMetadata().field("instance_type"),
        copied.standardMetadata().field("instance_type").int(cloneInstanceType)
      )
      .as[T]
    val postClone = restoreFieldList(toBeCloned, cl.as[T], omit = f => {
      f.getHeaderRef.getInstance().getName == STANDARD_METADATA &&
      (f.getField == CLONE_SPEC ||
      f.getField == "instance_type" ||
      f.getField == "egress_spec")
    }).update(cloneSpec, cloneSpec.int(0))

    //always zero out clone_spec to avoid bombarding the switch
    // mimic a merge operation if (clone_spec != 0) { set up the clone } else { noop; }
    val continuedPostClone = (postClone || noClone).as[P4Memory]
    // mgid handling
    val intrinsics = switch.intrinsic()
    val (afterMulticast, postResubmit) = if (intrinsics != null) {
      val (postResubmit, continueAfterResubmit, resub) =
        if (intrinsics.getLayout.getField(resubField) != null) {
          // do the resubmit logic
          val resubmitFlag =
            continuedPostClone.field(intrinsics.getName).field(resubField)
          val resub = continuedPostClone
            .where(resubmitFlag != resubmitFlag.int(0))
            .update(resubmitFlag, resubmitFlag.int(0))
          val toBeResubmited = resub
            .update(resub.root(), initial.root())
            .update(
              resub.standardMetadata().field("instance_type"),
              resub.standardMetadata().field("instance_type").int(recircType)
            )
          val postResubmit = restoreFieldList(
            toBeResubmited.as[T],
            resub.as[T],
            omit = f => {
              f.getHeaderRef.getInstance().getName == STANDARD_METADATA &&
              (f.getField == CLONE_SPEC ||
              f.getField == "instance_type" ||
              f.getField == "egress_spec" ||
              f.getField == resubField)
            }
          )
          (
            postResubmit,
            continuedPostClone.where(resubmitFlag === resubmitFlag.int(0)),
            resub
          )
        } else {
          // no more resubmit logic
          (
            continuedPostClone.where(continuedPostClone.boolVal(false)),
            continuedPostClone,
            continuedPostClone.where(continuedPostClone.boolVal(false))
          )
        }
      (if (ingress && intrinsics.getLayout.getField("mcast_grp") != null) {
        // do something with multicast
        val mgid =
          continueAfterResubmit.field(intrinsics.getName).field("mcast_grp")
        val multicasted = continueAfterResubmit
          .where(mgid != mgid.zeros())
          .update(
            resub.standardMetadata().field("instance_type"),
            resub.standardMetadata().field("instance_type").int(MULTICAST)
          )
          .update(
            continueAfterResubmit.standardMetadata().field("egress_spec"),
            continueAfterResubmit.multicastSession(mgid)
          )
        val noMulticast = continueAfterResubmit.where(mgid === mgid.zeros())
        (noMulticast || multicasted).as[P4Memory]
      } else {
        // do nothing with multicast
        continueAfterResubmit
      }, postResubmit)
    } else {
      (
        continuedPostClone,
        continuedPostClone.where(continuedPostClone.boolVal(false))
      )
    }

    val dropquery = (afterMulticast.standardMetadata().field("egress_spec") ===
      afterMulticast.standardMetadata().field("egress_spec").int(DROP_VALUE)) ||
      (!afterMulticast.egressAllowed(
        afterMulticast.standardMetadata().field("egress_spec")
      ))
    val dropped = afterMulticast.where(dropquery)
    val noDrop = afterMulticast.where(!dropquery)
    val continue =
      if (ingress)
        noDrop.update(
          noDrop.standardMetadata().field("egress_port"),
          noDrop.standardMetadata().field("egress_spec")
        )
      else noDrop
    BufferResult(
      cloned = cl.as[T],
      goesOn = continue.as[T],
      recirculated = postResubmit.as[T],
      dropped = dropped.as[T]
    )
  }

  def analyze(action: Add, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) + params(2))
  }

  def analyze(action: AddHeader,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    assert(params.size == 1)
    ctx
      .update(params.head, params.head.zeros())
      .update(params.head.valid(), ctx.boolVal(true))
  }

  def analyze(action: AddToField,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    assert(params.size == 2)
    ctx.update(params.head, params.head + params(1))
  }
  def analyze(action: BitAnd,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) & params(2))
  }
  def analyze(action: BitOr, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) | params(2))
  }
  def analyze(action: BitXor,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    assert(params.size == 3)
    ctx.update(params.head, params(1) ^ params(2))
  }
  def analyze(action: CloneEgressPktToEgress,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field(FIELD_LIST_REF)
    ctx
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }

  def analyze(action: CloneEgressPktToIngress,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
//    val cloneEgress = ctx.field("clone")
//    val cloneSpec = ctx.standardMetadata().field("clone_spec")
//    val flRef = ctx.field("field_list_ref")
//    ctx.update(cloneEgress, ctx.int(1))
//       .update(cloneSpec, params.head)
//       .update(flRef, params(1))
    throw new UnsupportedOperationException(
      "as per BMV2 and p4c, don't support " + action
    )
  }
  def analyze(action: CloneIngressPktToEgress,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
    val cloneSpec = ctx.standardMetadata().field("clone_spec")
    val flRef = ctx.field(FIELD_LIST_REF)
    ctx
      .update(cloneSpec, params.head)
      .update(flRef, params(1))
  }
  def analyze(action: CloneIngressPktToIngress,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    // TODO: please look at BMV2 and check how to do it
//    val cloneEgress = ctx.field("clone")
//    val cloneSpec = ctx.standardMetadata().field("clone_spec")
//    val flRef = ctx.field("field_list_ref")
//    ctx.update(cloneEgress, ctx.int(1))
//       .update(cloneSpec, params.head)
//       .update(flRef, params(1))
    throw new UnsupportedOperationException(
      "as per BMV2 and p4c, don't support " + action
    )
  }

  def analyze(action: CopyHeader,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx.update(params.head, params(1))
  }

  def analyze(action: Count, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    //TODO: add sema if need be
    ctx
  }

  def analyze(action: Drop, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    val fld = ctx.standardMetadata().field("egress_spec")
    ctx.update(fld, fld.int(DROP_VALUE))
  }

  def analyze(action: GenerateDigest,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx
  }

  def analyze(action: ModifyField,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx.update(params.head, params(1))
  }
  def analyze(action: ModifyFieldRngUniform,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    val v1 = params(1)
    val v2 = params(2)
    val dst = params.head
    val fresh = dst.fresh()
    ctx.where(fresh < v2 && fresh >= v1).update(dst, fresh)
  }

  def analyze(action: ModifyFieldWithHashBasedOffset,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    val base = params(1)
    val dst = params.head
    val sz = params(3)
    val fresh = dst.fresh()
    ctx.where(fresh < sz && fresh >= dst.int(0)).update(dst, fresh)
  }

  def analyze(action: NoOp, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = ctx

  def analyze(action: Pop, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    val arr = params.head
    val nxtidx = arr.next()
    val count = params(1)
    val fresh = arr.fresh()
    val len = arr.len().nr.toInt
    val newnext =
      (nxtidx - count >= arr.len().int(0)).ite(nxtidx - count, arr.len().int(0))
    ctx
      .where(ctx.and((0 until len).map(idx => {
        val i = ctx.int(idx)
        val zeros = fresh(i).zeros()
        fresh(i) === (count < count.int(len - idx)).ite(arr(i - count), zeros)
      })))
      .update(arr, fresh)
  }

  def analyze(action: Push, params: List[P4Query], stackTrace: List[P4Action])(
    implicit ctx: P4Memory
  ): P4Memory = {
    val arr = params.head
    val nxtidx = arr.next()
    val count = params(1)
    val fresh = arr.fresh()
    val len = arr.len().nr.toInt
    val newnext = (nxtidx + count <= arr.len()).ite(nxtidx + count, arr.len())
    ctx
      .where(ctx.and((0 until len).map(idx => {
        val i = ctx.int(idx)
        val zeros = fresh(i).zeros()
        val valid = zeros.valid()
        val flds = ctx.and(
          fresh(i)
            .fields()
            .filter(_ != "IsValid")
            .map(f => {
              fresh(i).field(f) === fresh(i).field(f).zeros()
            }) ++ List(zeros.valid() === ctx.boolVal(true))
        )
        (i < count).ite(flds, fresh(i) === arr(i - count))
      })))
      .update(arr, fresh)
      .update(nxtidx, newnext)
  }

  def analyze(action: Recirculate,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    val hi = switch.getInstance(INTRINSIC_METADATA)
    if (hi != null) {
      if (hi.getLayout.getField(RECIRCULATE_FLAG) != null) {
        ctx
          .update(
            ctx.field(INTRINSIC_METADATA).field(RECIRCULATE_FLAG),
            ctx.field(INTRINSIC_METADATA).field(RECIRCULATE_FLAG).int(1)
          )
          .update(ctx.field(FIELD_LIST_REF), params(1))
      } else ctx
    } else ctx
  }

  def analyze(action: RegisterRead,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    //TODO: add real semantics for registers
    ctx.update(params.head, params.head.fresh())
  }

  def analyze(action: Resubmit,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    val hi = switch.getInstance(INTRINSIC_METADATA)
    if (hi != null) {
      if (hi.getLayout.getField(RESUBMIT_FLAG) != null) {
        ctx
          .update(
            ctx.field(INTRINSIC_METADATA).field(RESUBMIT_FLAG),
            ctx.field(INTRINSIC_METADATA).field(RESUBMIT_FLAG).int(1)
          )
          .update(ctx.field(FIELD_LIST_REF), params(1))
      } else ctx
    } else ctx
  }

  def analyze(action: ShiftLeft,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory =
    ctx.update(params.head, params(1) << params(2))

  def analyze(action: ShiftRight,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory =
    ???

  def analyze(action: Subtract,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory =
    ctx.update(params.head, params(1) - params(2))

  def analyze(action: SubtractFromField,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory =
    ctx.update(params.head, params.head - params(1))

  def analyze(action: RemoveHeader,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx.update(params.head.valid(), params.head.valid().int(0))
  }
  def analyze(action: ExecuteMeter,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx
  }
  def analyze(action: RegisterWrite,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    ctx
  }

  def analyze(action: Truncate,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory =
    ctx.update(
      ctx.standardMetadata().field("target_packet_length"),
      params.head
    )
  def analyze(p4Action: P4Action,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    before((p4Action, params, stackTrace), ctx.as[T])
    val out = p4Action match {
      case ax: org.change.p4.model.actions.primitives.Add =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.AddHeader =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.AddToField =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.BitAnd =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.BitOr =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.BitXor =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.CloneEgressPktToEgress =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.CloneEgressPktToIngress =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.CloneIngressPktToEgress =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.CloneIngressPktToIngress =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.CopyHeader =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Count =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Drop =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ExecuteMeter =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.GenerateDigest =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ModifyField =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ModifyFieldRngUniform =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.NoOp =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Pop =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Push =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Recirculate =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.RegisterRead =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.RegisterWrite =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.RemoveHeader =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Resubmit =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ShiftLeft =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.ShiftRight =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Subtract =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.SubtractFromField =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.primitives.Truncate =>
        analyze(ax, params, stackTrace)
      case ax: org.change.p4.model.actions.P4ComplexAction =>
        analyze(ax, params, stackTrace)
    }
    after((p4Action, params, stackTrace), out.as[T])
    out
  }

  def analyze(action: P4ComplexAction,
              params: List[P4Query],
              stackTrace: List[P4Action])(implicit ctx: P4Memory): P4Memory = {
    assert(action.getParameterList.size() == params.size)
    val mapper = action.getParameterList.asScala
      .zip(params)
      .map(p => {
        p._1.getParamName -> p._2
      })
      .toMap
    val resolutionContext = mapper.foldLeft(ctx)((acc, parm) => {
      acc.update(acc.field(action.getActionName + "_" + parm._1), parm._2)
    })
    action.getActionList.asScala.foldLeft(resolutionContext)((acc, actcall) => {
      val parms = actcall
        .params()
        .asScala
        .map(_.asInstanceOf[ParamExpression].getExpression)
        .map(exp => acc(exp))
      before(actcall, acc.as[T])
      val res =
        analyze(actcall.getP4Action, parms.toList, action :: stackTrace)(acc)
      after(actcall, res.as[T])
      res
    })
  }

  def before(obj: Object, ctx: T): Unit = {}
  def after(obj: Object, ctx: T): Unit = {}

  protected def mkActionQuery(p4Action: P4Action, lst: List[P4Query])(
    implicit ctx: P4Memory
  ): P4Query = {
    analyze(p4Action, lst, Nil)
  }

  protected def mkTableQuery(
    tableDeclaration: TableDeclaration
  )(implicit ctx: P4Memory): T = {
    val matches = tableDeclaration.getMatches.asScala.map(tm => {
      if (tm.getMatchKind == MatchKind.Valid) {
        val hdr = tm.getReferenceKey.asInstanceOf[HeaderRef]
        ctx(hdr).valid()
      } else {
        tm.getReferenceKey match {
          case fr: FieldRef =>
            ctx(fr.getHeaderRef).valid().ite(ctx(fr), ctx(fr).zeros())
          case fr: MaskedFieldRef =>
            val href = fr.getReference.getHeaderRef
            //TODO: move this as a pass in SolveTables
            fr.getMask.setWidth(fr.getReference.getFieldReference.getLength)
            val qmask =
              RichContext.apply(ctx).apply(fr.getMask.asInstanceOf[Expression])
            val r = ctx(fr.getReference)
            ctx(href).valid().ite(r & qmask, r.zeros())
        }
      }
    })

    val newquery = ctx.update(
      ctx.lastQuery(tableDeclaration.getName),
      ctx.query(tableDeclaration.getName, matches)
//      ctx.lastQuery(tableDeclaration.getName).fresh()
    )
    val lastFlow = newquery.lastQuery(tableDeclaration.getName)

    val actionSema = newquery.or(
      (if (tableDeclaration.hasProfile) {
         tableDeclaration.actionProfile().getActions.asScala
       } else {
         tableDeclaration.getAllowedActions.asScala.map(_.getActionName)
       }).map(x => {
        mkActionQuery(
          switch.action(x),
          switch
            .action(x)
            .getParameterList
            .asScala
            .map(parm => {
              lastFlow.getParam(x, parm.getParamName)
            })
            .toList
        )(newquery.where(lastFlow.isAction(x)))
      })
    )
    actionSema.as[T]
  }

  protected def mkQuery(src: ControlStatement,
                        rho: Option[P4BExpr],
                        dst: ControlStatement)(implicit ctx: T): T = {
    before((src, rho, dst), ctx)
    val ret = if (rho.nonEmpty) {
      val what = ctx(rho.get)
      ctx.where(what)
    } else {
      src match {
        case _: EndOfControl => ctx
        case ss: SetStatement =>
          ctx.update(ctx(ss.getLeft), ctx(ss.getRightE))
        case es: ExtractStatement =>
          val hdr1 = ctx(es.getExpression)
          val validRef = hdr1.valid()
          val dostuff = es.getExpression
            .getInstance()
            .getLayout
            .getFields
            .asScala
            .map(_.getName)
            .foldLeft(ctx.update(validRef, validRef.int(1)))(
              (crtQuery, fname) => {
                val hdr1 = crtQuery(es.getExpression)
                val fld = hdr1.field(fname)
                val packet = crtQuery.packet()
                val taken = packet(fld.len().int(0), fld.len())
                val newpack = packet.pop(fld.len())
                crtQuery
                  .update(fld, taken)
                  .update(packet, newpack)
              }
            )
          if (es.getExpression.isArray &&
              es.getExpression.asInstanceOf[IndexedHeaderRef].isNext) {
            val nxtfield = dostuff.field(es.getExpression.getPath).next()
            dostuff.update(nxtfield, nxtfield + nxtfield.int(1))
          } else {
            dostuff
          }
        case es: EmitStatement =>
          val packet = ctx.packet()
          val hdr1 = ctx(es.getHeaderRef)
          val valid = hdr1.valid()
          // this reverse looks strange, but it isn't,
          // in order to be able to use prepend semantics
          // need to do deparsing the opposite way
          val dostuff = valid
            .ite(
              es.getHeaderRef
                .getInstance()
                .getLayout
                .getFields
                .asScala
                .reverse
                .map(_.getName)
                .foldLeft(ctx: P4Memory)((crtQuery, fld) => {
                  crtQuery.update(
                    crtQuery.packet(),
                    crtQuery.packet().prepend(hdr1.field(fld))
                  )
                }),
              ctx
            )
            .as[P4Memory]
          if (es.getHeaderRef.isArray &&
              es.getHeaderRef.asInstanceOf[IndexedHeaderRef].isNext) {
            dostuff.update(
              ctx.field(es.getHeaderRef.getPath).next(),
              ctx
                .field(es.getHeaderRef.getPath)
                .next() + ctx.field(es.getHeaderRef.getPath).next().int(1)
            )
          } else {
            dostuff
          }
        case ce: CaseEntry =>
          ctx.where(ctx(ce.getBExpr))
        case rs: ReturnStatement       => ctx
        case rs: ReturnSelectStatement => ctx
        case at: ApplyTableStatement =>
          mkTableQuery(at.getTable)
        case ast: ApplyAndSelectTableStatement =>
          mkTableQuery(ast.getTable)
        case tce: TableCaseEntry =>
          val lastFlow = ctx.lastQuery(tce.getTableDeclaration.getName)
          def toAST(tce: TableCaseEntry): P4Query = {
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
        case ac: ApplyControlStatement => ctx
      }
    }
    after((src, rho, dst), ret.as[T])
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
  override def success(region: T): T = region.as[T]

  override def lazyMerge: Boolean = true

  override def merge(ts: Iterable[T]): T = {
    if (ts.size == 1)
      ts.head
    else
      ts.head.or(ts).as[T]
  }
}
