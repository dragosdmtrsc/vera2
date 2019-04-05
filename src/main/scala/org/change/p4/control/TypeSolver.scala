package org.change.parser.p4.control

import java.util.logging.Logger

import org.change.plugins.vera.{BVType, IntType, P4Type}
import org.change.utils.FreshnessManager
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3Model, Z3Sort}

import scala.collection.mutable

class TypeSolver(context : Z3Context) {
  private val slv = context.mkSolver()

  private val constraints = mutable.Map.empty[Z3AST, Boolean]
  private val constraintsToInit = mutable.Map.empty[Z3AST, (Object, Object)]

  private val kindSorts = context.mkADTSorts(Seq(
    ("Kind",
      Seq("integer", "bv", "arr"),
      Seq(
        Seq(),
        Seq(("nr", RegularSort(context.mkIntSort()))),
        Seq(("k", RecursiveType(0)),
          ("nr", RegularSort(context.mkIntSort()))
        )
      )
    )
  ))
  val kindSort: Z3Sort = kindSorts.head._1
  def bv(w : Int): Z3AST = {
    kindSorts.head._2(1)(context.mkInt(w, context.mkIntSort()))
  }
  def intsort() : Z3AST = {
    kindSorts.head._2.head()
  }
  def constSort(name : String) : Z3AST = context.mkConst(name, kindSort)
  private val exprToType = mutable.Map.empty[Object, (String, Z3AST)]
  def getIdx(p : Object) : Z3AST = {
    exprToType.getOrElseUpdate(p, {
      val fm = FreshnessManager.next("type")
      (fm, constSort(fm))
    })._2
  }

  def isABV(z3AST: Z3AST, model : Z3Model) : Option[BVType] = {
    val test = kindSorts.head._3(1)(z3AST)
    val extract = kindSorts.head._4(1).head
    model.evalAs[Boolean](test).filter(x => x).flatMap(_ => {
      model.evalAs[Int](extract(z3AST))
    }).map(sz => BVType(sz))
  }
  def isAnInt(z3AST: Z3AST, model: Z3Model) : Boolean = {
    val test = kindSorts.head._3.head(z3AST)
    model.evalAs[Boolean](test).exists(x => x)
  }
  def type2sort(p4Type: P4Type) : Z3AST = p4Type match {
    case BVType(x) => bv(x)
    case IntType => intsort()
  }
  private def equalInternal(ast1 : Z3AST,
                            ast2 : Z3AST,
                            mandatory : Boolean): Z3AST = {
    val nxtIdx = constraints.size
    val nxtAst = context.mkConst(s"p$nxtIdx", context.mkBoolSort())
    constraints.put(nxtAst, mandatory)
    slv.assertCnstr(context.mkImplies(nxtAst, context.mkEq(ast1, ast2)))
    nxtAst
  }

  def equal(param : Object,
            p4Type: P4Type,
            mandatory : Boolean = true): Unit = {
    constraintsToInit.put(
      equalInternal(getIdx(param), type2sort(p4Type), mandatory),
      (p4Type, param)
    )
  }
  def paramsEqual(e1 : Object,
                  e2 : Object,
                  mandatory : Boolean = true): Unit = {
    constraintsToInit.put(
      equalInternal(getIdx(e1), getIdx(e2), mandatory),
      (e1, e2))
  }
  def getType(model : Z3Model, ast : Z3AST) : Option[P4Type] = {
    if (isAnInt(ast, model)) Some(IntType)
    else {
      isABV(ast, model)
    }
  }

  def solve(filter : Object => Boolean) : Iterable[(Object, Option[P4Type])] = {
    var assumptionsToCheck = constraints.keys.toList
    var removed = Set.empty[Z3AST]
    var res = false
    while (!res) {
      res = slv.checkAssumptions(assumptionsToCheck:_*).get
      if (!res) {
        val unsatCore = slv.getUnsatCore()
        val weak = unsatCore.filter(!constraints(_))
        if (weak.nonEmpty) {
          assumptionsToCheck = assumptionsToCheck.filter(!weak.contains(_))
          removed = removed ++ weak
        } else {
          val ucString = unsatCore.map(constraintsToInit).map(x => {
            x._1 + " == " + x._2
          }).mkString(",")
          throw new IllegalStateException("can't do type inference because: " + ucString)
        }
      }
    }
    val model = slv.getModel()
    for (
      x <- this.exprToType
      if filter(x._1)
    ) yield (x._1, model.eval(x._2._2).map(
      getType(model, _).getOrElse({
        Logger.getLogger("typeinference").warning("can't infer type for " +
          x._1 + ", resolve to bv8 same as p4c")
        BVType(8)
      })
    ))
  }
  def casts() : Map[Object, P4Type] = {
    if (slv.isModelAvailable) {
      val model = slv.getModel()
      this.constraintsToInit.filter(r => {
        !model.evalAs[Boolean](r._1).get
      }).map(r => {
        val castObj = r._2._2
        val to = r._2._1 match {
          case p4Type: P4Type => p4Type
          case other : Object =>
            val idx = getIdx(other)
            getType(model, idx).getOrElse({
              Logger.getLogger("typeinference").warning("can't infer type for " +
                other + ", resolve to bv8 same as p4c")
              BVType(8)
            })
        }
        castObj -> to
      }).toMap
    } else {
      throw new IllegalStateException("can't call for model when there is none, first" +
        "solve then go for the casts")
    }
  }

}
