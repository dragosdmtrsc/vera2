package org.change.v2.analysis.executor.solvers

import org.smtlib.ICommand.IScript
import org.change.v2.analysis.memory.MemorySpace
import org.smtlib.SMT
import org.smtlib.ISolver
import org.change.v2.analysis.executor.translators.SMTTranslator

class SMTSolver(pathToExec : String)
  extends AbstractSolver[(IScript, SMT)] {
  
  private var threadLocalSMT = new ThreadLocal[SMT] () {
    override def initialValue() : SMT = {
        createSMT
    }
  }
  
  private var threadLocalSolver = new ThreadLocal[ISolver]() {
    override def initialValue() : ISolver = {
        this.getClass.synchronized({
          val solve = createSolver(threadLocalSMT.get)
          solve.start()
          solve
        })
    }
  }
  
  protected def createSMT() : SMT = {
    this.getClass.synchronized({new SMT()})
  }
  
  protected def getSMT() : SMT = {
    this.threadLocalSMT.get
  }
  
  protected def getSolver() : ISolver = {
    threadLocalSolver.get
  }
  
  protected def createSolver(smt : SMT) : ISolver = {
    this.getClass.synchronized(
        new org.smtlib.solvers.Solver_z3_4_3(smt.smtConfig, 
            pathToExec))
  }
  
  override def translate(mem : MemorySpace) : (IScript, SMT) = {
    val smt = getSMT
    (new SMTTranslator(smt).translate(mem), smt)
  }
  
  override def decide(script : (IScript, SMT)) : Boolean = {
    val (scr, smt) = script
    val solver = getSolver
    val resp = scr.execute(solver)
    if (resp != null) 
      resp.toString().toLowerCase().equals("sat") 
    else
      false
  }
  
}

object SMTSolver {
  def main(args : Array[String]) {
    System.out.println(new SMTSolver("C:\\Users\\Dragos\\Documents\\GitHub\\jSMTLIB\\SMT\\solvers\\windows\\z3-4.4.0.exe").solve(MemorySpace.cleanWithSymolics(List[String]("a", "b", "c"))))
  }
  
  def apply(path : String) : SMTSolver = {
    new SMTSolver(path)
  }
}

