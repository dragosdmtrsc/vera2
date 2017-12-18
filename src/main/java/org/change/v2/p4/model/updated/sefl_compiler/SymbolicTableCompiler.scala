//package org.change.v2.p4.model.updated.sefl_compiler
//
//import org.change.v2.analysis.expression.abst.Expression
//import org.change.v2.analysis.expression.concrete.SymbolicValue
//import org.change.v2.p4.model.actions.{P4Action, P4ActionParameter}
//import org.change.v2.p4.model.updated.program.P4Program
//import org.change.v2.p4.model.updated.sefl_compiler.sefl_code.{EmptySEFLProgram, SEFLCode, SEFLCodeContainer}
//import org.change.v2.p4.model.updated.table.{BasicActionSpecification, TableDeclaration}
//
//object SymbolicTableCompiler extends SEFLCompiler[P4Program] {
//
//  override def compile(p4: P4Program): SEFLCode = {
//    /**
//      * For every table, symbolic match values and action parameters are allocated.
//      */
//    val matchValues: Map[String, Expression] = (for {
//      source <- p4.tableDeclarations.values
//      tableName = source.tableName
//    } yield source.allowedActions match {
//      case basicActionSpec: BasicActionSpecification => for {
//        action <- basicActionSpec.actionRefs
//        matchCondition <- source.tableMatches
//        symbolName = s"$tableName-${action.name}-${matchCondition.what.name}"
//      } yield symbolName -> SymbolicValue()
//      // TODO: Non basic action specification
//      case _ => ???
//    }).flatten.toMap
//
//    println("===")
//
//    import collection.JavaConverters._
//    val symbolicParameters = (for {
//      source <- p4.tableDeclarations.values
//      tableName = source.tableName
//    } yield source.allowedActions match {
//      case basicActionSpec: BasicActionSpecification => for {
//        actionRef <- basicActionSpec.actionRefs
//        actionId = actionRef.name
//        action: P4Action = p4.complexActions.getOrElse(actionId, p4.primitiveActions(actionId))
//        actionParameter: P4ActionParameter <- action.getParameterList.asScala
//        symbolName = s"$tableName-$actionId-${actionParameter.getParamName}"
//      } yield symbolName -> SymbolicValue()
//      // TODO: Non basic action specification
//      case _ => ???
//    }).flatten.toMap
//
//    assert((matchValues.keys.toSet intersect symbolicParameters.keys.toSet).isEmpty)
//
//    val symbolTable = (matchValues ++ symbolicParameters).mapValues(e => (e, None)) // these symbols do not get offsets
//
//    SEFLCodeContainer(
//      instructions = Map.empty,
//      symbolTable
//    )
//  }
//}
