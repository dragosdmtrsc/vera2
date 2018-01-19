package org.change.parser.p4.tables

import java.util.UUID

import org.change.parser.p4._
import org.change.parser.p4.tables.P4Utils.fieldDef
import org.change.v2.abstractnet.mat.condition.Range
import org.change.v2.abstractnet.mat.tree.Node
import org.change.v2.abstractnet.mat.tree.Node.Forest
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.{ConstantBValue, ConstantValue, SymbolicValue}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model.table.{MatchKind, TableMatch}
import org.change.v2.p4.model.{FlowInstance, ISwitchInstance, Switch, SwitchInstance}
import org.change.v2.util.conversion.RepresentationConversion._

import scala.collection.JavaConversions._
import scala.collection.mutable



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
      Forward(s"${switchInstance.getName}.table.$tableName.out" + (if (id.length != 0) s".$id" else ""))
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

  override def numberOfFlows(): Int = flowDefinitions.size
  override protected def generateConstrainableInstructions(index: Int) : List[InstructionBlock] = {
    val finstance = flowDefinitions(index)
    tableMatches.map(k => finstance.matchParams(k.getKey) match {
      case LPMMatch(va, prefix) =>
        val uuid = UUID.randomUUID().toString
        val size = switch.getSize(k.getKey)
        val varName = s"tmp$uuid"
        val (hdr, fieldName) = fieldDef(k.getKey)
        if (switch.getInstance(hdr) != null && !switch.getInstance(hdr).isMetadata) {
          InstructionBlock(
            Allocate(varName, size),
            Assign(varName, :&&:(va, :<<:(:-:(:<<:(ConstantValue(1), prefix), ConstantValue(1)), :-:(ConstantValue(size), prefix)))),
            Constrain(hdr + ".IsValid", :==:(ConstantValue(1))),
            Constrain(varName, :==:(:@(k.getKey)))
          )
        } else {
          InstructionBlock(
            Allocate(varName, size),
            Assign(varName, :&&:(va, :<<:(ConstantValue(1 << size - 1), :-:(ConstantValue(size), prefix)))),
            Constrain(varName, :==:(:@(k.getKey)))
          )
        }
      case RangeMatch(min, max) => InstructionBlock(
        Constrain(k.getKey, :&:(:>=:(min), :<=:(max)))
      )
      case TernaryMatch(va, mask) =>
        val uuid = UUID.randomUUID().toString
        val size = switch.getSize(
            k.getKey
        )
        val (hdr, fieldName) = fieldDef(k.getKey)

        val varName = s"tmp$uuid"
        if (!switch.getInstance(hdr).isMetadata) {
          mask match {
            case ConstantValue(0, _, _) => InstructionBlock()
            case ConstantBValue(v, _) if BigInt(v.substring(2), 16) == 0 => InstructionBlock()
            case _ => InstructionBlock(
              Allocate(varName, size),
              Assign(varName, :&&:(:@(k.getKey), mask)),
              Constrain(hdr + ".IsValid", :==:(ConstantValue(1))),
              Constrain(varName, :==:(va))
            )
          }
        } else {
          mask match {
            case ConstantValue(0, _, _) => InstructionBlock()
            case ConstantBValue(v, _) if BigInt(v.substring(2), 16) == 0 => InstructionBlock()
            case _ => InstructionBlock(
              Allocate(varName, size),
              Assign(varName, :&&:(:@(k.getKey), mask)),
              Constrain(varName, :==:(va))
            )
          }
        }

      case ValidMatch(v) => InstructionBlock(
        Constrain(k.getKey + ".IsValid", :==:(v))
      )
      case Equal(va) =>
        val (hdr, fieldName) = fieldDef(k.getKey)
        if (switch.getInstance(hdr) != null && !switch.getInstance(hdr).isMetadata) {
          InstructionBlock(
            Constrain(hdr + ".IsValid", :==:(ConstantValue(1))),
            Constrain(k.getKey, :==:(va))
          )
        } else {
          InstructionBlock(
            Constrain(k.getKey, :==:(va))
          )
        }

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

