package org.change.v2.abstractnet.linux.iptables.old

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:, :>=:, :|:, :&:, :<=:}

case class ConnmarkTarget(var ctMask : Int,
    var nfMask : Int, 
    var restore : Boolean = false) extends IpTablesTarget("CONNMARK")
{
  
  private def applySave()  : Instruction = {
    InstructionBlock(List.range(0, 32).map(s => {
      val cti = ctMask >> s & 1
      val nfi = nfMask >> s & 1
      if (cti == 1)
      {
        if (nfi == 0)
        {
          Assign("CTMark__" + s, ConstantValue(0))
        }
        else
        {
          If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
              Assign("CTMark__" + s, ConstantValue(1)),
              Assign("CTMark__" + s, ConstantValue(0)))
        }
      }
      else
      {
        if (nfi == 0)
        {
          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
              Assign("CTMark__" + s, ConstantValue(1)),
              Assign("CTMark__" + s, ConstantValue(0)))
        }
        else
        {
          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
              If (Constrain("NFMark__" + s, :==:(ConstantValue(0))),
                Assign("CTMark__" + s, ConstantValue(1)),
                Assign("CTMark__" + s, ConstantValue(0))),
              If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
                Assign("CTMark__" + s, ConstantValue(0)),
                Assign("CTMark__" + s, ConstantValue(1))))
        }
      }
      
    }))
    // ctmark = (ctmark & ~ctmask) ^ (nfmark & nfmask)
  }
  private def applyRestore() : Instruction = {
    InstructionBlock(List.range(0, 32).map(s => {
      val cti = ctMask >> s & 1
      val nfi = nfMask >> s & 1
      if (nfi == 1)
      {
        if (cti == 0)
        {
          Assign("NFMark__" + s, ConstantValue(0))
        }
        else
        {
          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
              Assign("NFMark__" + s, ConstantValue(1)),
              Assign("NFMark__" + s, ConstantValue(0)))
        }
      }
      else
      {
        if (cti == 0)
        {
          If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
              Assign("NFMark__" + s, ConstantValue(1)),
              Assign("NFMark__" + s, ConstantValue(0)))
        }
        else
        {
          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
              If (Constrain("NFMark__" + s, :==:(ConstantValue(0))),
                Assign("NFMark__" + s, ConstantValue(1)),
                Assign("NFMark__" + s, ConstantValue(0))),
              If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
                Assign("NFMark__" + s, ConstantValue(0)),
                Assign("NFMark__" + s, ConstantValue(1))))
        }
      }
    }))
    // nfmark = (nfmark & ~nfmask) ^ (ctmark & ctmask);
  }
  
  override def apply() : Instruction = {
    if (!restore) applySave()
    else applyRestore()
  }
}



case class MarkTarget(var nfMask : Int, 
    var value : Int,
    var fieldName : String = "NFMark")
  extends IpTablesTarget("MARK")
{
  def apply() : Instruction = {
    InstructionBlock(List.range(0, 32).map(s => {
      val nfi = (nfMask >> s) & 1
      val vi  = (value >> s) & 1
      if (nfi == 0)
      {
        Assign(fieldName + "__" + s, ConstantValue(vi))
      }
      else
      {
        if (vi == 1)
        {
          If (Constrain(fieldName + "__" + s, :==:(ConstantValue(0))),
              Assign(fieldName + "__" + s, ConstantValue(1)),
              Assign(fieldName + "__" + s, ConstantValue(0)))
        }
        else NoOp
      }
      
    }))
    // nfmark = (nfmark & ~nfmask) ^ value
  }
}

abstract class IpTablesTarget(name : String) {
  def name() : String = this.name
  def apply() : Instruction;
}

/**
 * This target only cares to jump (RETURN, ACCEPT, DROP, <chain-name>)
 * Does no other packet processing whatsoever
 */
case class JumpyTarget(theName : String) extends IpTablesTarget (theName) {
  def apply() = {
    NoOp
  }
}



case class InstructionTarget(theName : String, instr : Instruction) 
  extends IpTablesTarget(theName) {
  def apply() = {
    instr
  }
}


case class NoTrackTarget()
  extends IpTablesTarget("NOTRACK") {
  def apply() : Instruction = {
    Assign("NoTrack", ConstantValue(1))
  }
}

case class DNATTarget(ip : Long)
  extends IpTablesTarget("DNAT")
{
  def apply() : Instruction = {
    Assign("DstIp", ConstantValue(ip))
  }
}


