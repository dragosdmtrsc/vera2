package org.change.v2.verification

import org.change.v2.analysis.executor.AbstractInstructionExecutor
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.analysis.processingmodels.LocationId
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
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
  //def forward (s:String) : PolicyState
  def instructionAt (s:String) : Instruction
  def copy : PolicyState
  //def nextHop (l : LocationId): LocationId

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
  //override def forward (s:String) = this
  override def instructionAt (s:String) : Instruction = null
  override def copy = NoMapState(state)
  //override def nextHop(l : LocationId) = null

}

case class MapState (topology:Topology,
                     links:Map[LocationId,LocationId],
                     state:State,
                     exe : AbstractInstructionExecutor) extends PolicyState {

  override def execute(p:Instruction, logger : PolicyLogger) : (PolicyState,PolicyLogger) = {
    //Policy.verbose_print("Executed "+p+"\n************\n",OverallMode);
    //println("Executed "+p+"\n************\n");
    logger.addInstruction(p)

    //inPort has been already logged
    //this function will traverse an arbitrary number of links
    def logLinks (inPort : LocationId, logger: PolicyLogger) : PolicyLogger = {
      if (topology.contains(inPort)){
        logger // found a port with code, stop
      }
      else {
        if (links.contains(inPort)){
          logger.addInPort(links(inPort))
          logLinks(links(inPort),logger)
        }
        else {
          logger.addInPort("Stuck");logger
        } //throw new Exception("Logger: cannot find a next hop for port "+inPort)
      }
    }

    p match {
      case Forward(loc) => //semantics for Forward
        //logger.addInstruction(Forward(loc))
        logger.addOutPort(loc)

        //must execute the Forward instruction
        (exe.execute(Assign("CurrentPort",ConstantStringValue(loc)),state,true) match {
          case (sp :: _, _) => println("Executing Fwd("+loc+")"); new MapState(topology,links,sp,exe)
          case _ => throw new Exception ("Executing Forward("+loc+") failed")
        }


        ,logLinks(loc,logger))

      case _ => // standard symbolic execution
        //logger.addInstruction(p);
        (exe.execute(p,state,true) match {
          //case (Nil,sp :: _) => if (stuck(sp)) {UnsatisfState} else {FailedState}
          case (Nil, Nil) => /*println("Unsatif state at "+p + " port "+logger.currentPort);*/ logger.unsatifState(p); FailedState//UnsatisfState
          case (Nil, sp :: _) => /*println("Failed State at "+p + " port "+logger.currentPort);*/ logger.failedState(sp,p); UnsatisfState//FailedState
          case (sp :: _,_) => new MapState(topology,links,sp,exe)
        }, logger)
    }
  }

  override def execute (p:Instruction) : PolicyState  = {

    // hack for Exists

    exe.execute(p,state,true) match {
      case (Nil,Nil) => FailedState//UnsatisfState
      case (Nil, sp :: _) => println("Unsatisf state ",sp.errorCause); UnsatisfState //FailedState
      case (sp :: _,_) => new NoMapState(sp) //at least one successful state
    }
  }

  /*
  override def forward (s:String) : PolicyState = {

    if (!locationDefined(s)) throw new Exception("Location "+s+" not found")
      else Policy.verbose_print("Switching to location "+s,LocMode);
        var sp = step(s)
        sp // return the new state

  }*/


  override def copy = MapState(topology,links,state,exe)

  //def hasTopology = location != null && topology != null

  def locationDefined(l : LocationId) : Boolean = topology.contains(l) || links.contains(l)

  // this function assumes the location exists, and will skip links
  def instructionAt (l : LocationId) : Instruction = {
    if (topology.contains(l)) topology(l)
    else
      if (links.contains(l))
          instructionAt(links(l))
      else InstructionBlock(Fail("No new location"))  // execution stops here.  //throw new Exception ("The network does not have a link from "+l)
  }

  //override def nextHop(l : LocationId) : LocationId = links(l)

  // modifies the current state by moving to the next port
  // if there is a link, it is skipped to the next input port
  /*
  def step (l : LocationId) : MapState = if (topology.contains(l)) new MapState (l,topology,links,state,exe)
                                          else new MapState(links(l),topology,links,state,exe)
  */
}

case object FailedLocationState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  //override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = FailedState
  //override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}


case object FailedState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  //override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = FailedState
  //override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}

case object UnsatisfState extends PolicyState {
  override def execute (p:Instruction) : PolicyState = null
  override def state: State = null
  //override def forward (s:String) = null
  override def instructionAt (s:String) : Instruction = null
  override def copy = UnsatisfState
  //override def nextHop(l : LocationId) = null

  override def execute(p: Instruction, l: PolicyLogger): (PolicyState, PolicyLogger) = null
}