package org.change.v2.abstractnet.neutron.elements

import java.lang.System.out._

import scala.collection.JavaConversions._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.abstractnet.neutron.elements.NetAbsElement
import org.change.v2.abstractnet.neutron.elements.NetFirewallPolicy
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.abstractnet.neutron.elements.NetFirewallRule
import org.change.v2.abstractnet.neutron.elements.NeutronHelper._
import org.change.v2.analysis.memory.Intable
import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.instructions.{Fail, Constrain, If, InstructionBlock, Forward}
import org.change.v2.executor.clickabstractnetwork.executionlogging.OldStringifier._
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.util.canonicalnames._
import org.openstack4j.model.network.Port
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.openstack4j.model.network.AllowedAddressPair



class ExitNetInstance(portId : String, wrapper : NeutronWrapper) {
  
  lazy val port = wrapper.getOs.networking().port()get(portId)

  def symnetCode() : Instruction = {
    val x = port
    val alld = x.getMacAddress :: x.getAllowedAddressPairs.toArray().map { x => x.asInstanceOf[AllowedAddressPair].getMacAddress }.toList
    val alldIps = x.getFixedIps.map(x => x.getIpAddress).toList ++ x.getAllowedAddressPairs.toArray().map { x => x.asInstanceOf[AllowedAddressPair].getIpAddress }.toList
    val netid = x.getNetworkId
    val allowed =
      If (Constrain(EtherSrc, :|:(alld.map {y => :==:(ConstantValue(RepresentationConversion.macToNumber(y))) }.toList)),
        if (!alldIps.isEmpty)
          If (Constrain(IPSrc, :|:(alldIps.map(y => :==:(ConstantValue(RepresentationConversion.ipToNumber(y)))))),
            Forward(s"AntiSpoof/$portId/egress/out"),
            Fail("No match for IP Source")
          )
        else
          Forward(s"AntiSpoof/$portId/egress/out"),
        Fail("No match for Eth Source")
      )
    allowed
  }
}


class EnterNetInstance(portId : String, wrapper : NeutronWrapper) {
  lazy val port = wrapper.getOs.networking().port().get(portId)

  def symnetCode(): Instruction = {
    val acc : Instruction =
    If (Constrain(EtherDst, :==:(ConstantValue(macToNumber(port.getMacAddress)))),
      Forward(s"AntiSpoof/$portId/ingress/out"),
      Fail("AntiSpoofing failed")
    )
    val allowedAddressPair = port.getAllowedAddressPairs().foldRight(acc)((p, acc2) => {
      If (Constrain(IPDst, :==:(ConstantValue(ipToNumber(p.getIpAddress)))),
        If (Constrain(EtherDst, :==:(ConstantValue(macToNumber(p.getMacAddress)))),
          Forward(s"AntiSpoof/$portId/ingress/out"),
          acc2
        ),
        acc2
      )
    })
    allowedAddressPair
  }
}


object ExitNetInstance {
  def main(args: Array[String]): Unit = {
    val neutronWrapper = NeutronHelper.neutronWrapperFromFile()
    val machine = neutronWrapper.getOs.compute().servers().list()(0)
    val port = neutronWrapper.getOs.networking().port().list().find(p => p.getDeviceId == machine.getId).get
    println(port.getDeviceOwner)
    val eni = new ExitNetInstance(port.getId, neutronWrapper)
    println(eni.symnetCode())
  }
}