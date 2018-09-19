import java.io.{BufferedOutputStream, File, FileOutputStream, PrintStream}

import com.microsoft.z3._
import org.change.utils.RepresentationConversion
import org.change.utils.prettifier.JsonUtil
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.abstractnet.optimized.router.OptimizedRouter.getRoutingEntriesForBV
import org.change.v2.analysis.constraint._
import org.change.v2.analysis.equivalence.Equivalence
import org.change.v2.analysis.executor.solvers.Z3BVSolver
import org.change.v2.analysis.executor.{CodeAwareInstructionExecutor, OVSExecutor}
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:&&:, :-:, :@}
import org.change.v2.analysis.memory
import org.change.v2.analysis.memory.State._
import org.change.v2.analysis.processingmodels.instructions._
import org.scalatest.FunSuite
import org.change.v2.analysis.memory._
import org.change.v2.analysis.processingmodels.Instruction
import spray.json.JsArray
import spray.json._
import z3.scala.Z3Solver
import org.change.v2.util.canonicalnames._

import scala.collection.mutable


class EquivalenceTester extends FunSuite {

  def generateDatalog(context : Context,
                      fixedpoint: Fixedpoint,
                      router : String,
                      table : Iterable[((Long, Long), String)]): Iterable[String] = {
    var idx : Int = 3
    val portEntries = mutable.Map.empty[String, List[BoolExpr]]
    val ipList = mutable.Buffer.empty[(Long, Long)]
    val A = context.mkBound(idx, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val B = context.mkBound(idx + 1, context.mkBitVecSort(16)).asInstanceOf[BitVecExpr]
    idx = idx + 2

    for (((ip, mask), port) <- table) {
      val masked = ip & mask
      val pcs = ipList.filter(r => {
        r._2 > mask && (r._1 & mask) == masked
      })
      val asserted = context.mkEq(context.mkBVAND(
        A, context.mkBV(mask, 32)
      ), context.mkBV(ip, 32))
      val constrain = if (pcs.isEmpty)
        asserted
      else
        context.mkAnd(
          asserted :: pcs.map(v => {
            context.mkNot(context.mkEq(context.mkBVAND(
              A, context.mkBV(v._2, 32)
            ), context.mkBV(v._1, 32)))
          }).toList:_*
        )
      val crt = portEntries.getOrElse(port, Nil)
      portEntries.put(port, constrain :: crt)
      ipList.append((ip, mask))
    }
    val entryFdecl = context.mkFuncDecl(router, List[Sort](
      context.mkBitVecSort(32), context.mkBitVecSort(16)
    ).toArray, context.mkBoolSort())
    fixedpoint.registerRelation(entryFdecl)

    portEntries.map(h => {
      val ord = context.mkOr(h._2:_*)
      val whatGives = context.mkFuncDecl(router + "_" + h._1 + "_EXIT", List[Sort](
        context.mkBitVecSort(32), context.mkBitVecSort(16)
      ).toArray, context.mkBoolSort())
      fixedpoint.registerRelation(whatGives)
      val rl = context.mkImplies(
        context.mkAnd(
          context.mkApp(entryFdecl, A, B).asInstanceOf[BoolExpr],
          ord
        ), context.mkApp(whatGives, A, B).asInstanceOf[BoolExpr]
      )
      fixedpoint.addRule(rl, null)
      h._1
    })
  }

  test("basic vs basic datalog") {
    val routerFile = "routing_tables/medium.txt"
    val routerFile2 = "routing_tables/medium.txt"
    val table = getRoutingEntriesForBV(new File(routerFile))
    val table2 = getRoutingEntriesForBV(new File(routerFile2))
    val context = new Context()
    val A = context.mkBound(3, context.mkBitVecSort(32)).asInstanceOf[BitVecExpr]
    val B = context.mkBound(4, context.mkBitVecSort(16)).asInstanceOf[BitVecExpr]
    val p = context.mkParams
    p.add("fixedpoint.engine", "datalog")
    p.add("fixedpoint.datalog.default_relation", "doc")
    p.add("fixedpoint.print_answer", true)
    val fpoint = context.mkFixedpoint()
    fpoint.setParameters(p)
    val exits1 = generateDatalog(context, fpoint, "R0", table).toSet
    val exits2 = generateDatalog(context, fpoint, "R1", table2).toSet
    val diff = context.mkFuncDecl("diff", List[Sort](
      context.mkBitVecSort(32), context.mkBitVecSort(16)
    ).toArray, context.mkBoolSort())
    fpoint.registerRelation(diff)

    (exits1 ++ exits2).map(h => {
      val r1 = "R0" + "_" + h + "_EXIT"
      val r2 = "R1" + "_" + h + "_EXIT"
      val whatGives1 = context.mkFuncDecl(r1, List[Sort](
        context.mkBitVecSort(32), context.mkBitVecSort(16)
      ).toArray, context.mkBoolSort())
      val whatGives2 = context.mkFuncDecl(r2, List[Sort](
        context.mkBitVecSort(32), context.mkBitVecSort(16)
      ).toArray, context.mkBoolSort())
      if (!exits1.contains(r2)) {
        fpoint.registerRelation(whatGives2)
      }
      if (!exits2.contains(r1)) {
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
        a1, context.mkApp(diff, A, B).asInstanceOf[BoolExpr]
      ), null)
      fpoint.addRule(context.mkImplies(
        a2, context.mkApp(diff, A, B).asInstanceOf[BoolExpr]
      ), null)
    })

    val iPDst = context.mkBound(1, context.mkBitVecSort(32))
    val ttl = context.mkBound(2, context.mkBitVecSort(16))

    val entryFdecl1 = context.mkFuncDecl("R0", List[Sort](
      context.mkBitVecSort(32), context.mkBitVecSort(16)
    ).toArray, context.mkBoolSort())
    fpoint.addRule(context.mkImplies(
      context.mkTrue(),
      context.mkApp(entryFdecl1, iPDst, ttl).asInstanceOf[BoolExpr]
    ), null)
    fpoint.registerRelation(entryFdecl1)

    val entryFdecl2 = context.mkFuncDecl("R1", List[Sort](
      context.mkBitVecSort(32), context.mkBitVecSort(16)
    ).toArray, context.mkBoolSort())
    fpoint.registerRelation(entryFdecl1)
    fpoint.addRule(context.mkImplies(
      context.mkTrue(),
      context.mkApp(entryFdecl2, iPDst, ttl).asInstanceOf[BoolExpr]
    ), null)

    println(table.size, fpoint.toString)
    val start = System.currentTimeMillis()
    val stat = fpoint.query(List(diff).toArray)
    val end = System.currentTimeMillis()
    println(stat, end - start)
//    if (stat == Status.SATISFIABLE) {
//      println(fpoint.getAnswer)
//    }
  }


  test("basic vs optimized take 1") {
    var links  = Map[String, String]()
    val routerFile = "routing_tables/medium2.txt"
    var optimizedRouter = OptimizedRouter.makeOptimizedRouterForBV_d(new File(routerFile), "OPT_")
    var basicRouter = OptimizedRouter.makeOptimizedRouterForBV_d(new File(routerFile), "")

    val outputDict = basicRouter.instructions.map(i => i._1 -> ("OPT_" + i._1))
    def portOutput(x1 : String, y1 : String) = {
      outputDict.contains(x1) && outputDict(x1) == y1
    }
    def portInput(x : String, y : String) = {
      x == "0" && y == "OPT_0"
    }
    val equiv = new Equivalence(basicRouter.instructions, optimizedRouter.instructions + (
      "OPT_0" -> InstructionBlock(
          Assign(TTL, :-:(:@(TTL), ConstantValue(1))),
          optimizedRouter.instructions("OPT_0")
      )
    ))
    val init = new OVSExecutor(new Z3BVSolver()).execute(
      InstructionBlock(
        Assign("Pid", ConstantValue(0)),
        start,
        ehervlan,
        ipSymb,
        transport,
        end,
        tcpOptions), State.clean, true
    )
    import org.change.v2.util.canonicalnames._
    var time = System.currentTimeMillis()
    var outputEqTime = 0l
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
      outputEqTime += (end - start)
      eq
    }

    val (a, b, c) = equiv.show(SimpleMemory.apply(init._1.head) :: Nil, List[(String, String)](("0", "OPT_0")), portOutput, outputEquivalence)
    println(s"Equivalence testing took ${System.currentTimeMillis()-time} for ${(a.size, b.size, c.size)}ms, output: ${outputEqTime}ms")

    val osarity = new BufferedOutputStream(new FileOutputStream("wrongarity.json"))
    JsonUtil.toJson(a, osarity)
    osarity.close()
    val osport = new BufferedOutputStream(new FileOutputStream("wrongport.json"))
    JsonUtil.toJson(a, osport)
    osport.close()
    val osoutput = new BufferedOutputStream(new FileOutputStream("outputequivalence.json"))
    JsonUtil.toJson(a, osoutput)
    osoutput.close()
  }

}
