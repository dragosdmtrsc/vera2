package org.change.parser.p4

import com.sun.xml.internal.bind.util.Which
import org.change.v2.abstractnet.mat.tree.Node.Forest
import org.change.v2.abstractnet.mat.condition.Range
import org.change.v2.abstractnet.mat.tree.Node
import org.change.v2.analysis.constraint.Constraint
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.SwitchInstance
import org.change.v2.p4.model.table.{MatchKind, TableMatch}
import org.change.v2.util.conversion.RepresentationConversion._

import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable

trait Constrainable {
  def constraint(switchInstance: SwitchInstance, which : Int) : Instruction
  def instantiate(arg : String, prio : Int) : Unit
}

/**
  * Created by dragos on 06.09.2017.
  */
class TableRangeMatcher(tableMatch : TableMatch) extends Constrainable {
  import P4Utils._

  var forest = if (tableMatch.getMatchKind == MatchKind.Lpm || tableMatch.getMatchKind == MatchKind.Range) {
    List[Range]().asInstanceOf[Forest[Range]]
  } else {
    throw new UnsupportedOperationException("TableRangeMatcher only works for LPM and RANGE match kinds")
  }

  var rrng = mutable.Map[Int, Node[Range]]()

  override def instantiate(arg : String, prio : Int) : Unit = {
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
  }

  override def constraint(switchInstance: SwitchInstance, which : Int): Instruction = {
    val node = rrng(which)
    val (hdr, fieldName) = fieldDef(tableMatch.getKey)
    val cvalid = Constrain(s"$hdr.IsValid", :==:(ConstantValue(1)))
    val cinRange = :&:(:<=:(ConstantValue(node.condition.upper)), :>=:(ConstantValue(node.condition.lower)))
    val coutRange = (node.children ++ node.lateral).foldLeft(cinRange)((acc, x) => {
      :&:(acc, :~:(:&:(:<=:(ConstantValue(x.condition.upper)), :>=:(ConstantValue(x.condition.lower)))))
    })
    InstructionBlock(
      cvalid,
      Constrain(s"$hdr.$fieldName", coutRange)
    )
  }
}
object P4Utils {
  def fieldDef(theKey : String) = if (theKey.contains(".")) {
    val hdr = theKey.split("\\.")(0)
    val field = theKey.split("\\.")(1)
    (hdr, field)
  } else {
    (theKey, "")
  }
}


class TableExactMatcher(tableMatch: TableMatch) extends Constrainable {
  var prioToExact = mutable.Map[Int, Long]()
  import P4Utils._

  override def instantiate(arg : String, prio : Int) : Unit = {
    val lval = if (arg.contains(".")) {
      ipToNumber(arg)
    } else if (arg.contains(":")) {
      macToNumber(arg)
    } else {
      java.lang.Long.decode(arg).longValue()
    }
    prioToExact.put(prio, lval)
  }



  // no question asked: exact only matches field_refs (for the moment)
  private def handleExact(switchInstance: SwitchInstance, which: Int) : Instruction = {
    val (hdr, fieldName) = fieldDef(tableMatch.getKey)
    val switch = switchInstance.getSwitchSpec
    val actualHdr = switch.getInstance(hdr)
    InstructionBlock(
      Constrain(s"$hdr.IsValid", :==:(ConstantValue(1))),
      Constrain(s"$hdr.$fieldName", :==:(ConstantValue(prioToExact(which))))
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
    case _ => throw new UnsupportedOperationException(s"Cannot match kind ${tableMatch.getMatchKind} " +
      s"in table ${tableMatch.getTable}" +
      s" and key ${tableMatch.getKey}")
  }
}

class FullTable(tableName : String, switchInstance: SwitchInstance, id : String = "") {
  private val flows = switchInstance.flowInstanceIterator(tableName)
  private val matchKeys = switchInstance.getSwitchSpec.getTableMatches(tableName)
  private val constrainables = matchKeys.map(x => Constrainable(x))

  for (f <- flows.zipWithIndex) {
    for (arg <- f._1.getMatchParams.zip(constrainables)) {
      arg._2.instantiate(arg._1.toString, f._2)
    }
  }

  private def constraints(index : Int) = constrainables.map(_.constraint(switchInstance, index))
  private def action(index : Int) = new FireAction(tableName, index, switchInstance).symnetCode()
  private def default() : Instruction = InstructionBlock(
    new FireDefaultAction(tableName, switchInstance).symnetCode(),
    Assign("default.Fired", ConstantValue(1)),
    Assign(s"$tableName.Hit", ConstantValue(0))
  )
  private def initializeTable() : Instruction = {
    InstructionBlock(
      switchInstance.getSwitchSpec.getAllowedActions(tableName).map(x => {
        Assign(s"$x.Fired", ConstantValue(0))
      }) ++ List[Instruction](
        Assign("default.Fired", ConstantValue(0)),
        Assign(s"$tableName.Hit", ConstantValue(0))
      )
    )
  }

  private def actionDef(index : Int) = {
    val flow = switchInstance.flowInstanceIterator(tableName).get(index)
    flow.getFireAction
  }

  def fullAction() = InstructionBlock(
    initializeTable(),
    flows.zipWithIndex.foldRight(default())((x, acc) => {
      constraints(x._2).foldRight(action(x._2))((y, acc2) => {
        If (y,
          InstructionBlock(
            acc2,
            Assign(s"${actionDef(x._2)}.Fired", ConstantValue(1)),
            Assign(s"$tableName.Hit", ConstantValue(1))
          ),
          InstructionBlock(
            acc,
            Assign(s"${actionDef(x._2)}.Fired", ConstantValue(0)),
            Assign(s"$tableName.Hit", ConstantValue(0))
          )
        )
      })
    }),
    Forward(s"table.$tableName.out" + (if (id.length != 0) s".$id" else ""))
  )

}