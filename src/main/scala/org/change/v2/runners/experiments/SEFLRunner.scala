package org.change.v2.runners.experiments

import java.io.{FileOutputStream, PrintStream, File}
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.processingmodels.State
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.analysis.memory.Tag
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.utils.prettifier.StatePrettifier
import org.change.utils.prettifier.SomeWriter
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fail
import java.util.HashMap
import org.change.v2.analysis.expression.concrete.nonprimitive.:+:
object SEFLRunner {

  lazy val output = new PrintStream(new FileOutputStream(new File("outputs/pretty/sefl0.output")))

  lazy val jOutput = new PrintStream(new FileOutputStream(new File("outputs/pretty/sefl0.json")))
  lazy val output2 = new PrintStream(new FileOutputStream(new File("outputs/pretty/sefl1.output")))
  lazy val output3 = new PrintStream(new FileOutputStream(new File("outputs/pretty/sefl2.output")))
  lazy val output4 = new PrintStream(new FileOutputStream(new File("outputs/pretty/sefl3.output")))

  def testGeneric (output : PrintStream)(block : => (List[State], List[State])) = {
    val (successful, failed) = block
    var i = 0
    for (s <- successful) {
      output.println(StatePrettifier(s))
      i = i + 1
    }
    
    
    
    i = 0
    for (s <- failed) {
      output.println(StatePrettifier(s))
    }
  }
  
  def main (args: Array[String]){

    testGeneric(output)(ex0)
    testGeneric(output2)(ex1)
    testGeneric(output3)(ex2)
    testGeneric(output4)(ex3)
    
//    output.println(s"OK States (${successful.length}}):\n" + ClickExecutionContext.verboselyStringifyStates(successful))
//    output.println(s"\nFailed States (${failed.length}}):\n" + ClickExecutionContext.verboselyStringifyStates(failed))

    println("Check output @ sefl.output")
  }

  def ex0: (List[State], List[State]) = {
    val code = InstructionBlock (
      Assign("a", SymbolicValue()),
      Assign("zero", ConstantValue(0)),
      // State that a is positive
      Constrain("a", :>:(:@("zero"))),
      // Compute the sum
      Assign("sum", :+:(:@("a"), :@("zero"))),
      // We want the sum to be 0, meaning a should also be zero - impossible
      Constrain("sum", :==:(:@("zero")))
    )

    code(State.clean, true)
  }

  def ex1: (List[State], List[State]) = {
    val code = InstructionBlock (
      Assign("a", SymbolicValue()),
      Assign("zero", ConstantValue(0)),
      // State that a is positive
      Constrain("a", :>:(:@("zero"))),
      // Compute the sum
      Assign("sum", :+:(:@("a"), :@("zero"))),
      // Now, for every branch there will be a path, one successful, one not.
      If(Constrain("sum", :==:(:@("zero"))), {
          // This instruction will never get executed.
          Fail("Impossible")
        }
      )
    )

    code(State.clean, false)
  }

  /**
   * A simple IP filtering and forwarding example.
   */
  def ex2: (List[State], List[State]) = {
    val code = InstructionBlock (
      // At address 0 the L3 header starts
      CreateTag("L3HeaderStart", 0),
      // Also mark IP Src and IP Dst fields and allocate memory
      CreateTag("IPSrc", Tag("L3HeaderStart") + 96),
      // For raw memory access (via tags or ints), space has to be allocated beforehand.
      Allocate(Tag("IPSrc"), 32),
      CreateTag("IPDst", Tag("L3HeaderStart") + 128),
      Allocate(Tag("IPDst"), 32),


      //Initialize IPSrc and IPDst
      Assign(Tag("IPSrc"), ConstantValue(ipToNumber("127.0.0.1"))),
      Assign(Tag("IPDst"), SymbolicValue()),

      // If destination is 8.8.8.8, rewrite the Src address and forward it
      // otherwise, drop it
      If(Constrain(Tag("IPDst"), :==:(ConstantValue(ipToNumber("8.8.8.8")))),
        Assign(Tag("IPSrc"), SymbolicValue()),
        Fail("Packet dropped")
      )
    )

    code(State.clean, true)
  }
  
  /**
   * A simple IP filtering and forwarding example.
   */
  def ex3: (List[State], List[State]) = {
    val tagL3 = Tag("L3HeaderStart")
    val ipSrc = Tag("IPSrc")
    val ipDst = Tag("IPDst")
    val some = :+:(ConstantValue(21), ConstantValue(22))
    val other = :+:(SymbolicValue(), SymbolicValue())
    val code = InstructionBlock (
      // At address 0 the L3 header starts
      CreateTag("L3HeaderStart", 0),
      // Also mark IP Src and IP Dst fields and allocate memory
      CreateTag("IPSrc", tagL3 + 96),
      // For raw memory access (via tags or ints), space has to be allocated beforehand.
      Allocate(ipSrc, 32),
      CreateTag("IPDst", tagL3 + 128),
      Allocate(ipDst, 32),


      //Initialize IPSrc and IPDst
      Assign(ipSrc, "127.0.0.1"),
      Assign(ipDst, SymbolicValue()),

      Assign("Ceva", :+:(SymbolicValue(), SymbolicValue())),
      
      // If destination is 8.8.8.8, rewrite the Src address and forward it
      // otherwise, drop it
      If(Constrain(Tag("IPDst"), :==:(ConstantValue(ipToNumber("8.8.8.8")))),
        Assign(Tag("IPSrc"), SymbolicValue()),
        Fail("Packet dropped")
      )
    )

    code(State.clean, true)
  }
  
  
}


