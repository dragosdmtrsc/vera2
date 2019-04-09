package org.change.p4.control

import java.util.logging.Logger

import org.change.p4.control.types.{BVType, IntType, P4Type, UnboundedInt}
import org.change.utils.FreshnessManager
import com.microsoft.z3._
import org.change.utils.Z3Helper._

import scala.collection.mutable

class TypeSolver(context: Context) {
  private val slv = context.mkSolver()
  private val parms = context.mkParams()
  parms.add("core.minimize", true)
  slv.setParameters(parms)

  private val constraints = mutable.Map.empty[Expr, Boolean]
  private val constraintsToInit = mutable.Map.empty[Expr, (Object, Object)]

  val constructors = Array(
    context.mkConstructor("integer", "isinteger", null, null, null),
    context.mkConstructor("bv",
      "isbv",
      Array("nr"),
      Array(context.mkIntSort().asInstanceOf[Sort]),
      Array(0)
    ),
    context.mkConstructor("arr", "isarr",
      Array("k", "nr"),
      Array(null, context.mkIntSort().asInstanceOf[Sort]),
      Array(0, 0)
    )
  )

  val kindSort: Sort = context.mkDatatypeSort("Kind", constructors)
  def bv(w: Int): Expr = {
    constructors(1).ConstructorDecl().apply(
      context.mkNumeral(w, context.mkIntSort()))
  }
  def intsort(): Expr = {
    constructors.head.ConstructorDecl().apply()
  }
  def constSort(name: String): Expr = {
    context.mkConst(name, kindSort)
  }
  private val exprToType = mutable.Map.empty[Object, (String, Expr)]
  def getIdx(p: Object): Expr = {
    exprToType
      .getOrElseUpdate(p, {
        val fm = FreshnessManager.next("type")
        (fm, constSort(fm))
      })
      ._2
  }

  def isABV(AST: Expr, model: Model): Option[BVType] = {
    val test = constructors(1).getTesterDecl.apply(AST)
    val extract = constructors(1).getAccessorDecls.head.apply(AST)
    model.eval(test, false).getBool
      .filter(x => x)
      .flatMap(_ => {
        model.eval(extract, false).getInt
      })
      .map(sz => BVType(sz.toInt))
  }
  def isAnInt(AST: Expr, model: Model): Boolean = {
    val test = constructors.head.getTesterDecl.apply(AST)
    model.eval(test, false).getBool.exists(x => x)
  }
  def type2sort(p4Type: P4Type): Expr = p4Type match {
    case BVType(x) => bv(x)
    case _ : IntType   => intsort()
  }
  private def equalInternal(ast1: Expr,
                            ast2: Expr,
                            mandatory: Boolean): Expr = {
    val nxtIdx = constraints.size
    val nxtAst = context.mkConst(s"p$nxtIdx", context.mkBoolSort()).asBool
    constraints.put(nxtAst, mandatory)
    slv.assertCnstr(context.mkImplies(nxtAst, context.mkEq(ast1, ast2)))
    nxtAst
  }

  def equal(param: Object, p4Type: P4Type, mandatory: Boolean = true): Unit = {
    constraintsToInit.put(
      equalInternal(getIdx(param), type2sort(p4Type), mandatory),
      (p4Type, param)
    )
  }
  def paramsEqual(e1: Object, e2: Object, mandatory: Boolean = true): Unit = {
    constraintsToInit.put(
      equalInternal(getIdx(e1), getIdx(e2), mandatory),
      (e1, e2)
    )
  }
  def getType(model: Model, ast: Expr): Option[P4Type] = {
    if (isAnInt(ast, model)) Some(UnboundedInt)
    else {
      isABV(ast, model)
    }
  }

  def solve(filter: Object => Boolean): Iterable[(Object, Option[P4Type])] = {
    var assumptionsToCheck = constraints.keys.toList
    var removed = Set.empty[AST]
    var res = false
    while (!res) {
      res = slv.docheck(assumptionsToCheck).get
      if (!res) {
        val unsatCore = slv.getUnsatCore
        val weak = unsatCore.headOption
        if (weak.nonEmpty) {
          assumptionsToCheck = assumptionsToCheck.filter(_ != weak.get)
          removed = removed + weak.get
        } else {
          val ucString = unsatCore
            .map(constraintsToInit)
            .map(x => {
              x._1 + " == " + x._2
            })
            .mkString(",")
          throw new IllegalStateException(
            "can't do type inference because: " + ucString
          )
        }
      }
    }
    val model = slv.getModel()
    for (x <- this.exprToType
         if filter(x._1))
      yield
        (
          x._1,
          Some(getType(model, model.eval(x._2._2, false))
            .getOrElse({
              Logger
                .getLogger("typeinference")
                .warning(
                  "can't infer type for " +
                    x._1 + ", resolve to bv8 same as p4c"
                )
              BVType(8)
            })
          )
        )
  }
  def casts(): Map[Object, P4Type] = {
    val model = slv.getModel
    this.constraintsToInit
      .filter(r => {
        !model.eval(r._1, false).getBool.get
      })
      .map(r => {
        val castObj = r._2._2
        val to = r._2._1 match {
          case p4Type: P4Type => p4Type
          case other: Object =>
            val idx = getIdx(other)
            getType(model, idx).getOrElse({
              Logger
                .getLogger("typeinference")
                .warning(
                  "can't infer type for " +
                    other + ", resolve to bv8 same as p4c"
                )
              BVType(8)
            })
        }
        castObj -> to
      })
      .toMap
  }

}
