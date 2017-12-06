package org.change.v2.runners.experiments

import java.util.LinkedList

//import scala.collection.JavaConversions._
//import scala.collection.JavaConverters.iterableAsScalaIterableConverter
//import scala.collection.JavaConverters._
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{Minus, Plus, Reference}
import org.change.v2.analysis.memory.{MemorySpace, State, Value}
import org.change.v2.smt.RenameVisitor
import org.change.v2.util.StringIt
import org.smtlib.IExpr.ISymbol
import org.smtlib.{ICommand, IExpr, ISort, SMT}
import org.smtlib.command._

import scala.collection.JavaConversions._


object SMTRunner {


  def main(args: Array[String]) = {
    val (successful, failed) = SEFLRunner.ex2

    val smt = new SMT()
    val factory = smt.smtConfig.exprFactory
    val printer = smt.smtConfig.defaultPrinter
    failed.foreach(state => {
      val v = toSmtScript(state, smt)

      val solver = new org.smtlib.solvers.Solver_z3_4_3(smt.smtConfig, "/home/dragos/GitHub/jSMTLIB/SMT/solvers/linux/z3-4.4.0")
      solver.start()
      val iresp = v.execute(solver)
      println(iresp)
      solver.exit()
      println(printer.toString(v))
    })

    println("==================")

    successful.foreach(state => {
      val v = toSmtScript(state, smt)
      val solver = new org.smtlib.solvers.Solver_z3_4_3(smt.smtConfig, "/home/dragos/GitHub/jSMTLIB/SMT/solvers/linux/z3-4.4.0")
      solver.start()
      val iresp = v.execute(solver)
      println(iresp)
      solver.exit()
      println(printer.toString(v))
    })

  }

  def toSmtScript(state: State,
                  smt: org.smtlib.SMT) = {
    val factory = smt.smtConfig.exprFactory
    val v = visit(state.memory, smt)
    val newComms = renameCommands(v, factory)
    v.commands().clear()
    newComms.foreach { s => v.commands().add(s) }
    v.commands().add(0, new C_set_logic(factory.symbol("QF_LIA")))
    v.commands().add(new C_check_sat())

    v
  }

  def renameCommands(v: org.smtlib.ICommand.IScript, factory: org.smtlib.IExpr.IFactory) = {

    val stringIt = new StringIt()
    val mapOfSymbols = v.commands().filter(s =>
      s.isInstanceOf[C_declare_fun] ||
        s.isInstanceOf[C_define_fun]).map(s => {
      if (s.isInstanceOf[C_declare_fun])
        s.asInstanceOf[C_declare_fun].symbol().value()
      else if (s.isInstanceOf[C_define_fun])
        s.asInstanceOf[C_define_fun].symbol().value()
      else
        ""
    }).toSet

    val mappings = mapOfSymbols.filter { s => s.startsWith("Symbol_") }.map(s => {
      val what = stringIt.find { x => !mapOfSymbols.contains(x) }.get
      (s -> what)
    }).toMap
    val newComms = v.commands().filter(s =>
      s.isInstanceOf[C_declare_fun] ||
        s.isInstanceOf[C_define_fun] ||
        s.isInstanceOf[C_assert]
    ).map(s => {
      if (s.isInstanceOf[C_declare_fun]) {
        val c_decl = s.asInstanceOf[C_declare_fun]
        if (mappings.contains(c_decl.symbol().value())) {
          val new_c_decl = new C_declare_fun(
            factory.symbol(mappings(c_decl.symbol().value())),
            c_decl.argSorts(), c_decl.resultSort())
          new_c_decl
        }
        else
          c_decl

      }
      else if (s.isInstanceOf[C_define_fun]) {
        val c_decl = s.asInstanceOf[C_define_fun]
        val mapped = c_decl.symbol().accept(
          new RenameVisitor(mappings, factory))
          .asInstanceOf[ISymbol]
        val expp = c_decl.expression().accept(
          new RenameVisitor(mappings, factory))

        val new_c_def = new C_define_fun(mapped,
          c_decl.parameters(), c_decl.resultSort(), expp)
        new_c_def
      }
      else if (s.isInstanceOf[C_assert]) {
        val c_assert = s.asInstanceOf[C_assert]
        val exp = c_assert.expr().accept(
          new RenameVisitor(mappings, factory))
        val new_c_assert = new C_assert(exp)

        new_c_assert
      }
      else
        null
    })
    newComms
  }


  def visit(expression: Expression,
            smt: SMT,
            mappings: Map[String, Value],
            set: Set[String]): (IExpr, List[ICommand], Set[String]) = {
    val factory = smt.smtConfig.exprFactory
    val sortFactory = smt.smtConfig.sortFactory

    expression match {
      case Plus(left, right) =>
        hitMe(left, smt, mappings, set, right, factory, "+")
      case Minus(left, right) =>
        hitMe(left, smt, mappings, set, right, factory, "-")
      case ConstantValue(v, _, _) =>
        (factory.numeral(v), List[ICommand](), Set[String]())
      case SymbolicValue(_) => {
        val symId = "Symbol_" + expression.id
        val theSym = factory.symbol(symId)
        if (!set.contains(symId)) {
          val toBeAdded = new C_declare_fun(theSym,
            new LinkedList[ISort],
            sortFactory.createSortExpression(factory.symbol("Int")))
          (factory.symbol(symId),
            List[ICommand](toBeAdded),
            set + (symId))
        }
        else {
          (factory.symbol(symId),
            List[ICommand](),
            set)
        }

      }
      case Reference(value, _) =>
        (factory.symbol(mappings.find(s => s._2 == value).get._1),
          List[ICommand](), set)
    }
  }

  def hitMe(left: org.change.v2.analysis.memory.Value,
            smt: org.smtlib.SMT,
            mappings: Map[String, org.change.v2.analysis.memory.Value],
            set: Set[String], right: org.change.v2.analysis.memory.Value,
            factory: org.smtlib.IExpr.IFactory,
            sep: String): (IExpr.IFcnExpr, List[ICommand], Set[String]) = {
    val leftVisit = visit(left.e, smt, mappings, set)
    val rightVisit = visit(right.e, smt, mappings, leftVisit._3)
    (factory.fcn(factory.symbol(sep),
      leftVisit._1,
      rightVisit._1),
      leftVisit._2 ++ rightVisit._2,
      set ++ rightVisit._3)
  }


  def visit(c: Constraint,
            isym: IExpr,
            smt: SMT,
            mappings: Map[String, Value],
            set: Set[String] = Set[String]()): (IExpr, List[ICommand], Set[String]) = {
    val eFactory = smt.smtConfig.exprFactory
    val sortFactory = smt.smtConfig.sortFactory
    c match {
      case AND(head :: Nil) => {
        visit(head, isym, smt, mappings, set)
      }
      case AND(head :: tail) => {
        andOrSolve(head, isym, smt, mappings, set, tail, eFactory, "AND")
      }
      case OR(head :: Nil) => {
        visit(head, isym, smt, mappings, set)
      }
      case OR(head :: tail) => {
        andOrSolve(head, isym, smt, mappings, set, tail, eFactory, "OR")
      }
      case NOT(ct) => {
        val ctVisit = visit(ct, isym, smt, mappings, set)
        (eFactory.fcn(eFactory.symbol("not"),
          ctVisit._1),
          ctVisit._2,
          ctVisit._3)
      }
      case E(value) => {
        (eFactory.fcn(eFactory.symbol("="), isym, eFactory.numeral(value)),
          List[ICommand](), set)
      }
      case LT(value) => {
        (eFactory.fcn(eFactory.symbol("<"), isym, eFactory.numeral(value)),
          List[ICommand](), set)
      }
      case LTE(value) => {
        (eFactory.fcn(eFactory.symbol("<="), isym, eFactory.numeral(value)),
          List[ICommand](), set)
      }
      case Range(start, stop) => {
        visit(AND(List[Constraint](LTE(stop), GTE(start))),
          isym, smt, mappings, set)
      }
      case GT(value) => {
        (eFactory.fcn(eFactory.symbol(">"), isym, eFactory.numeral(value)),
          List[ICommand](), set)
      }
      case GTE(value) => {
        (eFactory.fcn(eFactory.symbol(">="), isym, eFactory.numeral(value)),
          List[ICommand](), set)
      }
      case EQ_E(value) => {
        opExpression(value, smt, mappings, set, eFactory, isym, "=")
      }
      case LT_E(value) => {
        opExpression(value, smt, mappings, set, eFactory, isym, "<")
      }
      case GT_E(value) => {
        opExpression(value, smt, mappings, set, eFactory, isym, ">")
      }
      case LTE_E(value) => {
        opExpression(value, smt, mappings, set, eFactory, isym, "<=")
      }
      case GTE_E(value) => {
        opExpression(value, smt, mappings, set, eFactory, isym, ">=")
      }
    }
  }

  def opExpression(value: org.change.v2.analysis.expression.abst.Expression,
                   smt: org.smtlib.SMT,
                   mappings: Map[String, org.change.v2.analysis.memory.Value],
                   set: Set[String],
                   eFactory: org.smtlib.IExpr.IFactory,
                   isym: org.smtlib.IExpr,
                   sep: String) = {
    val visited = visit(value, smt, mappings, set)
    (eFactory.fcn(eFactory.symbol(sep), isym, visited._1),
      visited._2, visited._3)
  }

  def andOrSolve(head: org.change.v2.analysis.constraint.Constraint,
                 isym: org.smtlib.IExpr,
                 smt: org.smtlib.SMT,
                 mappings: Map[String, org.change.v2.analysis.memory.Value],
                 set: Set[String],
                 tail: List[org.change.v2.analysis.constraint.Constraint],
                 eFactory: org.smtlib.IExpr.IFactory,
                 sep: String) = {
    val list = head :: tail
    val retVal = list.foldLeft((List[IExpr](), List[ICommand](), set))((acc, s) => {
      val visited = visit(s, isym, smt, mappings, acc._3)
      (acc._1.+:(visited._1), acc._2 ++ visited._2, acc._3 ++ visited._3)
    })
    val args = retVal._1.toArray


    (eFactory.fcn(eFactory.symbol(sep),
      args: _*),
      retVal._2,
      retVal._3)
  }

  def visit(value: Map[String, Value], smt: SMT):
  (List[ICommand],
    Set[String]) = {
    val sortFactory = smt.smtConfig.sortFactory
    val eFactory = smt.smtConfig.exprFactory
    val listof = value.foldLeft((List[ICommand](), Set[String]()))((acc, s) => {
      val set = acc._2
      val (sym, v) = s
      val isym = eFactory.symbol(sym)
      val visitExpr = visit(v.e, smt, value, set)
      val allCmds = visitExpr._2
      val cmdDecl = new C_define_fun(isym,
        new LinkedList[IExpr.IDeclaration],
        sortFactory.createSortExpression(eFactory.symbol("Int")),
        visitExpr._1)
      val currentCommands = (allCmds ++ acc._1).:+(cmdDecl)
      val cts = v.cts
      val visitedCtsFull = cts.foldLeft((List[ICommand](), visitExpr._3))((acc, s) => {
        val visited = visit(s, isym, smt, value, acc._2)
        val expr = visited._1
        val assertion = new C_assert(expr)
        (acc._1 ++ visited._2.:+(assertion), acc._2 ++ visited._3)
      })

      (currentCommands ++ visitedCtsFull._1, visitedCtsFull._2)
    })
    (listof._1, listof._2)
  }

  def visit(value: MemorySpace, smt: SMT): ICommand.IScript = {
    val (syms, raws, tags) = (value.symbols, value.rawObjects, value.memTags)
    val mSpace = syms.filter(p => !p._2.value.isEmpty).map(l =>
      (l._1, l._2.value.get))
    val rawSpace = raws.
      filter(s => !s._2.value.isEmpty).
      map(l => {
        val tag = tags.find(p => p._2 == l._1)
        val theName = if (tag.isEmpty)
          ""
        else
          tag.get._1 + ""
        val start = l._1
        val end = l._1 + l._2.size
        val full = theName + "_" + start + "_" + end
        (full, l._2.value.get)
      })
    val fullSpace = mSpace ++ rawSpace
    var script = new org.smtlib.impl.Script()
    visit(fullSpace, smt)._1.foreach { x => script.add(x) }


    script
  }
}