package org.change.v2.verification

import org.change.v2.analysis.processingmodels.LocationId
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.verification.Policy.{LocMode, OverallMode, Topology}
import org.change.v2.analysis.processingmodels._


/**
 * Created by matei on 07/03/17.
 */


trait PolicyState {
  def execute (p:Instruction):PolicyState
  def state : State
  def forward (s:String) : PolicyState
  def instructionAt (s:String) : Instruction
  def history : List[LocationId]
  def copy : PolicyState
}

case class NoMapState (val state:State) extends PolicyState {
  override def execute(p:Instruction) : PolicyState = {


    p.apply(state,true) match {
      case (Nil,_) => FailedState
      case (sp :: _,_) => new NoMapState(sp)
    }
  }
  override def forward (s:String) = this

  override def instructionAt (s:String) : Instruction = null

  override def history = null

  override def copy = NoMapState(state)
}

case class MapState (location : LocationId,
                     topology:Topology,
                     links:Map[LocationId,LocationId],
                     state:State,
                     var portHistory : List[LocationId]) extends PolicyState {

  override def history = portHistory

  override def execute(p:Instruction) : PolicyState = {
    //Policy.verbose_print("Executed "+p+"\n************\n",OverallMode);
    p.apply(state,true) match {
      case (Nil,_) => FailedState
      case (sp :: _,_) => new MapState(location,topology,links,sp,portHistory)
    }
  }

  override def forward (s:String) : PolicyState = {

    if (!locationDefined(s)) throw new Exception("Location "+s+" not found")
      else Policy.verbose_print("Switching to location "+s,LocMode);
        var sp = step(s)
        sp.portHistory = s :: portHistory // add to history the current port
        println("Port history "+portHistory)
        sp // return the new state

  }
  override def copy = MapState(location,topology,links,state,portHistory)

  //def hasTopology = location != null && topology != null

  def locationDefined(l : LocationId) : Boolean = topology.contains(l) || links.contains(l)

  // this function assumes the location exists
  def instructionAt (l : LocationId) : Instruction = if (topology.contains(l)) topology(l) else topology(links(l))

  // modifies the current state by moving to the next port
  // if there is a link, it is skipped to the next input port
  def step (l : LocationId) : MapState = if (topology.contains(l)) new MapState (l,topology,links,state,portHistory)
                                          else {Policy.verbose_print("Next hop:"+links(l),OverallMode); new MapState(links(l),topology,links,state,portHistory)}



}

case object FailedState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def history = null
  override def copy = FailedState
}