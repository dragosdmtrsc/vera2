package org.change.v2.analysis.executor.solvers

import org.change.v2.analysis.constraint.{GT, LTE}
import org.change.v2.analysis.executor.translators.{Z3BVTranslator, Z3Translator}
import org.change.v2.analysis.memory.MemorySpace
import z3.scala.{Z3Config, Z3Context}


class Z3BVSolver extends Z3Solver {
  override def translate(mem: MemorySpace): z3.scala.Z3Solver = {
    val z3 = createContext
    val transl = new Z3BVTranslator(z3)
    transl.translate(mem)
  }
}

class Z3Solver extends AbstractSolver[z3.scala.Z3Solver] {

  override def translate(mem: MemorySpace): z3.scala.Z3Solver = {
    val z3 = createContext
    val transl = new Z3Translator(z3)
    transl.translate(mem)
  }

  protected def createContext(): Z3Context = {
    this.getClass.synchronized({
      val z3 = new Z3Context(new Z3Config("MODEL" -> true))
      z3
    })
  }


  override def decide(ctx: z3.scala.Z3Solver): Boolean = {
    val decision = ctx.check.getOrElse(false)
    ctx.context.finalize()
    decision
  }
}

class Z3SolverEnhanced extends Z3Solver {

  override def translate(mem: MemorySpace): z3.scala.Z3Solver = {
    val z3 = createContext

    val transl = new Z3Translator(z3)
    transl.translate(mem)
  }

  private var threadLocalContext = new ThreadLocal[Z3Context]() {
    override def initialValue(): Z3Context = {
      this.getClass.synchronized({
        System.out.println("Create " +
          Thread.currentThread().getName +
          " #" + Thread.currentThread().getId)
        val z3 = new Z3Context(new Z3Config("MODEL" -> true))
        z3
      })
    }
  }

  override def decide(ctx: z3.scala.Z3Solver): Boolean = {
    val ans = ctx.check.getOrElse(false)
    //    ctx.context.pop(1)
    ans
  }

  override def createContext(): Z3Context = {
    this.getClass.synchronized({
      threadLocalContext.get
    })
  }
}
