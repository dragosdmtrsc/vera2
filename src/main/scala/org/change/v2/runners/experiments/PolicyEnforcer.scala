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
import org.change.v2.abstractnet.neutron.elements.NeutronHelper._
import org.change.v2.abstractnet.neutron.elements.NetSubnet
import org.change.v2.abstractnet.neutron.elements.NetSubnetIn
import System.out._
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import org.change.v2.abstractnet.neutron.elements.NetExternalRouterIn
import org.change.v2.runners.experiments.NeutronRunner._
import org.openstack4j.model.compute.Server
import org.change.v2.abstractnet.neutron.elements.IpHelpers
import org.change.v2.abstractnet.neutron.elements.NetAddress
import org.change.v2.runners.experiments.NeutronRunner._
import org.change.v2.abstractnet.neutron.elements.NetFirewall
import org.change.v2.abstractnet.neutron.elements.NetFirewall
import org.change.v2.analysis.processingmodels.instructions.NoOp
import java.util.ArrayList


object PolicyEnforcer {
  def main(argv : Array[String]) {    
     val (apiAddr, userName, password, project) = readCredentials()
    
    val wrapper = new NeutronWrapper(apiAddr,
          userName,
          password,
          project)
     val externalNet = List("vlan9", "vlan6", "Net224")
     val machines = wrapper.getOs.compute().servers().list()
     val sns = wrapper.subnets
     val firewalls = wrapper.getFirewalls.map ({ s =>
       NetFirewall(s, wrapper)
     })
     println(firewalls)
     val fwBlock = {
       if (firewalls.isEmpty)
         NoOp
       else
         InstructionBlock(firewalls.toList)
     }
     var listOfResults = new ArrayList[Double]()
     var listOfGenerationResults = new ArrayList[Double]()
     machines.foreach({x => 
       machines.foreach({ y =>
         x.getAddresses.getAddresses.foreach({ kv =>
           val key = kv._1
           val value = kv._2
           if (!externalNet.contains(key))
              x.getAddresses.getAddresses.foreach({ kvy =>
                val keyy = kvy._1
                val valuey = kvy._2
                if (!externalNet.contains(keyy))
                  for (addrx <- value)
                    for (addry <- valuey)
                      if (addrx.getType == "fixed" &&
                          addry.getType == "fixed" &&
                          addrx.getVersion == 4 &&
                          addry.getVersion == 4) 
                      {
                        val sx = sns.filter { x => NetAddress(x.getCidr).contains(addrx.getAddr) }.get(0)
                        val sy = sns.filter { x => NetAddress(x.getCidr).contains(addry.getAddr) }.get(0)
                        val startGenXy = System.currentTimeMillis()
                        val nsxy = NetSubnetIn(wrapper, sx)
                        listOfGenerationResults.add(System.currentTimeMillis() - startGenXy)
                        val startGenYx = System.currentTimeMillis()
                        val nsyx = NetSubnetIn(wrapper, sy)
                        listOfGenerationResults.add(System.currentTimeMillis() - startGenYx)

                        val setAddressXY = InstructionBlock(Assign(Tag("IPSrc"), ConstantValue(ipToNumber(addrx.getAddr))),
                            Assign(Tag("IPDst"), ConstantValue(ipToNumber(addry.getAddr))),
                            nsxy)
                        val setAddressYX = InstructionBlock(Assign(Tag("IPSrc"), ConstantValue(ipToNumber(addry.getAddr))),
                            Assign(Tag("IPDst"), ConstantValue(ipToNumber(addrx.getAddr))),
                            nsyx)
                        val startTimeXy = System.currentTimeMillis();

                        val (okxy, koxy) = genericTest(InstructionBlock(fwBlock, 
                            setAddressXY,
                            setAddressYX))(State.clean, true)
                        val deltaxy = System.currentTimeMillis - startTimeXy
                        println(deltaxy)
                        listOfResults.add(deltaxy.asInstanceOf[Double])
                        val startTimeyx = System.currentTimeMillis();
                        val (okyx, koyx) = genericTest(InstructionBlock(fwBlock, 
                            setAddressYX,
                            setAddressXY))(State.clean, true)
                        val deltayx=System.currentTimeMillis - startTimeyx
                        println(deltayx)
                        listOfResults.add(deltayx.asInstanceOf[Double])
                        val psxy = new PrintStream("test_" + addrx.getAddr.replace(".", "_") + "__" +  addry.getAddr.replace(".", "_") + ".txt")   
                        psxy.println(okxy + "\n================\n" + koxy)
                        psxy.close()
                        val psyx = new PrintStream("test_" + addry.getAddr.replace(".", "_") + "__" +  addrx.getAddr.replace(".", "_") + ".txt")   
                        psyx.println(okyx + "\n================\n" + koyx)
                        psyx.close
                      }
              })
         })
       })
     })
     
     val statsPs = new PrintStream("perfstats_run.txt")
     listOfResults.foreach({ x =>
       statsPs.println(x)
     })
     statsPs.close()
     val statsGen = new PrintStream("perfstats_gen.txt")
     listOfGenerationResults.foreach({ x =>
       statsGen.println(x)
     })
     statsGen.close()
  }
}