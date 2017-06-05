package org.change.v2.abstractnet.linux.iptables

import scala.collection.JavaConversions._

import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.Computer
import org.change.v2.model.NIC
import org.change.v2.model.Namespace
import org.change.v2.model.WorldModel
import org.change.v2.model.iptables.AcceptTarget
import org.change.v2.model.iptables.AddrOption
import org.change.v2.model.iptables.ConnmarkTarget
import org.change.v2.model.iptables.CtStateOption
import org.change.v2.model.iptables.DNATTarget
import org.change.v2.model.iptables.DropTarget
import org.change.v2.model.iptables.FragmentOption
import org.change.v2.model.iptables.IPTablesMatch
import org.change.v2.model.iptables.IPTablesTarget
import org.change.v2.model.iptables.IcmpCodeOption
import org.change.v2.model.iptables.IcmpTypeOption
import org.change.v2.model.iptables.IfaceOption
import org.change.v2.model.iptables.JumpyTarget
import org.change.v2.model.iptables.MarkOption
import org.change.v2.model.iptables.MarkTarget
import org.change.v2.model.iptables.MasqueradeTarget
import org.change.v2.model.iptables.ProtocolOption
import org.change.v2.model.iptables.RejectTarget
import org.change.v2.model.iptables.ReturnTarget
import org.change.v2.model.iptables.SNATTarget
import org.change.v2.model.iptables.SimpleOption
import org.change.v2.util.canonicalnames._
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.abstractnet.neutron.elements.IfProto
import org.change.v2.iptables.StateDefinitions
import org.change.v2.model.iptables.PortOption
import org.change.v2.model.iptables.RedirectTarget
import org.change.v2.model.iptables.NATConfig
import org.change.v2.model.iptables.ChecksumTarget



case class EnterIPInterface(pcName : String, 
    iface : String, 
    worldModel : WorldModel) extends Instruction {
  
  override def toString = s"EnterIPInterface($pcName, $iface)"
  
  def generateInstruction() : Instruction = {
    val pc = worldModel.getComputer(pcName)
    val nic = pc.getNic(iface)
    
    var links = Map[String, String]()
   
    InstructionBlock(
      Forward(toString),
      Allocate("InputInterface"),
      Allocate("ShouldTrack"),
      Assign("ShouldTrack", ConstantValue(1)),
      Assign("InputInterface", ConstantValue(iface.hashCode())),
      Constrain(EtherDst, :==:(ConstantValue(RepresentationConversion.macToNumber(nic.getMacAddress), isMac = true))),
      new EnterIPTablesChain(pc, nic, "raw", "PREROUTING", worldModel),
      new ConntrackTrack(pc, nic, worldModel).generateInstruction(),
      new ConntrackUnSnat(pc, nic, worldModel).generateInstruction(),
      new EnterIPTablesChain(pc, nic, "mangle", "PREROUTING", worldModel),
      new DoNatPrerouting(pc, nic, worldModel).generateInstruction(),
      new ConntrackDnat(pc, nic, worldModel).generateInstruction(),
      new RoutingDecision(pc, nic, worldModel),
      If (Constrain("Decision", :==:(ConstantValue(0))),
        InstructionBlock(
            new EnterIPTablesChain(pc, nic, "mangle", "INPUT", worldModel),
            new EnterIPTablesChain(pc, nic, "filter", "INPUT", worldModel),
            new DeliverToLocalIface(pc, nic, worldModel)
        ),
        InstructionBlock(
            new EnterIPTablesChain(pc, nic, "mangle", "FORWARD", worldModel),
            new EnterIPTablesChain(pc, nic, "filter", "FORWARD", worldModel),
            new RoutingDecision(pc, nic, worldModel),
            new EnterIPTablesChain(pc, nic, "mangle", "POSTROUTING", worldModel),
            // do NAT iptables if first packet, else do ConntrackSnat()
            new DoNatPostRouting(pc, nic, worldModel).generateInstruction(),
            new ConntrackUnDnat(pc, nic, worldModel).generateInstruction(),
            new ConntrackSnat(pc, nic, worldModel).generateInstruction(),
            new ConntrackCommit(pc, nic, worldModel).generateInstruction(),
            // Ok, now... Look if the routing decided to send the packet out a bridged interface
            // If so, inject there. Else, do ARP traversal
            new EnterARPTable(pc, nic, worldModel),
            InstructionBlock(
                pc.getNamespaceForNic(iface).getNICs.filter(x => x.getMacAddress != null &&
                  x.getMacAddress.length != 0).foldRight(Fail("No such output interface") : Instruction)( (nnn, acc) => {
                    val x = nnn.getName
                    val mac = nnn.getMacAddress
                    If (Constrain("OutputInterface", :==:(ConstantValue(x.hashCode))),
                        Assign(EtherSrc, ConstantValue(RepresentationConversion.macToNumber(mac), isMac = true)),
                        acc
                    )
                  }),
                  pc.getBridgedNICs().foldRight(Forward("External World...") : Instruction)( (nnn, acc) =>  
                    {
                      val x = nnn.getName
                      If (Constrain("OutputInterface", :==:(ConstantValue(x.hashCode))),
                          InstructionBlock(
                            new EnterIface(pc.getName, x, worldModel)
                          ),
                          acc
                      )
                    })
             )
          )
              
      ),
      Deallocate("InputInterface"),
      Deallocate("ShouldTrack")
    )
  }
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}

object ConntrackConstants {
  val IS_TRACKED = "Conntrack.Tracked"
  val IP_SRC_FW = "Conntrack.FW.IPSrc"
  val IP_DST_FW  = "Conntrack.FW.IPDst"
  val PROTO_FW = "Conntrack.FW.proto"
  
  val TCP_SRC_FW = "Conntrack.FW.TcpSrc"
  
}

// if packet is first => set forward expectation + set state = NEW + set packet IsFirst <- true
// if packet matches FW expectation => set IsForward = true, IsFirst <- false
// if packet matches BK expectation => set IsBackward <- true, IsFirst <- false if (State == NEW) => State <- ESTABLISHED
case class ConntrackTrack(pc : Computer, nic : NIC, worldModel : WorldModel) 
  extends Instruction {
  import ConntrackConstants._
  
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    val assignFwExpectations = InstructionBlock(
            Forward("InstallForward"),
            ct.installForward(),
            Assign("IsTracked", ConstantValue(1)),
            Assign("IsFirst", ConstantValue(1)),
            Assign("State", ConstantValue(StateDefinitions.NAMES.get("NEW").intValue()))
        )
    If (Constrain("ShouldTrack", :==:(ConstantValue(1))),
      If (Constrain("IsTracked", :==:(ConstantValue(0))),
        assignFwExpectations,
        If (Constrain("IsTracked", :==:(ConstantValue(1))),
          InstructionBlock(
            ct.checkIfForward(),
            ct.checkIfBackward(),
            // if IsBackward == 0 && IsForward == 0 =>
            // Wrong mapping => the packet is assumed new => install forward expectations
            If (Constrain("IsForward", :==:(ConstantValue(0))),
              If (Constrain("IsBackward", :==:(ConstantValue(0))),
                assignFwExpectations
              )
            )
          ),
          // i.e. "IsTracked" is not allocated => install forward expectation
          assignFwExpectations
        )
      )
    )
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


case class ConntrackCommit(pc : Computer, nic : NIC, worldModel : WorldModel) 
  extends Instruction
{
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    If (Constrain("IsTracked", :==:(ConstantValue(1))),
       If (Constrain("IsFirst", :==:(ConstantValue(1))),
           InstructionBlock(
               Forward("ConntrackCommit"),
               ct.installBackward()
           )
       )
    )
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
} 


case class ConntrackUnSnat(pc : Computer, nic : NIC, worldModel : WorldModel) 
  extends Instruction
{
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    ct.unSnatBack()
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


case class ConntrackSnat(pc : Computer, nic : NIC, worldModel : WorldModel)
  extends Instruction
{
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    ct.snatApplyFw()
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


case class ConntrackUnDnat(pc : Computer, nic : NIC, worldModel : WorldModel)
  extends Instruction
{
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    ct.unDnatBack()
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


case class ConntrackDnat(pc : Computer, nic : NIC, worldModel : WorldModel)
  extends Instruction
{
  lazy val ct = new Conntrack(pc, nic, worldModel)
  
  def generateInstruction() : Instruction = {
    ct.dnatApplyFw()
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}



case class DeliverToLocalIface(pc : Computer, nic : NIC, worldModel : WorldModel) 
  extends Instruction {
  override def toString = s"DeliverLocal(${pc.getName}, ${nic.getName})"
  def generateInstruction() : Instruction = {
    val ns = pc.getNamespaceForNic(nic.getName)
    InstructionBlock(
      Forward(toString),
      ns.getNICs.filter { x => x.getMacAddress != null && x.getMacAddress.length > 0 }.foldRight(Fail("No interface with MAC == EtherDst") : Instruction)((x, acc) => {
        If (Constrain(EtherDst, :==:(ConstantValue(RepresentationConversion.macToNumber(x.getMacAddress)))),
          Forward(s"DeliveredLocally(${pc.getName}, ${nic.getName})"),
          acc
        )
      })
    )
  }
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
  
}


case class DoNatPostRouting(pc : Computer, nic : NIC, worldModel : WorldModel)
  extends Instruction
{
  override def toString = s"DoNatPostRouting(${pc.getName}, ${nic.getName})"

  def generateInstruction() : Instruction = {
    println(toString)
    InstructionBlock(
        Forward(toString),
        If (Constrain("IsFirst", :==:(ConstantValue(1))),
          // first packet goes through nat postrouting.
          new EnterIPTablesChain(pc, nic, "nat", "POSTROUTING", worldModel)
        )
    )
  }
  
  override def apply(state : State, verbose : Boolean) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


case class DoNatPrerouting(pc : Computer, nic : NIC, worldModel : WorldModel) 
  extends Instruction {
  override def toString = s"EnterARPTable(${pc.getName}, ${nic.getName})"

  def generateInstruction() : Instruction = {
    InstructionBlock(
      Forward(toString),
      If (Constrain("IsFirst", :==:(ConstantValue(1))),
          // i.e. the packet is the first one in the connection
          new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel)
      )
    )
  }
  override def apply(state : State, verbose : Boolean) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}

case class EnterARPTable(pc : Computer, iface : NIC, worldModel : WorldModel) 
  extends Instruction
{
  override def toString = s"EnterARPTable(${pc.getName}, ${iface.getName})"
  
  def inferArpRules() : Instruction = {
    val arps = worldModel.getInferredArpTables()
    arps.foldRight(Forward("ExternalWorld maybe...") : Instruction)( (x, acc) => {
      If (Constrain("RoutingNextHop", :==:(ConstantValue(x._1, isIp = true))),
        InstructionBlock(
          Assign(EtherDst, (ConstantValue(x._2, isMac = true)))
//          new EnterIface(pc.getName, iface.getName, worldModel)
        ),
        acc
      )
    })
  }
  
  
  def generateInstructions() : Instruction = {
    println(toString)
    InstructionBlock(
      Forward(toString),
      worldModel.getPcs.flatMap { x => x.getNamespaces }.flatMap { y => y.getNICs }.
      filter { x => x.getMacAddress != null && x.getMacAddress.length() > 0 }.
      foldRight(inferArpRules()) ((x, acc) => {
        val ipAddr = x.getIPAddresss
        ipAddr.foldRight(acc)((y, acc2) => {
          If (Constrain("RoutingNextHop", :==:(ConstantValue(y.getAddress, isIp = true))),
            InstructionBlock(
                Assign(EtherDst, ConstantValue(RepresentationConversion.macToNumber(x.getMacAddress), isMac = true))
//                new EnterIface(pc.getName, iface.getName, worldModel)
            ),
            acc2
          )
        })
      })
    )
  }
  override def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    val arp = generateInstructions()
    arp(state, verbose)
  }
}




object SNATExtension {
  
  def snatV2(prefix : String, to : Long, portRange : (Long, Long)) : Instruction = {
    InstructionBlock(
      Assign("SNAT", ConstantValue(1)),
      Assign("SNAT.IPSrc", ConstantValue(to)),
      Assign("SNAT.Proto", :@(Proto)),
      If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
          If(Constrain("BK.TCPDst", :==:(:@(TcpDst))),
            If (Constrain("FW.TCPSrc", :==:(:@(TcpSrc))),
              Assign("IsForward", ConstantValue(1))
            )
          ),
          If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
            If(Constrain("FW.UDPDst", :==:(:@(UDPDst))),
              If (Constrain("FW.UDPSrc", :==:(:@(UDPSrc))),
                Assign("IsForward", ConstantValue(1))
              )
            ),
            If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
              If (Constrain("FW.ICMPCode", :==:(:@(ICMPCode))),
                If (Constrain("FW.ICMPType", :==:(:@(ICMPType))),
                  Assign("IsForward", ConstantValue(1))
                )
              ),
              Assign("IsForward", ConstantValue(1))
            )
          )
        )
    )
  }
  
  
  def dnat(prefix : String, 
      nc : NATConfig = NATConfig.create(), 
      default : Instruction = NoOp, 
      changeIp : Boolean = true) : Instruction = {
    val manip = "DNAT"
    nat(prefix, nc, default, manip, changeIp)
  }
  
  def nat(prefix : String, 
      nc : NATConfig, 
      default : Instruction, 
      manip : String, 
      changeIp : Boolean = true) : Instruction = {
    val defaultInstr = default
    val pprefix = if (prefix == "" || prefix.endsWith(".")) prefix else prefix + "." 
    val modIp = if (manip == "DNAT")
                IPDst
              else
                IPSrc
    
    val modTcp = if (manip == "DNAT")
                TcpDst
              else
                TcpSrc
    
    val modUdp = if (manip == "DNAT")
                UDPDst
              else
                UDPSrc
                
    
    InstructionBlock(
      Forward(s"${manip}($pprefix)"),
      Assign(pprefix + s"$manip.IPSrcOrig", :@(IPSrc)),
      Assign(pprefix + s"$manip.IPDstOrig", :@(IPDst)),
      IfProto(TCPProto,
        InstructionBlock(
          Assign(pprefix + s"$manip.TCPSrcOrig", :@(TcpSrc)),
          Assign(pprefix + s"$manip.TCPDstOrig", :@(TcpDst)),
          Assign(modTcp, SymbolicValue()),
          Constrain(modTcp, 
              if (!nc.isUniquePort)
                :&:(:<=:(ConstantValue(nc.getPortEnd)), :>=:(ConstantValue(nc.getPortStart)))
              else
                :==:(ConstantValue(nc.getPort))),
          Assign(pprefix + s"$manip.TCPMod", :@(TcpSrc))
        ),
        IfProto(UDPProto,
          InstructionBlock(
            Assign(pprefix + s"$manip.UDPSrcOrig", :@(UDPSrc)),
            Assign(pprefix + s"$manip.UDPDstOrig", :@(UDPDst)),
            Assign(modUdp, SymbolicValue()),
            Constrain(modUdp, 
              if (!nc.isUniquePort)
                :&:(:<=:(ConstantValue(nc.getPortEnd)), :>=:(ConstantValue(nc.getPortStart)))
              else
                :==:(ConstantValue(nc.getPort))),
            Assign(pprefix + s"$manip.UDPMod", :@(UDPSrc))
          )
        )
      ),
      if (changeIp)
      {
          if (!nc.isUniqueIp)
          {
            InstructionBlock(
              Assign(modIp, SymbolicValue()),
              Constrain(modIp, :&:(:<=:(ConstantValue(nc.getIpStart, isIp = true)), :>=:(ConstantValue(nc.getIpEnd, isIp = true))))
            )
          }
          else
            Assign(modIp, ConstantValue(nc.getIp, isIp = true))
      }
      else
      {
        NoOp
      },
      Assign(pprefix + s"$manip.IPMod", :@(modIp)),
      Assign(pprefix + s"$manip.IPProto", :@(Proto)),
      Assign(pprefix + s"$manip.Is$manip", ConstantValue(1)),
      defaultInstr
    )
  }
  
  
  
  def snat(prefix : String, nc : NATConfig = NATConfig.create(), default : Instruction = NoOp,
      changeIp : Boolean = true) : Instruction = {
    val manip = "SNAT"
    nat(prefix, nc, default, manip, changeIp)
  }
  
  
  def unNat(pprefix : String, manip : String) = {
    val modIp = if (manip == "DNAT")
                IPSrc
              else
                IPDst
    
    val modTcp = if (manip == "DNAT")
                TcpSrc
              else
                TcpDst
    
    val modUdp = if (manip == "DNAT")
                UDPSrc
              else
                UDPDst
    val srcOrDst = if (manip == "DNAT")
      "Dst"
    else
      "Src"
    
    If (Constrain(pprefix + s"$manip.Is$manip", :==:(ConstantValue(1))),
      If (Constrain("IsTracked", :==:(ConstantValue(1))),
         If (Constrain("IsBackward", :==:(ConstantValue(1))),
           InstructionBlock( 
             Forward(s"Un${manip}($pprefix)"),
             IfProto (TCPProto,
                InstructionBlock(
                  Assign(modTcp, :@(pprefix + s"$manip.TCP${srcOrDst}Orig"))
                ),
                IfProto(UDPProto,
                  InstructionBlock(
                    Assign(modUdp, :@(pprefix + s"$manip.UDP${srcOrDst}Orig"))
                  )
                )
              ),
              Assign(modIp, :@(pprefix + s"$manip.IP${srcOrDst}Orig"))
           )
         )
      )
    )
  }
  
  def unSnat(prefix : String) : Instruction = {
    val manip = "SNAT"
    val pprefix = if (prefix == "" || prefix.endsWith(".")) prefix else prefix + "." 
    unNat(pprefix, manip)
  }
  
  def unDnat(prefix : String) : Instruction = {
    val manip = "DNAT"
    val pprefix = if (prefix == "" || prefix.endsWith(".")) prefix else prefix + "." 
    unNat(pprefix, manip)
  }
  
  def natExisting(pprefix : String, manip : String) : Instruction = {
    val modIp = if (manip == "DNAT")
                IPDst
              else
                IPSrc
    
    val modTcp = if (manip == "DNAT")
                TcpDst
              else
                TcpSrc
    
    val modUdp = if (manip == "DNAT")
                UDPDst
              else
                UDPSrc
    If (Constrain(pprefix + s"$manip.Is$manip", :==:(ConstantValue(1))),
      If (Constrain("IsTracked", :==:(ConstantValue(1))),
         If (Constrain("IsForward", :==:(ConstantValue(1))),
           InstructionBlock(
             Forward(s"${manip}Existing($pprefix)"),
             IfProto (TCPProto,
                InstructionBlock(
                  Assign(modTcp, :@(pprefix + s"$manip.TCPMod"))
                ),
                IfProto(UDPProto,
                  InstructionBlock(
                    Assign(modUdp, :@(pprefix + s"$manip.UDPMod"))
                  )
                )
              ),
              Assign(modIp, :@(pprefix + s"$manip.IPMod"))
           )
         )
      )
    )
  }
  
  def snatExisting(prefix : String) : Instruction = {
    val manip = "SNAT"
    val pprefix = if (prefix == "" || prefix.endsWith(".")) prefix else prefix + "." 
    natExisting(pprefix, manip)
  }
  def dnatExisting(prefix : String) : Instruction = {
    val manip = "DNAT"
    val pprefix = if (prefix == "" || prefix.endsWith(".")) prefix else prefix + "." 
    natExisting(pprefix, manip)
  }
  
  
}


case class Conntrack(pc : Computer, iface : NIC, worldModel : WorldModel)
  extends Instruction
{
  lazy val ns = pc.getNamespaceForNic(iface.getName)
  lazy val prefix =  ns.getComputer.getName + "." + ns.getName
  
  override def toString = s"Conntrack(${pc.getName}, ${iface.getName})"
  
  def installForward() : Instruction = {
    InstructionBlock(
      Assign(IPTablesConstants.CTMARK_BOTTOM, ConstantValue(0)),
      Assign(IPTablesConstants.CTMARK_TOP, ConstantValue(0)),
      Assign("FW.IPSrc", :@(IPSrc)),
      Assign("FW.IPDst", :@(IPDst)),
      Assign("FW.Proto", :@(Proto)),
      If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
        InstructionBlock(
          Assign("FW.TCPSrc", :@(TcpSrc)),
          Assign("FW.TCPDst", :@(TcpDst))
        ),
        If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
          InstructionBlock(
            Assign("FW.UDPSrc", :@(UDPSrc)),
            Assign("FW.UDPDst", :@(UDPDst))
          ),
          If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            InstructionBlock(
              Assign("FW.ICMPType", :@(ICMPType)),
              Assign("FW.ICMPCode", :@(ICMPCode)),
              Assign("FW.ICMPId", :@(ICMPIdentifier))
            )
          )
        )
      )
    )
  }
  
  def installBackward() : Instruction = {
    InstructionBlock(
      Assign("BK.IPSrc", :@(IPDst)),
      Assign("BK.IPDst", :@(IPSrc)),
      Assign("BK.Proto", :@(Proto)),
      If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
        InstructionBlock(
          Assign("BK.TCPDst", :@(TcpSrc)),
          Assign("BK.TCPSrc", :@(TcpDst))
        ),
        If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
          InstructionBlock(
            Assign("BK.UDPDst", :@(UDPSrc)),
            Assign("BK.UDPSrc", :@(UDPDst))
          ),
          If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            InstructionBlock(
              Assign("BK.ICMPType", :@(ICMPType)),
              Assign("BK.ICMPCode", :@(ICMPCode)),
              Assign("BK.ICMPId", :@(ICMPIdentifier))
            )
          )
        )
      )
    )
  }
  
  
  def checkIfForward() : Instruction = {
    InstructionBlock(
      Assign("IsForward", ConstantValue(0)),
      If (Constrain("FW.IPSrc", :==:(:@(IPSrc))),
       If (Constrain("FW.IPDst", :==:(:@(IPDst))),
         If (Constrain("FW.Proto", :==:(:@(Proto))),
           If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
            If(Constrain("FW.TCPDst", :==:(:@(TcpDst))),
              If (Constrain("FW.TCPSrc", :==:(:@(TcpSrc))),
                InstructionBlock(
                  Assign("IsForward", ConstantValue(1)),
                  Assign("IsFirst", ConstantValue(0))
                )
              )
            ),
            If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
              If(Constrain("FW.UDPDst", :==:(:@(UDPDst))),
                If (Constrain("FW.UDPSrc", :==:(:@(UDPSrc))),
                  InstructionBlock(
                    Assign("IsForward", ConstantValue(1)),
                    Assign("IsFirst", ConstantValue(0))
                  )
                )
              ),
              If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                If (Constrain("FW.ICMPCode", :==:(:@(ICMPCode))),
                  If (Constrain("FW.ICMPType", :==:(:@(ICMPType))),
                    InstructionBlock(
                      Assign("IsForward", ConstantValue(1)),
                      Assign("IsFirst", ConstantValue(0))
                    )
                  )
                ),
                InstructionBlock(
                  Assign("IsForward", ConstantValue(1)),
                  Assign("IsFirst", ConstantValue(0))
                )
              )
            )
          )
         )
       )   
      )
    )
  }
  
  
  def checkIfBackward() : Instruction = {
    InstructionBlock(
      Assign("IsBackward", ConstantValue(0)),
      If (Constrain("BK.IPSrc", :==:(:@(IPSrc))),
       If (Constrain("BK.IPDst", :==:(:@(IPDst))),
         If (Constrain("BK.Proto", :==:(:@(Proto))),
           If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
              If(Constrain("BK.TCPDst", :==:(:@(TcpDst))),
                If (Constrain("BK.TCPSrc", :==:(:@(TcpSrc))),
                  Assign("IsBackward", ConstantValue(1))
                )
              ),
              If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
                If(Constrain("BK.UDPDst", :==:(:@(UDPDst))),
                  If (Constrain("BK.UDPSrc", :==:(:@(UDPSrc))),
                    Assign("IsBackward", ConstantValue(1))
                  )
                ),
                If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
                  If (Constrain("BK.ICMPCode", :==:(:@(ICMPCode))),
                    If (Constrain("BK.ICMPType", :==:(:@(ICMPType))),
                      Assign("IsBackward", ConstantValue(1))
                    )
                  ),
                  Assign("IsBackward", ConstantValue(1))
                )
              )
            )
         )
       )   
      )
    )
  }
  
  def unSnatBack() : Instruction = {
     SNATExtension.unSnat(prefix = prefix)
  }
  
  def unDnatBack() : Instruction = {
    SNATExtension.unDnat(prefix = prefix)
  }
  
  
  /**
   * in POSTROUTING chain!!!! After IPTABLES Postrouting
   * To be read => Apply the SNAT mappings decided upon in 
   * POSTROUTING iptables nat chain upon first packet. Be careful to update 
   * the backward expectations in the iptables nat
   * POSTROUTING chain and children 
   */
  def snatApplyFw() : Instruction = {
    SNATExtension.snatExisting(prefix)
  }
  
  def dnatApplyFw() : Instruction = {
    SNATExtension.snatExisting(prefix)
  }
  
  
  def generateInstruction() : Instruction = {
    
    InstructionBlock(
     Forward(toString),
     checkIfBackward,
     checkIfForward,
     If (Constrain("IsBackward", :==:(ConstantValue(1))),
       unSnatBack,
       If (Constrain("IsForward", :==:(ConstantValue(0))),
         InstructionBlock(
           installBackward,
           installForward
         )
       )
     )
    )
  }
  
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}



case class RoutingDecision(pc : Computer, iface : NIC, worldModel : WorldModel)
  extends Instruction
{
  
  override def toString = s"RoutingDecision(${pc.getName}, ${iface.getName})"
  
  def getRouteToLocal(continueWith : Instruction) : Instruction = {
    val ns = pc.getNamespaceForNic(iface.getName)
    val nics = ns.getNICs
    nics.foldRight(continueWith)((x, acc) => {
      val ips = x.getIPAddresss
      ips.foldRight(acc)((y, acc2) => {
         If (Constrain(IPDst, :==:(ConstantValue(y.getAddress))),
          if (x.getMacAddress != null && x.getMacAddress.length() > 0)
            If (Constrain(EtherDst, :==:(ConstantValue(RepresentationConversion.macToNumber(x.getMacAddress)))),
              InstructionBlock(
                Assign("Decision", ConstantValue(0)),
                Assign("OutputInterface", ConstantValue(x.getName.hashCode())), 
                Assign("EtherSourceFinal", ConstantValue(RepresentationConversion.macToNumber(x.getMacAddress)))
              ),
              Fail("Wrong IPDst/EtherDst combination")
            )
          else 
            InstructionBlock(
              Assign("Decision", ConstantValue(0)),
              Assign("OutputInterface", ConstantValue(x.getName.hashCode())),
              if (x.getMacAddress != null && x.getMacAddress.length > 0)
                Assign("EtherSourceFinal", ConstantValue(RepresentationConversion.macToNumber(x.getMacAddress)))
              else
                NoOp
            ),
          acc
        )
      })

    })
  }
  
  def generateInstruction() : Instruction = {
    val ns = pc.getNamespaceForNic(iface.getName)
    val rts = ns.getRoutingTables.toList.flatMap { x => x.getRoutes.toList }.sortWith((x, y) => y.getNetAddress.getMask < x.getNetAddress.getMask)
    val instrs = rts.foldRight(Fail("No route to host") : Instruction) { (x, acc) => {
        val ipRange = RepresentationConversion.ipAndMaskToInterval(x.getNetAddress.getAddress, x.getNetAddress.getMask.intValue())
          If (Constrain(IPDst, :&:(:<=:(ConstantValue(ipRange._2, true)), :>=:(ConstantValue(ipRange._1, true)))),
            InstructionBlock(
                Assign("Decision", ConstantValue(1)),
                if (x.getNextHop.getAddress == 0)
                {
                  Assign("RoutingNextHop", :@(IPDst))  
                }
                else
                {
                  Assign("RoutingNextHop", ConstantValue(x.getNextHop.getAddress))
                },
                Assign("OutputInterface", ConstantValue(x.getNic.getName.hashCode())), 
                if (x.getNic.getMacAddress != null && x.getNic.getMacAddress.length > 0)
                  Assign("EtherSourceFinal", ConstantValue(RepresentationConversion.macToNumber(x.getNic.getMacAddress)))
                else
                  NoOp
            ),
            acc
          )
      }
    }
    InstructionBlock(Forward(toString), getRouteToLocal(instrs))
  }
  
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
}


object IPTablesConstants {
  val NFMARK_BOTTOM  = "NfMark_bottom"
  val NFMARK_TOP = "NfMark_top"
  val CTMARK_BOTTOM = "CtMark_bottom"
  val CTMARK_TOP = "CtMark_top"
}

case class EnterIPTablesChain(pc : Computer, 
    iface : NIC, 
    table : String, 
    chain : String, 
    world : WorldModel,
    isInitial : Boolean = true)
  extends Instruction
{
  
  
  
  
  override def toString = s"EnterIPTablesChain(${pc.getName}, ${iface.getName}, $table, $chain)"
  
  
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
                List[Instruction](Constrain("InputInterface", 
                    {
                      val nicNames = :|:(pc.getNicsByFilter(y.getIfaceName).map { x => :==:(ConstantValue(x.getName.hashCode)) }.toList)
                      if (y.getNeg)
                      {
                        :~:(nicNames)
                      }
                      else
                      {
                        (nicNames)
                      }
                    }
                  )
                )
              }
              else if (y.getIo == "out")
              {
                List[Instruction](Constrain("OutputInterface", 
                    {
                      val nicNames = :|:(pc.getNicsByFilter(y.getIfaceName).map { x => :==:(ConstantValue(x.getName.hashCode)) }.toList)
                      if (y.getNeg)
                      {
                        :~:(nicNames)
                      }
                      else
                      {
                        (nicNames)
                      }
                    }
                  )
                )
              }
              else
              {
                throw new UnsupportedOperationException("Unknown iface type " + y.getIo)
              }
            }
            case y : AddrOption => {
              def generateRangeConstraint(x : AddrOption) = {
                if (y.getStart == y.getEnd)
                  :==:(ConstantValue(y.getStart, isIp = true))
                else
                  :&:(:<=:(ConstantValue(y.getEnd, isIp = true)), :>=:(ConstantValue(y.getStart, isIp = true)))
              }
              
              val ct = generateRangeConstraint(y)
              
              if (y.getName == "src")
              {
                List[Instruction](Constrain(IPSrc, 
                    if (y.getNeg) :~:(ct)
                    else ct)
                  )
              }
              else if (y.getName == "dst")
              {
                List[Instruction](Constrain(IPDst,
                    if (y.getNeg) :~:(ct)
                    else ct)
                  )
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
              val ored = computeOr(tagList)
              
               List[Instruction](Constrain("State", if (y.getNeg) :~:(ored) else ored))
            }
            case y : MarkOption => {
              if (y.getName == "nfmark")
              {
                if (y.getMask == 0xFFFFL)
                {
                  List[Instruction](Constrain(IPTablesConstants.NFMARK_BOTTOM, 
                      if (!y.getNeg)
                        :==:(ConstantValue(y.getValue))
                      else
                        :~:(:==:(ConstantValue(y.getValue)))
                     )
                   )
                }
                else if (y.getMask == 0xFFFF0000L)
                {
                   List[Instruction](Constrain(IPTablesConstants.NFMARK_TOP,  
                      if (!y.getNeg)
                        :==:(ConstantValue(y.getValue))
                      else
                        :~:(:==:(ConstantValue(y.getValue)))
                     )
                   )
                }
                else
                {
                  throw new UnsupportedOperationException("Cannot handle arbitrary masks")
                }
              }
              else if (y.getName == "ctmark")
              {
                if (y.getMask == 0xFFFFL)
                {
                  List[Instruction](Constrain(IPTablesConstants.CTMARK_BOTTOM,  
                      if (!y.getNeg)
                        :==:(ConstantValue(y.getValue))
                      else
                        :~:(:==:(ConstantValue(y.getValue)))
                     )
                   )
                }
                else if (y.getMask == 0xFFFF0000L)
                {
                   List[Instruction](Constrain(IPTablesConstants.CTMARK_TOP,  
                      if (!y.getNeg)
                        :==:(ConstantValue(y.getValue))
                      else
                        :~:(:==:(ConstantValue(y.getValue)))
                     )
                   )
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
            case y : PortOption => {
              y.getPortType match {
                case PortOption.PortType.TCP if (y.getName == "sport") => List[Instruction](Constrain(TcpSrc, :==:(ConstantValue(y.getPortNo.longValue()))))
                case PortOption.PortType.TCP if (y.getName == "dport") => List[Instruction](Constrain(TcpDst, :==:(ConstantValue(y.getPortNo.longValue()))))
                case PortOption.PortType.UDP if (y.getName == "sport") => List[Instruction](Constrain(UDPSrc, :==:(ConstantValue(y.getPortNo.longValue()))))
                case PortOption.PortType.UDP if (y.getName == "dport") => List[Instruction](Constrain(UDPDst, :==:(ConstantValue(y.getPortNo.longValue()))))
              } 
            }
            case _ => throw new UnsupportedOperationException("Cannot handle " + y.toString())
          }
          ctList
        })
        res1.toList
  }
                        
  def generateInstruction() : Instruction = {
    val rules = maybeChain.getIPTablesRules
    val ib = InstructionBlock(
      Forward(toString),
      Allocate("Continue"),
      Assign("Continue", ConstantValue(1)),
      InstructionBlock(rules.map (x => {
        val matches = x.getIPTablesMatches.flatMap {mapMatch}.foldRight(
          InstructionBlock(
            Assign("Continue", ConstantValue(0)),
            fireTarget(x.getIPTablesTarget)
          ) : Instruction
        ) { (v, acc) => {
              If (v, 
                  acc,
                  Assign("Continue", ConstantValue(1))
              )
            }
          }
          If (Constrain("Continue", :==:(ConstantValue(1))),
              matches,
              NoOp
          )
        })
      ),
      If (Constrain("Continue", :==:(ConstantValue(1))),
          fireTarget(maybeChain.getPolicy),
          NoOp
      ),
      Deallocate("Continue")
    )
    ib
  }
                        
  def apply(state : State, verbose : Boolean) : (List[State], List[State]) = {
    println(toString)
    generateInstruction()(state, verbose)
  }
                       
  def fireTarget(target : IPTablesTarget) : Instruction = {
    target match {
      case target : AcceptTarget => InstructionBlock(
        Assign("IsAccept", ConstantValue(1)),
        Assign("Continue", ConstantValue(0))
      )
      case target : RejectTarget => InstructionBlock(
        Assign("Continue", ConstantValue(0)), 
        Fail("Reject")
      )
      case target : DropTarget => InstructionBlock(
        Assign("Continue", ConstantValue(0)), 
        Fail("Reject")
      )
      case target : ReturnTarget  => {
        if (maybeChain.isDefault())
        {
          fireTarget(maybeChain.getPolicy)
        }
        else
        {
          Assign("Continue", ConstantValue(0))
        }
      }
      case target : JumpyTarget => {
        InstructionBlock(
            new EnterIPTablesChain(pc : Computer, 
              iface : NIC, 
              table : String, 
              target.getName : String, 
              world : WorldModel,
              false).generateInstruction(),
          If (Constrain("IsAccept", :==:(ConstantValue(1))),
              Assign("Continue", ConstantValue(0)),
              Assign("Continue", ConstantValue(1))
          )
        )
      }
      case target : MasqueradeTarget => {
        throw new UnsupportedOperationException("No masquerade yet")
      }
      case target : SNATTarget => {
        val nc = NATConfig.create().setIp(target.getIpAddress)
        SNATExtension.snat(prefix = 
//          ns.getComputer.getName + "." + ns.getName,
            "",
          nc = nc)
      }
      case target : DNATTarget => {
        val nc = NATConfig.create().setIp(target.getIpAddress)
        SNATExtension.dnat(prefix = 
//          ns.getComputer.getName + "." + ns.getName,
            "",
          nc = nc)
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
             Assign(IPTablesConstants.NFMARK_BOTTOM, :@(IPTablesConstants.CTMARK_BOTTOM))
          }
          else if (mask == 0xFFFF0000)
          {
             Assign(IPTablesConstants.NFMARK_BOTTOM, :@(IPTablesConstants.CTMARK_TOP))
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
              Assign(IPTablesConstants.NFMARK_BOTTOM, :@(IPTablesConstants.CTMARK_BOTTOM))
          }
          else if (mask == 0xFFFF0000)
          {
              Assign(IPTablesConstants.NFMARK_TOP, :@(IPTablesConstants.CTMARK_TOP))
          }
          else 
          {
            throw new UnsupportedOperationException("No support for arbitrary masks")
          } 
        }
      }
      case target : MarkTarget => {
        val mask = target.getMask
        if (mask == 0xFFFFL)
        {
          Assign(IPTablesConstants.NFMARK_BOTTOM, ConstantValue(target.getValue))
        }
        else if (mask == 0xFFFF0000L)
        {
          Assign(IPTablesConstants.NFMARK_TOP, ConstantValue(target.getValue))
        }
        else
        {
          throw new UnsupportedOperationException("No support for arbitrary masks")
        }
      }
      case target : RedirectTarget => {
        val toPortStart = target.getToPortStart
        val toPortEnd = target.getToPortEnd
        val nc = NATConfig.create().setPortStart(toPortStart).setPortEnd(toPortEnd)
        SNATExtension.dnat(prefix = 
//          ns.getComputer.getName + "." + ns.getName,
            "",
          nc = nc, 
          changeIp = false)
      }
      // Not yet implemented, but required as to not get unsupportedoperation
      case target : ChecksumTarget => NoOp
      case _ =>
      {
        throw new UnsupportedOperationException("Not yet " + target)
      }
    }
  }
}
