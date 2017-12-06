package org.change.v2.analysis.processingmodels.instructions

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.Instruction

/**
  * Created by dragos on 17.10.2017.
  */
case class Save(location: String) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    (List[State](s.save(location)), Nil)
  }
}


case class Load(location: String) extends Instruction {
  override def apply(s: State, verbose: Boolean): (List[State], List[State]) = {
    val newstate = s.load(location)
    if (newstate.isEmpty)
      Fail(s"No saved location $location")(s, verbose)
    else
      (List[State](newstate.get), Nil)
  }
}