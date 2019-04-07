package org.change.p4.control.queryimpl

import com.microsoft.z3.Context
import org.change.p4.control.types.{BVType, BoolType, P4Type, StructType}
import org.change.p4.model.Switch
import org.change.p4.model.parser.{FieldRef, HeaderRef, MaskedFieldRef}
import org.change.p4.model.table.{MatchKind, TableDeclaration}

import scala.collection.JavaConverters._

class FlowStruct(switch : Switch,
                      tableDeclaration: TableDeclaration,
                      fields : Map[String, P4Type],
                      context : Context)
  extends StructType(fields) {
  def query(vals : List[ScalarValue]) : Value = {
    val ef = ExternFunction.getFunction(s"query_${tableDeclaration.getName}")
    ef(vals.map(_.asInstanceOf[Value]))
  }
}

object FlowStruct {
  def apply(switch : Switch,
            tableDeclaration: TableDeclaration,
            context: Context) : FlowStruct = {
    val actionTypes = (if (tableDeclaration.hasProfile) {
      val prof = tableDeclaration.actionProfile()
      prof.getActions.asScala
    } else {
      tableDeclaration.getAllowedActions.asScala.map(_.getActionName)
    }).flatMap(act => {
      val p4act = switch.action(act)
      p4act.getParameterList.asScala.map(p =>
        act + "_" + p.getParamName -> p.getSort)
    }).toMap
    val str = Map(
      "exists" -> BoolType,
      "hits" -> BoolType,
      "action" -> BVType(8)
    ) ++ actionTypes

    val matchParms = tableDeclaration.getMatches.asScala.map(r => {
      r.getReferenceKey match {
        case fr : FieldRef => BVType(fr.getFieldReference.getLength)
        case masked : MaskedFieldRef => BVType(masked.getReference.getFieldReference.getLength)
        case hr : HeaderRef if r.getMatchKind == MatchKind.Valid =>
          BoolType
      }
    }).toList
    val res = new FlowStruct(switch, tableDeclaration, str, context)
    ExternFunction.declareFunction(ExternFunction(
        s"query_${tableDeclaration.getName}",
        matchParms,
        res,
        context))
    res
  }
}