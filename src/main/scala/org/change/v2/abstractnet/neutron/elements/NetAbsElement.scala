package org.change.v2.abstractnet.neutron.elements
import scala.collection.JavaConversions._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.abstractnet.neutron.NeutronWrapper
import collection.JavaConversions._
import org.openstack4j.model.network.options.PortListOptions
import org.openstack4j.model.network.Port
import org.openstack4j.model.network.Router
trait NetAbsElement {
  def symnetCode() : Instruction;
}


abstract class BaseNetElement(wrapper : NeutronWrapper) extends NetAbsElement {
  
  
  
  def os = wrapper.getOs
  def routers = os.networking.router.list
  def router(v : String) = os.networking().router().get(v)
  def ports = os.networking.port.list
  def subnet = os.networking.subnet
  
//  def machineByIp(ip : String) = {
//    val thePort = portByIp(ip)
//    if (thePort != null)
//    {
//      os.compute().thePort.getDeviceId
//    }
//    else
//    {
//      null
//    }
//  }
  
  def portByIp(ip : String) : Port = {
    val thePorts = ports.filter { x => x.getFixedIps.toSet.filter { x => x.getIpAddress == ip }.size != 0 }
    if (thePorts.size > 0) {
     thePorts.get(0)
    } else {
      null
    }
  }
  
  def routerByIp(ip : String) : Router = {
    val thePorts = ports.filter { x => x.getFixedIps.toSet.filter { x => x.getIpAddress == ip }.size != 0 }
    if (thePorts.size > 0) {
      val thePort = thePorts.get(0)
      val rr = router(thePort.getDeviceId)
      if (rr == null)
        null
      else
        rr
    }
    else
      null
  }
}