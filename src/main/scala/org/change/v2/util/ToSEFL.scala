package org.change.v2.util

import java.io.PrintWriter

import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete._
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{Intable, Tag, TagExp}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.ToDot.{mkIPCG, normalize}
import org.change.v2.analysis.memory.TagExp.IntImprovements
import scala.collection.mutable

object ToSEFL {
  def handleBlock(port : String,
                  stack : mutable.Buffer[String],
                  visited : mutable.Set[String],
                  instructions : Map[String, Instruction],
                  outputStream : PrintWriter) : Unit = {
    def handleIntable(a : Intable) : String = a match {
      case Tag(name) => "tag(" + name + ")"
      case x : IntImprovements => x.value.toString
      case TagExp(plusTags, minusTags, rest) => plusTags.map(handleIntable).mkString("+") +
        (if (minusTags.nonEmpty)
          " - " + minusTags.map(handleIntable).mkString("-")
        else "") + " + " + rest.toString
    }

    def handleFexp(floatingExpression: FloatingExpression) : String = floatingExpression match {
      case s : SymbolicValue => s"symbolic(${s.canonicalName()})"
      case cv : ConstantValue => cv.canonical
      case :<<:(left, right) => handleFexp(left) + " << " + handleFexp(right)
      case :!:(left) => "~(" + handleFexp(left) + ")"
      case Symbol(id) => id
      case :||:(left, right) => handleFexp(left) + " | " + handleFexp(right)
      case :^:(left, right) => handleFexp(left) + " ^ " + handleFexp(right)
      case :&&:(left, right) => handleFexp(left) + " & " + handleFexp(right)
      case :+:(left, right) =>  handleFexp(left) + " + " + handleFexp(right)
      case Havoc(prefix) => s"havoc($prefix)"
      case Address(a) => s"packet[${handleIntable(a)}]"
      case :-:(left, right) => handleFexp(left) + " - " + handleFexp(right)
      case Take(lv, from, width) => lv match {
        case Left(id) => id + s"[$from:${from + width}]"
        case Right(intable) => s"packet[${handleIntable(intable)}+$from:${handleIntable(intable)}+${from + width}]"
      }
      case ConstantBValue(value, size) => value
      case ConstantStringValue(value) => "string(" + value.toString + ")"
    }

    def handle(constrain : ConstrainFloatingExpression) : String = constrain.dc match {
      case :|:(a, b) => "(" + handle(ConstrainFloatingExpression(constrain.floatingExpression, a)) + " || "
        handle(ConstrainFloatingExpression(constrain.floatingExpression, b)) + ")"
      case :&:(a, b) => "(" +  handle(ConstrainFloatingExpression(constrain.floatingExpression, a)) + " && "
        handle(ConstrainFloatingExpression(constrain.floatingExpression, b)) + ")"
      case :==:(b) => handleFexp(constrain.floatingExpression) +  " == " + handleFexp(b)
      case :<=:(b) => handleFexp(constrain.floatingExpression) +  " <= " + handleFexp(b)
      case :>=:(b) => handleFexp(constrain.floatingExpression) +  " >= " + handleFexp(b)
      case :>:(b) => handleFexp(constrain.floatingExpression) +  " > " + handleFexp(b)
      case :<:(b) => handleFexp(constrain.floatingExpression) +  " < " + handleFexp(b)
      case :~:(ct) => "!(" + handle(ConstrainFloatingExpression(constrain.floatingExpression, ct)) + ")"
    }
    def handleCondition(instruction: Instruction) : String = instruction match {
      case InstructionBlock(cds) => cds.map(handleCondition).mkString(" && ")
      case Fork(cds) => cds.map(handleCondition).mkString(" || ")
      case ConstrainNamedSymbol(x, y, _) => handle(ConstrainFloatingExpression(:@(x), y))
      case ConstrainRaw(x, y, _) => handle(ConstrainFloatingExpression(:@(x), y))
      case c : ConstrainFloatingExpression => handle(c)
    }
    def handleAssign(n : Either[String, Intable], f : FloatingExpression) : String = n match {
      case Left(id) => handleFexp(:@(id)) + " = " + handleFexp(f)
      case Right(a) => handleFexp(:@(a)) + " = " + handleFexp(f)
    }

    def visit(instruction: Instruction) : String = instruction match {
      case CreateTag(t, v) => "create_tag(" + t + "," + handleIntable(v) + ")"
      case DestroyTag(t) => "destroy_tag(" + t + ")"
      case InstructionBlock(is) => "{\n" + is.map(visit).mkString("\n") + "\n}"
      case Fork(is) => "fork(" + is.map(visit).mkString(", ") + ")"
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
      case AllocateRaw(x, u) => "allocate(" + handleIntable(x) + s", $u)"
      case AllocateSymbol(x, u) => "allocate(" + x + s",$u)"
      case DeallocateRaw(x, u) => "deallocate(" + handleIntable(x) + ")"
      case DeallocateNamedSymbol(x) => "deallocate(" + x + ")"
      case Forward(loc) =>
        if (!visited.contains(loc))
          stack.append(loc)
        s"forward $loc"
    }
    visited.add(port)
    if (instructions.contains(port)) {
      val noparen = instructions(port).isInstanceOf[InstructionBlock]

      outputStream.println("block " + port + (if (!noparen) " {\n" else " ") + visit(instructions(port)) +
        "\n" + (if (!noparen) "}" else ""))
    } else
      outputStream.println("")
  }

  def apply(instructions : Map[String, Instruction],
            startingFrom : Set[String] = Set.empty,
            outputStream : PrintWriter): Unit = {
    val stack = mutable.Buffer.empty[String]
    val visited = mutable.Set.empty[String]
    stack.appendAll(startingFrom)
    while (stack.nonEmpty) {
      val top = stack.remove(0)
      if (!visited.contains(top))
        handleBlock(top, stack, visited, instructions, outputStream)
    }
  }
}
