package org.change.v2.p4.model.updated.sefl_compiler

import org.change.v2.p4.model.updated.program.P4Program
import org.change.v2.p4.model.updated.sefl_compiler.sefl_code.{EmptySEFLProgram, SEFLCode}
import org.change.v2.p4.model.updated.table.{BasicActionSpecification, TableDeclaration}

object SymbolicTableCompiler extends SEFLCompiler[P4Program] {

  override def compile(p4: P4Program): SEFLCode = {
    for {
      source <- p4.tableDeclarations.values
      tableName = source.tableName
    } yield source.allowedActions match {
      case basicActionSpec: BasicActionSpecification => for {
        action <- basicActionSpec.actionRefs
        matchCondition <- source.tableMatches
        symbolName = s"$tableName-${action.name}-${matchCondition.what.name}"
      } println(symbolName)
    }

    EmptySEFLProgram
  }
}
