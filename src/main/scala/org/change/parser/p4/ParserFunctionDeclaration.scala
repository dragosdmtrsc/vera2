package org.change.parser.p4


/**
  * A small gift from radu to symnetic.
  */
case class ParserFunctionDeclaration(
                                      name: String,
                                      instructions: Seq[ParserFunctionStatement]
                                      //,returnInstruction: AnyVal
                                    )

sealed trait ParserFunctionStatement

case class ExtractHeader(what: HeaderInstance) extends ParserFunctionStatement
case class SetFunction(field : String, metaExpr : String) extends ParserFunctionStatement
