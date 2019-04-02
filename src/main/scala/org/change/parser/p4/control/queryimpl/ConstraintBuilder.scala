package org.change.parser.p4.control.queryimpl

import org.change.parser.p4.control.{FlowStruct, IFlowInstance, SwitchTarget}
import org.change.parser.p4.parser._
import org.change.plugins.vera.BVType
import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.table.MatchKind
import z3.scala.{Z3AST, Z3Context}

import scala.collection.JavaConverters._

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

  def apply(switch: Switch, context : Z3Context, instance: Instance): Iterable[Z3AST] = {
    switch.tables().asScala.map(t => {
      val flowStruct = FlowStruct(context, t, switch)
      var bounds = flowStruct.queryParms.zipWithIndex.map(f => {
        context.mkFreshConst("p", f._1)
      })
      val structBound = context.mkFreshConst("p", flowStruct.sort())
      val boundFlowInstance = flowStruct.from(structBound)
      bounds = bounds :+ structBound
      val contents = instance.tables.getOrElse(t.getName, Nil)
      val default = instance.defaults(t.getName)
      val defcond = context.mkAnd(constrainParams(context,
        default.action,
        default.parms,
        structBound,
        setDefault = true,
        flowStruct,
        boundFlowInstance):_*)
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
          context.mkAnd(constrainParams(context,
            x.action,
            x.parms,
            structBound,
            setDefault = false,
            flowStruct,
            boundFlowInstance) :_*),
          acc
        )
      })
      val fullQuery = context.mkImplies(
        context.mkEq(flowStruct.query(bounds.take(bounds.size - 1)).z3AST,
          bounds.last
        ), forallBody
      )
      context.mkForAllConst(0, Seq.empty, bounds, fullQuery)
    })
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
