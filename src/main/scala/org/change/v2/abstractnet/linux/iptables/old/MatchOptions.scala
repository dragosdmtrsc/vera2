package org.change.v2.abstractnet.linux.iptables.old

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.FloatingConstraint
import org.change.v2.util.conversion.RepresentationConversion.macToNumber
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:, :>=:, :|:, :&:, :<=:}

trait MatchOption {
  def apply() : Instruction;
}
abstract class SimpleOption(neg : Boolean) extends MatchOption {
  def apply() : Instruction;
} 

abstract class TcpOption(neg : Boolean) extends SimpleOption(neg) {
}

case class PortOption(neg : Boolean, name : String, portNo : Int) extends TcpOption(neg) {
  override def apply() : Instruction = {
    val ct = ConstantValue(portNo)
    val constraint = :==:(ct)
    val actual = if (neg) :~:(constraint) else constraint
    if (name == "sport")
     Constrain(Tag("L4SrcPort"), actual)
    else if (name == "dport")
      Constrain(Tag("L4DstPort"), actual)
    else
      throw new Exception("wrong name, should be sport or dport")
  }
}


case class IcmpCodeOption(neg : Boolean, theCode : Int) 
  extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraintCode = if (neg) :~:(:==:(ConstantValue(theCode))) else :==:(ConstantValue(theCode))
    Constrain(Tag("ICMPCode"),constraintCode)
  }
}

case class IcmpTypeOption(neg : Boolean, theType : Int) 
  extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraintType = if (neg) :~:(:==:(ConstantValue(theType))) else :==:(ConstantValue(theType))
    Constrain(Tag("ICMPType"), constraintType)    
  }
}


case class MACOption(neg : Boolean, name : String, addr : String) extends SimpleOption(neg)
{
  override def apply() : Instruction = {
    val constant = ConstantValue(macToNumber(addr))
    val constraint = if (neg) :~:(:==:(constant)) else :==:(constant)
    if (name == "mac-source") Constrain("L2Src", constraint)
    else if (name == "mac-destination") Constrain("L2Dst", constraint)
    else throw new Exception("name expected mac-source or mac-destination")
  }
}

case class AddrOption(neg : Boolean, 
    name : String, 
    var start : Long = 0, 
    var end : Long = 0) 
  extends SimpleOption(neg) {
  
  override def apply() : Instruction = {
    val constraint = :&:(:>=:(ConstantValue(start)), :<=:(ConstantValue(end)))
    val actual = if (neg) :~:(constraint) else constraint
    if (name == "src")
      Constrain(Tag("IPSrc"), actual)
    else if (name == "dst")
      Constrain(Tag("IPDst"), actual)
    else
      throw new Exception("wrong name, should be src or dst")
  } 
}

case class ProtocolOption(neg : Boolean, proto : Int) extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraint = :==:(ConstantValue(proto))
    Constrain (Tag("IPL4Proto"), if (neg) :~:(constraint) else constraint)
  }
}

case class IfaceOption(neg : Boolean, io : String, ifaceNo : Int) extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraint = :==:(ConstantValue(ifaceNo))
    Constrain(Tag(if (io == "in") "InputIface"
    else if (io == "out") "OutIface"
    else throw new Exception("io must be in or out")), if (neg) :~:(constraint) else constraint)
  }
}

case class FragmentOption(neg : Boolean) extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraint = :==:(ConstantValue(1))
    Constrain(Tag("IPFragmentBit"), if (neg) :~:(constraint) else constraint)
  }
}

case class ConnmarkOption(neg : Boolean, 
    forTag : String = "NFMark", 
    start : Int,
    end : Int) extends
  SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraint = :&:(:>=:(ConstantValue(start)), :<=:(ConstantValue(end)))
    val actual = if (neg) :~:(constraint) else constraint
    Constrain(Tag(forTag), actual)
  }
}

class GenericOption(neg : Boolean, 
    tagName : String, 
    startValue : Long, 
    isTag : Boolean = true,
    isInterval : Boolean = false, 
    endValue : Long = -1) 
  extends SimpleOption(neg) {
  override def apply() : Instruction = {
    val constraint = getThatConstrain
    if (isTag) Constrain(Tag(tagName), constraint)
    else Constrain(tagName, constraint)
  }
  
  protected def getThatConstrain() = {
    val constraint = if (isInterval) :&:(:<=:(ConstantValue(endValue)), :>=:(ConstantValue(startValue)))
    else :==:(ConstantValue(startValue))
    if (neg) :~:(constraint) else constraint
  }
}

case class PhysdevInOption(neg : Boolean, value : Int)
  extends GenericOption(neg, "PhysdevIn", value, false, false)
{
}
case class PhysdevOutOption(neg : Boolean, value : Int)
  extends GenericOption(neg, "PhysdevOut", value, false, false)
{
}

case class CtStateOption(neg : Boolean, name : String, list : List[Int], isTag : Boolean = true)
  extends SimpleOption(neg)
{
  override def apply() : Instruction = {
    val listof = list.map(s => :==:(ConstantValue(s)))
    val head :: tail = listof
    val constraint = tail.foldLeft(head.asInstanceOf[FloatingConstraint])((acc, value) => :|:(value, acc))
    val actual = if (neg) :~:(constraint) else constraint
    if (isTag)
      Constrain(Tag(name), actual)
    else
      Constrain(name, actual)
  }
}

case class StateOption(neg : Boolean, value : Int)
  extends GenericOption(neg, "State", value, false, false)
{
}


case class PhysdevIsBridgedOption(neg : Boolean)
  extends GenericOption(neg, "PhysdevIsBridged", 1, false, false)
{
}

