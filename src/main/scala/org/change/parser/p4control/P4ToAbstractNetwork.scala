package org.change.parser.p4control

import java.io.{File, FileInputStream, InputStream}

import generated.p4control.{P4GrammarLexer, P4GrammarParser}
import generated.parse.p4.{P4GrammarLexer, P4GrammarParser}
import org.change.v2.abstractnet.generic.NetworkConfigBuilder
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.antlr.v4.runtime.tree.{ParseTree, ParseTreeWalker}
import org.change.v2.abstractnet.generic.{NetworkConfig, NetworkConfigBuilder}

/**
 * Costin Raiciu
 * 20/2/17
 */
object P4ToAbstractNetwork {

  /**
   * Builds an element out of a P4 control function definition.
   * @param input: P4 program
   * @return Resulting Abstract Network.
   */
  def buildConfig(input: InputStream, configId: String): NetworkConfig = {
    val parserInput = new ANTLRInputStream(input)
    val lexer: P4GrammarLexer = new P4GrammarLexer(parserInput)
    val tokens: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: P4GrammarParser = new P4GrammarParser(tokens)

    val tree: ParseTree = parser.p4_program

    val walker = new ParseTreeWalker
    val newConfig = new P4NetworkConfigBuilder(Some(configId))
    walker.walk(newConfig, tree)
    newConfig.buildNetworkConfig()
  }
}
