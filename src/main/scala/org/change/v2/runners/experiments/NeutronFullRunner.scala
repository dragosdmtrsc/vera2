package org.change.v2.runners.experiments

import org.openstack4j.model.network.ext.FirewallRule
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.openstack4j.model.network.ext.Firewall
import org.openstack4j.openstack.networking.domain.NeutronRouter
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
import org.change.v2.analysis.processingmodels.State
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
import org.change.v2.abstractnet.neutron.elements.NeutronHelper._
import org.change.v2.abstractnet.neutron.elements.NetSubnet
import org.change.v2.abstractnet.neutron.elements.NetSubnetIn
import System.out._
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import org.change.v2.abstractnet.neutron.elements.NetExternalRouterIn
import org.change.v2.runners.experiments.NeutronRunner._


object NeutronFullRunner {

   def main(argv : Array[String]) {    
    val (apiAddr, userName, password, project) = readCredentials("credentials3.txt")
    
    val wrapper = new NeutronWrapper(apiAddr,
          userName,
          password,
          project)
    
    runFromSubnet(wrapper, "tiny")
    runFromSubnet(wrapper, "private")
    runFromExternal(wrapper, "toInside")
    runFromExternal(wrapper, "management")
  }
   
  def runFromSubnet(wrapper : NeutronWrapper, name : String) {
    val subsInside = wrapper.subnet(name)
    val subInside = subsInside.get(0)
    val instrs = NetSubnetIn(wrapper, subInside)
    val ps = new PrintStream("file_subnet_" + name + "_instr.txt")
    ps.println(instrs + "\n")
    ps.println("===============\n\n")
    ps.close()
    
    val genTest = genericTest(instrs)(State.clean, true)
    val ps2 = new PrintStream("file_subnet_" + name + "_run.txt")
    ps2.println(genTest + "\n")
    ps2.println("===============\n\n")
    ps2.close()
  }
  
  def runFromExternal(wrapper : NeutronWrapper, name : String) {
    val r = wrapper.routerByName(name).asInstanceOf[NeutronRouter]
    val pw = new PrintStream("file_router_" + name + "_instr.txt")
    val instrs = NetExternalRouterIn(wrapper, r)
    pw.println(instrs + "\n")
    pw.println("===============\n\n")
    pw.close()
    
    val genTest = genericTest(instrs)(State.clean, true)
    val ps2 = new PrintStream("file_router_" + name + "_run.txt")
    ps2.println(genTest + "\n")
    ps2.println("===============\n\n")
    ps2.close()
  }
}