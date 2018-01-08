//package org.change.v2.p4.model.updated.program
//
//import generated.parse.p4.{P4GrammarLexer, P4GrammarParser}
//import org.antlr.v4.runtime.tree.ParseTreeWalker
//import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
//import org.change.parser.p4.P4GrammarListener
//import org.change.v2.p4.model.actions.{P4Action, P4ActionType, P4ComplexAction}
//import org.change.v2.p4.model.updated.control.ControlFunction
//import org.change.v2.p4.model.updated.header.HeaderDeclaration
//import org.change.v2.p4.model.updated.instance.{HeaderInstance}
//import org.change.v2.p4.model.updated.table.TableDeclaration
//
//case class P4Program(headerDeclarations: Map[String, HeaderDeclaration],
//                     headerOrMetadataInstances: Map[String, HeaderInstance],
//                     metadataInits: Map[String, Int],
//                     controlFunctions: Map[String, ControlFunction],
//                     tableDeclarations: Map[String, TableDeclaration],
//                     primitiveActions: Map[String, P4Action],
//                     complexActions: Map[String, P4ComplexAction]) {
//}
//
//object P4Program {
//  def buildFromGrammarListener(listener: P4GrammarListener): P4Program = {
//    import collection.JavaConverters._
//
//    // differentiate between primitive and complex actions
//    val (primitiveActions, complexActions) = listener.actionRegistrar.getDeclaredActions.asScala.partition(action => {
//      action.getActionType != P4ActionType.Complex
//    })
//
//    P4Program(
//      headerDeclarations = listener.declaredHeaders.toMap,
//      headerOrMetadataInstances = listener.headerInstances.toMap,
//      metadataInits = listener.metadataInit,
//      listener.controlFunctions.map(f => f.functionName -> f).toMap,
//      listener.tablesDeclarationsNEW.toMap,
//      primitiveActions.map(action => action.getActionName -> action).toMap,
//      complexActions.map(action => action.getActionName -> action.asInstanceOf[P4ComplexAction]).toMap
//    )
//  }
//
//  def fromP4File(p4FilePath: String): Either[String, P4Program] = {
//    val p4Input = CharStreams.fromFileName(p4FilePath)
//    val lexer = new P4GrammarLexer(p4Input)
//    val tokens = new CommonTokenStream(lexer)
//    val parser = new P4GrammarParser(tokens)
//    val tree = parser.p4_program
//    val walker = new ParseTreeWalker
//    val listener = new P4GrammarListener
//
//    walker.walk(listener, tree)
//
//    Right(buildFromGrammarListener(listener))
//  }
//}
