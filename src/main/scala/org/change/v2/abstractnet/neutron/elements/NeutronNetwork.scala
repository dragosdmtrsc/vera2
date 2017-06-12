package org.change.v2.abstractnet.neutron.elements

import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.openstack4j.model.network.Network

import scala.collection.JavaConversions._
import java.util.stream.Collectors

import org.openstack4j.model.network.IP
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.LocationId
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.openstack4j.model.network.Port
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.util.conversion.RepresentationConversion
import org.change.v2.util.canonicalnames._
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

import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.processingmodels.instructions.:|:
import org.openstack4j.model.network.options.PortListOptions
import org.change.v2.abstractnet.neutron.Networking
import org.change.v2.abstractnet.neutron.ServiceBackedNetworking

class NeutronNetwork(network : Network, inPort : Port, networking : Networking)
{
  lazy val allPorts = networking.getPorts.filter { x => x.getNetworkId == network.getId }
  lazy val netId = network.getId
  
  def symnetCode(): Instruction = {
    val macPerPort = allPorts.foldRight(Fail(s"Network $netId - no match for EtherDst") : Instruction)((x, acc) => {
      val macs = x.getMacAddress :: x.getAllowedAddressPairs.map(y => y.getMacAddress).toList
      If (Constrain(EtherDst, :|:(macs.map(y => :==:(ConstantValue(RepresentationConversion.macToNumber(y), isMac = true))).toList)),
        Forward(s"Network/${netId}/${x.getId}/out"),
        acc)
    })
    macPerPort
  }
}


object NeutronNetwork {
  def main(args: Array[String]): Unit = {
    val wrapper = NeutronHelper.neutronWrapperFromFile()
    val nw = wrapper.getOs.networking().network().list().get(0)
    val port = wrapper.getOs.networking().port().list(PortListOptions.create().deviceOwner("compute:None").networkId(nw.getId)).get(0)
    val nn  = new NeutronNetwork(nw, port, new ServiceBackedNetworking(wrapper.getOs))
    println(nn.symnetCode())
  }
}