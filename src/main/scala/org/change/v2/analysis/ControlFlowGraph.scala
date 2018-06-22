package org.change.v2.analysis

import org.change.parser.p4.parser.NewParserStrategy
import org.change.parser.p4.parser.StateExpander.{DeparserInstruction, ParsePacket}
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.networkproc._
import org.change.v2.analysis.processingmodels.{Instruction, Let, SuperFork, Unfail}

import scala.annotation.tailrec

class ControlFlowGraph(name : String, program : Map[String, Instruction]) {
  def crawl(from : String, instruction: Instruction) : List[String] = instruction match {
    case Unfail(instruction) =>  crawl(from, instruction)
    case Fork(forkBlocks) => forkBlocks.flatMap(crawl(from, _)).toList
    case InstructionBlock(instructions) => instructions.flatMap(crawl(from, _)).toList
    case Forward(place) => place :: Nil
    case If(testInstr, thenWhat, elseWhat) => crawl(from, thenWhat) ++ crawl(from, elseWhat)
    case Let(string, instruction) => crawl(from, instruction)
    case Call(place) => place :: Nil
    case t : Translatable => crawl(name, t.generateInstruction())
    case _ => Nil
  }

  val marks = scala.collection.mutable.Map[String, Int]()
  val sorted = scala.collection.mutable.ListBuffer[String]()
  val levels = scala.collection.mutable.Map[String, Int]()

  def visit(s : String, parent : String = "") : Unit = {
    if (marks.contains(s)) {
      if (marks(s) == 1) {
        System.err.println(s"back to $s $parent")
      }
    } else {
      marks.put(s, 1)
      if (program.contains(s)) {
        for (n <- crawl(s, program(s)).filter(r => {
          !(s.startsWith(s"$name.table.") && r == s"$name.buffer.in")
        })) {
          visit(n, s)
        }
      }
      marks.put(s, 2)
      System.err.println("reached " + s)
      sorted.prepend(s)
    }
  }

  def topoSort(startFrom : Iterable[String]) : Unit = {
    for (s <- startFrom) {
      if (!marks.contains(s))
        visit(s)
    }
    for (s <- sorted.zipWithIndex) {
      levels.put(s._1, s._2)
    }
  }

//  L ← Empty list that will contain the sorted nodes
//  while there are unmarked nodes do
//  select an unmarked node n
//  visit(n)
//  function visit(node n)
//  if n has a permanent mark then return
//  if n has a temporary mark then stop (not a DAG)
//  mark n temporarily
//  for each node m with an edge from n to m do
//  visit(m)
//  mark n permanently
//  add n to head of L
}
