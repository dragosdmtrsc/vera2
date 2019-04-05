package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.{FlowStruct, IFlowInstance, SwitchTarget}
import org.change.parser.p4.parser._
import org.change.plugins.vera.BVType
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.table.MatchKind
import z3.scala.{Z3AST, Z3Context}

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

object ConstraintBuilder {

  def constrainParams(context: Z3Context,
                      actionSpec: ActionSpec,
                      actionParams : List[ActionParam],
                      bound : Z3AST,
                      setDefault : Boolean,
                      flowStruct: FlowStruct,
                      boundFlowInstance : IFlowInstance): List[Z3AST] = {
    actionSpec match {
      case Action(x) =>
        val aname = x.getActionName
        val isAction = flowStruct.isAction(x.getActionName,
          boundFlowInstance.actionRun())
        val hits = if (setDefault)
          context.mkNot(boundFlowInstance.hits())
        else boundFlowInstance.hits()
        assert(actionParams.size == x.getParameterList.asScala.size)
        hits ::
          isAction ::
            actionParams.zip(x.getParameterList.asScala).map(f => f._1 match {
              case ParamValue(pv) =>
                val BVType(n) = f._2.getSort
                val nr = context.mkNumeral(pv.toString, context.mkBVSort(n))
                val access = boundFlowInstance.getActionParam(x.getActionName, f._2.getParamName)
                context.mkEq(access, nr)
            })
    }
  }

  def mkInstance(context : Z3Context,
                 actionSpec: ActionSpec,
                 actionParams : List[ActionParam],
                 setDefault : Boolean,
                 flowStruct: FlowStruct) : Z3AST = {
    var asts : ListBuffer[Z3AST] = ListBuffer(context.mkTrue(),
      if (setDefault) context.mkFalse() else context.mkTrue()
    )
    actionSpec match {
      case Action(x) =>
        val aname = x.getActionName
        asts.append(flowStruct.actionTypeConst(aname))
        assert(actionParams.size == x.getParameterList.asScala.size)
        flowStruct.matchKinds.foreach(s => {
          asts.append(if (s.isBoolSort) context.mkFalse()
          else context.mkInt(0, s))
        })
        val parmMap = actionParams.zip(x.getParameterList.asScala).map(f => {
          f._2.getParamName -> f._1
        }).toMap
        flowStruct.parmTypes.foreach(pt => {
          if (pt._1._1 == aname) {
            parmMap(pt._1._2) match {
              case ParamValue(pv) =>
                val nr = context.mkNumeral(pv.toString(), pt._2)
                asts.append(nr)
            }
          } else {
            asts.append(context.mkInt(0, pt._2))
          }
        })
        flowStruct.flowSort._2(asts:_*)
    }
  }

  def apply(switch: Switch, context : Z3Context, instance: Instance): Iterable[Z3AST] = {
    val stdMeta = switch.getInstance("standard_metadata")
    val clSpec = stdMeta.getLayout.getField("clone_spec")
    val cloneAxiom = if (clSpec != null) {
      val parm = context.mkFreshConst("p", context.mkBVSort(clSpec.getLength))
      val res = context.mkFreshConst("ret", context.mkBVSort(9))
      val initialPredicate = context.mkEq(
        res, context.mkInt(511, res.getSort)
      )
      val axiomBody = instance.mirrors.foldLeft(initialPredicate)((acc, m) => {
        val pred = context.mkOr(
          m._2.map(v => context.mkEq(res, context.mkNumeral(v.toString(), res.getSort))):_*
        )
        val nowPredicate = context.mkEq(
          context.mkNumeral(m._1.toString(), parm.getSort),
          parm
        )
        context.mkITE(nowPredicate, pred, acc)
      })
      val tm = TypeMapper()(context)
      val impl = context.mkEq(tm.applyFunction("clone_session", parm), res)
      context.mkImplies(impl, axiomBody)
    } else {
      context.mkTrue()
    }
    val tableAxioms = switch.tables().asScala.map(t => {
      val flowStruct = FlowStruct(context, t, switch)
      var bounds = flowStruct.queryParms.zipWithIndex.map(f => {
        context.mkFreshConst("p", f._1)
      })
      val contents = instance.tables.getOrElse(t.getName, Nil)
      val default = instance.defaults(t.getName)
      val defcond = mkInstance(context,
        default.action,
        default.parms,
        setDefault = true,
        flowStruct)
      val forallBody = contents.foldLeft(defcond)((acc, x) => {
          // good stuff
        val cond = t.getMatches.asScala.zip(x.matches).zipWithIndex.map(f => {
          f._1._1.getMatchKind match {
            case MatchKind.Exact =>
              val bound = bounds(f._2)
              val Exact(v) = f._1._2
              context.mkEq(
                context.mkNumeral(v.toString(), bound.getSort),
                bound
              )
            case MatchKind.Lpm =>
              val bound = bounds(f._2)
              val Prefix(v, p) = f._1._2
              val msk = (BigInt(1) << p) - 1
              context.mkEq(
                context.mkNumeral((v & msk).toString(), bound.getSort),
                context.mkBVAnd(bound, context.mkNumeral(msk.toString(), bound.getSort))
              )
            case MatchKind.Ternary =>
              val bound = bounds(f._2)
              val Masked(v, msk) = f._1._2
              context.mkEq(
                context.mkNumeral((v & msk).toString(), bound.getSort),
                context.mkBVAnd(bound, context.mkNumeral(msk.toString(), bound.getSort))
              )
            case MatchKind.Valid =>
              val bound = bounds(f._2)
              val Exact(v) = f._1._2
              context.mkEq(
                if (v == 1) context.mkTrue() else context.mkFalse(),
                bound
              )
            case MatchKind.Range =>
              val Range(min, max) = f._1._2
              val bound = bounds(f._2)
              val minast = context.mkNumeral(min.toString, bound.getSort)
              val maxast = context.mkNumeral(max.toString, bound.getSort)
              context.mkAnd(
                context.mkBVSle(bound, maxast),
                context.mkBVSge(bound, minast)
              )
          }
        })
        context.mkITE(
          context.mkAnd(cond:_*),
          mkInstance(context,
            x.action,
            x.parms,
            setDefault = false,
            flowStruct),
          acc
        )
      })
      val fullQuery = context.mkEq(
          flowStruct.query(bounds).z3AST,
          forallBody)
      context.mkForAllConst(0, Seq.empty, bounds, fullQuery)
    }).toList
    cloneAxiom :: tableAxioms
  }

  def apply(switch: Switch, context : Z3Context, switchTarget: SwitchTarget) : Iterable[Z3AST] = {
    val tm = TypeMapper()(context)
    Seq("constrain_ingress_port", "constrain_egress_port").map(f => {
      val funObj = tm.funMap(f)
      val const = tm.fresh(funObj._1).asInstanceOf[ScalarValue].z3AST
      val body = context.mkOr(
        switchTarget.inputPorts.toList.map(p => {
          context.mkEq(
            const,
            tm.literal(funObj._1, p).z3AST
          )
        }):_*
      )
      context.mkForAllConst(0, Seq(), Seq(const), context.mkEq(
        funObj._3(const), body
      ))
    })
  }
}
