package org.change.v2.abstractnet.neutron.elements

import scala.collection.JavaConversions._
import scala.collection.mutable.Buffer
import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.analysis.expression.concrete.{ConstantValue, SymbolicValue}
import org.change.v2.analysis.expression.concrete.nonprimitive.:@
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.util.conversion.RepresentationConversion._
import org.openstack4j.api.OSClient
import org.openstack4j.model.network.Router
import org.openstack4j.model.network.Port
import org.openstack4j.model.network.Subnet
import org.openstack4j.openstack.networking.domain.NeutronRouter
import org.change.v2.util.canonicalnames._
import org.change.v2.abstractnet.neutron.Networking
import org.change.v2.abstractnet.neutron.ServiceBackedNetworking
import org.change.v2.abstractnet.neutron.CSVBackedNetworking.ExtendedExternalGWInfo

class NetRouter(service : Networking,
                router : Router,
                throughExt : Boolean = false) {
  
  def checkDestNearby() : Instruction = {
    val thList = service.getPorts
    val filtered = thList.filter { x => 
      x.getDeviceId == router.getId 
    }
    NoOp
  }
  
  lazy val routerNets = service.getPorts.filter { z => z.getDeviceId == router.getId }.flatMap { x =>
              x.getFixedIps.map ( ip => {
                  val theSubnet = service.getSubnet(ip.getSubnetId)
                  (ip.getIpAddress, new NetAddress(theSubnet.getCidr), theSubnet, x)
                }
              )
            }.sortBy { u => - u._2.mask }

  private def checkMyDestination(list : List[(String, NetAddress, Subnet, Port)]) : Instruction = list match {
    case h :: tail => If (Constrain(IPDst,  :==:(ConstantValue(ipToNumber(h._1), true))),
        Forward(s"Router/${router.getId}/cpu/in"), // se numeste succes
        checkMyDestination(tail))
    case Nil => checkConnectedSubnet
  }
  
  private def checkMyDestination() : Instruction = {
    checkMyDestination(this.routerNets.toList)
  }
  
  private def checkConnectedSubnet(list :  List[(String, NetAddress, Subnet, Port)]) : Instruction =
  list match {
    case h :: tail => If (Constrain(IPDst, :&:(:>=:(ConstantValue(h._2.addressRange._1, true)),
        :<=:(ConstantValue(h._2.addressRange._2, true)))),
        {
          InstructionBlock(
            new org.change.v2.abstractnet.neutron.elements.NeutronSubnet(h._3, service).symnetCode(),
            Assign(EtherSrc, ConstantValue(macToNumber(h._4.getMacAddress), isMac = true)),
            Forward(s"Router/${router.getId}/${h._4.getId}/out")
          )
        },
        checkConnectedSubnet(tail))
    case Nil => checkRoutingTable
  }
  
  private def checkConnectedSubnet() : Instruction = {
    checkConnectedSubnet(this.routerNets.toList)
  }
  // arg 2 = next hop
  // arg 1 = cidr to dest
  private def checkRoutingTable(list : List[(NetAddress, String)]) : Instruction = 
  list match {
    case h :: tail => If (Constrain(IPDst, :&:(:>=:(ConstantValue(h._1.addressRange._1, true)),
        :<=:(ConstantValue(h._1.addressRange._2, true)))),
        {
          val rnets = routerNets.filter(x => x._2.inRange(h._2))
          if (rnets.isEmpty)
            Fail("Unknown next hop")
          else
            InstructionBlock(
              new org.change.v2.abstractnet.neutron.elements.NeutronSubnet(rnets(0)._3, service).symnetCode(),
              Assign(EtherSrc, ConstantValue(macToNumber(rnets(0)._4.getMacAddress), isMac = true)),
              Forward(s"Router/${router.getId}/${rnets(0)._4.getId}/out")
            )
        },
        checkRoutingTable(tail))
    case Nil => checkExternalGateway
  }

  
  private def checkExternalGateway() : Instruction = {
    if (this.router.getExternalGatewayInfo != null)
    {
      Forward(s"Router/${router.getId}/external/out")
    }
    else
    {
      Fail("No route to host")
    }
  }
  
  private def checkRoutingTable() : Instruction = {
    checkRoutingTable(this.router.getRoutes.map { x => 
      (new NetAddress(x.getDestination), x.getNexthop) 
    }.toList)
  }
  



  
  def symnetCode() : Instruction = {
    checkMyDestination
  }
}

class SNAT (router : Router,
            networking : Networking,
           fromOutside : Boolean)  {
  def symnetCode(): Instruction = {
    if (fromOutside)
      InstructionBlock(
        unSnat()
      )
    else
      snat()
  }
  
  lazy val fips = networking.getFloatingIps.filter { x => x.getRouterId == router.getId }
  
  
  
  def setExternalIp = {
    val extIp = if (router.getExternalGatewayInfo.isInstanceOf[ExtendedExternalGWInfo])
    {
      ConstantValue(ipToNumber(router.getExternalGatewayInfo.asInstanceOf[ExtendedExternalGWInfo].getIps.head.getIpAddress))
    }
    else
    {
      SymbolicValue()
    }
    fips.foldRight(Assign(IPSrc, extIp) : Instruction)((x, acc) => {
      If (Constrain(IPSrc, :==:(ConstantValue(ipToNumber(x.getFixedIpAddress), isIp = true))),
          Assign(IPSrc, ConstantValue(ipToNumber(x.getFloatingIpAddress), isIp = true)),
          acc
      )
    })
  }


  def snat() : Instruction = {
    If (Constrain(s"{router.getId}.DNAT", :==:(ConstantValue(0))),
      InstructionBlock(
        Assign(router.getId + ".SNAT.IPSrcOrig", :@(IPSrc)),
        Assign(router.getId + ".SNAT.IPDstOrig", :@(IPDst)),
        IfProto(TCPProto,
          InstructionBlock(Assign(router.getId + ".SNAT.TCPSrcOrig", :@(TcpSrc)),
            Assign(router.getId + ".SNAT.TCPDstOrig", :@(TcpDst)),
            Assign(TcpSrc, SymbolicValue()),
            Constrain(TcpSrc, :&:(:<=:(ConstantValue(65536)), :>=:(ConstantValue(0)))),
            Assign(router.getId + ".SNAT.TCPSrcMod", :@(TcpSrc))
          ),
          IfProto(UDPProto,
            InstructionBlock(
              Assign(router.getId + ".SNAT.UDPSrcOrig", :@(TcpSrc)),
              Assign(router.getId + ".SNAT.UDPDstOrig", :@(TcpDst)),
              Assign(UDPSrc, SymbolicValue()),
              Constrain(UDPSrc, :&:(:<=:(ConstantValue(65536)), :>=:(ConstantValue(0)))),
              Assign(router.getId + ".SNAT.UDPSrcMod", :@(UDPSrc))
            )
          )
        ),
        setExternalIp,
        Assign(router.getId + ".SNAT.IPSrcMod", :@(IPSrc)),
        Assign(router.getId + ".SNAT.IPProto", :@(Proto)),
        Assign(router.getId + ".SNAT", ConstantValue(1)),
        Forward(s"SNAT/${router.getId}/external/out")
      ),
      undnat()
    )
  }
  
  def undnat() : Instruction = {
    fips.foldRight(Fail("Cannot route internal traffic outvwards") : Instruction)((z, acc) => {
      If (Constrain(IPSrc, :==:(ConstantValue(ipToNumber(z.getFixedIpAddress), isIp = true))),
          InstructionBlock(
            Assign(IPSrc, ConstantValue(ipToNumber(z.getFloatingIpAddress), isIp = true)),
            Forward(s"SNAT/${router.getId}/external/out")
          ),
          acc
      )
    })
  }
  
  // floating ips
  def dnat() : Instruction = {
    val fipm = fips.foldRight(Fail("Cannot route external traffic invwards") : Instruction)((z, acc) => {
      If (Constrain(IPDst, :==:(ConstantValue(ipToNumber(z.getFloatingIpAddress), isIp = true))),
          InstructionBlock(
            Assign(router.getId + ".DNAT", ConstantValue(1)),
            Assign(IPDst, ConstantValue(ipToNumber(z.getFixedIpAddress), isIp = true)),
            Forward(s"SNAT/${router.getId}/internal/out")
          ),
          acc
      )
    })
    If (Constrain(router.getId + ".SNAT", :==:(ConstantValue(0))),
        fipm
    )
  }
  

  def unSnat() : Instruction = {
    If (Constrain(router.getId + ".SNAT", :==:(ConstantValue(1))),
      If (Constrain(Proto, :==:(:@(router.getId + ".SNAT.IPProto"))),
        If (Constrain(IPDst, :==:(:@(router.getId + ".SNAT.IPSrcMod"))),
          If (Constrain(IPSrc, :==:(:@(router.getId + ".SNAT.IPDstOrig"))),
            IfProto (TCPProto,
              If (Constrain(TcpSrc, :==:(:@(router.getId + ".SNAT.TCPDstOrig"))),
                If (Constrain(TcpDst, :==:(:@(router.getId + ".SNAT.TCPSrcMod"))),
                  InstructionBlock(
                    Assign(TcpDst, :@(router.getId + ".SNAT.TCPSrcOrig")),
                    Assign(IPDst, :@(router.getId + ".SNAT.IPSrcOrig")),
                    Forward(s"SNAT/${router.getId}/internal/out")
                  ),
                  Fail(s"No SNAT found at ${router.getId}, because TcpDst is not the same with the modified TCPSrc")
                ),
                Fail(s"No SNAT found at ${router.getId}, because TcpSrc is not the same with the original TCPDst")
              ),
              IfProto(UDPProto,
                If (Constrain(UDPSrc, :==:(:@(router.getId + ".SNAT.UDPDstOrig"))),
                  If (Constrain(UDPDst, :==:(:@(router.getId + ".SNAT.UDPSrcMod"))),
                    InstructionBlock(
                      Assign(UDPDst, :@(router.getId + ".SNAT.UDPSrcOrig")),
                      Assign(IPDst, :@(router.getId + ".SNAT.IPSrcOrig")),
                      Forward(s"SNAT/${router.getId}/internal/out")
                    ),
                    Fail(s"No SNAT found at ${router.getId}, because UDPDst is not the same with the modified UDPSrc")
                  ),
                  Fail(s"No SNAT found at ${router.getId}, because UDPSrc is not the same with the original UDPDst")
                ),
                InstructionBlock(
                  Assign(IPDst, :@(router.getId + ".SNAT.IPSrcOrig")),
                  Forward(s"SNAT/${router.getId}/internal/out")
                )
              )
            ),
            Fail(s"No SNAT found at ${router.getId}, because IPSrc is not the same with the original IPDst")
          ),
          Fail(s"No SNAT found at ${router.getId}, because IPDst is not the same with the modified IPSrc")
        ),
        Fail(s"No SNAT found at ${router.getId}, because Proto is not the same")
      ),
      dnat
    )
  }

}



object NetRouter {

  def apply(wrapper : NeutronWrapper, router : Router) : Instruction = {
    new NetRouter(new ServiceBackedNetworking(wrapper.getOs), router).symnetCode()
  }
  
  def main(argv : Array[String]) = {
   val wrapper = NeutronHelper.neutronWrapperFromFile()
   val r = wrapper.getOs.networking.router.list.get(0)
   System.out.println(new SNAT(r, new ServiceBackedNetworking(wrapper.getOs), true).symnetCode())
   System.out.println(new SNAT(r, new ServiceBackedNetworking(wrapper.getOs), false).symnetCode())
  }
  
}