package org.change.v2.abstractnet.neutron.elements

import java.io.PrintStream

import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.analysis.processingmodels.Instruction

import scala.collection.JavaConversions._
/**
  * Created by Dragos on 5/25/2017.
  */
object NeutronTopology {

  def generateTopology(wrapper : NeutronWrapper) : (Map[String, Instruction], List[(String, String)]) = {
    val vms = wrapper.getOs.compute().servers().list().toList
    val ports = wrapper.getOs.networking().port().list().toList
    val nws = wrapper.getOs.networking().network().list().toList
    val secGrps = wrapper.getOs.networking().securitygroup().list().toList
    val subnets = wrapper.getOs.networking().subnet().list().toList
    val routers = wrapper.getOs.networking().router().list().toList

    var dict = Map[String, Instruction]()
    val machineDicts = vms.foldLeft(dict)((acc, x) => {
      val id = x.getId
      ports.filter(p => p.getDeviceOwner == "compute:None" && p.getDeviceId == id).foldLeft(acc)((acc2, p) => {
        (acc2 + (s"AntiSpoof/${p.getId}/egress/in" -> new ExitNetInstance(p.getId, wrapper).symnetCode())) +
          (s"AntiSpoof/${p.getId}/ingress/in" -> new EnterNetInstance(p.getId, wrapper).symnetCode())
      })
    })


    val secDicts = ports.foldLeft(machineDicts)((acc, x) => {
      (acc + (s"SecurityGroup/${x.getId}/ingress/in" -> new SecurityGroup(wrapper, x.getId, true).symnetCode())) +
        (s"SecurityGroup/${x.getId}/egress/in" -> new SecurityGroup(wrapper, x.getId, false).symnetCode)
    })


    val nwIns = ports.foldLeft(secDicts)((acc, x) => {
      acc + (s"Network/${x.getNetworkId}/${x.getId}/in" -> new NeutronNetwork(x.getNetworkId, x.getId, wrapper).symnetCode())
    })

    val subnetIns = subnets.foldLeft(nwIns)((acc, x) => {
      acc + (s"Subnet/${x.getId}//in" -> new NeutronSubnet(x.getId, wrapper).symnetCode)
    })


    val routerIns = routers.foldLeft(subnetIns)((acc, x) => {
      val id = x.getId
      ports.filter(p => p.getDeviceId == id).map(p => {
        (s"Router/$id/${p.getId}/in" -> new NetRouter(wrapper, x).symnetCode())
      }).toMap ++ acc
    })


    val snats = routers.filter(p => p.getExternalGatewayInfo != null).foldLeft(routerIns)((acc, x) => {
      val id = x.getId
      acc + (s"Router/$id/external/in" -> new NetRouter(wrapper, x, true).symnetCode())
    })


    // and now, engage the links
    val links = List[(String, String)]()
    val vmToAntispoof = vms.foldLeft(links)((acc, x) => {
      val id = x.getId
      ports.filter(p => p.getDeviceOwner == "compute:None" && p.getDeviceId == id).foldLeft(acc)((acc2, p) => {
        (s"AntiSpoof/${p.getId}/ingress/out" -> s"VM/$id/${p.getId}/in" ) ::
        ((s"VM/$id/${p.getId}/out" -> s"AntiSpoof/${p.getId}/egress/in") :: acc2)
      })
    })


    val antispoofToSecGrps = vms.foldLeft(vmToAntispoof)((acc, x) => {
      val id = x.getId
      ports.filter(p => p.getDeviceOwner == "compute:None" && p.getDeviceId == id).foldLeft(acc)((acc2, p) => {
        (s"SecurityGroup/${p.getId}/ingress/out" -> s"AntiSpoof/${p.getId}/ingress/in") ::
        ((s"AntiSpoof/${p.getId}/egress/out" -> s"SecurityGroup/${p.getId}/egress/in") :: acc2)
      })
    })

    val secGroupsToNetwork = ports.foldLeft(antispoofToSecGrps)((acc, x) => {
      val nwId = x.getNetworkId
      val pid = x.getId
      (s"Network/$nwId/$pid/out" -> s"SecurityGroup/$pid/ingress/in") ::
      ((s"SecurityGroup/$pid/egress/out" -> s"Network/$nwId/$pid/in") :: acc)

    })

    val networkToRouter = routers.foldLeft(secGroupsToNetwork)((acc, x) => {
      ports.filter(p => p.getDeviceId == x.getId).map(y => {
        s"Network/${y.getNetworkId}/${y.getId}/out" -> s"Router/${x.getId}/${y.getId}/in"
      }) ++ acc
    })

    val routerToSubnet = routers.foldLeft(networkToRouter) ((acc, x) => {
      ports.filter(p => p.getDeviceId == x.getId).foldLeft(acc)((acc2, y) => {
        y.getFixedIps.foldLeft(acc2) ((acc3, z) => {
          (s"Router/${x.getId}/${y.getId}/${z.getSubnetId}/out" -> s"Subnet/${z.getSubnetId}//in") :: acc3
        })
      })
    })

    val routerToSnat = routers.filter(x => x.getExternalGatewayInfo != null).foldLeft(routerToSubnet)((acc, router) => {
      (s"SNAT/${router.getId}/internal/out" -> s"Router/${router.getId}/external/in") ::
      ((s"Router/${router.getId}/external/out" -> s"SNAT/${router.getId}/internal/in") :: acc)
    })


    (snats, routerToSnat)
  }

  def main(args: Array[String]): Unit = {
    System.setOut(new PrintStream("instructionmap.txt"))
    println(generateTopology(NeutronHelper.neutronWrapperFromFile()))
  }

}
