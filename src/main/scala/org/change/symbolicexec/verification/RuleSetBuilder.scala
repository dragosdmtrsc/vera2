package org.change.symbolicexec.verification

import java.io.{File, FileInputStream}

import generated.reachlang.{ReachLangLexer, ReachLangParser}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.change.parser.verification.TestsParser

/**
  * Author: Radu Stoenescu
  * Don't be a stranger,  symnetic.7.radustoe@spamgourmet.com
  */
object RuleSetBuilder {

  def buildRuleSetFromFile(path: String): List[List[Rule]] = {
    val input = new FileInputStream(new File(path))
    val parserInput = new ANTLRInputStream(input)
    val lexer: ReachLangLexer = new ReachLangLexer(parserInput)
    val tokens: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: ReachLangParser = new ReachLangParser(tokens)

    TestsParser.visitRequirements(parser.requirements())
  }

}
