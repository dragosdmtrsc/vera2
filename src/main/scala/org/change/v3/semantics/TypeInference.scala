package org.change.v3.semantics

import org.change.v3.semantics.ReferenceSolver.solveReferences
import org.change.v3.syntax.{LVExpr, _}

import scala.annotation.tailrec
import scala.collection.mutable

trait TypeConstraint
case class FixedSize(expr : BVExpr, sz : Int) extends TypeConstraint
case class EqSize(left : BVExpr, right: BVExpr) extends TypeConstraint
case class Unknown(left : BVExpr) extends TypeConstraint

class TypeInference {

}

object ReferenceSolver {
  def solveReferences(expr: Expr, state : SimpleMemory,
                      crt : Map[BVExpr, Option[SimpleMemoryObject]] = Map.empty) :
  Map[BVExpr, Option[SimpleMemoryObject]] = expr match {
    case LAnd(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case LNot(a) => solveReferences(a, state, crt)
    case LOr(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case BVAdd(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case BVSub(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case BVAnd(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case BVOr(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case BVNeg(a) => solveReferences(a, state, crt)
    case BVNot(a) => solveReferences(a, state, crt)
    case EQ(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case GT(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case LT(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case GTE(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case LTE(a, b) => solveReferences(b, state, solveReferences(a, state, crt))
    case s : Symbol => crt + (s -> state.symbols.get(s.path))
    case r : Reference => crt + (r -> state.eval(r.intable).flatMap(h => {
      state.rawObjects.get(h)
    }))
  }
}

object TypeInference {

  def gatherConstraintsInternal(expr : Expr,
                                crt : List[TypeConstraint] = Nil) : List[TypeConstraint] = {
    case LAnd(a, b) => gatherConstraintsInternal(b, gatherConstraintsInternal(a, crt))
    case LNot(a) => gatherConstraintsInternal(a, crt)
    case LOr(a, b) => gatherConstraintsInternal(b, gatherConstraintsInternal(a, crt))
    case e @ BVAdd(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(e, a) :: EqSize(a, b) :: crt))
    case e @ BVSub(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(e, a) :: EqSize(a, b) :: crt))
    case bva @ BVAnd(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(bva, a) :: EqSize(a, b) :: crt))
    case bvor @ BVOr(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(bvor, a) :: EqSize(a, b) :: crt))
    case bvneg : BVNeg => gatherConstraintsInternal(bvneg.a, EqSize(bvneg.a, bvneg) :: crt)
    case bvnot : BVNot => gatherConstraintsInternal(bvnot.b, EqSize(bvnot.b, bvnot) :: crt)
    case EQ(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(a, b) :: crt))
    case GT(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(a, b) :: crt))
    case LT(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(a, b) :: crt))
    case GTE(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(a, b) :: crt))
    case LTE(a, b) => gatherConstraintsInternal(b,
      gatherConstraintsInternal(a, EqSize(a, b) :: crt))
    case h : Havoc => Unknown(h) :: crt
    case ct : Literal => Unknown(ct) :: crt
    case _ => crt
  }
  def gatherTypeConstraints(expr : Expr,
                            references : Map[BVExpr, Option[SimpleMemoryObject]],
                            extra : List[TypeConstraint]) : List[TypeConstraint] = {
    gatherConstraintsInternal(expr, references.flatMap(h => h._2.map(x => {
      FixedSize(h._1, x.size) : TypeConstraint
    })).toList ++ extra)
  }

  def solveTypes(expr : Expr,
                 references : Map[BVExpr, Option[SimpleMemoryObject]],
                 extra : List[TypeConstraint] = Nil) = {
    val withConstraints = gatherTypeConstraints(expr, references, extra)
    val classMapping = mutable.Map.empty[BVExpr, (mutable.Set[BVExpr], Option[Int])]
    var bad = false
    for (ct <- withConstraints if !bad) {
      ct match {
        case FixedSize(a, x) =>
          val mset = classMapping.getOrElseUpdate(a, (mutable.Set.empty, Some(x)))
          if (mset._2.nonEmpty && mset._2.get != x)
            bad = true
          else {
            mset._1.add(a)
          }
        case EqSize(a, b) =>
          var mset1 = classMapping.getOrElse(a, (mutable.Set.empty, None))
          var mset2 = classMapping.getOrElse(b, (mutable.Set.empty, None))
          if (mset1._2.nonEmpty && mset2._2.nonEmpty && mset1._2.get != mset2._2.get) {
            bad = true
          } else {
            if (mset1._1.size < mset2._1.size) {
              mset2._1.add(a)
              mset2._1.add(b)
              for (x <- mset1._1)
                mset2._1.add(x)
              if (mset1._2.nonEmpty)
                mset2 = mset2.copy(_2 = mset1._2)
            } else {
              mset1._1.add(a)
              mset1._1.add(b)
              for (x <- mset2._1)
                mset1._1.add(x)
              if (mset2._2.nonEmpty)
                mset1 = mset1.copy(_2 = mset2._2)
            }
          }
        case Unknown(v) => ;
      }
    }
    if (!bad) {
      if (classMapping.exists(_._2._2.isEmpty))

    }
  }
}
