package org.change.v2.abstractnet.neutron.elements

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.abstractnet.neutron.NeutronWrapper
import scala.collection.JavaConversions._
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.openstack4j.model.network.SecurityGroupRule
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames._
import org.change.v2.util.conversion.RepresentationConversion._
import org.openstack4j.model.network.options.PortListOptions
import org.openstack4j.model.network.IP
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.abstractnet.neutron.NeutronWrapper
import scala.collection.JavaConversions._
import org.change.v2.util.conversion.RepresentationConversion._
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.openstack4j.model.network.ext.Firewall
import org.openstack4j.model.network.ext.FirewallRule
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.analysis.memory.State
import java.io.PrintStream
import java.io.FileOutputStream
import java.io.File
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.openstack4j.model.network.ext.FirewallPolicy
import org.change.v2.abstractnet.neutron.elements.NetAbsElement
import org.change.v2.abstractnet.neutron.elements.NetFirewallPolicy
import org.openstack4j.model.network.Router
import org.openstack4j.openstack.networking.domain.NeutronRouter
import org.change.v2.abstractnet.neutron.elements.NetRouter
import java.io.BufferedInputStream
import java.io.FileInputStream
import org.change.v2.executor.clickabstractnetwork.executionlogging.OldStringifier._
import org.change.v2.abstractnet.neutron.elements.NeutronHelper._
import System.out._
import java.io.PrintWriter
import org.change.v2.analysis.executor.InstructionExecutor
import org.change.v2.analysis.executor.DecoratedInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.abstractnet.generic.GenericElement
import org.change.v2.abstractnet.generic.GenericElementBuilder
import org.change.v2.abstractnet.generic.ElementBuilder
import org.change.v2.abstractnet.neutron.Networking
import org.openstack4j.model.network.Port
import org.change.v2.abstractnet.neutron.ServiceBackedNetworking
import org.junit.Assert
import org.change.v2.abstractnet.linux.iptables.ConntrackGeneric
import org.change.v2.model.WorldModel
import org.change.v2.abstractnet.linux.iptables.ConntrackZone
import org.change.v2.iptables.StateDefinitions
import org.change.v2.abstractnet.linux.iptables.ConntrackConstants
import org.change.v2.model.Computer
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions._

class SecurityGroup(networking : Networking, port : Port, ingress : Boolean = false)  {
  lazy val secGrps = port.getSecurityGroups.map { x => networking.getSecurityGroup(x) }

  lazy val secRules = if (ingress)
  {

    secGrps.flatMap { x => 
      x.getRules.filter { 
        y => y.getDirection == "ingress" 
      }.toList 
    }
  }
  else
  {
    secGrps.flatMap { x => x.getRules.filter { y => y.getDirection == "egress" }.toList }
  }
  
  def getInstruction(secRule : SecurityGroupRule): Instruction =
  {
    val continueWith : Instruction = Assign("Matched", ConstantValue(1)) 
    
    val proto = secRule.getProtocol
    
    val protoInstr = translateProtocol(proto, secRule, continueWith)
    
    val remoteSpec = translateIPs(secRule, protoInstr)
    
    val etherType = translateEtherType(secRule, remoteSpec)

    If (Constrain("Matched", :==:(ConstantValue(0))),
        etherType,
        NoOp)
  }

  def translateProtocol(proto: String, secRule: org.openstack4j.model.network.SecurityGroupRule, continueWith: org.change.v2.analysis.processingmodels.Instruction) = {
    val protoInstr = if (proto != null && proto.equalsIgnoreCase("tcp"))
    {
      val rg = (secRule.getPortRangeMin, secRule.getPortRangeMax)
      if (rg._1 == null || rg._2 == null)
      {
        If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
            continueWith,
            NoOp)
      }
      else
      {
        If (Constrain(Proto, :==:(ConstantValue(TCPProto))),
            If (Constrain(
                if (ingress)
                  TcpSrc
                else
                  TcpDst, 
                  if (rg._1.intValue != rg._2.intValue())
                    :&:(:<=:(ConstantValue(rg._2.intValue())), 
                              :>=:(ConstantValue(rg._1.intValue())))
                  else
                    :==:(ConstantValue(rg._1.intValue))
                ),
                continueWith,
                NoOp),
            NoOp)
      }
      
    }
    else if (proto != null && proto.equalsIgnoreCase("udp"))
    {
      val rg = (secRule.getPortRangeMin, secRule.getPortRangeMax)
      if (rg._1 == null || rg._2 == null)
      {
        If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
            continueWith,
            NoOp)
      }
      else
      {
        If (Constrain(Proto, :==:(ConstantValue(UDPProto))),
            If (Constrain(
                if (ingress)
                  UDPSrc
                else
                  UDPDst,
                  if (rg._1.intValue != rg._2.intValue())
                    :&:(:<=:(ConstantValue(rg._2.intValue())), 
                              :>=:(ConstantValue(rg._1.intValue())))
                  else
                    :==:(ConstantValue(rg._1.intValue))
                ),
                continueWith,
                NoOp),
            NoOp)
      }
    }
    else if (proto != null && proto.equalsIgnoreCase("icmp"))
    {
      val rg = (secRule.getPortRangeMin, secRule.getPortRangeMax)
      if (rg._1 == null && rg._2 == null)
      {
        If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            continueWith,
            NoOp)
      }
      else if (rg._2 != null && rg._1 == null)
      {
        If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            If (Constrain(ICMPType, :==:(ConstantValue(rg._2.intValue()))),
                continueWith,
                NoOp))
      }
      else if (rg._2 == null && rg._1 != null)
      {
        If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            If (Constrain(ICMPCode, :==:(ConstantValue(rg._1.intValue()))),
                continueWith,
                NoOp))
      }
      else
      {
        If (Constrain(Proto, :==:(ConstantValue(ICMPProto))),
            If (Constrain(ICMPCode, :==:(ConstantValue(rg._2.intValue()))),
                If (Constrain(ICMPType, :==:(ConstantValue(rg._1.intValue()))),
                  continueWith,
                  NoOp),
                NoOp))
      }

    }
    else if (proto != null && proto.length() > 0)
    {
      val theProto = Integer.decode(proto).intValue
      If (Constrain(Proto, :==:(ConstantValue(theProto))),
          continueWith,
          NoOp)
    }
    else
    {
      continueWith
    }
    protoInstr
  }
  
  def translateEtherType(secRule : SecurityGroupRule, continueWith : Instruction)
   : Instruction = {
    val etherType = 
      if (secRule.getEtherType == "IPv4")
      {
        0x0800
      }
      else if (secRule.getEtherType == "IPv6")
      {
        0x86DD
      }
      else
      {
        throw new UnsupportedOperationException("No such ethertype")
      }
    If (Constrain(EtherType, :==:(ConstantValue(etherType))), continueWith, NoOp)
  }

  def translateIPs(secRule: SecurityGroupRule, continueWith: Instruction) = {
    if (secRule.getRemoteGroupId != null && secRule.getRemoteGroupId.length() != 0)
    {
      val remoteSg = networking.getSecurityGroup(secRule.getRemoteGroupId)
      val all = networking.getPorts.filter { x => x.getDeviceOwner == "compute:None" }
      val filtered = all.filter { x => x.getSecurityGroups.contains(secRule.getRemoteGroupId) }
      val ips = filtered.flatMap { x => {
          val ips1 = x.getFixedIps.toArray().map { x => x.asInstanceOf[IP] }.map { y => 
              val interval = 
              if (y.getIpAddress.contains("/"))
              {
                val split = y.getIpAddress.split("/")
                ipToNumber(split(0))
              }
              else
              {
                ipToNumber(y.getIpAddress)
              }
              interval
            }.toList
          val pairs = x.getAllowedAddressPairs
          val ips2 = 
          if (pairs != null && pairs.size() > 0)
          {
            pairs.map { y => 
              val interval = 
              if (y.getIpAddress.contains("/"))
              {
                val split = y.getIpAddress.split("/")
                ipToNumber(split(0))
              }
              else
              {
                ipToNumber(y.getIpAddress)
              }
              interval
            }.toList
          }
          else
          {
            List[Long]()
          }
          ips1 ++ ips2
        }
      }.toList
      
      def ruleOut(list : List[Long]) : FloatingConstraint = list match {
        case head :: Nil => :==:(ConstantValue(head, isIp = true))
        case head :: tail => :|:(ruleOut(head :: Nil), ruleOut(tail))
        case _ => throw new UnsupportedOperationException()
      }
      if (ips.size > 0)
        If(
        if (ingress)
          Constrain(IPSrc, ruleOut(ips))
        else
          Constrain(IPDst, ruleOut(ips)),
          continueWith,
          NoOp)
      else
      {
        NoOp
      }
    }
    else if (secRule.getRemoteIpPrefix != null && secRule.getRemoteIpPrefix.length() != 0)
    {
      val interval = 
              if (secRule.getRemoteIpPrefix.contains("/"))
              {
                val split = secRule.getRemoteIpPrefix.split("/")
                ipAndMaskToInterval(split(0), split(1))
              }
              else
              {
                ipAndMaskToInterval(secRule.getRemoteIpPrefix, "32")
              }
      If (
      if (ingress)
        Constrain(IPSrc, :&:(:<=:(ConstantValue(interval._2, isIp = true)), :>=:(ConstantValue(interval._1, isIp = true))))
      else
        Constrain(IPDst, :&:(:<=:(ConstantValue(interval._2, isIp = true)), :>=:(ConstantValue(interval._1, isIp = true)))),
        continueWith, NoOp)
    }
    else
    {
      continueWith
    }
  }
  
  lazy val forPort = port.getId
  def symnetCode() : Instruction = {
    if (!this.secGrps.isEmpty) {
      val lst = (Assign("Matched", ConstantValue(0)) :: secRules.map { getInstruction }.toList ) :+  (If (Constrain("Matched", :==:(ConstantValue(0))),
          Fail("No matches for security groups of " + this.forPort),
          if (ingress) {
            InstructionBlock(
              ConntrackCommit(port, networking).generateInstruction,
              Forward(s"SecurityGroup/$forPort/ingress/out")
            )
          } else {
            InstructionBlock(
              ConntrackCommit(port, networking).generateInstruction,
              Forward(s"SecurityGroup/$forPort/egress/out")
            )
          }))
      val ib = InstructionBlock(lst)
      InstructionBlock(
        Assign("ShouldTrack", ConstantValue(1)),
        ConntrackTrackZone(this.port, networking).generateInstruction(),
        If (Constrain("IsTracked".scopeTo(s"Port.${forPort}"), :==:(ConstantValue(1))),
          If (Constrain("IsFirst".scopeTo(s"Port.${forPort}"), :==:(ConstantValue(1))),
            ib,
            If (Constrain("IsForward".scopeTo(s"Port.${forPort}"), :==:(ConstantValue(1))),
                if (ingress)
                  Forward(s"SecurityGroup/$forPort/ingress/out")
                else
                  Forward(s"SecurityGroup/$forPort/egress/out"),
                If (Constrain("IsBackward".scopeTo(s"Port.${forPort}"), :==:(ConstantValue(1))),
                    if (ingress)
                      Forward(s"SecurityGroup/$forPort/ingress/out")
                    else
                      Forward(s"SecurityGroup/$forPort/egress/out")
                )
            )
          )
        )
      )
    } else {
      if (ingress) {
        Forward(s"SecurityGroup/$forPort/ingress/out")
      } else {
        Forward(s"SecurityGroup/$forPort/egress/out")
      }
    }
  }
}

// if packet is first => set forward expectation + set state = NEW + set packet IsFirst <- true
// if packet matches FW expectation => set IsForward = true, IsFirst <- false
// if packet matches BK expectation => set IsBackward <- true, IsFirst <- false if (State == NEW) => State <- ESTABLISHED
case class ConntrackTrackZone(port : Port, 
    networking : Networking) {
  import ConntrackConstants._
  
  lazy val ct = new ConntrackGeneric("Port." + port.getId)
  override def toString = ct.toString() + ".track()"
  def generateInstruction() : Instruction = {
    val assignFwExpectations = InstructionBlock(
            ct.installForward(),
            Assign("IsTracked".scopeTo(ct.getPrefix()), ConstantValue(1)),
            Assign("IsFirst".scopeTo(ct.getPrefix()), ConstantValue(1)),
            Assign("IsForward".scopeTo(ct.getPrefix()), ConstantValue(1)),
            Assign("IsBackward".scopeTo(ct.getPrefix()), ConstantValue(0)),
            Assign("State".scopeTo(ct.getPrefix()), ConstantValue(StateDefinitions.NAMES.get("NEW").intValue()))
        )
    If (Constrain("ShouldTrack", :==:(ConstantValue(1))),
      InstructionBlock(
        Forward(toString),
        If (Constrain("IsTracked".scopeTo(ct.getPrefix()), :==:(ConstantValue(0))),
          assignFwExpectations,
          If (Constrain("IsTracked".scopeTo(ct.getPrefix()), :==:(ConstantValue(1))),
            InstructionBlock(
              ct.checkIfForward(),
              ct.checkIfBackward(),
              // if IsBackward == 0 && IsForward == 0 =>
              // Wrong mapping => the packet is assumed new => install forward expectations
              If (Constrain("IsForward".scopeTo(ct.getPrefix()), :==:(ConstantValue(0))),
                If (Constrain("IsBackward".scopeTo(ct.getPrefix()), :==:(ConstantValue(0))),
                  assignFwExpectations,
                  Assign("IsFirst".scopeTo(ct.getPrefix()), ConstantValue(0))
                ),
                Assign("IsFirst".scopeTo(ct.getPrefix()), ConstantValue(0))
              )
            ),
            // i.e. "IsTracked" is not allocated => install forward expectation
            assignFwExpectations
          )
        ),
        ct.handleState
      )
    )
  }
}



case class ConntrackCommit(port : Port, networking : Networking) 
{
  lazy val ct = new ConntrackGeneric("Port." + port.getId)
  override def toString = ct.toString() + ".commit()"

  def generateInstruction() : Instruction = {
    If (Constrain("IsTracked".scopeTo(ct.getPrefix()), :==:(ConstantValue(1))),
       If (Constrain("IsFirst".scopeTo(ct.getPrefix()), :==:(ConstantValue(1))),
           InstructionBlock(
               Forward(toString),
               ct.installBackward()
           )
       )
    )
  }
}




object SecurityGroup {
  def main(argv : Array[String]) {
    val (apiAddr, userName, password, project) = readCredentials()
    val wrapper = new NeutronWrapper(apiAddr,
          userName,
          password,
          project)
    val server = wrapper.getOs.networking().port().list(PortListOptions.create().deviceOwner("compute:None")).get(0)
    val nw = new ServiceBackedNetworking(wrapper.getOs)
    val sec = new SecurityGroup(nw, server, true)
    var pw = new PrintWriter("code.txt")
    
    val exec = new DecoratedInstructionExecutor(new Z3Solver())
    val state = State.eher(State.bigBang(false))._1(0)
    
    val sc = sec.symnetCode()
    pw.println(sc)
    pw.close()
    val (ok, failed) = exec.execute(sc, state, true)
    println(ok.size)
    pw = new PrintWriter("ok.txt")
    pw.println(ok)
    pw.close()
    pw = new PrintWriter("fail.txt")
    pw.println(failed)
    pw.close()
  }
}

