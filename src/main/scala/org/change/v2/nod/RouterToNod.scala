package org.change.v2.nod

import com.microsoft.z3._
import org.change.v2.abstractnet.optimized.router.OptimizedRouter.generateConditionsPerPort
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.equivalence.Equivalence
import org.change.v2.analysis.executor.OVSExecutor
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.{:-:, :@}
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.{SimpleMemory, State}
import org.change.v2.analysis.memory.State._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.memory.State._
import org.change.v2.util.canonicalnames._
import z3.scala.Z3Solver

import scala.collection.mutable

class Router(val router: String,
             table: Iterable[((Long, Long), String)],
             decTtl: Boolean = false) {

  val portEntries = mutable.Map.empty[String, List[List[Int]]]
  val ipList = mutable.ListBuffer.empty[(Long, Long)]
  for (((ip, mask), port) <- table) {
    val masked = ip & mask
    val pcs = ipList.zipWithIndex.filter(r => {
      r._1._2 > mask && (r._1._1 & mask) == masked
    })
    val asserted = ipList.size
    val constrain = asserted :: pcs.map(_._2).toList
    val crt = portEntries.getOrElse(port, Nil)
    portEntries.put(port, constrain :: crt)
    ipList.append((ip, mask))
  }

  def exitPort(x: String): String = router + x + "_EXIT"

  lazy val exitPorts: Map[String, String] =
    portEntries.keySet.map(x => x -> exitPort(x)).toMap
  def enter(): String = router

  def sefl(): Instruction =
    Fork(generateConditionsPerPort(table).map(h => {
      InstructionBlock(
        Assume(Fork(h._2)),
        if (decTtl) {
          Assign(TTL, :-:(:@(TTL), ConstantValue(1)))
        } else {
          NoOp
        },
        Forward(exitPort(h._1))
      )
    }))

  def generateDatalog(context: Context, fixedpoint: Fixedpoint): Unit = {
    var idx: Int = 3
    val A =
      context.mkBound(idx, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val B = context
      .mkBound(idx + 1, context.mkBitVecSort(8))
      .asInstanceOf[BitVecExpr]
    val E = context.mkBound(5, context.mkBitVecSort(8))
      .asInstanceOf[BitVecExpr]
    idx = idx + 2
    val entryFdecl = context.mkFuncDecl(enter(),
                                        List[Sort](
                                          context.mkBitVecSort(32),
                                          context.mkBitVecSort(8)
                                        ).toArray,
                                        context.mkBoolSort())
    fixedpoint.registerRelation(entryFdecl)
    for ((port, indices) <- portEntries) {
      val q = indices.map(f => {
        val (ip, mask) = ipList(f.head)
        val negd = f.tail
          .map(ipList)
          .map(v => {
            context.mkNot(
              context.mkEq(context.mkBVAND(
                             A,
                             context.mkBV(v._2, 32)
                           ),
                           context.mkBV(v._1, 32)))
          })
        val asserted = context.mkEq(context.mkBVAND(
                                      A,
                                      context.mkBV(mask, 32)
                                    ),
                                    context.mkBV(ip, 32))
        if (negd.isEmpty)
          asserted
        else
          context.mkAnd(
            asserted :: negd: _*
          )
      })

      val ord = context.mkOr(q: _*)
      val whatGives = context.mkFuncDecl(exitPort(port),
                                         List[Sort](
                                           context.mkBitVecSort(32),
                                           context.mkBitVecSort(8)
                                         ).toArray,
                                         context.mkBoolSort())
      fixedpoint.registerRelation(whatGives)

      val rl = if (decTtl) {
        context.mkImplies(
          context.mkAnd(
            context.mkApp(entryFdecl, A, B).asInstanceOf[BoolExpr],
            context.mkEq(E, context.mkBVSub(B, context.mkBV(1, 8))),
            ord
          ),
          context
            .mkApp(whatGives, A, E/*context.mkBVSub(B, context.mkBV(1, 8))*/)
            .asInstanceOf[BoolExpr]
        )
      } else {
        context.mkImplies(
          context.mkAnd(
            context.mkApp(entryFdecl, A, B).asInstanceOf[BoolExpr],
            ord
          ),
          context
            .mkApp(whatGives, A, B)
            .asInstanceOf[BoolExpr]
        )
      }

      fixedpoint.addRule(rl, null)
    }
  }
}

object RouterToNod {

  def outputEquivalence(slv : Z3Solver, s1 : SimpleMemory, s2 : SimpleMemory) : Boolean = {
    val start = System.currentTimeMillis()
    val trans = new memory.SimpleMemory.Translator(slv.context, slv)
    s2.pathCondition.cd match {
      case FAND(cds) =>
        cds.foreach(r => slv.assertCnstr(trans.createAST(r)))
      case _ => slv.assertCnstr(trans.createAST(s2.pathCondition.cd))
    }

    val layoutEquiv = s1.memTags == s2.memTags && s1.rawObjects.keySet == s2.rawObjects.keySet
    val eq = if (layoutEquiv) {
      val mustBeEqual = List(
        IPDst, IPSrc, Proto, EtherDst, EtherSrc, EtherType, TTL
      )
      val or = FOR.makeFOR(mustBeEqual.map(r => {
        s1.eval(r).map(h => {
          FNOT.makeFNOT(OP(s1.rawObjects(h).expression, EQ_E(s2.rawObjects(h).expression), s1.rawObjects(h).size))
        }).getOrElse(FALSE)
      }))
      if (or == FALSE) {
        // sounds stupid, but it means that no expression was correctly evaluated
        true
      } else {
        slv.assertCnstr(trans.createAST(or))
        !slv.check().get
      }
    } else {
      false
    }
    val end = System.currentTimeMillis()
    eq
  }

  def testEquivSEFL(router1: Router, router2: Router, woutput : Boolean) = {
    val outputEq = (slv: Z3Solver, m1: SimpleMemory, m2: SimpleMemory) => if (woutput) {
      true
    } else {
      outputEquivalence(slv, m1, m2)
    }
    val (init, eq, opc) = generateSeflContext(router1, router2)
    val start = System.currentTimeMillis()
    val bad = eq.show(init.map(SimpleMemory.apply),
                      List((router1.enter(), router2.enter())),
                      opc,
                      outputEq)
    val end = System.currentTimeMillis()
    println(
      (if (woutput) "eq,out," else "eq,noout,") +
        router1.ipList.size + "," +
        router2.ipList.size + "," +
        (bad._1.size + bad._2.size + bad._3.size) + "," +
        (end - start))
  }

  private def generateSeflContext(router1: Router, router2: Router) = {
    val init = new OVSExecutor(new Z3BVSolver())
      .execute(
        InstructionBlock(
          State.start,
          State.ehervlan,
          State.ipSymb,
          Assign(TTL, SymbolicValue("ttl"))
        ),
        State.clean,
        true
      )
      ._1
    val i1 = Map(router1.enter() -> router1.sefl())
    val i2 = Map(router2.enter() -> router2.sefl())
    val eq = new Equivalence(i1, i2)
    val outPorts = (router1.portEntries.keySet ++ router2.portEntries.keySet)
      .map(h => {
        router1.exitPort(h) -> router2.exitPort(h)
      })
      .toMap
    val opc = (p1: String, p2: String) => {
      outPorts.contains(p1) && outPorts(p1) == p2
    }
    (init, eq, opc)
  }

  def testEquivDatalog(routerToNod1: Router, routerToNod2: Router, woutput : Boolean) = {
    val context = new Context()
    val p = context.mkParams
    p.add("fixedpoint.engine", "datalog")
    p.add("fixedpoint.datalog.default_relation", "doc")
    p.add("fixedpoint.print_answer", true)
    val fpoint = context.mkFixedpoint()
    fpoint.setParameters(p)
    val diff = createDiff(context, fpoint)
    routerToNod1.generateDatalog(context, fpoint)
    routerToNod2.generateDatalog(context, fpoint)
    makeLocationEQQuery(routerToNod1, routerToNod2, context, fpoint, diff)
    if (woutput) {
      makeOutputQuery(routerToNod1, routerToNod2, context, fpoint, diff)
    }
    entryPointsAndVars(routerToNod1, routerToNod2, context, fpoint)
    val start = System.currentTimeMillis()
    val stat = fpoint.query(List(diff).toArray)
    val end = System.currentTimeMillis()
    println(
      (if (woutput) "nod,out," else "nod,noout,") +
        routerToNod1.ipList.size + "," +
        routerToNod2.ipList.size + "," +
        stat + "," +
        (end - start))
    context.close()
  }

  private def entryPointsAndVars(routerToNod1: Router,
                                 routerToNod2: Router,
                                 context: Context,
                                 fpoint: Fixedpoint) = {
    val iPDst = context.mkBound(1, context.mkBitVecSort(32))
    val ttl = context.mkBound(2, context.mkBitVecSort(8))

    val entryFdecl1 = context.mkFuncDecl(routerToNod1.router,
                                         List[Sort](
                                           context.mkBitVecSort(32),
                                           context.mkBitVecSort(8)
                                         ).toArray,
                                         context.mkBoolSort())
    fpoint.addRule(
      context.mkImplies(
        context.mkTrue(),
        context.mkApp(entryFdecl1, iPDst, ttl).asInstanceOf[BoolExpr]
      ),
      null)
    fpoint.registerRelation(entryFdecl1)

    val entryFdecl2 = context.mkFuncDecl(routerToNod2.router,
                                         List[Sort](
                                           context.mkBitVecSort(32),
                                           context.mkBitVecSort(8)
                                         ).toArray,
                                         context.mkBoolSort())
    fpoint.registerRelation(entryFdecl2)
    fpoint.addRule(
      context.mkImplies(
        context.mkTrue(),
        context.mkApp(entryFdecl2, iPDst, ttl).asInstanceOf[BoolExpr]
      ),
      null)
  }

  private def makeOutputQuery(routerToNod1: Router,
                                  routerToNod2: Router,
                                  context: Context,
                                  fpoint: Fixedpoint,
                                  diff: FuncDecl) = {
    val A =
      context.mkBound(3, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val B =
      context.mkBound(4, context.mkBitVecSort(8)).asInstanceOf[BitVecExpr]
    val C =
      context.mkBound(5, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val D =
      context.mkBound(6, context.mkBitVecSort(8)).asInstanceOf[BitVecExpr]
    (routerToNod1.exitPorts.keySet ++ routerToNod2.exitPorts.keySet)
      .foreach(h => {
        val r1 = routerToNod1.exitPort(h)
        val r2 = routerToNod2.exitPort(h)
        val whatGives1 = context.mkFuncDecl(r1,
          List[Sort](
            context.mkBitVecSort(32),
            context.mkBitVecSort(8)
          ).toArray,
          context.mkBoolSort())
        val whatGives2 = context.mkFuncDecl(r2,
          List[Sort](
            context.mkBitVecSort(32),
            context.mkBitVecSort(8)
          ).toArray,
          context.mkBoolSort())
        if (!routerToNod1.exitPorts.contains(r2)) {
          fpoint.registerRelation(whatGives2)
        }
        if (!routerToNod2.exitPorts.contains(r1)) {
          fpoint.registerRelation(whatGives1)
        }
        val a1 = context.mkAnd(
          context.mkApp(whatGives1, A, B).asInstanceOf[BoolExpr],
          context.mkApp(whatGives2, A, D).asInstanceOf[BoolExpr],
          context.mkNot(context.mkEq(B, D))
        )
        fpoint.addRule(context.mkImplies(
          a1,
          context.mkApp(diff, A, B).asInstanceOf[BoolExpr]
        ),
          null)
      })
  }


  private def makeLocationEQQuery(routerToNod1: Router,
                                  routerToNod2: Router,
                                  context: Context,
                                  fpoint: Fixedpoint,
                                  diff: FuncDecl) = {
    val A =
      context.mkBound(3, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val B =
      context.mkBound(4, context.mkBitVecSort(8)).asInstanceOf[BitVecExpr]
    (routerToNod1.exitPorts.keySet ++ routerToNod2.exitPorts.keySet)
      .foreach(h => {
        val r1 = routerToNod1.exitPort(h)
        val r2 = routerToNod2.exitPort(h)
        val whatGives1 = context.mkFuncDecl(r1,
                                            List[Sort](
                                              context.mkBitVecSort(32),
                                              context.mkBitVecSort(8)
                                            ).toArray,
                                            context.mkBoolSort())
        val whatGives2 = context.mkFuncDecl(r2,
                                            List[Sort](
                                              context.mkBitVecSort(32),
                                              context.mkBitVecSort(8)
                                            ).toArray,
                                            context.mkBoolSort())
        if (!routerToNod1.exitPorts.contains(r2)) {
          fpoint.registerRelation(whatGives2)
        }
        if (!routerToNod2.exitPorts.contains(r1)) {
          fpoint.registerRelation(whatGives1)
        }
        val a1 = context.mkAnd(
          context.mkApp(whatGives1, A, B).asInstanceOf[BoolExpr],
          context.mkNot(context.mkApp(whatGives2, A, B).asInstanceOf[BoolExpr])
        )
        val a2 = context.mkAnd(
          context.mkNot(context.mkApp(whatGives2, A, B).asInstanceOf[BoolExpr]),
          (context.mkApp(whatGives1, A, B).asInstanceOf[BoolExpr])
        )
        fpoint.addRule(context.mkImplies(
                         a1,
                         context.mkApp(diff, A, B).asInstanceOf[BoolExpr]
                       ),
                       null)
        fpoint.addRule(context.mkImplies(
                         a2,
                         context.mkApp(diff, A, B).asInstanceOf[BoolExpr]
                       ),
                       null)
      })
  }

  private def createDiff(context: Context, fpoint: Fixedpoint): FuncDecl = {
    val diff = context.mkFuncDecl("diff",
                                  List[Sort](
                                    context.mkBitVecSort(32),
                                    context.mkBitVecSort(8)
                                  ).toArray,
                                  context.mkBoolSort())
    fpoint.registerRelation(diff)
    diff
  }
}
