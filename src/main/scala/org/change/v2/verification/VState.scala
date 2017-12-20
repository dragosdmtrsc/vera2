package org.change.v2.verification

import org.change.v2.analysis.processingmodels.LocationId
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Forward, NoOp}
import org.change.v2.verification.Policy.{LocMode, OverallMode, Topology}
import org.change.v2.analysis.processingmodels._


/**
 * Created by matei on 07/03/17.
 */


trait PolicyState {
  def instructionHistory = state.instructionHistory

  def execute (p:Instruction,l:PolicyLogger):(PolicyState,PolicyLogger)
  def execute (p:Instruction):PolicyState
  def state : State
  def forward (s:String) : PolicyState
  def instructionAt (s:String) : Instruction
  def copy : PolicyState
  def nextHop (l : LocationId): LocationId

  def stuck (s:State) : Boolean = s.errorCause match {case Some(str) => !(str.startsWith("Memory object") || str.startsWith("Symbol")) case _ => false}
}
// this has to be re-checked
case class NoMapState (val state:State) extends PolicyState {

  override def execute(p:Instruction,l:PolicyLogger): (PolicyState,PolicyLogger) = {
    (p.apply(state,true) match {
      case (Nil,sp :: _) => if (stuck(sp)) UnsatisfState else FailedState
      case (sp :: _,_) => new NoMapState(sp)
    },l)
  }
  override def execute (p:Instruction) : PolicyState  = {
    p.apply(state,true) match {
      case (Nil,sp :: _) => if (stuck(sp)) UnsatisfState else FailedState
      case (sp :: _,_) => new NoMapState(sp)
    }
  }
  override def forward (s:String) = this
  override def instructionAt (s:String) : Instruction = null
  override def copy = NoMapState(state)
  override def nextHop(l : LocationId) = null

}

case class MapState (location : LocationId,
                     topology:Topology,
                     links:Map[LocationId,LocationId],
                     state:State) extends PolicyState {

  override def execute(p:Instruction, logger : PolicyLogger) : (PolicyState,PolicyLogger) = {
    //Policy.verbose_print("Executed "+p+"\n************\n",OverallMode);
    //logger.addInstruction(p)

    p match {
      case Forward(loc) => //semantics for Forward
        logger.addPort(loc)
        logger.addInstruction(Forward(loc))
        logger.addPort(nextHop(loc))
        logger.addLink(loc,nextHop(loc))
        logger.newCodeBlock(nextHop(loc))
        (forward(loc),logger)

      case _ => // standard symbolic execution
        logger.addInstruction(p);
        (p.apply(state,true) match {
          //case (Nil,sp :: _) => if (stuck(sp)) {UnsatisfState} else {FailedState}
          case (Nil, Nil) => println("Unsatif state at "+p ); logger.unsatifState(p); UnsatisfState
          case (Nil, sp :: _) => println("Failed State at"+p ); logger.failedState(p); FailedState
          case (sp :: _,_) => new MapState(location,topology,links,sp)
        }, logger)

    }
  }
  override def execute (p:Instruction) : PolicyState  = {
    p.apply(state,true) match {
      case (Nil,sp :: _) => if (stuck(sp)) UnsatisfState else FailedState
      case (sp :: _,_) => new NoMapState(sp)
    }
  }

  override def forward (s:String) : PolicyState = {

    if (!locationDefined(s)) throw new Exception("Location "+s+" not found")
      else Policy.verbose_print("Switching to location "+s,LocMode);
        var sp = step(s)
        sp // return the new state

  }
  override def copy = MapState(location,topology,links,state)

  //def hasTopology = location != null && topology != null

  def locationDefined(l : LocationId) : Boolean = topology.contains(l) || links.contains(l)

  // this function assumes the location exists
  def instructionAt (l : LocationId) : Instruction = if (topology.contains(l)) topology(l) else topology(links(l))

  override def nextHop(l : LocationId) : LocationId = links(l)

  // modifies the current state by moving to the next port
  // if there is a link, it is skipped to the next input port
  def step (l : LocationId) : MapState = if (topology.contains(l)) new MapState (l,topology,links,state)
                                          else {Policy.verbose_print("Next hop:"+links(l),OverallMode); new MapState(links(l),topology,links,state)}

}

case object FailedLocationState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = FailedState
  override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}


case object FailedState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = FailedState
  override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}

case object UnsatisfState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = UnsatisfState
  override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}