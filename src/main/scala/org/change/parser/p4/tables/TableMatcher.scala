package org.change.parser.p4.tables

import java.util.UUID

import org.change.parser.p4._
import org.change.v2.abstractnet.mat.condition.Range
import org.change.v2.abstractnet.mat.tree.Node
import org.change.v2.abstractnet.mat.tree.Node.Forest
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.table.{MatchKind, TableMatch}
import org.change.v2.p4.model.{FlowInstance, ISwitchInstance, Switch, SwitchInstance}
import org.change.v2.util.conversion.RepresentationConversion._

import scala.collection.JavaConversions._
import scala.collection.mutable

trait Constrainable {
  def constraint(switchInstance: SwitchInstance, which : Int) : Instruction
  def instantiate(arg : String, prio : Int) : Constrainable
}

/**
  * Created by dragos on 06.09.2017.
  */
class TableRangeMatcher(tableMatch : TableMatch) extends Constrainable {
  import P4Utils._

  private var forest = if (tableMatch.getMatchKind == MatchKind.Lpm || tableMatch.getMatchKind == MatchKind.Range) {
    List[Range]().asInstanceOf[Forest[Range]]
  } else {
    throw new UnsupportedOperationException("TableRangeMatcher only works for LPM and RANGE match kinds")
  }

  private var rrng = mutable.Map[Int, Node[Range]]()

  override def instantiate(arg : String, prio : Int) : Constrainable = {
    if (tableMatch.getMatchKind == MatchKind.Lpm) {
      val spl = arg.split("/")
      val mask = java.lang.Integer.decode(spl(1))
      val range = ipAndMaskToInterval(spl(0), spl(1))
      val (newForest, newNode) = Node.add(forest, Range(range._1, range._2))
      this.forest = newForest
      rrng.put(prio, newNode)
    } else if (tableMatch.getMatchKind == MatchKind.Range) {
      val spl = arg.split(",")
      val start = java.lang.Long.decode(spl(0)).longValue()
      val end = java.lang.Long.decode(spl(1)).longValue()
      val (newForest, newNode) = Node.add(forest, Range(start, end))
      this.forest = newForest
      rrng.put(prio, newNode)
    } else {
      throw new UnsupportedOperationException("Don't know how to do it")
    }
    this
  }

  override def constraint(switchInstance: SwitchInstance, which : Int): Instruction = {
    val node = rrng(which)
    val (hdr, fieldName) = fieldDef(tableMatch.getKey)
    val actual = switchInstance.getSwitchSpec.getInstance(hdr)
    val cvalid = Constrain(s"$hdr.IsValid", :==:(ConstantValue(1)))
    val cinRange = :&:(:<=:(ConstantValue(node.condition.upper)), :>=:(ConstantValue(node.condition.lower)))
    val coutRange = (node.children ++ node.lateral).foldLeft(cinRange)((acc, x) => {
      :&:(acc, :~:(:&:(:<=:(ConstantValue(x.condition.upper)), :>=:(ConstantValue(x.condition.lower)))))
    })
    if (!actual.isMetadata)
      InstructionBlock(
        cvalid,
        Constrain(s"$hdr.$fieldName", coutRange)
      )
    else
      InstructionBlock(
        Constrain(s"$hdr.$fieldName", coutRange)
      )
  }
}
object P4Utils {
  def toNumber(s: String): Long = {
    if (s.contains(".")) {
      ipToNumber(s)
    } else if (s.contains(":")) {
      macToNumber(s)
    } else {
      java.lang.Long.decode(s).longValue()
    }
  }

  def fieldDef(theKey : String) = if (theKey.contains(".")) {
    val hdr = theKey.split("\\.")(0)
    val field = theKey.split("\\.")(1)
    (hdr, field)
  } else {
    (theKey, "")
  }
}
object MaskTests {

  def extractMask(long : Long, width : Int) = {
    var crt = long

    (0 until width).map( i => {
      val b = (crt & 1).toByte
      crt = crt >> 1
      b
    })
  }
  def main(args: Array[String]): Unit = {
    val mask = 256L.toLong
    val value = (256+512+2048).toLong
    val width = 32
    val actualMask = extractMask(mask, width)
    println(actualMask)
    val badBasis = actualMask.zipWithIndex.filter(x => x._1 == 0)
    val contiguous = badBasis.drop(1).foldLeft(((badBasis.head._2, badBasis.head._2), List[(Int, Int)]()))((acc, x) => {
      val end = x._2
      if (end == acc._1._2+1) {
        ((acc._1._1, end), acc._2)
      } else {
        ((end, end), acc._2 :+ acc._1)
      }
    })
    val intervals = (contiguous._2 :+ contiguous._1)
    println(intervals)
//      .map(x => 1l << x._2)
//    println(badBasis)
    val A =value&mask
      //extractMask(value, width).zip(actualMask).map(x => x._1 & x._2).zipWithIndex.map(x => x._1 * 1l << x._2).sum
    println(A)
    val newsyms = intervals.map(x => {
      SymbolicValue()
    })
    val fexp = intervals.zip(newsyms).map(x => {
      val lk = x._1._2 - x._1._1
      (0l until 1l<<x._1._1).foldLeft(ConstantValue(0) : FloatingExpression)((acc, _) => {
        :+:(acc, x._2)
      })
    }).foldLeft(ConstantValue(A) : FloatingExpression)((acc, x) => {
      :+:(acc, x)
    })
    val ass = newsyms.map(x => {
      Assign("sym" + x.id, x)
    })
    val constrains = Constrain("Abc", :==:(fexp)) :: intervals.zip(newsyms).
      map(x => Constrain("sym" + x._2.id, :&:(:<=:(ConstantValue((1l<<((x._1._2 - x._1._1)+1))-1)), :>=:(ConstantValue(0)))))
    InstructionBlock(ass ++ constrains)
  }
}
class TableTernaryMatcher(tableMatch: TableMatch, useBv : Boolean = true) extends Constrainable {

  def extractMask(long : Long, width : Int) = {
    var crt = long

    (0 until width).map( i => {
      val b = (crt & 1).toByte
      crt = crt >> 1
      b
    })
  }


  val flowToValMask : mutable.Map[Int, (Long, Long)] = mutable.Map[Int, (Long, Long)]()

  override def constraint(switchInstance: SwitchInstance, which: Int): Instruction = {
    val (mask, value) = flowToValMask(which)
    val (hdr, fld) = P4Utils.fieldDef(tableMatch.getKey)
    val width = switchInstance.getSwitchSpec.getInstance(hdr).getLayout.getFields.find(x => x.getName == fld).get.getLength
    val actualMask = extractMask(mask, width)
    if (!useBv) {
      val badBasis = actualMask.zipWithIndex.filter(x => x._1 == 0)
      val contiguous = badBasis.drop(1).foldLeft(((badBasis.head._2, badBasis.head._2), List[(Int, Int)]()))((acc, x) => {
        val end = x._2
        if (end == acc._1._2+1) {
          ((acc._1._1, end), acc._2)
        } else {
          ((end, end), acc._2 :+ acc._1)
        }
      })
      val intervals = (contiguous._2 :+ contiguous._1)
      val A =value&mask
      val newsyms = intervals.map(x => {
        UUID.randomUUID().toString.replace("-", "")
      })
      val fexp = intervals.zip(newsyms).map(x => {
        (0l until 1l<<x._1._1).foldLeft(ConstantValue(0) : FloatingExpression)((acc, _) => {
          :+:(acc, :@(s"sym${x._2}"))
        })
      }).foldLeft(ConstantValue(A) : FloatingExpression)((acc, x) => {
        :+:(acc, x)
      })
      val ass = newsyms.map(x => {
        Assign("sym" + x, SymbolicValue())
      })
      val constrains = Constrain(tableMatch.getKey, :==:(fexp)) :: intervals.zip(newsyms).
        map(x => Constrain("sym" + x._2, :&:(:<=:(ConstantValue((1l<<((x._1._2 - x._1._1)+1))-1)), :>=:(ConstantValue(0)))))
      InstructionBlock(ass ++ constrains)
    } else {
      val crtrnd = UUID.randomUUID().toString
      InstructionBlock(
        Allocate(s"tmp$crtrnd", width),
        Assign(s"tmp$crtrnd", :&&:(:@(tableMatch.getKey), ConstantValue(mask))),
        Constrain(s"tmp$crtrnd", :==:(ConstantValue(mask & value)))
      )
    }

  }

  override def instantiate(arg: String, prio: Int): Constrainable = {
    if (!arg.contains("&&&"))
      throw new IllegalArgumentException(s"$arg not of ternary type")
    val split = arg.split("&&&")
    flowToValMask.put(prio, (P4Utils.toNumber(split(0)), P4Utils.toNumber(split(1))))
    this
  }
}
class TableExactMatcher(tableMatch: TableMatch) extends Constrainable {
  var prioToExact = mutable.Map[Int, Long]()
  import P4Utils._

  override def instantiate(arg : String, prio : Int) : Constrainable = {
    val lval = if (arg.contains(".")) {
      ipToNumber(arg)
    } else if (arg.contains(":")) {
      macToNumber(arg)
    } else {
      java.lang.Long.decode(arg).longValue()
    }
    prioToExact.put(prio, lval)
    this
  }



  // no question asked: exact only matches field_refs (for the moment)
  private def handleExact(switchInstance: SwitchInstance, which: Int) : Instruction = {
    val (hdr, fieldName) = fieldDef(tableMatch.getKey)
    val switch = switchInstance.getSwitchSpec
    val actualHdr = switch.getInstance(hdr)
    InstructionBlock(
      if (!actualHdr.isMetadata)
        List[Instruction](
          Constrain(s"$hdr.IsValid", :==:(ConstantValue(1))),
          Constrain(s"$hdr.$fieldName", :==:(ConstantValue(prioToExact(which))))
        )
      else
        List[Instruction](Constrain(s"$hdr.$fieldName", :==:(ConstantValue(prioToExact(which)))))
    )
  }
  private def handleValid(switchInstance: SwitchInstance, which: Int) : Instruction = {
    val (hdr, fieldName) = fieldDef(tableMatch.getKey)
    val switch = switchInstance.getSwitchSpec
    val actualHdr = switch.getInstance(hdr)
    InstructionBlock(
      Constrain(s"$hdr.IsValid", :==:(ConstantValue(prioToExact(which))))
    )
  }


  override def constraint(switchInstance: SwitchInstance, which : Int): Instruction = {
    tableMatch.getMatchKind match {
      case MatchKind.Exact => handleExact(switchInstance, which)
      case MatchKind.Valid => handleValid(switchInstance, which)
      case _ => throw new UnsupportedOperationException(s"Can't handle this kind of operation ${tableMatch.getMatchKind}")
    }
  }
}
object Constrainable {
  def apply(tableMatch: TableMatch): Constrainable = tableMatch.getMatchKind match {
    case MatchKind.Exact => new TableExactMatcher(tableMatch)
    case MatchKind.Lpm => new TableRangeMatcher(tableMatch)
    case MatchKind.Range => new TableRangeMatcher(tableMatch)
    case MatchKind.Valid => new TableExactMatcher(tableMatch)
    case MatchKind.Ternary => new TableTernaryMatcher(tableMatch)
    case _ => throw new UnsupportedOperationException(s"Cannot match kind ${tableMatch.getMatchKind} " +
      s"in table ${tableMatch.getTable}" +
      s" and key ${tableMatch.getKey}")
  }
}

abstract class FullTableGeneric[T<:ISwitchInstance](tableName : String,
                                           id : String,
                                           switchInstance: T,
                                           switch: Switch) {
  private val matchKeys = switch.getTableMatches(tableName)
  def numberOfFlows() : Int

  protected def generateConstraintsInternal(index : Int) : List[Instruction] = Nil
  protected def generatePriorAssignmentsInternal(index : Int) : List[Instruction] = Nil

  private def generateConstraints(index : Int) : List[Instruction] = {
    val res = generateConstraintsInternal(index)
    res.foldLeft(res)((acc, x) => {
      assert(x.isInstanceOf[ConstrainNamedSymbol] || x.isInstanceOf[ConstrainRaw])
      acc
    })
  }

  private def generatePriorAssignments(index : Int) : List[Instruction] = {
    val res = generatePriorAssignmentsInternal(index)
    res.foldLeft(res)((acc, x) => {
      assert(x.isInstanceOf[AssignNamedSymbol] || x.isInstanceOf[AssignRaw] ||
      x.isInstanceOf[AllocateRaw] || x.isInstanceOf[AllocateSymbol])
      acc
    })
  }

  protected def generateConstrainableInstructions(index : Int) : List[InstructionBlock] = {
    generateConstraints(index).zip(generatePriorAssignments(index)).map(r => {
      InstructionBlock(r._1, r._2)
    })
  }
  protected def action(index : Int): Instruction
  protected def defaultActionCode() : Instruction
  protected def actionDef(index : Int): String

  protected def priorAndConstraints(index : Int): (List[Instruction], List[Instruction]) = {
    val init = generateConstrainableInstructions(index)
    (init.map ( x => {
      InstructionBlock(x.instructions.filter(x => x.isInstanceOf[AssignNamedSymbol] ||
        x.isInstanceOf[AllocateSymbol] || x.isInstanceOf[AllocateRaw]))
    }), init.map ( x => {
      InstructionBlock(x.instructions.filter(x => x.isInstanceOf[ConstrainNamedSymbol] || x.isInstanceOf[ConstrainRaw]))
    }))
  }
  protected def default() : Instruction = If (Constrain("IsClone", :==:(ConstantValue(0))),
    InstructionBlock(
      defaultActionCode(),
      Assign("default.Fired", ConstantValue(1)),
      Assign(s"$tableName.Hit", ConstantValue(0))
    )
  )
  protected def initializeTable() : Instruction = {
    InstructionBlock(
      switch.getAllowedActions(tableName).map(x => {
        Assign(s"$x.Fired", ConstantValue(0))
      }) ++ List[Instruction](
        Assign("default.Fired", ConstantValue(0)),
        Assign(s"$tableName.Hit", ConstantValue(0))
      ) ++ List[Instruction](
        Allocate("IsClone", 1),
        Assign("IsClone", ConstantValue(0))
      )
    )
  }
  def fullAction() : Instruction = InstructionBlock(
    initializeTable(),
    (0 until numberOfFlows).foldRight(default())((x, acc) => {
      val priorAndCt = priorAndConstraints(x)
      priorAndCt._2.zip(priorAndCt._1).foldRight(action(x))((y, acc2) => {
        If (Constrain("IsClone", :==:(ConstantValue(0))),
          InstructionBlock(
            y._2,
            If (y._1,
              InstructionBlock(
                acc2,
                Assign(s"${actionDef(x)}.Fired", ConstantValue(1)),
                Assign(s"$tableName.Hit", ConstantValue(1))
              ),
              InstructionBlock(
                Assign(s"${actionDef(x)}.Fired", ConstantValue(0)),
                Assign(s"$tableName.Hit", ConstantValue(0)),
                acc
              )
            )
          )
        )
      })
    }),
    If (Constrain("IsClone", :==:(ConstantValue(0))),
      Forward(s"table.$tableName.out" + (if (id.length != 0) s".$id" else ""))
    )
  )
}

class FullTableWithInstances[T<:ISwitchInstance](tableName : String,
                             id : String,
                             switchInstance: T,
                             switch: Switch,
                             flowDefinitions : List[P4FlowInstance],
                             defaultAction : ActionDefinition) extends FullTableGeneric[T](tableName, id, switchInstance, switch) {
  private val tableMatches = switch.getTableMatches(tableName).toList
  private val matchKeys = tableMatches.map(_.getKey)

  override def numberOfFlows(): Int = flowDefinitions.size
  override protected def generateConstrainableInstructions(index: Int) : List[InstructionBlock] = {
    val finstance = flowDefinitions(index)
    tableMatches.map(k => finstance.matchParams(k.getKey) match {
      case LPMMatch(va, prefix) =>
        val uuid = UUID.randomUUID().toString
        val size = switch.getSize(k.getKey)
        val varName = s"tmp$uuid"
        InstructionBlock(
          Allocate(varName, size),
          Assign(varName, :&&:(va, :<<:(ConstantValue(1 << size - 1), :-:(ConstantValue(size), prefix)))),
          Constrain(varName, :==:(:@(k.getKey)))
        )
      case RangeMatch(min, max) => InstructionBlock(
        Constrain(k.getKey, :&:(:>=:(min), :<=:(max)))
      )
      case TernaryMatch(va, mask) =>
        val uuid = UUID.randomUUID().toString
        val size = switch.getSize(k.getKey)
        val varName = s"tmp$uuid"
        InstructionBlock(
          Allocate(varName, size),
          Assign(varName, :&&:(:@(k.getKey), mask)),
          Constrain(varName, :==:(va))
        )
      case ValidMatch(v) => InstructionBlock(
        Constrain(k.getKey + ".IsValid", :==:(v))
      )
      case Equal(va) => InstructionBlock(
        Constrain(k.getKey, :==:(va))
      )
      case _ => ???
    })
  }
  private def toSefl(actionDefinition: ActionDefinition, index : Int = -1) : Instruction = {
    val p4action = switch.getActionRegistrar.getAction(actionDefinition.action)
    val actionInstance = new ActionInstance(p4Action = p4action,
      argList = p4action.getParameterList.map(r => actionDefinition.actionParams(r.getParamName)).toList,
      table = tableName,
      switchInstance = switchInstance,
      switch = switch,
      flowNumber = index,
      dropMessage = s"Dropped at $tableName@$index"
    )
    actionInstance.sefl()
  }
  override protected def action(index: Int): Instruction = {
    val flowDef = flowDefinitions(index)
    toSefl(flowDef.action, index)
  }
  override protected def defaultActionCode(): Instruction = toSefl(defaultAction)
  override protected def actionDef(index: Int): String = flowDefinitions(index).action.action
}

class InstanceBasedFullTable(tableName : String, switchInstance: SwitchInstance, id : String = "")
  extends FullTableGeneric[SwitchInstance](switchInstance = switchInstance,
    switch = switchInstance.getSwitchSpec,
    tableName = tableName,
    id = id) {
  private val switch: Switch = switchInstance.getSwitchSpec
  private lazy val flows: List[FlowInstance] = if (flows != null)
    switchInstance.flowInstanceIterator(tableName).toList
  else
    Nil

  private val matchKeys = switch.getTableMatches(tableName)
  private val constrainables = flows.zipWithIndex.foldLeft(matchKeys.map(x => Constrainable(x)).toList)((acc, f) => {
    f._1.getMatchParams.zip(acc).map(arg => {
      arg._2.instantiate(arg._1.toString, f._2)
    }).toList
  })

  override def numberOfFlows(): Int = flows.size
  override protected def generateConstrainableInstructions(index: Int): List[InstructionBlock] =
    constrainables.map(_.constraint(switchInstance, index)).map(x => {
      x.asInstanceOf[InstructionBlock]
    })
  override protected def action(index: Int): Instruction = new FireAction(tableName, index, switchInstance).symnetCode()
  override protected def defaultActionCode(): Instruction = new FireDefaultAction(tableName, switchInstance).symnetCode()
  override protected def actionDef(index: Int): String = switchInstance.flowInstanceIterator(tableName).get(index).getFireAction
}

class FullTable(tableName : String, switchInstance: SwitchInstance, id : String = "") {

  val switch: Switch = switchInstance.getSwitchSpec
  private val flows = switchInstance.flowInstanceIterator(tableName)
  private val matchKeys = switch.getTableMatches(tableName)
  private val constrainables = matchKeys.map(x => Constrainable(x)).toList

  if (flows != null) {
    for (f <- flows.zipWithIndex) {
      for (arg <- f._1.getMatchParams.zip(constrainables)) {
        arg._2.instantiate(arg._1.toString, f._2)
      }
    }
  }


  protected def priorAndConstraints(index : Int): (List[Instruction], List[Instruction]) = {
    val init = constrainables.map(_.constraint(switchInstance, index)).map(x => {
      x.asInstanceOf[InstructionBlock]
    })
    (init.map ( x => {
      InstructionBlock(x.instructions.filter(x => x.isInstanceOf[AssignNamedSymbol] ||
          x.isInstanceOf[AllocateSymbol] || x.isInstanceOf[AllocateRaw]))
    }), init.map ( x => {
      InstructionBlock(x.instructions.filter(x => x.isInstanceOf[ConstrainNamedSymbol] || x.isInstanceOf[ConstrainRaw]))
    }))
  }

  protected def action(index : Int): Instruction = new FireAction(tableName, index, switchInstance).symnetCode()
  protected def default() : Instruction = If (Constrain("IsClone", :==:(ConstantValue(0))),
    InstructionBlock(
      new FireDefaultAction(tableName, switchInstance).symnetCode(),
      Assign("default.Fired", ConstantValue(1)),
      Assign(s"$tableName.Hit", ConstantValue(0))
    )
  )
  protected def initializeTable() : Instruction = {
    InstructionBlock(
      switch.getAllowedActions(tableName).map(x => {
        Assign(s"$x.Fired", ConstantValue(0))
      }) ++ List[Instruction](
        Assign("default.Fired", ConstantValue(0)),
        Assign(s"$tableName.Hit", ConstantValue(0))
      ) ++ List[Instruction](
        Allocate("IsClone", 1),
        Assign("IsClone", ConstantValue(0))
      )
    )
  }

  protected def actionDef(index : Int): String =
    switchInstance.flowInstanceIterator(tableName).get(index).getFireAction

  def fullAction() : Instruction = InstructionBlock(
      initializeTable(),
      flows.zipWithIndex.foldRight(default())((x, acc) => {
        val priorAndCt = priorAndConstraints(x._2)
        priorAndCt._2.zip(priorAndCt._1).foldRight(action(x._2))((y, acc2) => {
          If (Constrain("IsClone", :==:(ConstantValue(0))),
            InstructionBlock(
              y._2,
              If (y._1,
                InstructionBlock(
                  acc2,
                  Assign(s"${actionDef(x._2)}.Fired", ConstantValue(1)),
                  Assign(s"$tableName.Hit", ConstantValue(1))
                ),
                InstructionBlock(
                  Assign(s"${actionDef(x._2)}.Fired", ConstantValue(0)),
                  Assign(s"$tableName.Hit", ConstantValue(0)),
                  acc
                )
              )
            )
          )
        })
      }),
      If (Constrain("IsClone", :==:(ConstantValue(0))),
        Forward(s"table.$tableName.out" + (if (id.length != 0) s".$id" else ""))
      )
    )

}