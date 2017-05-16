package org.change.v2.abstractnet.linux.iptables


import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.BaseVisitor
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.Yes
import org.change.v2.model.iptables.MarkOption
import org.change.v2.analysis.processingmodels.instructions.FloatingConstraint
import org.change.v2.model.iptables.ConnmarkOption
import org.change.v2.model.iptables.PhysdevIsBridgedOption
import org.change.v2.model.iptables.MACOption
import org.change.v2.model.iptables.IcmpCodeOption
import org.change.v2.model.iptables.FragmentOption
import org.change.v2.model.iptables.ProtocolOption
import org.change.v2.model.iptables.PhysdevOutOption
import org.change.v2.model.iptables.GenericOption
import org.change.v2.model.iptables.PhysdevInOption
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.model.iptables.PortOption
import org.change.v2.analysis.memory.Tag
import org.change.v2.model.iptables.IcmpTypeOption
import org.change.v2.model.iptables.IPTablesRule
import org.change.v2.model.iptables.AddrOption
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.model.iptables.CtStateOption
import org.change.v2.model.iptables.StateOption
import org.change.v2.model.iptables.IfaceOption
import scala.collection.JavaConversions._
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.model.iptables.MasqueradeTarget
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.conversion.RepresentationConversion._


//
//class IPTablesIndexedVisitor(start : Int, 
//    stack : Stack[(String, Int)] = Stack[(String, Int)]()) extends BaseVisitor {
//  
//  private var instruction : Instruction = NoOp
//  
//  def getInstruction = instruction
//  
//  override def visit(chain : IPTablesChain) {
//    val theTarget = new TargetVisitor
//	  chain.getPolicy.accept(theTarget)
//	  val policyInstruction = theTarget.getInstruction match {
//	    case Forward("Return") => {
//	      if (stack.isEmpty)
//	            throw new Exception("Tried to return, but stack was already empty")
//        else
//        {
//          val (where, what) = stack.head
//          val visitor = new IPTablesIndexedVisitor(what + 1, stack.tail)
//          chain.getIPTablesTables.getIPTablesChains.find(_.getName == where).get.accept(visitor)
//          visitor.getInstruction
//        }
//	    }
//	    case _ => theTarget.getInstruction
//	  }
//	  
//	  
//	  val list = chain.getIPTablesRules.drop(start).map(s => {
//	    val newv = new RuleVisitor()
//	    s.accept(newv)
//	    newv
//	  }).foldRight(policyInstruction)((v, acc) => {
//	    val instructions = v.getMatches
//	    val target = v.getTarget
//	    val actual = target match {
//	      case Forward(where) => {
//	        if (where == "Return")
//	        {
//	          if (stack.isEmpty)
//	            throw new Exception("Tried to return, but stack was already empty")
//	          else
//	          {
//	            val (where, what) = stack.head
//	            val visitor = new IPTablesIndexedVisitor(what, stack.tail)
//	            chain.getIPTablesTables.getIPTablesChains.find(_.getName == where).get.accept(visitor)
//	            visitor.getInstruction
//	          }
//	        }
//	        else
//	        {
//            val visitor = new IPTablesIndexedVisitor(0, stack.push((chain.getName, start + 1)))
//	          chain.getIPTablesTables.getIPTablesChains.find(_.getName == where).get.accept(visitor)
//	          visitor.getInstruction
//	        }
//	      }
//	      case _ => target
//	    }
//	    instructions.foldRight(actual)((vv, accc) => {
//	      If (vv, accc, acc)
//	    })
//	  })
//  }
//}
//
//class IPTablesVisitor extends BaseVisitor {
//
//  private var instruction : Instruction = NoOp
//  
//  def getInstruction = instruction
//  
//	override def visit(ipTablesChain : IPTablesChain) {
//	  this.visit(ipTablesChain, 0)
//	}
//	
//	def visit(chain : IPTablesChain, start : Int, 
//	    stack : Stack[(String, Int)] = Stack[(String, Int)]()) {
//	  val visitor = new IPTablesIndexedVisitor(start, stack)
//	  chain.accept(visitor)
//	  this.instruction = visitor.getInstruction
//	}
//}
//
class IPTablesVisitor extends BaseVisitor {
  private var instructions : List[Instruction] = List[Instruction]()
  def getMatches = instructions
  
  private var instruction : Instruction = NoOp
  def getTarget = instruction

  override def visit(option : AddrOption) {
    val constraint = :&:(:>=:(ConstantValue(option.getStart)), 
        :<=:(ConstantValue(option.getEnd)))
    val actual = if (option.getNeg) :~:(constraint) else constraint
    instructions = List(if (option.getName == "src")
      Constrain(Tag("IPSrc"), actual)
    else if (option.getName == "dst")
      Constrain(Tag("IPDst"), actual)
    else
      throw new Exception("wrong name, should be src or dst"))
  }
  
  override def visit(option : ConnmarkOption) {

    val ctMask = option.getMask
    val ctMark = option.getValue
    val baseVarName = "CTMark__"
    instructions = handleMarkOptions(ctMask, ctMark, baseVarName)
  }

  def handleMarkOptions(ctMask: Long, ctMark: Long, baseVarName: String) = {
    List.range(0, 32).map(s => {
      val maski = ctMask >> s & 1
      val marki = ctMark >> s & 1
      
      val varName = baseVarName + s
      if (maski == 0)
      {
        Constrain(varName, Yes())
      }
      else
      {
        Constrain(varName, :==:(ConstantValue(marki)))
      }
    })
  }
  
  override def visit(option : CtStateOption) {
    val listof = option.getList.map(s => :==:(ConstantValue(s.intValue()))).toList
    val head :: tail = listof
    val constraint = tail.foldLeft(head.asInstanceOf[FloatingConstraint])((acc, value) => :|:(value, acc))
    val actual = if (option.getNeg) :~:(constraint) else constraint
    instructions = List(if (option.getIsTag)
      Constrain(Tag(option.getName), actual)
    else
      Constrain(option.getName, actual))
  }
  
  override def visit(option : FragmentOption) {
    val constraint = :==:(ConstantValue(1))
    List(Constrain(FragmentOfset, if (option.getNeg) :~:(constraint) else constraint))
  }
  
  override def visit(option : GenericOption) {
    this.visit(option.getNeg, option.getStartValue, option.getTagName, 
        option.getIsInterval, option.getEndValue, 
        option.getIsTag)
  }
  
  private def visit(neg : Boolean, 
      start : Long, 
      tagName : String, 
      isInterval : Boolean, 
      end : Long, 
      isTag : Boolean)  
  {
    val constraint = if (isInterval) :&:(:<=:(ConstantValue(end)), 
        :>=:(ConstantValue(start)))
    else :==:(ConstantValue(start))
    if (neg) :~:(constraint) else constraint
  
    this.instructions = List(if (isTag) Constrain(Tag(tagName), constraint)
    else Constrain(tagName, constraint))
  }
  
  override def visit(option : IcmpCodeOption) {
    //Long endValue, Long startValue, Boolean isInterval, Boolean isTag, String tagName
    this.visit(option.getNeg, 
        option.getTheCode.intValue(), 
        "IcmpCode", 
        false, 
        option.getTheCode.intValue, 
        true)
  }
  
  override def visit(option : IcmpTypeOption) {
    this.visit(option.getNeg, 
        option.getTheType.intValue(), 
        "IcmpType", 
        false, 
        option.getTheType.intValue, 
        true)
  }
  
  override def visit(option : IfaceOption) {
    this.visit(option.getNeg, 
        option.getIfaceNo.intValue(), 
        if (option.getIo == "in") "InputIface" else "OutputIface", 
        false, 
        option.getIfaceNo.intValue, 
        true)
  }
  
  override def visit(option : MACOption) {
    this.visit(option.getNeg, 
        macToNumber(option.getAddr), 
        "L2Src", false, macToNumber(option.getAddr), true)
  }
  
  override def visit(option : MarkOption) {
    val ctMask = option.getMask
    val ctMark = option.getValue
    val baseVarName = "CTMark__"
    instructions = handleMarkOptions(ctMask, ctMark, baseVarName)
  }
  
  override def visit(option : PhysdevInOption) {
    //neg, "PhysdevIn", value, false, false
    this.visit(option.getNeg, 
        option.getValue.intValue, 
        "PhysdevIn",
        false, 
        option.getValue.intValue,
        false)
        
  }
  
  override def visit(option : PhysdevOutOption) {
    //neg, "PhysdevIn", value, false, false
    this.visit(option.getNeg, 
        option.getValue.intValue, 
        "PhysdevIn",
        false, 
        option.getValue.intValue,
        false)
  }
  
  override def visit(option : PortOption) {
    this.visit(option.getNeg, 
        option.getPortNo.intValue, 
        if (option.getName == "sport") "L4SrcPort" else "L4DstPort",
        false, 
        option.getPortNo.intValue,
        true)
  }
  
  override def visit(option : PhysdevIsBridgedOption) {
    //neg, "PhysdevIn", value, false, false
    this.visit(option.getNeg, 1, "PhysdevIsBridged", false, 1, false)
  }
  
  override def visit(option : ProtocolOption) {
    this.visit(option.getNeg, 
        option.getProto, 
        "IPL4Proto", 
        false, 
        option.getProto, 
        true)
  }
  
  override def visit(option : StateOption) {
    this.visit(option.getNeg, 
        option.getValue.intValue(), 
        "State", 
        false, 
        option.getValue.intValue(), 
        false)
  }
}
//
//
//class TargetVisitor extends BaseVisitor {
//  private var instruction : Instruction = NoOp
//  
//  def getInstruction = instruction
//  
//  override def visit(target : AcceptTarget) {
//    this.instruction = NoOp
//  }
//  
//  override def visit(target : ChecksumTarget) {
//    this.instruction = NoOp
//  }
//  private def applySave(ctMask : Int, nfMask : Int)  : Instruction = {
//    InstructionBlock(List.range(0, 32).map(s => {
//      val cti = ctMask >> s & 1
//      val nfi = nfMask >> s & 1
//      if (cti == 1)
//      {
//        if (nfi == 0)
//        {
//          Assign("CTMark__" + s, ConstantValue(0))
//        }
//        else
//        {
//          If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
//              Assign("CTMark__" + s, ConstantValue(1)),
//              Assign("CTMark__" + s, ConstantValue(0)))
//        }
//      }
//      else
//      {
//        if (nfi == 0)
//        {
//          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
//              Assign("CTMark__" + s, ConstantValue(1)),
//              Assign("CTMark__" + s, ConstantValue(0)))
//        }
//        else
//        {
//          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
//              If (Constrain("NFMark__" + s, :==:(ConstantValue(0))),
//                Assign("CTMark__" + s, ConstantValue(1)),
//                Assign("CTMark__" + s, ConstantValue(0))),
//              If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
//                Assign("CTMark__" + s, ConstantValue(0)),
//                Assign("CTMark__" + s, ConstantValue(1))))
//        }
//      }
//      
//    }))
//    // ctmark = (ctmark & ~ctmask) ^ (nfmark & nfmask)
//  }
//  private def applyRestore(ctMask : Int, nfMask : Int) : Instruction = {
//    InstructionBlock(List.range(0, 32).map(s => {
//      val cti = ctMask >> s & 1
//      val nfi = nfMask >> s & 1
//      if (nfi == 1)
//      {
//        if (cti == 0)
//        {
//          Assign("NFMark__" + s, ConstantValue(0))
//        }
//        else
//        {
//          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
//              Assign("NFMark__" + s, ConstantValue(1)),
//              Assign("NFMark__" + s, ConstantValue(0)))
//        }
//      }
//      else
//      {
//        if (cti == 0)
//        {
//          If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
//              Assign("NFMark__" + s, ConstantValue(1)),
//              Assign("NFMark__" + s, ConstantValue(0)))
//        }
//        else
//        {
//          If (Constrain("CTMark__" + s, :==:(ConstantValue(1))),
//              If (Constrain("NFMark__" + s, :==:(ConstantValue(0))),
//                Assign("NFMark__" + s, ConstantValue(1)),
//                Assign("NFMark__" + s, ConstantValue(0))),
//              If (Constrain("NFMark__" + s, :==:(ConstantValue(1))),
//                Assign("NFMark__" + s, ConstantValue(0)),
//                Assign("NFMark__" + s, ConstantValue(1))))
//        }
//      }
//    }))
//    // nfmark = (nfmark & ~nfmask) ^ (ctmark & ctmask);
//  }
//  
//  override def visit(target : ConnmarkTarget) {
//    this.instruction = if (!target.isRestore()) applySave(target.getCtMask, target.getNfMask)
//    else applyRestore(target.getCtMask, target.getNfMask)
//  }
//  
//  override def visit(target : DNATTarget) {
//    this.instruction = InstructionBlock(Assign("DstIp", :@(Tag("IPDst"))),
//        Assign("DstPort", :@(Tag("L4DstPort"))),
//        Assign(Tag("IPDst"), ConstantValue(target.getIpAddress)))
//    //TODO: Please do this properly
//  }
//  
//  override def visit(target : SNATTarget) {
//    this.instruction = InstructionBlock(
//        Assign("InternalSrcIp", :@(IpSrc)),
//        Assign("InternalSrcPort", :@(Tag("L4Port"))),
//        Assign(Tag("IPSrc"), ConstantValue(target.getIpAddress)),
//        Assign(Tag("L4Port"), SymbolicValue()))
//  }
//  
//  override def visit(target : DropTarget) {
//    Fail("Explicit drop")
//  }
//  
//  override def visit(target : JumpyTarget) {
//    this.instruction = Forward(target.getName)
//  }
//  
//  override def visit(target : MarkTarget) {
//    val nfMask = target.getMask
//    val value  = target.getValue
//    val fieldName = "NFMark"
//    markTarget(nfMask, value, fieldName)
//  }
//
//  def markTarget(nfMask: Long, value: Long, fieldName: String) = {
//    this.instruction = InstructionBlock(List.range(0, 32).map(s => {
//      val nfi = (nfMask >> s) & 1
//      val vi  = (value >> s) & 1
//      if (nfi == 0)
//      {
//        Assign(fieldName + "__" + s, ConstantValue(vi))
//      }
//      else
//      {
//        if (vi == 1)
//        {
//          If (Constrain(fieldName + "__" + s, :==:(ConstantValue(0))),
//              Assign(fieldName + "__" + s, ConstantValue(1)),
//              Assign(fieldName + "__" + s, ConstantValue(0)))
//        }
//        else NoOp
//      }
//      
//    }))
//  } 
//  
//  override def visit(target : RedirectTarget) {
//    // TODO: Confused
//    this.instruction = NoOp
//  }
//  
//  override def visit(target : RejectTarget) {
//    this.instruction = Fail("Explicit reject")
//  }
//  override def visit(target : ReturnTarget) {
//    //TODO: Implement it in the chain
//    this.instruction = Forward("Return")
//  }
//  
//  override def visit(target : MasqueradeTarget)
//  {
//  }
//  
//  
//}
