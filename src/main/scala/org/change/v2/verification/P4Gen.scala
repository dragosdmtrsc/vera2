package org.change.v2.verification

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.{Fork, Forward}
import org.change.v2.verification.Tester.Topo

/**
  * Created by mateipopovici on 14/12/17.
  */

trait Translatable {
  def generate : Topo
}

class P4 extends Translatable {
  def gen : Topo = ???
    /**
      * sequence of tables : SEFL Code
      *
      * a table : marks as symbolic each read headerfield (e.g. ipv4)
      *           also, allocates possible metadata
      *
      * executes a fork over all possible actions
      *
      * action execution: treat all parameters as symbolic values;
      * implement the respective modification.
      */

  override def generate = ???
}

class P4Table (name : String, reads : List[String], actions : List[P4Action]) extends Translatable  {
  override def generate = {

    (actions.map((a)=> a.generate).foldRight(Map(name -> Fork(
      actions.map((a)=> Forward(a.getName)))):Map[String,Instruction])((elm,acc)=>elm._1++acc)
      ,Map())
  }
}

class P4Action (name : String, effect : Instruction) extends Translatable  {
  def getName = name
  override def generate = (Map(name -> effect),Map())
}
