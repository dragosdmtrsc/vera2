package org.change.v2.runners.experiments

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

object SecurityGroupsCrawler {
  def main(argv : Array[String])
  {
    val (apiAddr, userName, password, project) = readCredentials()
    
    val wrapper = new NeutronWrapper(apiAddr,
          userName,
          password,
          project)
    val pcs = wrapper.getOs.compute().servers().list()
    for (pc <- pcs)
    {
      System.out.println(pc.getName)
      val secs = wrapper.getOs.compute().securityGroups().listServerGroups(pc.getId)
      for (sec <- secs)
      {
        val actual = wrapper.getOs.networking().securitygroup().get(sec.getId)
        println(actual.getName)
        val rules  =actual.getRules
        for (r <- rules)
        {
          println("\t" + r.getId + "," + r.getDirection + "," + r.getProtocol)
        }
      }
    }
    
  }
}