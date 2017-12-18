package org.change.parser.p4

import generated.parse.p4.{P4GrammarLexer, P4GrammarParser}
import org.antlr.v4.runtime.{CharStreams, CommonTokenStream}
import org.antlr.v4.runtime.tree.ParseTreeWalker

object P4ParserRunner {

  def parse(p4File: String): P4GrammarListener = {
    val p4Input = CharStreams.fromFileName(p4File)
    val lexer = new P4GrammarLexer(p4Input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new P4GrammarParser(tokens)
    val tree = parser.p4_program()
    val walker = new ParseTreeWalker
    val listener = new P4GrammarListener()
    walker.walk(listener,tree)
    listener
  }

}
