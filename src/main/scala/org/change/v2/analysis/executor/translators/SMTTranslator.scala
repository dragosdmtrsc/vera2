package org.change.v2.analysis.executor.translators

import java.util.LinkedList

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{Minus, Plus, Reference}
import org.change.v2.analysis.memory.{MemorySpace, Value}
import org.change.v2.smt.RenameVisitor
import org.change.v2.util.StringIt
import org.smtlib.ICommand.IScript
import org.smtlib.IExpr.ISymbol
import org.smtlib.{ICommand, IExpr, ISort, SMT}
import org.smtlib.command.{C_assert, C_declare_fun, C_define_fun}

import scala.collection.JavaConversions._

class SMTTranslator(smt: SMT)
  extends Translator[IScript] {

  override def translate(mem: MemorySpace): IScript = {
    val factory = smt.smtConfig.exprFactory
    val v = visit(mem, smt)
    //    v.commands().add(0, new C_push(factory.numeral(1)))
    //    v.commands().add(0, new C_set_logic(factory.symbol("QF_LIA")))
    //    v.commands().add(new C_check_sat())
    //    v.commands().add(new C_pop(factory.numeral(1)))
    v
  }

  private def visit(expression: Expression,
                    smt: SMT,
                    set: Set[String]): (IExpr, List[ICommand], Set[String]) = {
    val factory = smt.smtConfig.exprFactory
    val sortFactory = smt.smtConfig.sortFactory

    expression match {
      case Plus(left, right) =>
        hitMe(left, smt, set, right, factory, "+")
      case Minus(left, right) =>
        hitMe(left, smt, set, right, factory, "-")
      case ConstantValue(v, _, _) =>
        (factory.numeral(v), List[ICommand](), Set[String]())
      case SymbolicValue(_) => {
        val symId = "Symbol_" + expression.id
        if (!set.contains(symId)) {
          val theSym = factory.symbol(symId)
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
      case Reference(value, _) => {
        val visited = visit(value.e, smt, set)
        (visited._1, visited._2, visited._3)
      }
    }
  }

  private def hitMe(left: org.change.v2.analysis.memory.Value,
                    smt: org.smtlib.SMT,
                    set: Set[String], right: org.change.v2.analysis.memory.Value,
                    factory: org.smtlib.IExpr.IFactory,
                    sep: String): (IExpr.IFcnExpr, List[ICommand], Set[String]) = {
    val leftVisit = visit(left.e, smt, set)
    val rightVisit = visit(right.e, smt, leftVisit._3)
    (factory.fcn(factory.symbol(sep),
      leftVisit._1,
      rightVisit._1),
      leftVisit._2 ++ rightVisit._2,
      set ++ rightVisit._3)
  }


  private def visit(c: Constraint,
                    isym: IExpr,
                    smt: SMT,
                    set: Set[String] = Set[String]()): (IExpr, List[ICommand], Set[String]) = {
    val eFactory = smt.smtConfig.exprFactory
    val sortFactory = smt.smtConfig.sortFactory
    c match {
      case AND(head :: Nil) => {
        visit(head, isym, smt, set)
      }
      case AND(head :: tail) => {
        andOrSolve(head, isym, smt, set, tail, eFactory, "and")
      }
      case OR(head :: Nil) => {
        visit(head, isym, smt, set)
      }
      case OR(head :: tail) => {
        andOrSolve(head, isym, smt, set, tail, eFactory, "or")
      }
      case NOT(ct) => {
        val ctVisit = visit(ct, isym, smt, set)
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
          isym, smt, set)
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
        opExpression(value, smt, set, eFactory, isym, "=")
      }
      case LT_E(value) => {
        opExpression(value, smt, set, eFactory, isym, "<")
      }
      case GT_E(value) => {
        opExpression(value, smt, set, eFactory, isym, ">")
      }
      case LTE_E(value) => {
        opExpression(value, smt, set, eFactory, isym, "<=")
      }
      case GTE_E(value) => {
        opExpression(value, smt, set, eFactory, isym, ">=")
      }
    }
  }

  private def opExpression(value: org.change.v2.analysis.expression.abst.Expression,
                           smt: org.smtlib.SMT,
                           set: Set[String],
                           eFactory: org.smtlib.IExpr.IFactory,
                           isym: org.smtlib.IExpr,
                           sep: String) = {
    val visited = visit(value, smt, set)
    (eFactory.fcn(eFactory.symbol(sep), isym, visited._1),
      visited._2, visited._3)
  }

  private def andOrSolve(head: org.change.v2.analysis.constraint.Constraint,
                         isym: org.smtlib.IExpr,
                         smt: org.smtlib.SMT,
                         set: Set[String],
                         tail: List[org.change.v2.analysis.constraint.Constraint],
                         eFactory: org.smtlib.IExpr.IFactory,
                         sep: String) = {
    val list = head :: tail
    val retVal = list.foldLeft((List[IExpr](), List[ICommand](), set))((acc, s) => {
      val visited = visit(s, isym, smt, acc._3)
      (acc._1.+:(visited._1), acc._2 ++ visited._2, acc._3 ++ visited._3)
    })
    val args = retVal._1.toArray


    (eFactory.fcn(eFactory.symbol(sep),
      args: _*),
      retVal._2,
      retVal._3)
  }

  private def visit(v: Value, smt: SMT, set: Set[String]): (List[ICommand], Set[String]) = {
    val visitExpr = visit(v.e, smt, set)
    val allCmds = visitExpr._2
    val currentCommands = List[ICommand]()
    val visitedCtsFull = v.cts.foldLeft((visitExpr._2, visitExpr._3))((acc, s) => {
      val visited = visit(s, visitExpr._1, smt, acc._2)
      val expr = visited._1
      val assertion = new C_assert(expr)
      (acc._1 ++ visited._2.:+(assertion), acc._2 ++ visited._3)
    })

    (visitedCtsFull._1, visitedCtsFull._2)
  }

  private def visit(values: List[Value], smt: SMT):
  (List[ICommand],
    Set[String]) = {
    val sortFactory = smt.smtConfig.sortFactory
    val eFactory = smt.smtConfig.exprFactory
    val listof = values.foldLeft((List[ICommand](), Set[String]()))((acc, v) => {
      val res = visit(v, smt, acc._2)
      (acc._1 ++ res._1, acc._2 ++ res._2)
    })
    (listof._1, listof._2)
  }

  def visit(value: MemorySpace, smt: SMT): ICommand.IScript = {
    val (syms, raws, tags) = (value.symbols, value.rawObjects, value.memTags)
    val mSpace = syms.filter(p => !p._2.value.isEmpty).map(l =>
      l._2.value.get).toList
    val rawSpace = raws.
      filter(s => !s._2.value.isEmpty).
      map(l => {
        l._2.value.get
      })
    val fullSpace = mSpace ++ rawSpace
    var script = new org.smtlib.impl.Script()
    visit(fullSpace, smt)._1.foreach { x => script.add(x) }
    script
  }

  private def renameCommands(v: org.smtlib.ICommand.IScript, factory: org.smtlib.IExpr.IFactory) = {

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
}