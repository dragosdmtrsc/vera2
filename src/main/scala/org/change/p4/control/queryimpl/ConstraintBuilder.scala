package org.change.p4.control.queryimpl

import org.change.p4.control._
import org.change.p4.control.types.BVType
import org.change.p4.model.Switch
import org.change.p4.model.table.MatchKind
import com.microsoft.z3.{AST, Context, Expr}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.change.utils.Z3Helper._
//TODO: redo in the new flow structure
object ConstraintBuilder {

//  def constrainParams(context: Context,
//                      actionSpec: ActionSpec,
//                      actionParams: List[ActionParam],
//                      bound: AST,
//                      setDefault: Boolean,
//                      flowStruct: FlowStruct,
//                      boundFlowInstance: IFlowInstance): List[AST] = {
//    actionSpec match {
//      case Action(x) =>
//        val aname = x.getActionName
//        val isAction =
//          flowStruct.isAction(x.getActionName, boundFlowInstance.actionRun())
//        val hits =
//          if (setDefault)
//            context.mkNot(boundFlowInstance.hits().asBool)
//          else boundFlowInstance.hits()
//        assert(actionParams.size == x.getParameterList.asScala.size)
//        hits ::
//          isAction ::
//          actionParams
//          .zip(x.getParameterList.asScala)
//          .map(
//            f =>
//              f._1 match {
//                case ParamValue(pv) =>
//                  val BVType(n) = f._2.getSort
//                  val nr = context.mkNumeral(pv.toString, context.mkBVSort(n))
//                  val access = boundFlowInstance
//                    .getActionParam(x.getActionName, f._2.getParamName)
//                  context.mkEq(access, nr)
//            }
//          )
//    }
//  }
//
//  def mkInstance(context: Context,
//                 actionSpec: ActionSpec,
//                 actionParams: List[ActionParam],
//                 setDefault: Boolean,
//                 flowStruct: FlowStruct): Expr = {
//    var asts: ListBuffer[Expr] = ListBuffer(
//      context.mkTrue(),
//      if (setDefault) context.mkFalse() else context.mkTrue()
//    )
//    actionSpec match {
//      case Action(x) =>
//        val aname = x.getActionName
//        asts.append(flowStruct.actionTypeConst(aname))
//        assert(actionParams.size == x.getParameterList.asScala.size)
//        flowStruct.matchKinds.foreach(s => {
//          asts.append(
//            if (s.equals(context.getBoolSort)) context.mkFalse()
//            else context.mkNumeral(0, s)
//          )
//        })
//        val parmMap = actionParams
//          .zip(x.getParameterList.asScala)
//          .map(f => {
//            f._2.getParamName -> f._1
//          })
//          .toMap
//        flowStruct.parmTypes.foreach(pt => {
//          if (pt._1._1 == aname) {
//            parmMap(pt._1._2) match {
//              case ParamValue(pv) =>
//                val nr = context.mkNumeral(pv.toString(), pt._2)
//                asts.append(nr)
//            }
//          } else {
//            asts.append(context.mkNumeral(0, pt._2))
//          }
//        })
//        flowStruct.flowSort._2(asts: _*)
//    }
//  }

  def apply(switch: Switch,
            context: Context,
            instance: Instance): Iterable[Expr] = {
    //TODO: redo in the new flow structure
//    val stdMeta = switch.getInstance("standard_metadata")
//    val clSpec = stdMeta.getLayout.getField("clone_spec")
//    val cloneAxiom = if (clSpec != null) {
//      val parm = context.mkFreshConst("p", context.mkBVSort(clSpec.getLength))
//      val res = context.mkFreshConst("ret", context.mkBVSort(9))
//      val initialPredicate : Expr = context.mkEq(res, context.mkNumeral(511, res.getSort))
//      val axiomBody = instance.mirrors.foldLeft(initialPredicate)((acc, m) => {
//        val pred = context.mkOr(
//          m._2.map(
//            v => context.mkEq(res, context.mkNumeral(v.toString(), res.getSort))
//          ): _*
//        )
//        val nowPredicate =
//          context.mkEq(context.mkNumeral(m._1.toString(), parm.getSort), parm)
//        context.mkITE(nowPredicate, pred, acc)
//      })
//      val tm = TypeMapper()(context)
//      val impl = context.mkEq(tm.applyFunction("clone_session", parm), res)
//      context.mkImplies(impl, axiomBody.asBool)
//    } else {
//      context.mkTrue()
//    }
//    val tableAxioms = switch
//      .tables()
//      .asScala
//      .map(t => {
//        val flowStruct = FlowStruct(context, t, switch)
//        var bounds = flowStruct.queryParms.zipWithIndex.map(f => {
//          context.mkFreshConst("p", f._1)
//        })
//        val contents = instance.tables.getOrElse(t.getName, Nil)
//        val default = instance.defaults(t.getName)
//        val defcond = mkInstance(
//          context,
//          default.action,
//          default.parms,
//          setDefault = true,
//          flowStruct
//        )
//        val forallBody = contents.foldLeft(defcond)((acc, x) => {
//          // good stuff
//          val cond = t.getMatches.asScala
//            .zip(x.matches)
//            .zipWithIndex
//            .map(f => {
//              f._1._1.getMatchKind match {
//                case MatchKind.Exact =>
//                  val bound = bounds(f._2)
//                  val Exact(v) = f._1._2
//                  context
//                    .mkEq(context.mkNumeral(v.toString(), bound.getSort), bound)
//                case MatchKind.Lpm =>
//                  val bound = bounds(f._2)
//                  val Prefix(v, p) = f._1._2
//                  val msk = (BigInt(1) << p) - 1
//                  context.mkEq(
//                    context.mkNumeral((v & msk).toString(), bound.getSort),
//                    context.mkBVAND(
//                      bound.asBV,
//                      context.mkNumeral(msk.toString(), bound.getSort).asBV
//                    )
//                  )
//                case MatchKind.Ternary =>
//                  val bound = bounds(f._2)
//                  val Masked(v, msk) = f._1._2
//                  context.mkEq(
//                    context.mkNumeral((v & msk).toString(), bound.getSort),
//                    context.mkBVAND(
//                      bound.asBV,
//                      context.mkNumeral(msk.toString(), bound.getSort).asBV
//                    )
//                  )
//                case MatchKind.Valid =>
//                  val bound = bounds(f._2)
//                  val Exact(v) = f._1._2
//                  context.mkEq(
//                    if (v == 1) context.mkTrue() else context.mkFalse(),
//                    bound
//                  )
//                case MatchKind.Range =>
//                  val Range(min, max) = f._1._2
//                  val bound = bounds(f._2)
//                  val minast = context.mkNumeral(min.toString, bound.getSort)
//                  val maxast = context.mkNumeral(max.toString, bound.getSort)
//                  context.mkAnd(
//                    context.mkBVSLE(bound.asBV, maxast.asBV),
//                    context.mkBVSGE(bound.asBV, minast.asBV)
//                  )
//              }
//            })
//          context.mkITE(
//            context.mkAnd(cond: _*),
//            mkInstance(
//              context,
//              x.action,
//              x.parms,
//              setDefault = false,
//              flowStruct
//            ),
//            acc
//          )
//        })
//        val fullQuery = context.mkEq(flowStruct.query(bounds).AST, forallBody)
//        context.mkForall(
//          bounds.toArray,
//          forallBody,
//          0,
//          Array(),
//          null, null, null
//        )
//      })
//      .toList
//    cloneAxiom :: tableAxioms
    Nil
  }

  def apply(switch: Switch,
            context: Context,
            switchTarget: SwitchTarget): Iterable[Expr] = {
    val tm = TypeMapper()(context)
    Seq("constrain_ingress_port", "constrain_egress_port").map(f => {
      val funObj = tm.funMap(f)
      val const = tm.fresh(funObj._1).asInstanceOf[ScalarValue].AST
      val body = context.mkOr(switchTarget.inputPorts.toList.map(p => {
        context.mkEq(const, tm.literal(funObj._1, p).AST)
      }): _*)
      context.mkForall(
        Array(const),
        context.mkEq(funObj._3(const), body),
        0, Array(), null, null, null
      )
    })
  }
}
