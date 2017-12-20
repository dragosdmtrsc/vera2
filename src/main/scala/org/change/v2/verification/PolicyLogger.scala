package org.change.v2.verification
import org.change.v2.analysis.processingmodels.instructions.{Fork, InstructionBlock, NoOp, Fail}
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.verification.Tester.Topo

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
/**
  * Created by mateipopovici on 02/08/17.
  */


class PolicyLogger(start : LocationId) {
  def unsatifState(p: Instruction) = if (newBlock(codePort)) {
            codeToAdd += Fail("Unsatisfstate");
            code += (currentPort -> InstructionBlock(codeToAdd));
            currentCode = ListBuffer();
            codeToAdd = currentCode;
  }

  def failedState(p: Instruction) = if (newBlock(codePort)) {
    codeToAdd += Fail("FailedState");
    code += (currentPort -> InstructionBlock(codeToAdd));
    currentCode = ListBuffer();
    codeToAdd = currentCode;
  }


  var paths = 0
  var history : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer(start))
  var instructionTrace : ListBuffer[ListBuffer[Instruction]] = ListBuffer(ListBuffer())
  var lastInstruction : Instruction = null


  // set of variables for constructing a partial topology
  var forkStack : ListBuffer[Fork] = ListBuffer()
  var (code,links) : Topo = (Map(),Map())
  var currentCode : ListBuffer[Instruction] = ListBuffer(NoOp)
  var codeToAdd : ListBuffer[Instruction] = currentCode;
  var codePort : LocationId = start;


  // prepares a new codeBlock to write to

  def newBlock(port : LocationId) : Boolean = !(code.contains(port))

  def newCodeBlock(port : LocationId) = {
    if (newBlock(port)) {
      if (codeToAdd.length == 1)
        code += (codePort -> codeToAdd(0));
      else
        code += (codePort -> InstructionBlock(codeToAdd))
    }
    //println("Creating block !!",InstructionBlock(codeToAdd));
    currentCode = ListBuffer();
    codeToAdd = currentCode;
    codePort = port;
  }

  def addLink(from: LocationId, to: LocationId) = links += (from -> to)

  def pathEnded() = if (newBlock(codePort)) {code += (currentPort -> InstructionBlock(codeToAdd)); currentCode = ListBuffer(); codeToAdd = currentCode;}

  //end...



  var branchingHistoryList : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer())
  def branchingHistory = branchingHistoryList.last

  var branchingTraceList : ListBuffer[ListBuffer[Instruction]] = ListBuffer(ListBuffer())
  def branchingTrace = branchingTraceList.last

  def initFork = {branchingHistoryList += currentHistory; branchingTraceList += currentInstructionTrace;
                  // for the partial topology
                  currentCode = ListBuffer() //reset the current code pointer
                  var forkList :ListBuffer[Instruction] = ListBuffer(); forkList += InstructionBlock(currentCode); // create the fork list
                  var i = Fork( forkList );
                  codeToAdd += Fork(i);
                  forkStack += i;
                  }

  def endFork = { branchingHistoryList.remove(branchingHistoryList.length-1);
                  branchingTraceList.remove(branchingTraceList.length-1);
                  history.remove(history.length-1); paths -=1; instructionTrace.remove(instructionTrace.length-1);

                  // for the partial topology
                  forkStack.remove(forkStack.length-1);
                  //todo no need to improve, we are unfolding Fork(l);....
                  codeToAdd = ListBuffer()
                  currentCode = ListBuffer()

                }

  def addPath = {paths+=1; history += (ListBuffer() ++ branchingHistory); instructionTrace += (ListBuffer() ++ branchingTrace);

                  // for the partial topology
                  // must create a suitable "current code"; "codeToAdd" must be empty, since we are modifying an existing codeblock
                  codeToAdd = ListBuffer()
                  var f : Instruction = forkStack.last;
                  currentCode = ListBuffer()
                  f match {case Fork(l:ListBuffer[Instruction]) => l += InstructionBlock(currentCode);}

                }

  def addPort (port : LocationId) = history.last += port
  def addInstruction(i : Instruction) = {instructionTrace.last += i; currentCode += i }

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
