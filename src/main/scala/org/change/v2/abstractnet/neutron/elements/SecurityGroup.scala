package org.change.v2.abstractnet.neutron.elements

import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.abstractnet.neutron.NeutronWrapper
import scala.collection.JavaConversions._
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.openstack4j.model.network.SecurityGroupRule

class SecurityGroup(wrapper : NeutronWrapper, forPc : String, ingress : Boolean = false) 
  extends BaseNetElement(wrapper) {
  lazy val thePc = wrapper.getOs.compute().servers().get(forPc)
  lazy val secGrps = wrapper.getOs.compute().securityGroups().listServerGroups(forPc).
      map { x => wrapper.getOs.networking().securitygroup().get(x.getId) }.toList
  lazy val secRules = if (ingress)
  {
    secGrps.flatMap { x => x.getRules.filter { y => y.getDirection == "ingress" }.toList }
  }
  else
  {
    secGrps.flatMap { x => x.getRules.filter { y => y.getDirection == "egress" }.toList }
  }
  
      
  def getInstruction(secRule : SecurityGroupRule) : Instruction =
  {
    NoOp
  }
      
  
  def symnetCode() : Instruction = {
    InstructionBlock(secRules.map { getInstruction })
  }

}