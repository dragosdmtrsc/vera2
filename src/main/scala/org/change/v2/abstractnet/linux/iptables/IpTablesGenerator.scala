package org.change.v2.abstractnet.linux.iptables

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.model.WorldModel
import org.change.v2.analysis.memory.State
import org.change.v2.model.NIC
import org.change.v2.model.Computer
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.model.Namespace
import scala.collection.JavaConversions._
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.model.iptables.IPTablesTarget
import org.change.v2.model.iptables.AcceptTarget
import org.change.v2.model.iptables.RejectTarget
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.model.iptables.DropTarget
import org.change.v2.model.iptables.ReturnTarget
import org.change.v2.model.iptables.JumpyTarget
import org.change.v2.model.iptables.MasqueradeTarget
import org.change.v2.model.iptables.SNATTarget
import org.change.v2.model.iptables.DNATTarget
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.model.iptables.ProtocolOption
import org.change.v2.model.iptables.IcmpCodeOption
import org.change.v2.model.iptables.IcmpTypeOption
import org.change.v2.model.iptables.FragmentOption
import org.change.v2.model.iptables.IfaceOption
import org.change.v2.model.iptables.AddrOption
import org.change.v2.model.iptables.IPTablesMatch
import org.change.v2.model.iptables.ConnmarkOption
import org.change.v2.model.iptables.MarkOption
import org.change.v2.model.iptables.CtStateOption
import org.change.v2.iptables.StateDefinitions
import org.change.v2.model.iptables.SimpleOption
import org.change.v2.analysis.constraint.NOT
import org.change.v2.model.iptables.ConnmarkTarget
import org.change.v2.model.iptables.MarkTarget

case class EnterIPInterface(pcName : String, 
    iface : String, 
    worldModel : WorldModel) extends Instruction {
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    val pc = worldModel.getComputer(pcName)
    val nic = pc.getNic(iface)
    val instr = InstructionBlock(
      new EnterIPTablesChain(pc, nic, "raw", "PREROUTING", worldModel),
      new Conntrack(pc, nic, worldModel),
      new EnterIPTablesChain(pc, nic, "mangle", "PREROUTING", worldModel),
      new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel),
      new RoutingDecision(pc, nic, worldModel),
      If (Constrain("Decision", :==:(ConstantValue(0))),
        InstructionBlock(
            new EnterIPTablesChain(pc, nic, "mangle", "INPUT", worldModel),
            new EnterIPTablesChain(pc, nic, "filter", "INPUT", worldModel)
        ),
        InstructionBlock(
            new EnterIPTablesChain(pc, nic, "mangle", "FORWARD", worldModel),
            new EnterIPTablesChain(pc, nic, "filter", "FORWARD", worldModel),
            new RoutingDecision(pc, nic, worldModel),
            new EnterIPTablesChain(pc, nic, "mangle", "POSTROUTING", worldModel),
            new EnterIPTablesChain(pc, nic, "nat", "POSTROUTING", worldModel)
        )
      )
    )
    instr(state, verbose)
  }
}


case class Conntrack(pc : Computer, iface : NIC, worldModel : WorldModel)
  extends Instruction
{
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    NoOp(state, verbose)
  }
}



case class RoutingDecision(pc : Computer, iface : NIC, worldModel : WorldModel)
  extends Instruction
{
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    NoOp(state, verbose)
  }
}

case class EnterIPTablesChain(pc : Computer, 
    iface : NIC, 
    table : String, 
    chain : String, 
    world : WorldModel,
    isInitial : Boolean = true)
  extends Instruction
{
  
  lazy val nic = pc.getNic(iface.getName)
  lazy val ns : Namespace = pc.getNamespaceForNic(nic.getName)
  lazy val maybeChain = ns.getIPTablesTables.find { x => x.getName == table }.get.
                        getIPTablesChains.find { x => x.getName == chain }.get
  
                        
  def mapMatch(x : IPTablesMatch) : List[Instruction] = {
      val res1 = x.getMatchOptions.flatMap(y => {
          val ctList = y match {
            case y : ProtocolOption => {
              List[Instruction](Constrain(Proto, 
                  if (y.getNeg) :~:(:==:(ConstantValue(y.getProto)))
                  else :==:(ConstantValue(y.getProto))))
            }
            case y : IcmpCodeOption => {
              List[Instruction](
                  Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                  Constrain(ICMPCode, 
                      if (y.getNeg) :~:(:==:(ConstantValue(y.getTheCode.intValue())))
                      else :==:(ConstantValue(y.getTheCode.intValue()))))
            }
            case y : IcmpTypeOption => {
              List[Instruction](
                  Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                  Constrain(ICMPType,  
                    if (y.getNeg) :~:(:==:(ConstantValue(y.getTheType.intValue())))
                    else :==:(ConstantValue(y.getTheType.intValue()))
                  )
              )
            }
            case y : FragmentOption => {
              List[Instruction](Constrain(FragmentOfset, if (y.getNeg) :~:(:==:(ConstantValue(1)))
              else :==:(ConstantValue(1)) ))
            }
            case y : IfaceOption => {
              if (y.getIo == "in")
              {
                List[Instruction](Constrain("in_port", 
                    if (y.getNeg)
                    {
                      :~:(:==:(ConstantValue(y.getIfaceName.hashCode)))
                    }
                    else
                    {
                      :==:(ConstantValue(y.getIfaceName.hashCode))
                    }))
              }
              else if (y.getIo == "out")
              {
                List[Instruction](Constrain("out_port", 
                    if (y.getNeg)
                    {
                      :~:(:==:(ConstantValue(y.getIfaceName.hashCode)))
                    }
                    else
                    {
                      :==:(ConstantValue(y.getIfaceName.hashCode))
                    }))
              }
              else
              {
                throw new UnsupportedOperationException("Unknown iface type " + y.getIo)
              }
            }
            case y : AddrOption => {
              if (y.getName == "src")
              {
                List[Instruction](Constrain(IPDst, 
                    if (y.getNeg) :~:(:&:(:<=:(ConstantValue(y.getEnd)), :>=:(ConstantValue(y.getStart))))
                    else :&:(:<=:(ConstantValue(y.getEnd)), :>=:(ConstantValue(y.getStart)))))
              }
              else if (y.getName == "dst")
              {
                List[Instruction](Constrain(IPDst,
                    if (y.getNeg) :~:(:&:(:<=:(ConstantValue(y.getEnd)), :>=:(ConstantValue(y.getStart))))
                    else :&:(:<=:(ConstantValue(y.getEnd)), :>=:(ConstantValue(y.getStart)))))
              }
              else
              {
                throw new UnsupportedOperationException("Unknown type " + y.getName)
              }
            }
            case y : CtStateOption => {
              val tagList = y.getList.toList
              def computeOr (list : List[Integer]) : FloatingConstraint = list match {
                case head :: Nil => :==:(ConstantValue(head.intValue()))
                case head :: tail => :|:(computeOr(head :: Nil), computeOr(tail))
                case Nil => throw new UnsupportedOperationException("Can't be")
              }
               List[Instruction](Constrain("State", computeOr(tagList)))
            }
            case y : MarkOption => {
              if (y.getName == "nfmark")
              {
                if (y.getMask == 0xFFFF)
                {
                  List[Instruction](Constrain("NFMark_bottom", :==:(ConstantValue(y.getValue))))
                }
                else if (y.getMask == 0xFFFF0000)
                {
                   List[Instruction](Constrain("NFMark_top", :==:(ConstantValue(y.getValue))))
                }
                else
                {
                  throw new UnsupportedOperationException("Cannot handle arbitrary masks")
                }
              }
              else if (y.getName == "ctmark")
              {
                if (y.getMask == 0xFFFF)
                {
                  List[Instruction](Constrain("CTMark_bottom", :==:(ConstantValue(y.getValue))))
                }
                else if (y.getMask == 0xFFFF0000)
                {
                   List[Instruction](Constrain("CTMark_top", :==:(ConstantValue(y.getValue))))
                }
                else
                {
                  throw new UnsupportedOperationException("Cannot handle arbitrary masks")
                }
              }
              else
              {
                throw new UnsupportedOperationException("Cannot handle arbitrary masks")
              }
            }
            case _ => throw new UnsupportedOperationException("Cannot handle " + y.toString())
          }
          ctList
        })
        res1.toList
  }
                        
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    
    val rules = maybeChain.getIPTablesRules
    val stt = 
      if (isInitial)
        InstructionBlock(
          Assign("Continue", ConstantValue(1)),
          Assign("IPTablesRuleMatch", ConstantValue(-1)))(state, verbose)._1(0)
      else
        state
    InstructionBlock(InstructionBlock(rules.map (x => {
      val matches = x.getIPTablesMatches.map {mapMatch}.foldLeft (List[Instruction]()) { _ ++ _ }.
              foldLeft(fireTarget(x.getIPTablesTarget)) { (acc, v) => {
                  If (v, acc, Assign("Continue", ConstantValue(1)))
                }
              }
      If (Constrain("Continue", :==:(ConstantValue(1))),
          matches,
          NoOp)
    })),
    If (Constrain("Continue", :==:(ConstantValue(1))),
        fireTarget(maybeChain.getPolicy),
        NoOp))(state, verbose)
    
  }
  
                        
                        
                       
  def fireTarget(target : IPTablesTarget) : Instruction = {
    target match {
      case target : AcceptTarget => Assign("IsAccept", ConstantValue(0))
      case target : RejectTarget => InstructionBlock(Assign("Continue", ConstantValue(0)), Fail("Reject"))
      case target : DropTarget => InstructionBlock(Assign("Continue", ConstantValue(0)), Fail("Reject"))
      case target : ReturnTarget  => {
        if (maybeChain.isDefault())
        {
          fireTarget(maybeChain.getPolicy)
        }
        else
        {
          Assign("Continue", ConstantValue(1))
        }
      }
      case target : JumpyTarget => {
        InstructionBlock(new EnterIPTablesChain(pc : Computer, 
          iface : NIC, 
          table : String, 
          chain : String, 
          world : WorldModel,
          false),
          If (Constrain("IsAccept", :==:(ConstantValue(1))),
              Assign("Continue", ConstantValue(0)),
              NoOp))
      }
      case target : MasqueradeTarget => {
        throw new UnsupportedOperationException("No masquerade yet")
      }
      case target : SNATTarget => {
        InstructionBlock(
          Assign("IsSNAT", ConstantValue(1)),
          Assign("OriginalIpSrc", :@(IPSrc)),
          Assign(IPSrc, ConstantValue(target.getIpAddress)),
          If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
            InstructionBlock(Assign("OriginalPortSrc", :@(TcpSrc)),
                Assign(TcpSrc, SymbolicValue())), 
            If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
                InstructionBlock(Assign("OriginalPortSrc", :@(UDPSrc)),
                    Assign(UDPSrc, SymbolicValue())),
                If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                    InstructionBlock(Assign("OriginalPortSrc", :@(ICMPIdentifier)),
                        Assign(ICMPIdentifier, SymbolicValue())),
                    NoOp)))
          )
      }
      case target : DNATTarget => {
        InstructionBlock(
          Assign("IsDNAT", ConstantValue(1)),
          Assign("OriginalIpDst", :@(IPDst)),
          Assign(IPDst, ConstantValue(target.getIpAddress)),
          If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
            InstructionBlock(Assign("OriginalPortDst", :@(TcpDst)),
                Assign(TcpDst, SymbolicValue())), 
            If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
                InstructionBlock(Assign("OriginalPortDst", :@(UDPDst)),
                    Assign(UDPDst, SymbolicValue())),
                If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                    InstructionBlock(Assign("OriginalPortSrc", :@(ICMPIdentifier)),
                        Assign(ICMPIdentifier, SymbolicValue())),
                    NoOp)))
          )
      }
      case target : ConnmarkTarget => {
        val mask = target.getCtMask
        if (mask != target.getNfMask)
          throw new UnsupportedOperationException("CtMark is different from NfMark. Why?")
        if (target.isRestore())
        {
          // ctmark = nfmark
          if (mask == 0xFFFF)
          {
             Assign("CtMark_bottom", :@("NFMark_bottom"))
          }
          else if (mask == 0xFFFF0000)
          {
             Assign("CtMark_top", :@("NFMark_top"))
          }
          else 
          {
            throw new UnsupportedOperationException("No support for arbitrary masks")
          }
        }
        else
        {
          // nfmark = ctmark
          if (mask == 0xFFFF)
          {
              Assign("NFMark_bottom", :@("CtMark_bottom"))
          }
          else if (mask == 0xFFFF0000)
          {
              Assign("NFMark_top", :@("CtMark_top"))
          }
          else 
          {
            throw new UnsupportedOperationException("No support for arbitrary masks")
          }
        }
      }
      case target : MarkTarget => {
        val mask = target.getMask
        if (mask == 0xFFFF)
        {
          Assign("NFMark_bottom", ConstantValue(target.getValue))
        }
        else if (mask == 0xFFFF0000)
        {
          Assign("NFMark_top", ConstantValue(target.getValue))
        }
        else
        {
          throw new UnsupportedOperationException("No support for arbitrary masks")
        }
      }
      
      case _ =>
      {
        throw new UnsupportedOperationException("Not yet " + target.getClass)
      }
    }
  }
}
