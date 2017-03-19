package org.change.v2.abstractnet.linux.iptables

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.instructions.{:==:, :~:,
    :<:, :<=:, :>=:, :>:, :&:};
import org.change.v2.util.conversion.RepresentationConversion._;


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

case class ConnmarkOption(neg : Boolean, value : Int, forTag : String = "mark", mask : Int = 32) extends
  SimpleOption(neg) {
  override def apply() : Instruction = {
    val (start, end) = ipAndMaskToInterval(value, mask)
    val constraint = :&:(:>=:(ConstantValue(start)), :<=:(ConstantValue(end)))
    val actual = if (neg) :~:(constraint) else constraint
    Constrain(Tag(forTag), actual)
  }
}
