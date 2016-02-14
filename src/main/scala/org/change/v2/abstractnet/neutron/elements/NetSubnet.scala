package org.change.v2.abstractnet.neutron.elements

import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.openstack4j.model.network.Subnet

class NetSubnet(wrapper : NeutronWrapper, subnet : Subnet) extends BaseNetElement(wrapper) {
  def symnetCode : Instruction = {
    NoOp
  }
}

object NetSubnet {
  def apply(wrapper : NeutronWrapper, subnet : Subnet) : Instruction = {
    new NetSubnet(wrapper, subnet).symnetCode()
  }
}