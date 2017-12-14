package org.change.v2.verification
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
/**
  * Created by mateipopovici on 02/08/17.
  */


class PolicyLogger(start : LocationId) {
  var paths = 0
  var history : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer(start))
  var instructionTrace : ListBuffer[ListBuffer[Instruction]] = ListBuffer(ListBuffer())
  var lastInstruction : Instruction = null

  var branchingHistoryList : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer())
  def branchingHistory = branchingHistoryList.last

  var branchingTraceList : ListBuffer[ListBuffer[Instruction]] = ListBuffer(ListBuffer())
  def branchingTrace = branchingTraceList.last

  def initFork = {branchingHistoryList += currentHistory; branchingTraceList += currentInstructionTrace}

  def endFork = {branchingHistoryList.remove(branchingHistoryList.length-1);
                  branchingTraceList.remove(branchingTraceList.length-1);
                 history.remove(history.length-1); paths -=1; instructionTrace.remove(instructionTrace.length-1);}

  def addPath = {paths+=1; history += (ListBuffer() ++ branchingHistory); instructionTrace += (ListBuffer() ++ branchingTrace)}

  def addPort (port : LocationId) = history.last += port
  def addInstruction(i : Instruction) = {instructionTrace.last += i}

  def currentPort = history.last.last
  def currentHistory = history.last
  def currentInstructionTrace = instructionTrace.last

  def getInstructionTrace = {
    //prettyPrint(instructionTrace)

    println ("+++++++++++++")
    for (i <- 0 to paths) {
      println("Branches: "+i)
      println ("Path ")
      println (history(i))
      println ("Trace ")
      println (instructionTrace(i))
    }
    println ("-------------")

  }

}


/*
class PolicyLogger(start : LocationId) {
  var paths = 0
  var history : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer(start))
  var instructionTrace : ListBuffer[ListBuffer[Instruction]] = ListBuffer(ListBuffer())
  var lastInstruction : Instruction = null

  var branchingPortList : ListBuffer[LocationId] = ListBuffer()
  def branchingPort = branchingPortList.last

  def initFork = branchingPortList += currentPort;
  def endFork = {branchingPortList.remove(branchingPortList.length-1); history.remove(history.length-1); paths -=1; instructionTrace.remove(instructionTrace.length-1);}

  def addPath = {paths+=1; history += ListBuffer(branchingPort); instructionTrace += ListBuffer()}

  def addPort (port : LocationId) = history.last += port
  def addInstruction(i : Instruction) = {instructionTrace.last += i}

  def currentPort = history.last.last


  //def getHistory = prettyPrint(history)
  /*
  def prettyPrint[T] (l : ListBuffer[ListBuffer[T]]) =
    l.fold("")(_ +"\n"+ _)
*/


  def getInstructionTrace = {
    //prettyPrint(instructionTrace)

    println ("+++++++++++++")
    for (i <- 0 to paths) {
      println("Branches: "+i)
      println ("Path ")
      println (history(i))
      println ("Trace ")
      println (instructionTrace(i))
    }
    println ("-------------")

  }

}*/
