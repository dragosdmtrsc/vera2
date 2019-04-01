package org.change.parser.p4.control

import java.util.logging.Logger

import org.change.plugins.vera.BVType
import org.change.v2.p4.model.actions.P4ActionCall.ParamExpression
import org.change.v2.p4.model.actions.{P4Action, P4ActionParameter}
import org.change.v2.p4.model.control.TableCaseEntry
import org.change.v2.p4.model.control.exp._
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.table.{MatchKind, TableDeclaration, TableMatch}
import org.change.v2.p4.model.{ArrayInstance, FlowInstance, Switch}
import org.change.v3.semantics.{Condition, SimpleMemory}
import z3.scala.{Z3AST, Z3Context}

import scala.collection.JavaConverters._

class TableHandler(switch: Switch,
                   tableDeclaration: TableDeclaration,
                   context : Z3Context) {
  private val evalWrapper = new EvalWrapper(switch, context)
  lazy val struct = TablesToLogics(switch, tableDeclaration, context)
  def tableMatch2AST(tableMatch: TableMatch)
                    (simpleMemory: SimpleMemory) :
  (Z3AST, Z3AST) = {
    tableMatch.getMatchKind match {
      case MatchKind.Valid =>
        val vref = new ValidRef(tableMatch.getReferenceKey.asInstanceOf[HeaderRef])
        evalWrapper.evaluate(vref, simpleMemory, zeroOnInvalid = true,
          asBV = true)
      case _ =>
        tableMatch.getReferenceKey match {
          case fieldRef : FieldRef =>
            val fieldRefExpr = new FieldRefExpr(fieldRef)
            evalWrapper.evaluate(fieldRefExpr, simpleMemory, zeroOnInvalid = true)
          case maskedFieldRef: MaskedFieldRef =>
            val fieldRefExpr = new FieldRefExpr(maskedFieldRef.getReference)
            val width = maskedFieldRef.getReference.getFieldReference.getLength
            val lit = maskedFieldRef.getMask.setWidth(width)
            val bvand = BinExpr.bvand(fieldRefExpr, lit)
            evalWrapper.evaluate(bvand, simpleMemory, zeroOnInvalid = true)
        }
    }
  }

  // evaluates a parameter as:
  // 1) a reference to an infrastructure object (counter, fieldlist,
  //    header, meter, register)
  // 2) a value - (header field, parameter value)
  def paramEval(paramExpression: ParamExpression,
                isLv : Boolean)
               (simpleMemory: SimpleMemory, parmMap : Map[String, Z3AST])
   : (Object, Z3AST) = {
    val maybecast = if (paramExpression.getCastTo != null) {
      (ast : Z3AST, len : Int) => {
        val BVType(w) = paramExpression.getCastTo
        if (w == len) {
          ast
        } else if (w < len) {
          context.mkExtract(w - 1, 0, ast)
        } else {
          context.mkSignExt(w - len, ast)
        }
      }
    } else (ast : Z3AST, _ : Int) => ast
    paramExpression.getExpression match {
      case ce : LiteralExpr =>
        if (ce.getWidth > 0) {
          (context.mkNumeral(ce.getValue.toString(), context.mkBVSort(ce.getWidth)),
            context.mkFalse())
        } else {
          (context.mkNumeral(ce.getValue.toString(), context.mkIntSort()),
            context.mkFalse())
        }
      // references solvable to a value
      case pr: ParmRef =>
        if (isLv) {
          (pr, context.mkFalse())
        } else {
          val BVType(len) = pr.getParameter.getSort
          (maybecast(parmMap(pr.getPath), len), context.mkFalse())
        }
      case fr: FieldRef => val fieldRefExpr = new FieldRefExpr(fr)
        if (isLv) {
          (fr, context.mkFalse())
        } else {
          val what =
            evalWrapper.evaluate(fieldRefExpr, simpleMemory, zeroOnInvalid = false)
          (maybecast(what._1, fr.getFieldReference.getLength), what._2)
        }
      case calculationRef: CalculationRef => (calculationRef, context.mkFalse())
      case headerRef: HeaderRef => (headerRef, context.mkFalse())
      case fieldListRef: FieldListRef => (fieldListRef, context.mkFalse())
      case registerRef: RegisterRef => (registerRef, context.mkFalse())
      case sref : StringRef => throw new IllegalStateException(sref.getRef + " reference unsolved reference")
    }
  }
  import ExpressionEvaluator._
  def set(fieldRef: FieldRef,
          value : Z3AST,
          simpleMemory: SimpleMemory) : (SimpleMemory, Z3AST, Set[String]) = {
    val (_, err) = evalWrapper.evaluate(new FieldRefExpr(fieldRef),
      simpleMemory, zeroOnInvalid = false
    )
    val changed = indexedHeaderRef(fieldRef.getHeaderRef).map(ihr => {
      val ai = arrayInstance(ihr)
      index(ihr).map(idx => {
        List(ai.getName + s"[$idx]." + fieldRef.getField -> context.mkTrue())
      }).getOrElse({
        val nxtRef = simpleMemory.getAST(ai.getName + ".next")
        (0 until ai.getLength).map(idx => {
          ai.getName + s"[$idx]." + fieldRef.getField ->
            context.mkEq(nxtRef, context.mkInt(idx, nxtRef.getSort))
        })
      })
    }).getOrElse({
      List(fieldRef.getHeaderRef.getPath + "." + fieldRef.getField -> context.mkTrue())
    })
    val newmem = if (changed.size == 1) {
      val theChange = changed.head
      simpleMemory.assignNewValue(theChange._1, value).get
    } else {
      changed.foldLeft(simpleMemory)((crt, r) => {
        val old = crt.getAST(r._1)
        crt.assignNewValue(r._1, context.mkITE(r._2, value, old)).get
      })
    }
    (newmem, err, changed.map(_._1).toSet)
  }

  def isValid(headerRef: HeaderRef, simpleMemory: SimpleMemory) :
    (Z3AST, Z3AST) = {
    evalWrapper.evaluate(new ValidRef(headerRef),
      simpleMemory,
      zeroOnInvalid = false)
  }

  def setValid(headerRef: HeaderRef,
               valid : Boolean,
               simpleMemory: SimpleMemory) : (SimpleMemory, Z3AST, Set[String]) = {
    val asAst = if (valid) context.mkInt(1, context.mkBVSort(1))
    else context.mkInt(0, context.mkBVSort(1))
    val (_, err) = evalWrapper.evaluate(new ValidRef(headerRef),
      simpleMemory, zeroOnInvalid = false
    )
    val changed = indexedHeaderRef(headerRef).map(id => {
      val ai = arrayInstance(id)
      index(id).map(idx => {
        val hdrFld = ai.getName + s"[$idx].IsValid"
        List(hdrFld -> context.mkTrue())
      }).getOrElse({
        val nxtRef = simpleMemory.getAST(ai.getName + ".next")
        (0 until ai.getLength).map(idx => {
          ai.getName + s"[$idx]." + "IsValid" ->
            context.mkEq(nxtRef, context.mkInt(idx, nxtRef.getSort))
        })
      })
    }).getOrElse({
      List(headerRef.getPath + "." + "IsValid" -> context.mkTrue())
    })
    val newmem = if (changed.size == 1) {
      val theChange = changed.head
      simpleMemory.assignNewValue(theChange._1, asAst).get
    } else {
      changed.foldLeft(simpleMemory)((crt, r) => {
        val old = crt.getAST(r._1)
        crt.assignNewValue(r._1, context.mkITE(r._2, asAst, old)).get
      })
    }
    (newmem, err, changed.map(_._1).toSet)
  }

  def toTriple(sm : SimpleMemory, err : Z3AST, mod : Set[String],
               failReason : String) :
    (SimpleMemory, List[SimpleMemory], Set[String]) = {
    val errors = if (context.simplifyAst(err) == context.mkFalse())
      Nil
    else sm.addCondition(err).fail(failReason) :: Nil
    (sm, errors, mod)
  }
  def toTriple(sm : SimpleMemory,
               err : Z3AST,
               mod : Set[String]): (SimpleMemory, List[SimpleMemory], Set[String]) =
    toTriple(sm, err, mod, "")
  def toTriple(sm : (SimpleMemory, Z3AST, Set[String]), failReason : String) :
    (SimpleMemory, List[SimpleMemory], Set[String]) = {
    toTriple(sm._1, sm._2, sm._3, failReason)
  }
  def toTriple(sm : (SimpleMemory, Z3AST, Set[String])) :
    (SimpleMemory, List[SimpleMemory], Set[String]) = toTriple(sm, "")

  def errToMems(ast : Z3AST, simpleMemory: SimpleMemory) : List[SimpleMemory] = {
    if (context.simplifyAst(ast) == context.mkFalse()) {
      Nil
    } else {
      simpleMemory.addCondition(ast) :: Nil
    }
  }

  def standardMeta(metaName : String) : FieldRef = {
    val standardMeta = switch.getInstance("standard_metadata")
    val fld = standardMeta.getLayout.getField(metaName)
    val href = new HeaderRef().setInstance(standardMeta).setPath(standardMeta.getName)
    new FieldRef().setHeaderRef(href).setField(fld.getName).setFieldReference(fld)
  }

  def clone(cloneType : Int,
            cloneSession : Option[Z3AST],
            fieldListRef: FieldListRef,
            simpleMemory: SimpleMemory) : (SimpleMemory, Z3AST, Set[String]) = {
    val (m1, e1, c1) = set(
      standardMeta("instance_type"),
      context.mkInt(cloneType, context.mkBVSort(32)),
      simpleMemory
    )
    val (m2, e2, c2) = cloneSession.map(cs => set(
      standardMeta("clone_spec"),
      cs,
      m1
    )).getOrElse((m1, e1, c1))
    (m2,
      if (e1 != e2)
        context.mkOr(e1, e2)
      else e1, c1 ++ c2)
  }
  def clone(cloneType : Int,
            cloneSession: Z3AST,
            fref : FieldListRef,
            simpleMemory: SimpleMemory) : (SimpleMemory, Z3AST, Set[String]) = {
    clone(cloneType, Some(cloneSession), fref, simpleMemory)
  }
  private def recirculate(cloneType : Int,
                          fref : FieldListRef,
                          simpleMemory: SimpleMemory): (SimpleMemory, Z3AST, Set[String]) = {
    clone(cloneType, None, fref, simpleMemory)
  }

  private def nextIndex(arrayInstance: ArrayInstance,
                        simpleMemory: SimpleMemory) : Z3AST = {
    val fld = arrayInstance.getName + ".next"
    simpleMemory.getAST(fld)
  }

  // executes an action given some params and
  // returns 1 succesful memory state after execution
  // returns * failed memory states
  // returns * fields modified as a result of executing the action successfully
  def executeAction(p4Action: List[P4Action],
                    simpleMemory: SimpleMemory,
                    params : List[Object]) :
    (SimpleMemory, List[SimpleMemory], Set[String]) = {
    //TODO: add proper action handling code here
    p4Action.head match {
      case ax : org.change.v2.p4.model.actions.primitives.Add =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVAdd(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dest in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.AddHeader =>
        assert(params.size == 1)
        toTriple(setValid(params.head.asInstanceOf[HeaderRef], false, simpleMemory))
      case ax : org.change.v2.p4.model.actions.primitives.AddToField =>
        val dstobj = params.head.asInstanceOf[FieldRef]
        val parm = new ParamExpression(dstobj)
        val src1 = paramEval(parm, isLv = false)(simpleMemory, Map.empty)
        val src2 = params(1).asInstanceOf[Z3AST]
        toTriple(set(dstobj,
          context.mkBVAdd(src1._1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.BitAnd =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVAnd(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.BitOr =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVOr(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.BitXor =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVXor(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.ShiftLeft =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVShl(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.ShiftRight =>
        throw new UnsupportedOperationException("cannot handle >>")
      case ax : org.change.v2.p4.model.actions.primitives.Subtract =>
        val dest = params.head
        val src1 = params(1)
        val src2 = params(2)
        val dstobj = dest.asInstanceOf[FieldRef]
        toTriple(set(dstobj,
          context.mkBVSub(src1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.SubtractFromField =>
        val dstobj = params.head.asInstanceOf[FieldRef]
        val parm = new ParamExpression(dstobj)
        val src1 = paramEval(parm, isLv = false)(simpleMemory, Map.empty)
        val src2 = params(1).asInstanceOf[Z3AST]
        toTriple(set(dstobj,
          context.mkBVSub(src1._1.asInstanceOf[Z3AST], src2.asInstanceOf[Z3AST]),
          simpleMemory), s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.ModifyField =>
        val dstobj = params.head.asInstanceOf[FieldRef]
        val src2 = params(1).asInstanceOf[Z3AST]
        toTriple(set(dstobj, src2, simpleMemory),
          s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.CloneEgressPktToEgress =>
        toTriple(clone(
          2, params.head.asInstanceOf[Z3AST],
          params(1).asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.CloneEgressPktToIngress =>
        toTriple(clone(
          1, params.head.asInstanceOf[Z3AST],
          params(1).asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.CloneIngressPktToEgress =>
        toTriple(clone(
          2, params.head.asInstanceOf[Z3AST],
          params(1).asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.CloneIngressPktToIngress =>
        toTriple(clone(
          1, params.head.asInstanceOf[Z3AST],
          params(1).asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.CopyHeader =>
        copyHeader(simpleMemory, params)
      case ax : org.change.v2.p4.model.actions.primitives.Count =>
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.Drop =>
        toTriple(set(
          standardMeta("egress_spec"),
          context.mkInt(DROP_VALUE, context.mkBVSort(9)),
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.ExecuteMeter =>
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.GenerateDigest =>
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.ModifyFieldRngUniform =>
        assert(params.size == 3)
        val dstobj = params.head.asInstanceOf[FieldRef]
        val src1 = params(1).asInstanceOf[Z3AST]
        val src2 = params(2).asInstanceOf[Z3AST]
        val newast = context.mkFreshConst("random", src1.getSort)
        toTriple(set(dstobj, newast,
          simpleMemory.addCondition(context.mkBVSge(newast, src1)).
            addCondition(context.mkBVSlt(newast, src2))),
          s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.ModifyFieldWithHashBasedOffset =>
//        Logger.getLogger(this.getClass.getName).info("cannot handle " + ax.getActionName)
//        toTriple(simpleMemory, context.mkFalse(), Set.empty)
        val dstobj = params.head.asInstanceOf[FieldRef]
        val base = params(1).asInstanceOf[Z3AST]
        val sz = params(3).asInstanceOf[Z3AST]
        val hash = context.mkFreshConst("hash", base.getSort)
        val fieldVal = context.mkBVAdd(base, hash)
        val alsoAdd1 = context.mkBVSge(hash, context.mkInt(0, hash.getSort))
        val alsoAdd2 = context.mkBVSlt(hash,sz)
        val cd = context.mkAnd(alsoAdd1, alsoAdd2)
        toTriple(set(dstobj, fieldVal, simpleMemory.addCondition(cd)),
          s"cannot set $dstobj in $p4Action")
      case ax : org.change.v2.p4.model.actions.primitives.NoOp =>
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.Pop =>
        val headerRef = params.head.asInstanceOf[HeaderRef]
        val count = params(1).asInstanceOf[Z3AST]
        val ai = headerRef.getInstance().asInstanceOf[ArrayInstance]
        val ni = nextIndex(ai, simpleMemory)
        val errcond = context.mkGT(
          context.mkAdd(ni, count),
          context.mkInt(ai.getLength, ni.getSort)
        )
        val rex = s"${ai.getName}\\[[0-9]+\\]\\..*".r
        val filteredSymbols = simpleMemory.symbols
          .filterKeys(rex.findFirstIn(_).nonEmpty)
        val changed = filteredSymbols
          .foldLeft(simpleMemory)((crt, sym) => {
            val srt = sym._2.ast.get.getSort
            val nxt = context.mkFreshConst(sym._1, srt)
            simpleMemory.assignNewValue(sym._1, nxt).get
          })
        val (asgLogic, errs) = (0 until ai.getLength).map(idx => {
          val i = context.mkInt(idx, ni.getSort)
          val (valsLv, errLv) = evalWrapper.evaluate(ai,
            i, changed
          )
          val (valsRv, errRv) = evalWrapper.evaluate(ai,
            context.mkAdd(i, count), simpleMemory
          )
          val newcond = valsLv.map(lv => {
            context.mkEq(lv._2,
              context.mkITE(
                context.mkLT(count, context.mkInt(ai.getLength - idx, context.mkIntSort())),
                valsRv(lv._1),
                context.mkInt(0, lv._2.getSort)
              )
            )
          })
          val err = context.mkOr(context.mkAnd(
            context.mkLT(count, context.mkInt(ai.getLength - idx, context.mkIntSort())),
            errRv
          ), errLv)
          (newcond, err)
        }).unzip
        toTriple(
          changed.addCondition(context.mkAnd(asgLogic.flatten:_*)),
          context.mkOr(errs:_*),
          filteredSymbols.keys.toSet
        )
      case ax : org.change.v2.p4.model.actions.primitives.Push =>
        val headerRef = params.head.asInstanceOf[HeaderRef]
        val count = params(1).asInstanceOf[Z3AST]
        val ai = headerRef.getInstance().asInstanceOf[ArrayInstance]
        val ni = nextIndex(ai, simpleMemory)
        val errcond = context.mkGT(
            context.mkAdd(ni, count),
            context.mkInt(ai.getLength, ni.getSort)
          )
        val rex = s"${ai.getName}\\[[0-9]+\\]\\..*".r
        val filteredSymbols = simpleMemory.symbols
          .filterKeys(rex.findFirstIn(_).nonEmpty)
        val changed = filteredSymbols
          .foldLeft(simpleMemory)((crt, sym) => {
              val srt = sym._2.ast.get.getSort
              val nxt = context.mkFreshConst(sym._1, srt)
              simpleMemory.assignNewValue(sym._1, nxt).get
        })
        val (asgLogic, errs) = (0 until ai.getLength).map(idx => {
          val i = context.mkInt(idx, ni.getSort)
          val (valsLv, errLv) = evalWrapper.evaluate(ai,
            i, changed
          )
          val (valsRv, errRv) = evalWrapper.evaluate(ai,
            context.mkSub(i, count), simpleMemory
          )
          val newcond = valsLv.map(lv => {
            context.mkEq(lv._2,
              context.mkITE(
                context.mkLT(i, count),
                if (lv._1 == "IsValid") {
                  context.mkInt(1, lv._2.getSort)
                } else {
                  context.mkInt(0, lv._2.getSort)
                },
                valsRv(lv._1)
              )
            )
          })
          val err = context.mkOr(context.mkAnd(
            context.mkGE(i, count),
            errRv
          ), errLv)
          (newcond, err)
        }).unzip
        toTriple(
          changed.addCondition(context.mkAnd(asgLogic.flatten:_*)),
          context.mkOr(errs:_*),
          filteredSymbols.keys.toSet
        )
      case ax : org.change.v2.p4.model.actions.primitives.Recirculate =>
        toTriple(recirculate(
          4,
          params.head.asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.RegisterRead =>
        Logger.getLogger(this.getClass.getName).info("cannot handle " + ax.getActionName)
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.RegisterWrite =>
        Logger.getLogger(this.getClass.getName).info("cannot handle " + ax.getActionName)
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.primitives.RemoveHeader =>
        assert(params.size == 1)
        toTriple(setValid(params.head.asInstanceOf[HeaderRef], valid = false, simpleMemory))
      case ax : org.change.v2.p4.model.actions.primitives.Resubmit =>
        toTriple(recirculate(
          3,
          params.head.asInstanceOf[FieldListRef],
          simpleMemory
        ))
      case ax : org.change.v2.p4.model.actions.primitives.Truncate =>
        Logger.getLogger(this.getClass.getName).info("cannot handle " + ax.getActionName)
        toTriple(simpleMemory, context.mkFalse(), Set.empty)
      case ax : org.change.v2.p4.model.actions.P4ComplexAction =>
        val asamap = params.zip(ax.getParameterList.asScala)
                           .map(u => u._2.getParamName -> u._1.asInstanceOf[Z3AST]).toMap
        ax.getActionList.asScala.foldLeft((simpleMemory, List.empty[SimpleMemory], Set.empty[String]))(
          (acc, call) => {
          val (callParams, errs) = call.params().asScala
              .zip(call.getP4Action.getParameterList.asScala).map(f => {
            val (pexp : ParamExpression, p4parm : P4ActionParameter) = f
            paramEval(pexp, p4parm.isLeftValue)(simpleMemory, asamap)
          }).unzip
          val errd = errs.filter(cd => {
            context.simplifyAst(cd) != context.mkFalse
          }).map(cd => {
            acc._1.addCondition(cd).fail(s"can't read parm in action $call $p4Action")
          })
          val (out, fail, modified) = executeAction(
            call.getP4Action :: p4Action,
            acc._1,
            callParams.toList)
          val allmodified = acc._3 ++ modified
          val allfail = acc._2 ++ fail
          (out, allfail ++ errd, allmodified)
        })
    }
  }

  private def copyHeader(simpleMemory: SimpleMemory, params: List[Object]):
    (SimpleMemory, List[SimpleMemory], Set[String]) = {
    val hr1 = params.head.asInstanceOf[HeaderRef]
    val hr2 = params(1).asInstanceOf[HeaderRef]
    val (v1, e1) = isValid(hr1, simpleMemory)
    val (v2, e2) = isValid(hr2, simpleMemory)
    val dstLayout = hr1.getInstance().getLayout
    hr2.getInstance().getLayout.getFields.asScala.foldLeft(
      (simpleMemory,
        errToMems(e1, simpleMemory) ++ errToMems(e2, simpleMemory),
        Set.empty[String])
    )((acc, fld) => {
      val (simpleMemory, fails, modified) = acc
      val fref = new FieldRef().setFieldReference(fld).setHeaderRef(hr2)
      val (vl, _) = evalWrapper.evaluate(new FieldRefExpr(fref),
        simpleMemory, zeroOnInvalid = false
      )
      val dstRef = new FieldRef()
        .setFieldReference(dstLayout.getField(fld.getName))
        .setHeaderRef(hr1)
      val (oldVal, _) = evalWrapper.evaluate(new FieldRefExpr(dstRef),
        simpleMemory,
        zeroOnInvalid = false)
      val (sm1, err, mod) = toTriple(
        set(dstRef,
            context.mkITE(v2, vl, oldVal),
            simpleMemory
        )
      )
      (sm1, err ++ fails, modified ++ mod)
    })
  }

  def handleTableEntry(tableCaseEntry: TableCaseEntry,
                       simpleMemory: SimpleMemory) : SimpleMemory = {
    val lastFlow = struct.from(simpleMemory.getAST("last_flow"))

    def toAST(tce : TableCaseEntry) : Z3AST = {
      if (tce.isHitMiss) {
        if (tce.hit()) {
          lastFlow.hits()
        } else if (tce.miss()) {
          context.mkNot(lastFlow.hits())
        } else {
          context.mkAnd(
            tce.getNegated.asScala.map(x => context.mkNot(toAST(x))).toList:_*
          )
        }
      } else {
        if (tce.defaultCase()) {
          context.mkAnd(
            tce.getNegated.asScala.map(x => context.mkNot(toAST(x))).toList:_*
          )
        } else {
          val act = tce.action()
          struct.isAction(act, lastFlow.actionRun())
        }
      }
    }
    simpleMemory.addCondition(toAST(tableCaseEntry)).assignNewValue("last_flow",
      context.mkInt(0, context.mkBVSort(1))
    ).get
  }
  def handle(simpleMemory: SimpleMemory): List[SimpleMemory] = {
    val (vs, fails) = tableDeclaration.getMatches.asScala.map(m => {
      tableMatch2AST(m)(simpleMemory)
    }).unzip
    val failed = if (!tableDeclaration.getMatches.isEmpty) {
      context.mkOr(fails:_*)
    } else {
      context.mkFalse()
    }
    val failedState =
      if (context.simplifyAst(failed) != context.mkFalse())
        Some(simpleMemory.addCondition(failed : Condition).fail("keys are all wrong"))
      else None
    val flowinstance = struct.query(vs.toList)
    val crt = simpleMemory.addCondition(context.mkNot(failed))
      .assignNewValue("last_flow", flowinstance.z3AST).get
    val condfree = crt.copy(pathCondition = Nil)
    val all = struct.allowed.map(r => {
      val act = switch.action(r)
      executeAction(
        List(act),
        condfree
          .addCondition(struct.isAction(r, flowinstance.actionRun())),
        act.getParameterList.asScala.map(p => {
          flowinstance.getActionParam(r, p.getParamName)
        }).toList
      )
    })
    val allChanged = all.flatMap(x => {
      x._3
    }).toSet
    val first = all.head._1
    val failures = failedState.map(List(_)).getOrElse(Nil) ++ all.flatMap(_._2)
      .map(_.addCondition(crt.pathCondition))
      .toList
    val newsymbols = allChanged.map(ch => {
      ch -> context.mkFreshConst(ch, first.symbols(ch).ast.get.getSort)
    }).toMap
    val withCondition = all.map(l => {
      l._1.addCondition(allChanged.map(x => {
        val newast = newsymbols(x)
        context.mkEq(newast, l._1.getAST(x))
      }).toList)
    })
    val finalCondition = withCondition.foldLeft(context.mkFalse())((acc, sigma) => {
      context.mkOr(
        acc, context.mkAnd(sigma.pathCondition:_*)
      )
    })
    newsymbols.foldLeft(first)((crt, nv) => {
      crt.assignNewValue(nv._1, nv._2).get
    }).copy(pathCondition = finalCondition :: crt.pathCondition) :: failures
  }
}
