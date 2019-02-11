package org.change.v2.util

import java.io.PrintWriter

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.ToDot.{mkIPCG, normalize}

import scala.collection.mutable

object ToSEFL {
  def handleBlock(port : String,
                  stack : mutable.Buffer[String],
                  visited : mutable.Set[String],
                  instructions : Map[String, Instruction],
                  outputStream : PrintWriter) : Unit = {

    def handle(constrain : ConstrainFloatingExpression) : String = {
      ???
    }
    def handleCondition(instruction: Instruction) : String = {
      ???
    }
    def handleAssign(n : Either[String, Intable], f : FloatingExpression) : String = {
      ???
    }

    def visit(instruction: Instruction) : String = instruction match {
      case InstructionBlock(is) => "{\n" + is.map(visit).mkString("\n") + "}"
      case Fork(is) => is.map(visit).mkString(" ||\n")
      case AssignNamedSymbol(n, x, _) => handleAssign(Left(n), x)
      case AssignRaw(n, x, _) => handleAssign(Right(n), x)
      case If(a, b, NoOp) => "if (" + handleCondition(a) + ")\n" + visit(b)
      case If(a, b, c) => "if (" + handleCondition(a) + ")\n" + visit(b) + "\nelse\n" + visit(c)
      case NoOp => "nop"
      case c : ConstrainFloatingExpression => handle(c)
      case ConstrainNamedSymbol(x, c, _) => visit(Assume(ConstrainFloatingExpression(:@(x), c)))
      case ConstrainRaw(x, c, _) => visit(Assume(ConstrainFloatingExpression(:@(x), c)))
      case Assume(x) => "assume(" + handleCondition(x) + ")"
      case Fail(x) => s"fail($x)"
      case Forward(loc) =>
        if (!visited.contains(loc))
          stack.append(loc)
        s"forward $loc"
    }
    visited.add(port)
    if (instructions.contains(port))
      outputStream.println("block " + port + " {\n" + visit(instructions(port)) + "\n" + "}")
    else
      outputStream.println("")
  }

  def apply(instructions : Map[String, Instruction],
            startingFrom : Set[String] = Set.empty,
            outputStream : PrintWriter): Unit = {
    val stack = mutable.ListBuffer.empty[String]
    val visited = mutable.Set.empty[String]
    stack.appendAll(startingFrom)
    while (stack.nonEmpty) {
      val top = stack.remove(0)
      handleBlock(top, stack, visited, instructions, outputStream)
    }
  }
}
