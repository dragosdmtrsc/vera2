package org.change.v2.analysis.executor.solvers

import org.change.v2.analysis.executor.translators.SMTTranslator
import org.change.v2.analysis.memory.MemorySpace
import org.smtlib.ICommand.IScript
import org.smtlib.{ISolver, SMT}
import org.smtlib.command.C_set_logic


class SerialSMTSolver(pathToExec: String)
  extends AbstractSolver[(IScript, SMT)] {
  override def translate(memory: MemorySpace): (IScript, SMT) = {
    val smt = new SMT()
    (new SMTTranslator(smt).translate(memory), smt)
  }

  override def decide(scr: (IScript, SMT)): Boolean = {
    val slv = new org.smtlib.solvers.Solver_z3_4_3(scr._2.smtConfig,
      pathToExec)
    slv.start()
    var script = new org.smtlib.impl.Script()
    script.commands().add(0, new C_set_logic(scr._2.smtConfig.exprFactory.symbol("QF_LIA")))
    script.execute(slv)
    //    slv.push(1)
    scr._1.execute(slv)
    val resp = slv.check_sat()
    //    slv.pop(1)
    if (resp != null)
      resp.toString().toLowerCase().equals("sat")
    else
      false
  }

}

class SMTSolver(pathToExec: String)
  extends AbstractSolver[(IScript, SMT)] {

  private var threadLocalSMT = new ThreadLocal[SMT]() {
    override def get(): SMT = {
      this.getClass.synchronized({
        createSMT
      })


    }
  }

  private var threadLocalSolver = new ThreadLocal[ISolver]() {
    override def initialValue(): ISolver = {
      this.getClass.synchronized({
        val solve = createSolver(threadLocalSMT.get)
        solve.start()
        solve
      })
    }
  }

  protected def createSMT(): SMT = {
    this.getClass.synchronized({
      new SMT()
    })
  }

  protected def getSMT(): SMT = {
    this.threadLocalSMT.get
  }

  protected def getSolver(): ISolver = {
    threadLocalSolver.get
  }

  protected def createSolver(smt: SMT): ISolver = {
    this.getClass.synchronized({
      val slv = new org.smtlib.solvers.Solver_z3_4_3(smt.smtConfig,
        pathToExec)
      slv.start()
      var script = new org.smtlib.impl.Script()
      script.commands().add(0, new C_set_logic(smt.smtConfig.exprFactory.symbol("QF_LIA")))
      script.execute(slv)
      slv
    })
  }

  override def translate(mem: MemorySpace): (IScript, SMT) = {
    val smt = getSMT
    (new SMTTranslator(smt).translate(mem), smt)
  }

  override def decide(script: (IScript, SMT)): Boolean = {
    val (scr, smt) = script
    val printer = smt.smtConfig.defaultPrinter

    val solver = getSolver
    solver.push(1)
    System.out.println(scr.execute(solver))
    val resp = solver.check_sat()
    solver.pop(1)
    System.out.println(printer.toString(scr))

    System.out.println(resp + " " + resp.getClass)
    if (resp != null)
      resp.toString().toLowerCase().equals("sat")
    else
      false
  }

}

object SMTSolver {
  def main(args: Array[String]) {
    System.out.println(new SMTSolver("C:\\Users\\Dragos\\Documents\\GitHub\\jSMTLIB\\SMT\\solvers\\windows\\z3-4.4.0.exe").solve(MemorySpace.cleanWithSymolics(List[String]("a", "b", "c"))))
  }

  def apply(path: String): SMTSolver = {
    new SMTSolver(path)
  }
}

