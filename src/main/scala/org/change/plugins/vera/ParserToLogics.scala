package org.change.plugins.vera

import org.change.parser.p4.control.{ASTVisitor, Traverse}
import org.change.utils.graph.{Graph, LabeledGraph}
import org.change.v2.p4.model.control.exp.FieldRefExpr
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.{ArrayInstance, HeaderInstance, ISwitchInstance, Switch}
import org.change.v3.semantics.{SimpleMemory, SimpleMemoryObject}
import smtlib.theories.Ints.IntSort
import z3.scala.Z3Context.{RecursiveType, RegularSort}
import z3.scala.{Z3AST, Z3Context, Z3FuncDecl, Z3Sort}

import scala.collection.JavaConverters._
import scala.collection.mutable

class SetEmpty(headerRef: HeaderRef) extends ASTVisitor {
  override def postorder(fieldRefExpr: FieldRefExpr): Unit = {
    if (fieldRefExpr.getFieldRef.getHeaderRef == null) {
      System.err.println("non null " +
        fieldRefExpr.getFieldRef + " " +
        fieldRefExpr.getFieldRef.hashCode())
      fieldRefExpr.getFieldRef.setHeaderRef(headerRef)
    }
  }
}

class ParserHelper(switch : Switch) {
  lazy val cfg: Graph[Statement] = buildCFG()
  lazy val lfcfg: Graph[Statement] = cfg.rmLoops(start)
  lazy val start : Statement = switch.getParserState("start").getStatements.iterator().next()
  def nextState(ret : ReturnStatement) : List[State] = {
    if (!ret.isError && switch.getParserState(ret.getWhere) != null)
      switch.getParserState(ret.getWhere) :: Nil
    else Nil
  }

  def solveEmptyFieldRefs() : Unit = {
    val lastExtract =
      mutable.Map[Statement, Option[ExtractStatement]](start -> None)
    val noloops = lfcfg
    noloops.scc().reverse.foreach(nds => {
      assert(nds.size == 1)
      val n = nds.head
      val prop = n match {
        case es : ExtractStatement => Some(es)
        case _ => lastExtract.get(n).flatten
      }
      for (neigh <- noloops.edges.getOrElse(n, Nil)) {
        if (lastExtract.get(neigh).isEmpty)
          lastExtract.put(neigh, prop)
        else {
          val neighlast = lastExtract(neigh)
          if (neighlast != prop)
            lastExtract.put(neigh, None)
        }
      }
    })
    lastExtract.filter(_._2.nonEmpty).foreach(x => {
      Traverse(new SetEmpty(x._2.get.getExpression))(x._1)
    })
  }

  lazy val unrolledCFG : Graph[(Statement, Int)] = {
    val loops = cfg.nloops(start)
    val annotated = loops.map(loop => {
      val max = loop._2._1.map {
        case es: ExtractStatement =>
          val hexp = es.getExpression
          if (hexp.isArray) {
            switch.getInstance(hexp.getPath).asInstanceOf[ArrayInstance].getLength
          } else {
            1
          }
        case _ => 0
      }.sum
      loop._1 -> (loop._2, max)
    })
    val arities = annotated.flatMap(r => r._2._1._1.map(q => q -> (r._2._2 + 1)))
    val mmap = mutable.Map.empty[(Statement, Int), List[(Statement, Int)]]
    cfg.edges.foreach(ed => {
      var src = ed._1
      ed._2.toSet[Statement].foreach(dst => {
        val isback = annotated.get(dst).exists(x => {
          x._1._2.contains(src)
        })
        if (isback) {
          val nrsrc = arities.getOrElse(src, 1)
          (0 until (nrsrc - 1)).foreach(idx => {
            val vl = mmap.getOrElseUpdate((src, idx), Nil)
            mmap.put((src, idx), (dst, idx + 1) :: vl)
          })
          mmap.put((src, nrsrc - 1), (new ReturnStatement("")
            .setError(true).setMessage("out-of-bounds"), 0) :: Nil)
        } else {
          val belongsto1 = annotated.find(an => {
            an._2._1._1.contains(src)
          })
          val belongsto2 = annotated.find(an => {
            an._2._1._1.contains(dst)
          })
          val nrsrc = arities.getOrElse(src, 1)
          if (belongsto1.equals(belongsto2) && belongsto1.nonEmpty) {
            val nrdst = arities.getOrElse(src, 1)
            assert(nrsrc == nrdst)
            (0 until nrsrc).foreach(idx => {
              val vl = mmap.getOrElse((src, idx), Nil)
              mmap.put((src, idx), (dst, idx) :: vl)
            })
          } else {
            (0 until nrsrc).foreach(idx => {
              val vl = mmap.getOrElse((src, idx), Nil)
              mmap.put((src, idx), (dst, 0) :: vl)
            })
          }
        }
      })
    })
    new Graph[(Statement, Int)](mmap.toMap)
  }

  def nextStates(ret : ReturnSelectStatement) : List[State] =
    ret.getCaseEntryList.asScala.flatMap(ce => nextState(ce.getReturnStatement)).toList
  def buildCFG() : Graph[Statement] = {
    val adjList = mutable.Map.empty[Statement, List[Statement]]
    val visited = mutable.Set.empty[org.change.v2.p4.model.parser.State]
    def doBuild(st : org.change.v2.p4.model.parser.State,
                outstanding : Option[Statement]): Unit = {
      if (!visited.contains(st)) {
        val last = st.getStatements.asScala.foldLeft(outstanding)((acc, x) => {
          acc.foreach(o => {
            val crt = adjList.getOrElse(o, Nil)
            adjList.put(o, x :: crt)
          })
          Some(x)
        })
        visited.add(st)
        last.get match {
          case rss : ReturnSelectStatement =>
            val nondef = rss.getCaseEntryList.asScala.filter(!_.isDefault).toList
            val default = rss.getCaseEntryList.asScala.filter(_.isDefault).map(ce => {
              nondef.foldLeft(new CaseNotEntry())((acc, x) => {
                acc.addCaseEntry(x)
              }).setReturnStatement(ce.getReturnStatement)
            })
            val crt = adjList.getOrElse(rss, Nil)
            adjList.put(rss, crt ++ nondef ++ default)
            (nondef ++ default).foreach {
              case ce : CaseEntry =>
                val crt = adjList.getOrElse(ce, Nil)
                adjList.put(ce, ce.getReturnStatement :: crt)
                nextState(ce.getReturnStatement).foreach(doBuild(_, Some(ce.getReturnStatement)))
              case cne : CaseNotEntry =>
                val crt = adjList.getOrElse(cne, Nil)
                adjList.put(cne, cne.getReturnStatement :: crt)
                nextState(cne.getReturnStatement).foreach(doBuild(_, Some(cne.getReturnStatement)))
            }
          case rs : ReturnStatement => nextState(rs).foreach(x => doBuild(x, last))
        }
      }
    }
    doBuild(switch.getParserState("start"), None)
    new Graph(adjList.toMap)
  }
}

case class SSAInfo[T](inv : Map[T, Map[String, Int]],
                      outv : Map[T, Map[String, Int]],
                      copy : Map[(T, T), Set[String]]) {
  def deltas() : Map[T, Map[String, (Int, Int)]] = {
    outv.foldLeft(Map.empty[T, Map[String, (Int, Int)]])((acc, out) => {
      val in = inv(out._1)
      val diff = out._2.map(o => {
        val ino = in.get(o._1)
        if (ino.isEmpty) {
          (o._1 -> (-1, o._2))
        } else {
          (o._1 -> (ino.get, o._2))
        }
      }).filter(x => x._2._2 != x._2._1)
      if (diff.nonEmpty)
        acc + (out._1 -> diff)
      else acc
    })
  }
}

trait P4Type
case class BVType(sz : Int) extends P4Type
object IntType extends P4Type
object PacketType extends P4Type
case class FixedArrayType(inner : P4Type, sz : Int) extends P4Type

case class TypeHolder(typeMapping : Map[Expression, P4Type],
                      castRequired : Map[Expression, P4Type]) {
  def isCastRequired(expression: Expression): Boolean =
    castRequired.contains(expression)

  def cast(expression: Expression): Option[Int] = {
    castRequired.get(expression).map(histype => {
      val mytype = typeMapping(expression)
      val BVType(mine) = mytype
      val BVType(his) = histype
      his - mine
    })
  }
}

class ParserToLogics(switch : Switch,
                     ifaceSpec : Map[Int, String]) {
  val context: Z3Context = org.change.v3.semantics.context
  val helper = new ParserHelper(switch)
  private val extractDefs = mutable.Map.empty[Int, Z3FuncDecl]
  val packetSort = context.mkADTSorts(Seq(
    ("Packet",
      Seq("orig", "take"),
      Seq(
        Seq(),
        Seq(("pin", RecursiveType(0)),
            ("nr", RegularSort(context.mkIntSort()))
        )
      )
    )
  )).head

  def takeFromPacket(nr : Int): Z3FuncDecl = {
    extractDefs.getOrElseUpdate(nr,
      context.mkFuncDecl(s"take_${nr}", Seq(packetSort._1, context.mkIntSort()), context.mkBVSort(nr))
    )
  }

  type VersionMap = Map[String, Int]
  class CurrencyMap() {
    val versionMap = mutable.Map.empty[String, Int]
    def next(field : String) : Int = {
      if (versionMap.contains(field)) {
        val ret = versionMap(field)
        versionMap(field) = ret + 1
        ret + 1
      } else {
        versionMap.put(field, 0)
        0
      }
    }
  }
  lazy val computeSSA: SSAInfo[(Statement, Int)] = {
    val crmap = new CurrencyMap()
    val initial = switch.getInstances.asScala.flatMap(r => {
      r.getLayout.getFields.asScala.map(fld => {
        val qname = r.getName + "." + fld.getName
        qname -> crmap.next(qname)
      }) ++ (if (!r.isMetadata) {
        val qname = r.getName + ".IsValid"
        Map(qname -> crmap.next(qname))
      } else { Map.empty }) ++ (r match {
        case ai : ArrayInstance => Map(r.getName + ".next" -> crmap.next(r.getName + ".next"))
        case _ => Map.empty
      })
    }).toMap + ("packet" -> crmap.next("packet"))
    val inv = mutable.Map((helper.start, 0) -> initial)
    val outv = mutable.Map.empty[(Statement, Int), Map[String, Int]]
    val copy = mutable.Map.empty[((Statement, Int), (Statement, Int)), Set[String]]
    helper.unrolledCFG.scc().reverse.foreach(node => {
      assert(node.size == 1)
      val n = node.head
      val crtobj = inv.get(n)
      crtobj.map(o => {
        val outo = n match {
          case (es : ExtractStatement, _) =>
            val inst = switch.getInstance(es.getExpression.getPath)
            val flds = inst.getLayout.getFields.asScala.map(_.getName) ++ Seq("IsValid")
            flds.foldLeft(o)((acc, fld) => {
              val qname = inst.getName + "." + fld
              acc + (qname -> crmap.next(qname))
            }) + ("packet" -> crmap.next("packet")) ++ (if (es.getExpression.isArray && es.getExpression.asInstanceOf[IndexedHeaderRef].isLast)
              Map(es.getExpression.getPath + ".next" -> crmap.next(es.getExpression.getPath + ".next"))
            else Map.empty)
          case (setStatement: SetStatement, _) =>
            val qname = setStatement.getLeft.getHeaderRef.getPath + "." + setStatement.getLeft.getField
            o + (qname -> crmap.next(qname))
          case _ => o
        }
        outv(n) = outo
        outo
      }).foreach(prop => {
        helper.unrolledCFG.edges.get(n).foreach(others => {
          others.foreach(dst => {
            val dstobj = inv.get(dst)
            val finalobj = if (dstobj.nonEmpty) {
              val mergeWith = dstobj.get
              prop.foldLeft(mergeWith)((acc, newversion) => {
                val oldversion = acc.get(newversion._1)
                if (oldversion.nonEmpty) {
                  if (newversion._2 != oldversion.get) {
                    acc + (newversion._1 -> crmap.next(newversion._1))
                  } else {
                    acc
                  }
                } else {
                  acc + newversion
                }
              })
            } else {
              prop
            }
            inv.put(dst, finalobj)
          })
        })
      })
    })
    helper.unrolledCFG.scc().reverse.foreach(nodes => {
      val n = nodes.head
      outv.get(n).foreach(outval => {
        helper.unrolledCFG.edges.get(n).foreach(others =>
          others.foreach(dst => {
            val copies = inv(dst).filter(f => {
              outval.get(f._1).exists(outver => {
                outver != f._2
              })
            }).keySet
            if (copies.nonEmpty) {
              copy((n, dst)) = copies
            }
          }))
      })
    })
    SSAInfo(inv.toMap, outv.toMap, copy.toMap)
  }

  lazy val typeMap : Map[String, P4Type] = {
    switch.getInstances.asScala.flatMap(inst => {
      val basic = inst.getLayout.getFields.asScala.map(fld => {
        inst.getName + "." + fld.getName -> BVType(fld.getLength)
      }).toMap ++ (if (inst.isMetadata) Map.empty
             else Map(inst.getName + ".IsValid" -> BVType(1)))
      inst match {
        case arrayInstance: ArrayInstance =>
          basic.mapValues(sort => FixedArrayType(sort, arrayInstance.getLength)) ++
            Map(arrayInstance.getName + ".next" -> IntType)
        case hi: HeaderInstance => basic
      }
    }).toMap ++ Map("packet" -> PacketType)
  }

  def type2sort(p4Type: P4Type) : Z3Sort = p4Type match {
    case BVType(x) => context.mkBVSort(x)
    case IntType => context.mkIntSort()
    case PacketType => packetSort._1
    case FixedArrayType(inner, sz) => context.mkArraySort(context.mkIntSort(), type2sort(inner))
  }
  def fundamental(p4Type: P4Type) : P4Type = p4Type match {
    case FixedArrayType(inner, _) => inner
    case _ => p4Type
  }

  lazy val computeTypes : TypeHolder = {
    val kindSorts = context.mkADTSorts(Seq(
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
    def typeToKind(p4Type: P4Type) = p4Type match {
      case BVType(x) => kindSorts.head._2(1)(context.mkInt(x, context.mkIntSort()))
    }
    val slv = context.mkSolver()
    val declmap = mutable.Map.empty[Expression, (Int, Z3AST)]
    val shouldCast = mutable.Map.empty[Expression, (Z3AST, Z3AST)]
    val currencyMap = new CurrencyMap()
    def computeFundamentalConstraints(expression: Expression) : Unit = {
      val i = currencyMap.next("")
      val mytype = declmap.getOrElseUpdate(expression, (i, context.mkConst(s"type$i", kindSorts.head._1)))
      expression match {
        case dr : DataRef =>
          slv.assertCnstr(context.mkEq(mytype._2, kindSorts.head._2(1)(context.mkInt(
            (dr.getEnd - dr.getStart).toInt, context.mkIntSort()))))
        case ce : ConstantExpression =>
        case fr : FieldRef =>
          val qname = fr.getHeaderRef.getPath + "." + fr.getField
          val thetype = fundamental(typeMap(qname))
          slv.assertCnstr(context.mkEq(mytype._2, typeToKind(thetype)))
        case lr : LatestRef =>
          val hr = latestToRef(lr)
          val mapped = new FieldRef().setField(lr.getFieldName).setHeaderRef(hr)
          computeFundamentalConstraints(mapped)
          slv.assertCnstr(context.mkEq(mytype._2, declmap(mapped)._2))
        case ce : CompoundExpression =>
          computeFundamentalConstraints(ce.getLeft)
          computeFundamentalConstraints(ce.getRight)
          val lt = declmap(ce.getLeft)
          val rt = declmap(ce.getRight)
          slv.assertCnstr(context.mkEq(lt._2, rt._2))
          slv.assertCnstr(context.mkEq(mytype._2, rt._2))
      }
    }
    helper.lfcfg.scc().foreach(r => {
      val n = r.head
      n match {
        case setStatement: SetStatement =>
          computeFundamentalConstraints(setStatement.getLeft)
          computeFundamentalConstraints(setStatement.getRight)
          val lt = declmap(setStatement.getLeft)
          val rt = declmap(setStatement.getRight)
          slv.assertCnstr(context.mkOr(
            context.mkEq(lt._2, rt._2),
            context.mkAnd(kindSorts.head._3(1)(lt._2),
              kindSorts.head._3(1)(rt._2))
          ))
          shouldCast.getOrElseUpdate(setStatement.getRight,
            (context.mkNot(context.mkEq(lt._2, rt._2)), lt._2))
        case ce : CaseEntry => ce.getExpressions.asScala.foreach(expr => {
          computeFundamentalConstraints(expr)
        })
        case _ => ;
      }
    })
    if (!slv.check().get) {
      throw new IllegalStateException("no type unification achievable. fix parser")
    } else {
      val typemap = mutable.Map.empty[Expression, P4Type]
      val castmap = mutable.Map.empty[Expression, P4Type]
      val model = slv.getModel()
      val is_bv = kindSorts.head._3(1)
//      val is_int = kindSorts.head._3(0)
//      val is_arr = kindSorts.head._3(2)
      val bv_extractor = kindSorts.head._4(1).head
      for (d <- declmap) {
        val ast = model.evalAs[Boolean](is_bv(d._2._2))
        if (ast.nonEmpty) {
          if (ast.get) {
            val sz = model.evalAs[Int](bv_extractor(d._2._2))
            if (sz.nonEmpty) {
              val maybecast = shouldCast.get(d._1)
              val doicast = maybecast.exists(m => {
                model.evalAs[Boolean](m._1).getOrElse(false)
              })
              if (doicast) {
                val castto = model.evalAs[Int](bv_extractor(maybecast.get._2))
                if (d._1.isInstanceOf[ConstantExpression]) {
//                  System.err.println(s"type inferred for ${d._1} = bv${castto.get}")
                  typemap.put(d._1, BVType(castto.get))
                } else {
                  typemap.put(d._1, BVType(sz.get))
                  castmap.put(d._1, BVType(castto.get))
//                  System.err.println(s"type inferred for ${d._1} = bv${sz.get}, need to cast to bv${castto.get}")
                }
              } else {
                typemap.put(d._1, BVType(sz.get))
//                System.err.println(s"type inferred for ${d._1} = bv${sz.get}")
              }
            } else {
              throw new IllegalStateException(s"can't infer bitwidth for ${d._1}")
            }
          } else {
//            System.out.println(s"non bv type inferred for ${d._1} = ${ast.get}")
            throw new IllegalStateException(s"non bv type inferred for ${d._1} = ${ast.get}")
          }
        } else {
          throw new IllegalStateException(s"no type inferred for ${d._1}")
        }
      }
      TypeHolder(typemap.toMap, castmap.toMap)
    }
  }

  def evalR(expression: Expression)
          (version : Map[String, Int]) : Z3AST = {
    val orig = expression match {
      case dr : DataRef =>
        val len = (dr.getEnd - dr.getStart).toInt
        val qname = "packet." + version("packet")
        val offset = context.mkInt(dr.getStart.toInt, context.mkIntSort())
        takeFromPacket(len)(context.mkConst(qname, packetSort._1), offset)
      case ce : ConstantExpression =>
        val tp = type2sort(computeTypes.typeMapping(ce))
        context.mkNumeral(ce.getValue.toString, tp)
      case fr : FieldRef =>
        val qname = fr.getHeaderRef.getPath + "." + fr.getField
        val vr = version(qname)
        val versioned = qname + "_" + vr
        val thetype = typeMap(qname)
        val srt = type2sort(thetype)
        if (fr.getHeaderRef.isArray) {
          val nxtref = fr.getHeaderRef.getPath + ".next"
          val nextver = version(nxtref)
          val idhr = fr.getHeaderRef.asInstanceOf[IndexedHeaderRef]
          context.mkSelect(context.mkConst(versioned, srt),
            if (idhr.isLast) {
              context.mkSub(
                context.mkIntConst(nxtref + "_" + nextver),
                context.mkInt(1, context.mkIntSort())
              )
            } else {
              context.mkInt(idhr.getIndex, context.mkIntSort())
            })
        } else {
          context.mkConst(versioned, srt)
        }
      case lr : LatestRef =>
        val hr = latestToRef(lr)
        evalR(new FieldRef().setField(lr.getFieldName).setHeaderRef(hr))(version)
      case ce : CompoundExpression =>
        val r = evalR(ce.getRight)(version)
        val l = evalR(ce.getLeft)(version)
        if (ce.isPlus)
          context.mkBVAdd(r, l)
        else context.mkBVSub(l, r)
    }
    doCast(expression, orig)
  }

  private def doCast(expression: Expression, orig: Z3AST): Z3AST = {
    if (computeTypes.isCastRequired(expression)) {
      val BVType(mine) = computeTypes.typeMapping(expression)
      val thecast = computeTypes.cast(expression).getOrElse(0)
      if (thecast < 0) {
        context.mkExtract(mine + thecast, 0, orig)
      } else if (thecast > 0) {
        context.mkZeroExt(thecast, orig)
      } else {
        orig
      }
    } else orig
  }

  def getLatestExpr(expression : Expression) : Set[LatestRef] = expression match {
    case lr : LatestRef => Set(lr)
    case compoundExpression: CompoundExpression =>
      getLatestExpr(compoundExpression.getLeft) ++
        getLatestExpr(compoundExpression.getRight)
    case _ => Set.empty
  }

  def getLatest(statement : Statement) : Set[LatestRef] = statement match {
    case ce : CaseEntry =>
      ce.getExpressions.asScala.flatMap(getLatestExpr).toSet
    case setStatement: SetStatement =>
      getLatestExpr(setStatement.getRight)
    case _ => Set.empty
  }

  lazy val latestToRef : Map[LatestRef, HeaderRef] = {
    val lastExtract =
      mutable.Map[Statement, Option[ExtractStatement]](helper.start -> None)
    val noloops = helper.lfcfg
    noloops.scc().reverse.foreach(nds => {
      assert(nds.size == 1)
      val n = nds.head
      val prop = n match {
        case es : ExtractStatement => Some(es)
        case _ => lastExtract.get(n).flatten
      }
      for (neigh <- noloops.edges.getOrElse(n, Nil)) {
        if (lastExtract.get(neigh).isEmpty)
          lastExtract.put(neigh, prop)
        else {
          val neighlast = lastExtract(neigh)
          if (neighlast != prop)
            lastExtract.put(neigh, None)
        }
      }
    })
    lastExtract.filter(_._2.nonEmpty).flatMap(r => {
      getLatest(r._1).map(lr => lr -> r._2.get.getExpression)
    }).toMap
  }

  def transform(node : (Statement, Int)) : Z3AST = {
    val inv = computeSSA.inv(node)
    val outv = computeSSA.outv(node)
    transform(node, inv, outv)
  }

  def testTransforms() : Unit = {
    helper.unrolledCFG.scc().reverse.filter(node => {
      val n = node.head
      n._1.isInstanceOf[ExtractStatement]
    }).foreach(node => {
      println(node.head + " -> " + transform(node.head))
    })
  }

  def getNext(headerRef: HeaderRef, inipack : Map[String, Z3AST]): Z3AST = {
    val ihr = headerRef.asInstanceOf[IndexedHeaderRef]
    if (ihr.isLast) {
      inipack(headerRef.getPath + ".next")
    } else {
      context.mkInt(ihr.getIndex, context.mkIntSort())
    }
  }

  private def transform(node: (Statement, Int),
                        inv: Map[String, Int],
                        outv: Map[String, Int]) : Z3AST = {
    node._1 match {
      case setStatement: SetStatement =>
        val rast = evalR(setStatement.getRight)(inv)
        val last = evalR(setStatement.getLeft)(outv)
        context.mkEq(last, rast)
      case caseEntry: CaseEntry =>
        context.mkAnd(
          caseEntry.getExpressions.asScala.zip(caseEntry.getValues.asScala).map(a => {
            val ast1 = evalR(a._1)(inv)
            val t1 = type2sort(computeTypes.typeMapping(a._1))
            val ast2 = context.mkNumeral(a._2.getValue.toString, t1)
            val ast3 = context.mkNumeral(a._2.getMask.toString, t1)
            context.mkEq(context.mkBVAnd(ast1, ast3),
              context.mkBVAnd(ast2, ast3))
          }): _*
        )
      case caseNotEntry: CaseNotEntry =>
        context.mkAnd(caseNotEntry.getCaseEntryList.asScala.map(ce => {
          context.mkNot(transform((ce, 0), inv, outv))
        }):_*)
      case extractStatement: ExtractStatement =>
        val href = extractStatement.getExpression
        val nm = href.getPath
        val inipack = inv.map(x => {
          val tp = type2sort(typeMap(x._1))
          x._1 -> context.mkConst(x._1 + "_" + x._2, tp)
        })
        val validref = nm + ".IsValid"
        val exit = switch.getInstance(href.getPath)
          .getLayout
          .getFields
          .asScala
          .foldLeft(inipack)((acc, fld) => {
            val totakefrom = takeFromPacket(fld.getLength)(acc("packet"),
              context.mkInt(0, context.mkIntSort())
            )
            val qname = nm + "." + fld.getName
            val astcrt = acc(qname)
            val prchange = if (href.isArray) {
              val idhr = href.asInstanceOf[IndexedHeaderRef]
              acc + (qname -> context.mkStore(astcrt,
                  getNext(href, acc),
                  totakefrom))
            } else {
              acc + (qname -> totakefrom)
            }
            prchange + ("packet" -> packetSort._2(1)(acc("packet"),
              context.mkInt(fld.getLength, context.mkIntSort())
            ))
        }) + (validref -> {
          if (href.isArray) {
            context.mkStore(inipack(validref),
              getNext(href, inipack),
              context.mkInt(1, context.mkBVSort(1)))
          } else {
            context.mkInt(1, context.mkBVSort(1))
          }
        }) ++ (if (href.isArray) {
          val ihr = href.asInstanceOf[IndexedHeaderRef]
          if (ihr.isLast)
            Map(href.getPath + ".next" ->
              context.mkAdd(inipack(href.getPath + ".next"),
                context.mkInt(1, context.mkIntSort()))
            )
          else Map.empty
        } else {
          Map.empty
        })
        val guards = if (href.isArray) {
          val ihr = href.asInstanceOf[IndexedHeaderRef]
          if (ihr.isLast) {
            val inst = switch.getInstance(href.getPath).asInstanceOf[ArrayInstance]
            val astlen = context.mkInt(inst.getLength, context.mkIntSort())
            context.mkLT(inipack(href.getPath + ".next"), astlen) :: Nil
          } else Nil
        } else {
          context.mkNot(context.mkEq(inipack(validref),
            context.mkInt(1, context.mkBVSort(1)))) :: Nil
        }
        context.mkAnd(outv.filter(kv => !inv.get(kv._1).contains(kv._2)).map(x => {
          val lastval = exit(x._1)
          val tp = type2sort(typeMap(x._1))
          context.mkEq(
            context.mkConst(x._1 + "_" + x._2, tp),
            exit(x._1))
        }).toList ++ guards:_*)
      case _ => context.mkTrue()
    }
  }

  lazy val getInitialsAsMap: Map[String, Z3AST] = {
    switch.getInstances.asScala.flatMap(hi => {
      val lay = hi.getLayout
      val meta = hi.isMetadata
      val arrlen = hi match {
        case ai : ArrayInstance => ai.getLength
        case _ => 0
      }
      val flds = lay.getFields.asScala.map(f => (f.getName, f.getLength)) ++
        (if (!meta) ("IsValid", 1) :: Nil else Nil)
      flds.map(f => {
        val qname = hi.getName + "." + f._1
        val tp = type2sort(fundamental(typeMap(qname)))
        def mkOne() : Z3AST = {
          context.mkInt(0, tp)
        }
        qname -> (if (arrlen != 0) {
          context.mkConstArray(context.mkIntSort(),
            mkOne()
          )
        } else {
          mkOne()
        })
      }).toMap ++ (if (arrlen != 0) {
        val nxt = hi.getName + ".next"
        Map(nxt -> context.mkInt(0, context.mkIntSort()))
      } else {
        Map.empty
      })
    }).toMap
  }

  def extraAssumptions(ver : Map[String, Int]) : List[Z3AST] = {
    val ingressPortName = "ingress_metadata.ingress_port"
    val iptype = type2sort(typeMap(ingressPortName))
    context.mkOr(ifaceSpec.keySet.map(port => {
      context.mkEq(
        produceAST(ingressPortName, ver(ingressPortName)),
        context.mkInt(port.intValue(), iptype)
      )
    }).toList:_*) :: Nil
  }

  lazy val initialFormula : Z3AST = {
    val ssa = computeSSA
    val init = ssa.inv((helper.start, 0))
    context.mkAnd(getInitialsAsMap.map(x => {
      context.mkEq(
        context.mkConst(x._1 + "_" + init(x._1), type2sort(typeMap(x._1))),
        x._2
      )
    }).toList ++ extraAssumptions(init):_*)
  }

  def produceAST(nm : String, ver : Int): Z3AST = {
    val tp = type2sort(typeMap(nm))
    context.mkConst(nm + "_" + ver, tp)
  }

  def produceTerminalState(statement: Statement, nr : Int = 0) : SimpleMemory = {
    val node = (statement, nr)
    val term = terminalCondition(node)
    val ssa = computeSSA.inv(node)
    val sm = SimpleMemory(pathCondition = term :: Nil)
      .Allocate("packet", 0).Allocate("last_flow", 0)
      .assignNewValue("packet", produceAST("packet", ssa("packet"))).get
      .assignNewValue("last_flow", context.mkInt(0, context.mkBVSort(1))).get
    switch.getInstances.asScala.foldLeft(sm)((sm, h) => {
      val flds = h.getLayout.getFields.asScala.map(f => (f.getName, f.getLength)) ++
        (if (h.isMetadata) {
          Nil
        } else {
          ("IsValid", 1) :: Nil
        })
      h match {
        case ai : ArrayInstance =>
          val nxtfld = ai.getName + ".next"
          val newsm = sm.copy(
            symbols = sm.symbols + (nxtfld ->
              SimpleMemoryObject(Some(produceAST(nxtfld, ssa(nxtfld))), 32)
              )
          )
          val len = ai.getLength
          (0 until len).foldLeft(newsm)((acc, idx) => {
            val crt = ai.getName + s"[$idx]"
            val oldid = ai.getName
            flds.foldLeft(acc)((acc2, fdef) => {
              val qname = crt + "." + fdef._1
              val canonical = oldid + "." + fdef._1
              val width = fdef._2
              acc2.copy(symbols = acc2.symbols +
                (qname -> SimpleMemoryObject(Some(context.mkSelect(produceAST(canonical, ssa(canonical)),
                  context.mkInt(idx, context.mkIntSort())
                )), width))
              )
            })
          })
        case hi : HeaderInstance =>
          val crt = hi.getName
          flds.foldLeft(sm)((acc, fdef) => {
            val canonical = crt + "." + fdef._1
            val width = fdef._2
            acc.copy(symbols = acc.symbols + (canonical -> SimpleMemoryObject(
              Some(produceAST(canonical, ssa(canonical))), width)))
          })
      }
    })
  }

  lazy val terminalCondition: Map[(Statement, Int), Z3AST] = {
    val propd = reachabilityConditions()
    helper.unrolledCFG.terminals.map(x => x -> propd(x)).toMap
  }
  def reachabilityConditions() : Map[(Statement, Int), Z3AST] = {
    val crtreach = mutable.Map[(Statement, Int), Z3AST](
      (helper.start, 0) -> initialFormula
    )
    helper.unrolledCFG.scc().reverse.foreach(nd => {
      val n = nd.head
      crtreach.get(n).foreach(crtfi => {
        val outbound = context.mkAnd(transform(n), crtfi)
        for (neigh <- helper.unrolledCFG.edges.getOrElse(n, Nil)) {
          if (crtreach.contains(neigh)) {
            crtreach(neigh) = context.mkOr(
              crtreach(neigh), outbound)
          } else {
            crtreach(neigh) = outbound
          }
        }
      })
    })
    crtreach.toMap
  }

}
