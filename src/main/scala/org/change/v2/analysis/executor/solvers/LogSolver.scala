package org.change.v2.analysis.executor.solvers

import scala.collection.JavaConversions.asScalaBuffer

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{ :@ => :@ }
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.NIC
import org.change.v2.model.OVSBridge
import org.change.v2.model.OpenFlowNIC
import org.change.v2.model.OpenFlowTable
import org.change.v2.model.WorldModel
import org.change.v2.model.openflow.Action
import org.change.v2.model.openflow.ActionType
import org.change.v2.model.openflow.FlowEntry
import org.change.v2.model.openflow.actions.ApplyActionsAction
import org.change.v2.model.openflow.actions.DropAction
import org.change.v2.model.openflow.actions.LoadAction
import org.change.v2.model.openflow.actions.ModDlDstAction
import org.change.v2.model.openflow.actions.ModDlSrcAction
import org.change.v2.model.openflow.actions.ModNwDstAction
import org.change.v2.model.openflow.actions.ModNwSrcAction
import org.change.v2.model.openflow.actions.ModTpDstAction
import org.change.v2.model.openflow.actions.ModTpSrcAction
import org.change.v2.model.openflow.actions.ModVlanVidAction
import org.change.v2.model.openflow.actions.MoveAction
import org.change.v2.model.openflow.actions.NormalAction
import org.change.v2.model.openflow.actions.OutputAction
import org.change.v2.model.openflow.actions.PushVlanAction
import org.change.v2.model.openflow.actions.ResubmitAction
import org.change.v2.model.openflow.actions.SetFieldAction
import org.change.v2.model.openflow.actions.SetTunnelAction

import org.change.v2.model.openflow.actions.StripVlanAction
import org.change.v2.analysis.processingmodels.instructions.Deallocate
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.{:==:, :<=:, :>=:, :&:}
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fork
import org.change.v2.analysis.memory.Value
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.Intable
import java.io.PrintStream
import org.change.v2.model.openflow.actions.LearnAction
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.abstractnet.linux.iptables.EnterIPInterface
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.abstractnet.linux.iptables.EnterIPTablesChain
import org.change.v2.abstractnet.linux.iptables.IPTablesConstants
import org.change.v2.abstractnet.linux.iptables.ConntrackTrack
import org.change.v2.abstractnet.linux.iptables.ConntrackUnDnat
import org.change.v2.abstractnet.linux.iptables.ConntrackUnSnat
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions._
import scala.util.matching.Regex._
import org.change.v2.model.openflow.actions.CTAction
import org.change.v2.abstractnet.linux.iptables.ConntrackZone
import org.change.v2.abstractnet.linux.iptables.ConntrackCommitZone
import org.change.v2.abstractnet.linux.iptables.ConntrackTrackZone
import org.change.v2.abstractnet.linux.ovs.PacketMirror
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.neutron.elements.Experiment._
import org.change.v2.abstractnet.neutron.CSVBackedNetworking
import java.io.PrintWriter
import org.change.v2.analysis.z3.Z3Util
import org.change.v2.analysis.executor.DecoratedInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.abstractnet.linux.Translatable
import org.change.v2.analysis.processingmodels.instructions.ConstrainRaw
import org.change.v2.analysis.processingmodels.instructions.ConstrainNamedSymbol
import org.change.v2.analysis.memory.TagExp

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.expression.concrete.nonprimitive.Plus
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.expression.concrete.nonprimitive.Minus
import org.change.v2.analysis.memory.ValueStack
import org.change.v2.analysis.executor.solvers.AbstractSolver
import org.change.v2.analysis.expression.concrete.nonprimitive.Reference


class LogSolver[T](slv : AbstractSolver[T]) extends AbstractSolver[T] {
  
  private var lastTranslate : Long = 0
  private var lastSolve : Long = 0
  def stats = (lastTranslate, lastSolve)
  
  override def translate(memory : MemorySpace) : T = {
    val init = System.nanoTime()
    val ast = slv.translate(memory)
    lastTranslate = (System.nanoTime() - init)
    ast
  }
  
  override def decide(what : T) : Boolean = {
      val init = System.nanoTime()
      val res = slv.decide(what)
      lastSolve = (System.nanoTime() - init)
      res
  }
}

class ConstraintLogger(file : String) {
  private var logNr = 0
  private var pw = new PrintWriter(file)
  
  pw.println("Nr,Field,Expr,Constraints,NewConstraint,TranslationTime,SatTime")
  
  def log(field : Either[Int, String], 
      value : Value, 
      constraint : Constraint, 
      transTime : Long, 
      satTime : Long,
      res : Boolean) : Unit = {
    val asStr = field match {
      case Right(s) => s
      case Left(i) => i + ""
    }
    val str = "\"%d\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%d\",\"%d\"".format( 
        new Integer(logNr), 
        asStr, 
        value.e.toString(), 
        value.cts.toString(), 
        constraint.toString(), 
        transTime, 
        satTime,
        new Integer(if (res) 1 else 0)
        )
    pw.println(str)
    logNr = logNr + 1
  }
  
  def close() {
    pw.close()
  }
}