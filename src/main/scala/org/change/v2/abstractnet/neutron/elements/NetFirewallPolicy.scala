package org.change.v2.abstractnet.neutron.elements

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
import org.change.v2.analysis.processingmodels.Instruction
import org.openstack4j.model.network.ext.FirewallRule
import org.change.v2.helpers.Utilss
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.If
import org.openstack4j.model.network.ext.FirewallRule
import org.openstack4j.openstack.networking.domain.ext.NeutronFirewallRule.FirewallRuleAction
import org.openstack4j.model.compute.IPProtocol

class NetFirewallPolicy(policy : FirewallPolicy, wrapper : NeutronWrapper) extends NetAbsElement {
  def firewallRules() : List[FirewallRule] = {
    wrapper.getRules(policy.getFirewallRuleIds).asScala.toList
  }
  
  def symnetCode() : Instruction = {
      InstructionBlock(firewallRules.map { x => new NetFirewallRule(x).symnetCode() })
  }

}