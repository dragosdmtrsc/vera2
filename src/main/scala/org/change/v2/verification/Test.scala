package org.change.v2.verification

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.util.canonicalnames._
import org.change.v2.verification.Policy._
//import org.change.v2.verification.Formula._


/**
 * Created by matei on 12/01/17.
 */
object Test {
  def main(args: Array[String]): Unit = {


  //extractFork_checker_2
  //test_checker_4
  // fork_checker

    costin_example
  }


  def costin_example = {

    var model = Fork (InstructionBlock(Constrain(IPSrc, :>=:(ConstantValue(5))), Constrain(TcpSrc, :>=:(ConstantValue(5))))
      ,InstructionBlock(Fork(Assign(IPSrc,ConstantValue(3)),Assign(IPSrc,ConstantValue(4)),Assign(IPSrc,ConstantValue(5))),Assign(TcpSrc,ConstantValue(5)))
      ,InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),Fork(Constrain(TcpSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
    )
/*
    var modp = InstructionBlock(Constrain(IPSrc,:==:(ConstantValue(5))),
      Fork(Constrain(TcpSrc,:>=:(ConstantValue(1))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))

    var modpp = Fork(Constrain(TcpSrc,:>=:(ConstantValue(1))),Constrain(TcpSrc,:>=:(ConstantValue(4))),Constrain(TcpSrc,:>=:(ConstantValue(5))))
    */

    //var policy = AF(And(Constrain(IPSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(3)))))
    //should be true

    //var policy = AG(Or(Constrain(IPSrc,:>=:(ConstantValue(5))),Constrain(TcpSrc,:>=:(ConstantValue(5)))))
    //should be false


    var policy = And(AG (Or(Constrain(IPSrc,:>=:(ConstantValue(3))),Constrain(TcpSrc,:>=:(ConstantValue(3))))),
                 Or(EF(Constrain(IPSrc,:>=:(ConstantValue(5)))),EF(Constrain(TcpSrc,:>=:(ConstantValue(5))))))


    verify(policy,model)


  }

  def fork_checker = {

    // {IPSrc == 1 ; { {IPSrc >= 21 || IPSrc >= 22 } || IPSrc >= 9 } ; IPSrc >= 3 } || {IPSrc >= 3 || IPSrc >= 4}

    var model = Fork(InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Fork(Fork(Constrain(IPSrc, :>=:(ConstantValue(21))),Constrain(IPSrc, :>=:(ConstantValue(22)))),Constrain(IPSrc, :>=:(ConstantValue(9)))),
      Constrain(IPSrc, :>=:(ConstantValue(3)))
    ),
      Fork(Constrain(IPSrc, :>=:(ConstantValue(4))),Constrain(IPSrc, :>=:(ConstantValue(5))))
    )

    var policy = AG(EF(Constrain(IPSrc,:>=:(ConstantValue(2)))))
    verify(policy,model)


  }

  /*
  // deprecated
  def extractFork_checker_2 = {
    var model = Fork(InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Fork(Fork(Constrain(IPSrc, :>=:(ConstantValue(21))),Constrain(IPSrc, :>=:(ConstantValue(22)))),Constrain(IPSrc, :>=:(ConstantValue(9)))),
      Constrain(IPSrc, :>=:(ConstantValue(3)))
    ),
      Fork(Constrain(IPSrc, :>=:(ConstantValue(3))))
    )

    println(Policy.containsFork(model))
    println(Policy.extractFork(model))

  }

  //deprectated
  def extractFork_checker_1 = {
    var model = Fork(InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Fork(Constrain(IPSrc, :>=:(ConstantValue(21))),Constrain(IPSrc, :>=:(ConstantValue(22)))),
      Constrain(IPSrc, :>=:(ConstantValue(3)))
    ),
      Fork(Constrain(IPSrc, :>=:(ConstantValue(3))))
    )

    println(Policy.containsFork(model))
    println(Policy.extractFork(model))

  }
  */

  //checking the implementation of the tree-like verification procedure
  def test_checker_4 = {

    var model = InstructionBlock(
      Constrain(IPSrc,  :>=:(ConstantValue(1))),
      Constrain(IPSrc,  :>=:(ConstantValue(2))),
      Constrain(IPSrc,  :>=:(ConstantValue(3))),
      Constrain(TcpSrc, :==:(ConstantValue(10))),
      Constrain(IPSrc,  :>=:(ConstantValue(5))),
      Constrain(IPSrc,  :>=:(ConstantValue(6))),
      Constrain(IPSrc,  :>=:(ConstantValue(7))),
      Constrain(IPSrc,  :==:(ConstantValue(10)))

    )

    var p = And (
       Or(EF(Constrain(IPSrc, :>=:(ConstantValue(2)))),
       EF(Constrain(IPSrc, :==:(ConstantValue(99))))
       ),
      EF(Constrain(TcpSrc, :==:(ConstantValue(10))))
    )

    Policy.verify(p,model)

  }

  def test_checker_3 = {

    var model = InstructionBlock(
      Constrain(TcpSrc, :==:(ConstantValue(10))),
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Constrain(IPSrc, :>=:(ConstantValue(2))),
      Constrain(IPSrc, :>=:(ConstantValue(3))),
      Constrain(IPSrc, :>=:(ConstantValue(4))),
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(IPSrc, :>=:(ConstantValue(6))),
      Constrain(IPSrc, :>=:(ConstantValue(7)))

    )

    var p = AG(EF(Or(
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(TcpSrc,:==:(ConstantValue(10)))
    )))

    Policy.verify(p,model)

  }

  def test_checker_2 = {

    var model = InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Constrain(IPSrc, :>=:(ConstantValue(2))),
      Constrain(IPSrc, :>=:(ConstantValue(3))),
      Constrain(IPSrc, :>=:(ConstantValue(4))),
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(IPSrc, :>=:(ConstantValue(6))),
      Constrain(IPSrc, :>=:(ConstantValue(7))),
      Constrain(TcpSrc, :==:(ConstantValue(10)))

    )

    var p = AG(Or(
        And(
          EF(Constrain(IPSrc, :>=:(ConstantValue(5)))),
          EF(Constrain(IPDst, :==:(ConstantValue(69))))
        ),
      EF(Constrain(TcpSrc,:==:(ConstantValue(10))))
    ))

    Policy.verify(p,model)

  }

  def test_checker_1 = {

    var model = InstructionBlock(
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Constrain(IPSrc, :>=:(ConstantValue(2))),
      Constrain(IPSrc, :>=:(ConstantValue(3))),
      Constrain(IPSrc, :>=:(ConstantValue(4))),
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(IPSrc, :>=:(ConstantValue(6))),
      Constrain(IPSrc, :>=:(ConstantValue(7))),
      Constrain(TcpSrc, :==:(ConstantValue(10)))

    )

    var p = AG(Or(
      EF(Constrain(IPSrc, :>=:(ConstantValue(5)))),
        EF(Constrain(TcpSrc,:==:(ConstantValue(10))))
    ))

    Policy.verify(p,model)

  }
  def test_checker_0 = {

    var model = InstructionBlock(
      Constrain(TcpSrc, :==:(ConstantValue(10))),
      Constrain(IPSrc, :>=:(ConstantValue(1))),
      Constrain(IPSrc, :>=:(ConstantValue(2))),
      Constrain(IPSrc, :>=:(ConstantValue(3))),
      Constrain(IPSrc, :>=:(ConstantValue(4))),
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(IPSrc, :>=:(ConstantValue(6))),
      Constrain(IPSrc, :>=:(ConstantValue(7)))

    )

    var p = AG(EF(Or(
      Constrain(IPSrc, :>=:(ConstantValue(5))),
      Constrain(TcpSrc,:==:(ConstantValue(10)))
    )))

    Policy.verify(p,model)

  }


  def test_isSatisfied = {
    var i = InstructionBlock(
      Constrain(IPSrc,:>=:(ConstantValue(RepresentationConversion.ipToNumber("10.0.0.1")))),
      Constrain(TcpSrc, :>=:(ConstantValue(100)))
    )
    //print(Policy.complement(i))

    var s = State.bigBang
    var p = InstructionBlock(
      Constrain(IPSrc,:==:(ConstantValue(RepresentationConversion.ipToNumber("10.0.0.1")))),
      Constrain(TcpSrc, :==:(ConstantValue(100)))
    )
    var (sp::l1,l2) = p.apply(s) // constrain IPSrc in the current state

    print("Initial state")
    print(sp)

    //print(l1)
    //print(l2)

    print(Policy.isSatisfied(sp,i))

  }



}

