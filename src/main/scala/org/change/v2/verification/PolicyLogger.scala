package org.change.v2.verification
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.{Fail, Fork, InstructionBlock, NoOp}
import org.change.v2.analysis.processingmodels.{Instruction, LocationId}
import org.change.v2.verification.Tester.Topo

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
/**
  * Created by mateipopovici on 02/08/17.
  */


class PolicyLogger(start : LocationId) {

  // prepares a new codeBlock to write to

  def newBlock() : Boolean = !(code.contains(currentPort))

  // Todo:
  def resetCodePointers = {currentCode = ListBuffer(); codeToAdd = currentCode;}

  def createCodeblock = {
    if (newBlock) {
      if (codeToAdd.length == 1)
        code += (currentPort -> codeToAdd(0));
      else
        code += (currentPort -> InstructionBlock(codeToAdd))
    }
    //println("New Codeblock "+codeToAdd)

    resetCodePointers
  }

  // marks the termination of a code block
  def addOutPort(port : LocationId) = {
    //a codeblock has ended
    createCodeblock
    history.last += port
    //println("History addoutport ",history)

  }

  //creates a link
  def addInPort(port : LocationId) = {
    links += (currentPort -> port)
    history.last += port
    //println("History addinport ",history)
  }


  def unsatifState(p: Instruction) = {
    currentCode += Fail("Unsatifstate");
    //createCodeblock
    if (locationChanged) createCodeblock
  }

  def failedState(s:State, p: Instruction) = {
    //should not be necessary, as this is triggered by a Fail instruction which is already documented
    currentCode += Fail("FailedState: "+s.errorCause);
    //createCodeblock
    if (locationChanged) createCodeblock
  }


  var history : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer(start))


  // set of variables for constructing a partial topology
  var forkStack : ListBuffer[Fork] = ListBuffer()
  var (code,links) : Topo = (Map(),Map())
  var currentCode : ListBuffer[Instruction] = ListBuffer(NoOp)
  var codeToAdd : ListBuffer[Instruction] = currentCode;
  var codeLoc : ListBuffer[LocationId] = ListBuffer(start)



  def InstructionBlockEnded() = {//Todo
    if (locationChanged && newBlock()) createCodeblock
    }

  //end...

  def initFork = {
    history += ListBuffer(currentPort)
    //println("History init fork ",history)
    //println("Beginning of init fork: "+codeToAdd);

    // for the partial topology
    var crt:ListBuffer[Instruction] = ListBuffer() //reset the current code pointer
    var forkList :ListBuffer[Instruction] = ListBuffer(InstructionBlock(crt));  // create the fork list
    var i = Fork( forkList );
    currentCode += i;
    currentCode = crt;
    forkStack += i;
    codeLoc += currentPort; //we are now writing instructions in a Fork at the current location


    //println("End of init fork"+codeToAdd);
  }

  def addPath = {
    history.remove(history.length-1);
    history += ListBuffer(currentPort)
    //println("History addpath ",history, "current path ",codeToAdd)

    // for the partial topology
    // must create a suitable "current code"; "codeToAdd" must be empty, since we are modifying an existing codeblock
    //codeToAdd = ListBuffer() // the instructions preceding the fork have been added to the topology

    var f : Instruction = forkStack.last; // get the last Fork that has been executed

    //println("Before : "+codeToAdd+"\n.........\n")

    currentCode = ListBuffer()

    f match {case Fork(l:ListBuffer[Instruction]) => l += InstructionBlock(currentCode);} //add a new path to it
    //println("After : "+codeToAdd+"\n.........\n")
    codeLoc.remove(codeLoc.length-1);    //update the location at which we are writing
    codeLoc += currentPort;
  }

  def endFork = {
    history.remove(history.length-1);
    //println("History endfork ",history)

    // for the partial topology
    forkStack.remove(forkStack.length-1);

    codeLoc.remove(codeLoc.length-1);
    //todo no need to improve, we are unfolding Fork(l);....
    //resetCodePointers

    if (locationChanged && newBlock()) createCodeblock

  }
  def locationChanged : Boolean = codeLoc.last == currentPort


  //def addPort (port : LocationId) = history.last += port
  def addInstruction(i : Instruction) = {
    currentCode += i; //println("Instruction "+i+"\n*********\n added to \n"+codeToAdd+"\n-------\n")
  }

  def currentPort = history.last.last


}




/*

class PolicyLogger(start : LocationId) {

  // prepares a new codeBlock to write to

  def newBlock() : Boolean = !(code.contains(currentPort))
  def resetCodePointers = {currentCode = ListBuffer(); codeToAdd = currentCode;}


  def createCodeblock = {
    if (newBlock) {
      if (codeToAdd.length == 1)
        code += (currentPort -> codeToAdd(0));
      else
        code += (currentPort -> InstructionBlock(codeToAdd))
    }
    resetCodePointers
  }

  // marks the termination of a code block
  def addOutPort(port : LocationId) = {
    //a codeblock has ended
    createCodeblock
    history.last += port
    println("History addoutport ",history)

  }

  //creates a link
  def addInPort(port : LocationId) = {
    links += (currentPort -> port)
    history.last += port
    println("History addinport ",history)
  }


  def unsatifState(p: Instruction) = {
    currentCode += Fail("Unsatifstate");
    createCodeblock
  }

  def failedState(s:State, p: Instruction) = {
    //should not be necessary, as this is triggered by a Fail instruction which is already documented
    currentCode += Fail("FailedState: "+s.errorCause);
    createCodeblock
  }


  var history : ListBuffer[ListBuffer[LocationId]] = ListBuffer(ListBuffer(start))


  // set of variables for constructing a partial topology
  var forkStack : ListBuffer[Fork] = ListBuffer()
  var (code,links) : Topo = (Map(),Map())
  var currentCode : ListBuffer[Instruction] = ListBuffer(NoOp)
  var codeToAdd : ListBuffer[Instruction] = currentCode;



  def pathEnded() = newBlock()

  //end...

  def initFork = {
    history += ListBuffer(currentPort)
    println("History init fork ",history)

    // for the partial topology
    currentCode = ListBuffer() //reset the current code pointer
    var forkList :ListBuffer[Instruction] = ListBuffer(InstructionBlock(currentCode));  // create the fork list
    var i = Fork( forkList );
    codeToAdd += i;
    forkStack += i;
  }

  def addPath = {
    history.remove(history.length-1);
    history += ListBuffer(currentPort)
    println("History addpath ",history, "current path ",currentCode)

    // for the partial topology
    // must create a suitable "current code"; "codeToAdd" must be empty, since we are modifying an existing codeblock
    //codeToAdd = ListBuffer() // the instructions preceding the fork have been added to the topology

    var f : Instruction = forkStack.last; // get the last Fork that has been executed
    currentCode = ListBuffer()
    f match {case Fork(l:ListBuffer[Instruction]) => l += InstructionBlock(currentCode);} //add a new path to it
    //println("Current fork: "+f+"\n.........\n")
  }

  def endFork = {
    history.remove(history.length-1);
    println("History endfork ",history)

    // for the partial topology
    forkStack.remove(forkStack.length-1);
    //todo no need to improve, we are unfolding Fork(l);....
    resetCodePointers

  }



  //def addPort (port : LocationId) = history.last += port
  def addInstruction(i : Instruction) = { currentCode += i; println("Instruction "+i+"\n*********\n added to \n"+forkStack+"\n-------\n") }

  def currentPort = history.last.last


}*/
