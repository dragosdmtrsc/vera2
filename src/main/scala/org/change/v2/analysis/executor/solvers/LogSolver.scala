package org.change.v2.analysis.executor.solvers

import java.io.PrintWriter

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.memory.{MemorySpace, Value}


class LogSolver[T](slv: AbstractSolver[T]) extends AbstractSolver[T] {

  private var lastTranslate: Long = 0
  private var lastSolve: Long = 0

  def stats = (lastTranslate, lastSolve)

  override def translate(memory: MemorySpace): T = {
    val init = System.nanoTime()
    val ast = slv.translate(memory)
    lastTranslate = (System.nanoTime() - init)
    ast
  }

  override def decide(what: T): Boolean = {
    val init = System.nanoTime()
    val res = slv.decide(what)
    lastSolve = (System.nanoTime() - init)
    res
  }
}

class ConstraintLogger(file: String) {
  private var logNr = 0
  private var pw = new PrintWriter(file)

  pw.println("Nr,Field,Expr,Constraints,NewConstraint,TranslationTime,SatTime")

  def log(field: Either[Int, String],
          value: Value,
          constraint: Constraint,
          transTime: Long,
          satTime: Long,
          res: Boolean): Unit = {
    val asStr = field match {
      case Right(s) => s
      case Left(i) => i + ""
    }
    val str = "\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%d\"".format(
      new Integer(logNr),
      asStr,
      value.e.toString(),
      value.cts.toString(),
      constraint.toString(),
      transTime,
      satTime,
      new Integer(if (res) 1 else 0)
    )
    pw.println(str)
    logNr = logNr + 1
  }

  def close() {
    pw.close()
  }
}