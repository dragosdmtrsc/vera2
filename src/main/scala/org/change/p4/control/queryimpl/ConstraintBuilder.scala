package org.change.p4.control.queryimpl

import java.util.logging.Logger

import com.microsoft.z3.{BoolExpr, Context, Expr}
import org.change.p4.control._
import org.change.p4.control.types.BoolType
import org.change.p4.model.Switch
import org.change.p4.model.table.TableDeclaration
import org.change.utils.Z3Helper._

import scala.collection.JavaConverters._

class TableConstraintBuilder(switch: Switch,
                             instance: Instance,
                             context: Context) {
  def mkInstance(actionSpec: ActionSpec,
                 actionParams: List[ActionParam],
                 setDefault: Boolean,
                 tableDeclaration: TableDeclaration,
                 ek : EnumKind,
                 flowStruct: FlowStruct): Value = {
    val tm  = TypeMapper()(context)
    val allzeros = tm.zeros(flowStruct).asInstanceOf[StructObject]
    actionSpec match {
      //TODO: handle action profile dynamic selectors - member should be the
      //disjunction of all allowed actions in the called profile -
      //cater for nondeterminism with extra fresh variable
      case Action(x) =>
        val aname = x.getActionName
        val enumConstant = tm.literal(ek, ek.getId(aname))
        val parmMap = actionParams
          .zip(x.getParameterList.asScala)
          .map(f => {
            (aname + "_" + f._2.getParamName) -> f._1
          })
          .toMap
          .map(v => v._2 match {
            case ParamValue(u) =>
              v._1 -> tm.literal(flowStruct.members(v._1), u)
          }) ++ Map("action" -> enumConstant) ++ Map("hits" ->
          tm.literal(BoolType, if (setDefault) 0 else 1))
        allzeros.copy(fieldRefs = allzeros.fieldRefs ++ parmMap)
      case ProfileMember(p, idx) =>
        val profileEntries = instance.profiles.getOrElse(p.getName, Nil)
        val (act, parms) = if (idx >= profileEntries.size) {
          Logger.getLogger("constraintsBuilder")
            .warning(s"profile ${p.getName} member index $idx out of bounds => ignoring this entry")
          (Action(switch.action("no_op")), List.empty[ActionParam])
        } else {
          val act = profileEntries(idx).action
          val parms = profileEntries(idx).parms
          (act, parms)
        }
        mkInstance(act, parms, setDefault, tableDeclaration, ek, flowStruct)
    }
  }


  def mkTableAxioms(): List[Expr] = {
    val tm = TypeMapper()(context)
    val tableAxioms = switch
      .tables()
      .asScala
      .filter(t =>
        ExternFunction.hasFunction(s"query_${t.getName}")
      )
      .flatMap(t => {
        val calls = ExternFunction.getFunction(s"query_${t.getName}")
        val fs = FlowStruct.apply(switch, t, context)
        val rootMemory = new RootMemory(
          tm.zeros(fs).asInstanceOf[StructObject],
          tm.literal(BoolType, 1))(context)
        val root = P4RootMemory(switch, rootMemory)
        calls.callsToRets.map(currentCall => {
          val contents = instance.tables.getOrElse(t.getName, Nil)
          val default = instance.defaults(t.getName)
          val ek = EnumKind.enumKinds(s"action_type_${t.getName}")
          val defcond = mkInstance(
            default.action,
            default.parms,
            setDefault = true,
            t,
            ek,
            fs
          )
          val output = contents.foldLeft(root.ValueWrapper.rv(defcond): P4Query)((acc, x) => {
            val whenever = root.and(x.matches.zip(currentCall._1).map(x => {
              val x2 = root.ValueWrapper.rv(x._2)
              x._1 match {
                case Range(a, b) =>
                  x2 <= x2.int(b) && x2 >= x2.int(a)
                case Masked(a, b) =>
                  (x2 & x2.int(b)) === (x2.int(a) & x2.int(b))
                case Prefix(a, b) =>
                  val msk = x2.int(1) << x2.int(b) - x2.int(1)
                  (x2 & msk) === (x2.int(a) & msk)
                case Exact(a) =>
                  x2 === x2.int(a)
              }
            }))
            whenever.ite(root.ValueWrapper.rv(mkInstance(
              x.action,
              x.parms,
              setDefault = false,
              t,
              ek,
              fs
            )), acc)
          })
          (root.ValueWrapper.rv(currentCall._2) === output)
            .as[AbsValueWrapper]
            .value
            .asInstanceOf[ScalarValue]
            .AST
        })
      }).toList
    tableAxioms
  }
}

object ConstraintBuilder {

  def mkTableAxioms(switch: Switch,
                    context: Context,
                    instance: Instance): List[Expr] = {
    new TableConstraintBuilder(switch, instance, context).mkTableAxioms()
  }
  def apply(switch: Switch,
            context: Context,
            instance: Instance): List[Expr] = {
    val stdMeta = switch.getInstance("standard_metadata")
    val clSpec = stdMeta.getLayout.getField("clone_spec")
    val cloneAxiom = if (clSpec != null) {
      val parm = context.mkFreshConst("p", context.mkBVSort(clSpec.getLength))
      val res = context.mkFreshConst("ret", context.mkBVSort(9))
      val initialPredicate : Expr = context.mkEq(res, context.mkNumeral(511, res.getSort))
      val axiomBody = instance.mirrors.foldLeft(initialPredicate)((acc, m) => {
        val pred = context.mkOr(
          m._2.map(
            v => context.mkEq(res, context.mkNumeral(v.toString(), res.getSort))
          ): _*
        )
        val nowPredicate =
          context.mkEq(context.mkNumeral(m._1.toString(), parm.getSort), parm)
        context.mkITE(nowPredicate, pred, acc)
      })
      val tm = TypeMapper()(context)
      val impl = context.mkEq(tm.applyFunction("clone_session", parm), res)
      context.mkImplies(impl, axiomBody.asBool)
    } else {
      context.mkTrue()
    }
    val tableAxioms: scala.List[Expr] = mkTableAxioms(switch, context, instance)
    cloneAxiom :: tableAxioms
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
