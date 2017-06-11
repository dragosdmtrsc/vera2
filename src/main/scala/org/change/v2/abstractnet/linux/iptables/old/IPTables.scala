package org.change.v2.abstractnet.linux.iptables.old

import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Fail
import scala.collection.mutable.Stack
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:, :>=:, :|:, :&:, :<=:}


case class IPTables (namespace : String, tables : List[IPTablesTable]) {

  /**
   * Should be read: Get instructions for IpTables starting at chain
   */
  def apply(table : String, chain : String) : Instruction = {
    tables.find(s => s.name == table).get.startProcessing((chain, 0))

  }
}

case class IPTablesTable(
    parent : IPTables,
    name : String, 
    var chains : List[IPTablesChain]) {
  def startProcessing(startAt : (String, Int), 
      returnTo : Stack[(String, Int)] = Stack()) : Instruction = {
      val chain = chains.filter(s => s.name == startAt._1)(0)
      if (chain.rules.length <= startAt._2)
        startProcessing(returnTo.head, returnTo.tail)
      else
      {
        val listOf = chain.rules.drop(startAt._2)
        val defaultAction = 
        if (returnTo.isEmpty)
        {
          if (chain.default == "ACCEPT")
            NoOp
          else if (chain.default == "REJECT" || chain.default == "DROP")
            Fail("Explicit REJECT or DROP on policy for chain " + startAt._1)
          else throw new Exception("Functionality not yet implemented")
        }
        else
          startProcessing(returnTo.head, returnTo.tail)
        listOf.foldRight(defaultAction)((v, acc) => {
          val applied = v.apply(returnTo.push((startAt._1, startAt._2 + 1)))
//          If (applied._1,
//              applied._2,
//              acc)
          val matches = applied._1
          val target = applied._2
          matches.foldRight(target)((value, current) => {
            If (value, current, acc)
          })
        })
      }
  }
}

case class IPTablesChain(name : String, 
    var rules : List[IPTablesRule], 
    parent : IPTablesTable, 
    var default : String = "RETURN") {
}


case class IPTablesRule(
    var theMatches : List[IpTablesMatch], 
    var theTarget : IpTablesTarget,
    parent : IPTablesChain) {
  
  def apply(returnTo : Stack[(String, Int)]) : (List[Instruction], Instruction) = {
    // TODO : Match logic
    val matchedLogic = theMatches.map(s => s.apply()).foldRight(List[Instruction]())((v, acc) => {
      v ::: acc
    })
    if (parent.parent.chains.exists(s => s.name == theTarget.name()))
      (matchedLogic, parent.parent.startProcessing((theTarget.name, 0), returnTo))
    else if (theTarget.name() == "RETURN")
      (matchedLogic, parent.parent.startProcessing(returnTo.tail.head, returnTo.tail.tail))
    else if (theTarget.name() == "ACCEPT")
      (matchedLogic, NoOp)
    else if (theTarget.name() == "REJECT" || theTarget.name() == "DROP")
      (matchedLogic, Fail("Explicit failure in rule"))
    else
      //TODO : Match Logic 
      // TODO : Default targets logic
      (matchedLogic, theTarget.apply)
  }
}



trait IpTablesMatch {
  def apply() : List[Instruction];
}



abstract class SimpleMatch extends IpTablesMatch
{
  override def apply() : List[Instruction] = {
    NoOp :: Nil
  }
}

class ListMatch(var options : List[MatchOption]) extends IpTablesMatch {
  override def apply() : List[Instruction] = {
    options.map(s => s()).toList
  }
}
