package org.change.v2.analysis.memory.bitvector

import org.stringtemplate.v4.compiler.Bytecode.Instruction

import z3.scala._

/**
  * Created by dragos on 11.09.2017.
  */
case class State(history : List[String] = List[String](),
                 instructionHistory : List[Instruction] = List[Instruction]()) {
  def addInstructionToHistory(instruction: Instruction) : State = State(history = history, instructionHistory = instruction :: instructionHistory)
  def forward(port : String) : State = State(instructionHistory = instructionHistory, history = port :: history)
}

case class SubstringRef(start : Int, end : Int)

case class BVMemorySpace() {
  def width(objectRef : String) : Int = range(objectRef)._2 - range(objectRef)._1
  def range(objectRef : String) : (Int, Int) = (0, 0)
  def assign(objectRef : String) = ???
  def insert(pos : Int, w : Int) = ???
  def insertAfter(objectRef : String, w : Int) = ???
  def insertBefore(objectRef: String, w : Int) = ???
  def allocate(objectRef : String, w : Int) = ???
  def createSemantics(field : String, semantics : Map[SubstringRef, String]) = ???
}

