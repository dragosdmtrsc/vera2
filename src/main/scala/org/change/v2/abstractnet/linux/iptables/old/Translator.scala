package org.change.v2.abstractnet.linux.iptables.old

import generated.iptables_grammar.IptablesParser.TableContext
import scala.collection.JavaConversions._
import org.antlr.v4.runtime.tree.ParseTreeWalker


object Translator {
  def parseTable(ctx : TableContext, 
      withName : String,
      parent : IPTables,
      namespace : String) : IPTablesTable = {
    // first things first => Add all the default chains
    var table = IPTablesTable(parent, withName, Nil)
    // prerouting
    var chains = IPTablesChain("PREROUTING", Nil, table) :: Nil
    chains = IPTablesChain("INPUT", Nil, table) :: chains
    chains = IPTablesChain("FORWARD", Nil, table) :: chains
    chains = IPTablesChain("OUTPUT", Nil, table) :: chains
    chains  = IPTablesChain("POSTROUTING" , Nil, table) :: chains
    
    var newOnes = ctx.chain()
    val theChains = newOnes.map(s => IPTablesChain(s.chainname().NAME().getText, Nil, table))
    for (chain <- theChains) 
    {
      chains = chain :: chains
    }
    
    var policies = ctx.policy()
    for (policy <- policies) 
    {
      val theChain = policy.chainname().NAME().getText
      val theActualChain = chains.find(s => s.name == theChain).get
      theActualChain.default = policy.targetName().getText
    }
    val ptw = new ParseTreeWalker()
    ptw.walk(new RulesListener(chains), ctx)
    
    table
  }
}