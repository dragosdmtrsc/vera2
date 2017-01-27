package org.change.v2.analysis.executor.solvers

import org.smtlib.ICommand.IScript
import org.change.v2.analysis.memory.MemorySpace
import org.smtlib.SMT
import org.smtlib.ISolver
import org.change.v2.analysis.executor.translators.SMTTranslator
import org.smtlib.impl.Pos

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
    this.getClass.synchronized({
        val slv = new org.smtlib.solvers.Solver_z3_4_3(smt.smtConfig, 
            pathToExec)
        slv.start()
        System.out.println(slv.sendCommand("(set-logic QF_LIA)"))
        slv
    })
  }
  
  override def translate(mem : MemorySpace) : (IScript, SMT) = {
    val smt = getSMT
    (new SMTTranslator(smt).translate(mem), smt)
  }
  
  override def decide(script : (IScript, SMT)) : Boolean = {
    val (scr, smt) = script
    val printer = smt.smtConfig.defaultPrinter

    System.out.println(printer.toString(scr))
    val solver = getSolver
    solver.push(1)
    scr.execute(solver)
    val resp = solver.check_sat()
    solver.pop(1)
    System.out.println(resp + " " + resp.getClass)
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

