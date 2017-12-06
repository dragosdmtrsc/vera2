package org.change.parser.clickfile

import java.io.{File, FileInputStream, InputStream}

import generated.click.{ClickLexer, ClickParser}
import org.antlr.v4.runtime.tree.{ParseTree, ParseTreeWalker}
import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.change.v2.abstractnet.generic.{NetworkConfig, NetworkConfigBuilder}

/**
  * radu
  * 3/5/14
  */
object ClickToAbstractNetwork {

  /**
    * Builds an abstract network representation out of a Click.
    *
    * @param input Click
    * @return Resulting Abstract Network.
    */
  def buildConfig(input: InputStream, configId: String, prefixedElements: Boolean): NetworkConfig = {
    val parserInput = new ANTLRInputStream(input)
    val lexer: ClickLexer = new ClickLexer(parserInput)
    val tokens: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: ClickParser = new ClickParser(tokens)

    val tree: ParseTree = parser.configFile

    val walker = new ParseTreeWalker
    val newConfig = new NetworkConfigBuilder(Some(configId))
    walker.walk(newConfig, tree)
    if (!prefixedElements) newConfig.buildNetworkConfig() else newConfig.buildNetworkConfigWithPrefixes().get
  }

  def buildConfig(fileInput: File, prefixedElements: Boolean): NetworkConfig =
    buildConfig(new FileInputStream(fileInput), fileInput.getName.stripSuffix(".click"), prefixedElements)

  def buildConfig(path: String, prefixedElements: Boolean = false): NetworkConfig =
    buildConfig(new File(path), prefixedElements)
}
