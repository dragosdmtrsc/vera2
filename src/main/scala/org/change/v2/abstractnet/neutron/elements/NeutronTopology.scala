package org.change.v2.abstractnet.neutron.elements

import java.io.PrintStream

import org.change.v2.abstractnet.neutron.NeutronWrapper
import org.change.v2.analysis.processingmodels.Instruction

import scala.collection.JavaConversions._
import org.change.v2.abstractnet.neutron.Networking
import org.change.v2.abstractnet.neutron.ServiceBackedNetworking
import org.openstack4j.model.network.Router
import org.openstack4j.model.network.Port
import org.change.v2.abstractnet.neutron.CSVBackedNetworking
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.clickfile.ClickToAbstractNetwork
import java.io.FileOutputStream
import org.change.v2.executor.clickabstractnetwork.ClickExecutionContext
import org.change.v2.executor.clickabstractnetwork.executionlogging.JsonLogger
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.analysis.executor.DecoratedInstructionExecutor
import org.change.v2.model.WorldModel
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.abstractnet.linux.iptables.IPTablesConstants
import org.change.v2.util.canonicalnames._
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions._
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.abstractnet.click.sefl.IPMirror
import org.change.v2.abstractnet.linux.ovs.PacketMirror

/**
  * Created by Dragos on 5/25/2017.
  */
object NeutronTopology {

  def generateTopology(service : Networking) : (Map[String, Instruction], List[(String, String)]) = {
    val ports = service.getPorts
    val nws = service.getNetworks
    val secGrps = service.getSecurityGroups
    val subnets = service.getSubnets
    val routers = service.getRouters

    val machineDicts = 
      ports.foldLeft(Map[String, Instruction]())((acc2, p) => {
        (acc2 + (s"AntiSpoof/${p.getId}/egress/in" -> new ExitNetInstance(p).symnetCode())) +
          (s"AntiSpoof/${p.getId}/ingress/in" -> new EnterNetInstance(p).symnetCode())
    })


    val secDicts = ports.foldLeft(machineDicts)((acc, x) => {
      (acc + (s"SecurityGroup/${x.getId}/ingress/in" -> new SecurityGroup(service, x, true).symnetCode())) +
        (s"SecurityGroup/${x.getId}/egress/in" -> new SecurityGroup(service, x, false).symnetCode)
    })


    val nwIns = ports.foldLeft(secDicts)((acc, x) => {
      val nw = service.getNetwork(x.getNetworkId)
      acc + (s"Network/${x.getNetworkId}/${x.getId}/in" -> new NeutronNetwork(nw, x, service).symnetCode())
    })

    val routerIns = routers.foldLeft(nwIns)((acc, x) => {
      val id = x.getId
      ports.filter(p => p.getDeviceId == id).map(p => {
        (s"Router/$id/${p.getId}/in" -> new NetRouter(service, x).symnetCode())
      }).toMap ++ acc
    })


    val snats = routers.filter(p => p.getExternalGatewayInfo != null).foldLeft(routerIns)((acc, x) => {
      val id = x.getId
      acc + (s"Router/$id/external/in" -> new NetRouter(service, x, true).symnetCode())
    })
    
    val snatInternal = routers.filter { p => p.getExternalGatewayInfo != null && 
      p.getExternalGatewayInfo.isEnableSnat }.foldLeft(snats)((acc, x) => {
      (acc + (s"SNAT/${x.getId}/internal/in" -> new SNAT(x, service, false).symnetCode())) + 
      (s"SNAT/${x.getId}/external/in" -> new SNAT(x, service, true).symnetCode())
    })
    


    // and now, engage the links
    val links = List[(String, String)]()
    val vmToAntispoof = ports.filter(p => p.getDeviceOwner == "compute:None").foldLeft(links)((acc2, p) => {
        (s"AntiSpoof/${p.getId}/ingress/out" -> s"VM/${p.getDeviceId}/${p.getId}/in" ) ::
        ((s"VM/${p.getDeviceId}/${p.getId}/out" -> s"AntiSpoof/${p.getId}/egress/in") :: acc2)
      })


    val antispoofToSecGrps = 
      ports.foldLeft(vmToAntispoof)((acc2, p) => {
        (s"SecurityGroup/${p.getId}/ingress/out" -> s"AntiSpoof/${p.getId}/ingress/in") ::
        ((s"AntiSpoof/${p.getId}/egress/out" -> s"SecurityGroup/${p.getId}/egress/in") :: acc2)
      })

    val secGroupsToNetwork = ports.foldLeft(antispoofToSecGrps)((acc, x) => {
      val nwId = x.getNetworkId
      val pid = x.getId
      (s"Network/$nwId/$pid/out" -> s"SecurityGroup/$pid/ingress/in") ::
      ((s"SecurityGroup/$pid/egress/out" -> s"Network/$nwId/$pid/in") :: acc)
    })

    val networkToRouter = ports.filter(p => p.getDeviceOwner == "network:router_interface").foldLeft(secGroupsToNetwork)((acc2, p) => {
        s"AntiSpoof/${p.getId}/ingress/out" -> s"Router/${p.getDeviceId}/${p.getId}/in" :: acc2
      })
    val routerToNetwork = ports.filter(p => p.getDeviceOwner == "network:router_interface").foldLeft(networkToRouter)((acc2, p) => {
        s"Router/${p.getDeviceId}/${p.getId}/out" -> s"AntiSpoof/${p.getId}/egress/in"  :: acc2
      })
//    val routerToSubnet = routers.foldLeft(routerToNetwork) ((acc, x) => {
//      ports.filter(p => p.getDeviceId == x.getId).foldLeft(acc)((acc2, y) => {
//        y.getFixedIps.foldLeft(acc2) ((acc3, z) => {
//          (s"Router/${x.getId}/${y.getId}/${z.getSubnetId}/out" -> s"Subnet/${z.getSubnetId}//in") :: acc3
//          (s"Router/${x.getId}/${y.getId}/${z.getSubnetId}/out" -> s"Subnet/${z.getSubnetId}//in") :: acc3
//        })
//      })
//    })

    val routerToSnat = routers.filter(x => x.getExternalGatewayInfo != null).foldLeft(routerToNetwork)((acc, router) => {
      (s"SNAT/${router.getId}/internal/out" -> s"Router/${router.getId}/external/in") ::
      ((s"Router/${router.getId}/external/out" -> s"SNAT/${router.getId}/internal/in") :: acc)
    })


    (snatInternal, routerToSnat)
  }

  def main(args: Array[String]): Unit = {
    Experiment.runInboundThenOutbound("stack-inputs/generated2")
  }

}

object Experiment {
  import org.change.v2.abstractnet.neutron.elements.NeutronTopology._
  
  def runInboundThenOutbound(dir : String) : Unit = {
    val expName = "provider-fip-then-out"
    val nw = CSVBackedNetworking.loadFromFolder(dir)
    val (i, links) = generateTopology(nw)
    println(i)
    println (links)
    val outFile = s"$dir/outcomes-$expName.txt"
    val startOfBuild = System.currentTimeMillis()
    val fos = new FileOutputStream(outFile)
    val solver = new Z3Solver()
    val executor = new DecoratedInstructionExecutor(solver)

    val ipsrc = "8.8.8.8"
    val ipDst = "203.0.113.104"
    
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    init = System.currentTimeMillis()
    
    val actual = s"SNAT/b77acd74-91f9-49d5-b478-123726ec4cd8/external/in"

    val initcode = InstructionBlock(nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsFirst".scopeTo(s"Port.${x.getId}"), ConstantValue(1))
        }
      } ++ nw.getRouters.map { x => {
          Assign("SNAT".scopeTo(x.getId), ConstantValue(0)) 
        }
      }
    )
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true)),
        Forward(actual)
      )(State.bigBang, true)
      
    
    
    var ctx = new ClickExecutionContext(i + ("VM/123b6de4-b79b-4854-93ae-bb9bdcaf9bdf/72d5355f-c6c1-4f2b-a021-afdc4df7510b/in" -> InstructionBlock(
            PacketMirror(),
            Forward("VM/123b6de4-b79b-4854-93ae-bb9bdcaf9bdf/72d5355f-c6c1-4f2b-a021-afdc4df7510b/out")
          )
        ), links.toMap, initials._1, Nil, Nil).setExecutor(executor).setLogger(new JsonLogger(fos))
    var steps = 0
    while(! ctx.isDone && steps < 1000) {
      steps += 1
      ctx = ctx.execute(true)
    }
    psStats.println("Total time: " + (System.currentTimeMillis() - init) + " ms")
    psStats.println("Number of states forward: " + ctx.okStates.size + "," + ctx.failedStates.size)
    psStats.close()
    val psGen = new PrintStream(s"$dir/generated-$expName.out")
    psGen.println(ctx.okStates)
    psGen.close()
    
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    psFailed.println(ctx.failedStates)
    psFailed.close()
  
  }
  
  
  
  def runInbound(dir : String) : List[State] = {
    val expName = "provider-fip"
    val nw = CSVBackedNetworking.loadFromFolder(dir)
    val (i, links) = generateTopology(nw)
    println(i)
    println (links)
    val outFile = s"$dir/outcomes-$expName.txt"
    val startOfBuild = System.currentTimeMillis()
    val fos = new FileOutputStream(outFile)
    val solver = new Z3Solver()
    val executor = new DecoratedInstructionExecutor(solver)

    val ipsrc = "8.8.8.8"
    val ipDst = "203.0.113.104"
    
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    init = System.currentTimeMillis()
    
    val actual = s"SNAT/b77acd74-91f9-49d5-b478-123726ec4cd8/external/in"

    val initcode = InstructionBlock(nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsFirst".scopeTo(s"Port.${x.getId}"), ConstantValue(1))
        }
      } ++ nw.getRouters.map { x => {
          Assign("SNAT".scopeTo(x.getId), ConstantValue(0)) 
        }
      }
    )
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true)),
        Forward(actual)
      )(State.bigBang, true)
    var ctx = new ClickExecutionContext(i, links.toMap, initials._1, Nil, Nil).setExecutor(executor).setLogger(new JsonLogger(fos))
    var steps = 0
    while(! ctx.isDone && steps < 1000) {
      steps += 1
      ctx = ctx.execute(true)
    }
    psStats.println("Total time: " + (System.currentTimeMillis() - init) + " ms")
    psStats.println("Number of states forward: " + ctx.okStates.size + "," + ctx.failedStates.size)
    psStats.close()
    val psGen = new PrintStream(s"$dir/generated-$expName.out")
    psGen.println(ctx.stuckStates)
    psGen.close()
    
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    psFailed.println(ctx.failedStates)
    psFailed.close()
    
    ctx.stuckStates
  }
  
  
  def runOutbound(dir : String): Unit = {
    val expName = "provider-outbound"
    val nw = CSVBackedNetworking.loadFromFolder(dir)
    val (i, links) = generateTopology(nw)
    println(i)
    println (links)
    val outFile = s"$dir/outcomes-$expName.txt"
    val startOfBuild = System.currentTimeMillis()
    val fos = new FileOutputStream(outFile)
    val solver = new Z3Solver()
    val executor = new DecoratedInstructionExecutor(solver)

    val ipsrc = "192.168.13.3"
    val startAt = "72d5355f-c6c1-4f2b-a021-afdc4df7510b"
    val pcName = "compute1"
    val ipDst = "8.8.8.8"
    
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    init = System.currentTimeMillis()
    
    val prtStart = nw.getPort(startAt)
    val actual = s"VM/${prtStart.getDeviceId}/${prtStart.getId}/out"

    val initcode = InstructionBlock(nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsFirst".scopeTo(s"Port.${x.getId}"), ConstantValue(1))
        }
      } ++ nw.getRouters.map { x => {
          Assign("SNAT".scopeTo(x.getId), ConstantValue(0)) 
        }
      }
    )
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true)),
        Forward(actual)
      )(State.bigBang, true)
    var ctx = new ClickExecutionContext(i, links.toMap, initials._1, Nil, Nil).setExecutor(executor).setLogger(new JsonLogger(fos))
    var steps = 0
    while(! ctx.isDone && steps < 1000) {
      steps += 1
      ctx = ctx.execute(true)
    }
    psStats.println("Total time: " + (System.currentTimeMillis() - init) + " ms")
    psStats.println("Number of states forward: " + ctx.okStates.size + "," + ctx.failedStates.size)
    psStats.close()
    val psGen = new PrintStream(s"$dir/generated-$expName.out")
    psGen.println(ctx.okStates)
    psGen.close()
    
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    psFailed.println(ctx.failedStates)
    psFailed.close()
    
  }
  
  def runOutboundThenBack(dir : String) : Unit = {
    
    val expName = "provider-outbound-then-back"
    val nw = CSVBackedNetworking.loadFromFolder(dir)
    val (i, links) = generateTopology(nw)
    println(i)
    println (links)
    val outFile = s"$dir/outcomes-$expName.txt"
    val startOfBuild = System.currentTimeMillis()
    val fos = new FileOutputStream(outFile)
    val solver = new Z3Solver()
    val executor = new DecoratedInstructionExecutor(solver)

    val ipsrc = "192.168.13.3"
    val startAt = "72d5355f-c6c1-4f2b-a021-afdc4df7510b"
    val pcName = "compute1"
    val ipDst = "8.8.8.8"
    
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    init = System.currentTimeMillis()
    
    val prtStart = nw.getPort(startAt)
    val actual = s"VM/${prtStart.getDeviceId}/${prtStart.getId}/out"

    val initcode = InstructionBlock(nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsForward".scopeTo(s"Port.${x.getId}"), ConstantValue(0))
        }
      } ++ nw.getPorts.map { x => {
          Assign("IsFirst".scopeTo(s"Port.${x.getId}"), ConstantValue(1))
        }
      } ++ nw.getRouters.map { x => {
          Assign("SNAT".scopeTo(x.getId), ConstantValue(0)) 
        }
      }
    )
    val initials = 
      InstructionBlock(
        State.eher,
        State.tunnel,
        initcode,
        Assign("IsUnicast", ConstantValue(1)),
        Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true)),
        Forward(actual)
      )(State.bigBang, true)
      
    
    var ctx = new ClickExecutionContext(i + ("SNAT/b77acd74-91f9-49d5-b478-123726ec4cd8/external/out" -> InstructionBlock(
          PacketMirror(),
          Forward("SNAT/b77acd74-91f9-49d5-b478-123726ec4cd8/external/in")
        )
    ), links.toMap, initials._1, Nil, Nil).setExecutor(executor).setLogger(new JsonLogger(fos))
    var steps = 0
    while(! ctx.isDone && steps < 1000) {
      steps += 1
      ctx = ctx.execute(true)
    }
    psStats.println("Total time: " + (System.currentTimeMillis() - init) + " ms")
    psStats.println("Number of states forward: " + ctx.okStates.size + "," + ctx.failedStates.size)
    psStats.close()
    val psGen = new PrintStream(s"$dir/generated-$expName.out")
    psGen.println(ctx.okStates)
    psGen.close()
    
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    psFailed.println(ctx.failedStates)
    psFailed.close()
    
  }
  
  
  
}



