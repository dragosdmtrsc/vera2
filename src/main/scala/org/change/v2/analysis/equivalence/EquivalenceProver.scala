package org.change.v2.analysis.equivalence

import org.change.v2.analysis.memory.State
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.expression.concrete.nonprimitive.{ :@ => :@ }
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.:==:
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import z3.scala.Z3Context
import org.change.v2.analysis.z3.Z3Util
import z3.scala.Z3Pattern
import z3.scala.Z3Sort
import z3.scala.Z3Symbol



object Z3Test {
  def main(argv : Array[String]) {
    val z3 = new Z3Context
    val symbol = z3.mkIntSymbol(1)
    val constant = z3.mkIntConst("a")
    val slv = z3.mkSolver()
    
    z3.mkForAll(1, List[Z3Pattern](), 
        List[(Z3Symbol, Z3Sort)]((symbol, z3.mkIntSort())), 
        z3.mkBound(1, z3.mkIntSort())    
    )
    
  }
  
}

class EquivalenceProver(portMap : (String, String) => Boolean, tags : List[Either[Intable, String]]) { 
  
  def exists(left : State, right : List[State]) : (Boolean, String) = {
    val z3 = Z3Util.z3Context
    
    val validPorts = right.filter { x => portMap(left.history.head, x.history.head) }
    if (validPorts.isEmpty)
      (false, "No Output Equivalent")
    else
      (validPorts.exists { x => areEquivalent(left, x)._1 }, "No memory equivalent")
  }
  
  def outcomesEquivalent(left : List[State], right : List[State]) : List[(Boolean, String)]  = {
    left.map { x => exists(x, right) }
  }
  
  
   def areEquivalent(left : State, right : State) : (Boolean, String) = {
    if (!portMap(left.history.head, right.history.head))
      (false, "Place")
    else
    {
      val errCause = "Memory"
      val cts = InstructionBlock(tags.map(_ match {
          case Left(t) => Constrain(s"packetRight.${t(right)}", :==:(:@(t)))
          case Right(t) => Constrain(s"packetRight.${t}", :==:(:@(t)))
        })
      )
      val assigns = right.memory.symbols.foldLeft(left.memory) { (acc2, x) => {
          val vvv = x._2
          val newName = s"packetRight.${x._1}"
          moveSymbols(acc2, vvv, newName)
        }
      }
      val newMem = right.memory.rawObjects.foldLeft(assigns)((acc2, x) => {
        val vvv = x._2
        val newName = s"packetRight.${x._1}"
        moveSymbols(acc2, vvv, newName)
      })
      val stateLeftFinal = left.copy(memory = newMem)
      if (stateLeftFinal.memory.isZ3Valid)
      {
        (true, "")
      }
      else
      {
        (false, errCause)
      }
    }
  }
  
  def moveSymbols(acc2: MemorySpace, vvv: MemoryObject, newName: String) = {
    val cts = vvv.value.map { y => {
        val cts = y.cts
        val expr = y.e
        val Some(mem) = acc2.Allocate(newName)
        val Some(evenNewer) = mem.Assign(newName, expr)
        cts.foldLeft(evenNewer)((acc, z) => {
            val Some(newMem) = acc.addConstraint(newName, z, true)
            newMem
          }
        )
      }
    }
    cts match {
      case Some(m) => m
      case None => acc2
    }
  }
}