package org.change.v2.runners.experiments

import scala.collection.JavaConversions.asScalaBuffer

import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{ :@ => :@ }
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.memory.Tag
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions.Allocate
import org.change.v2.analysis.processingmodels.instructions.Assign
import org.change.v2.analysis.processingmodels.instructions.Fail
import org.change.v2.analysis.processingmodels.instructions.InstructionBlock
import org.change.v2.analysis.processingmodels.instructions.NoOp
import org.change.v2.model.NIC
import org.change.v2.model.OVSBridge
import org.change.v2.model.OpenFlowNIC
import org.change.v2.model.OpenFlowTable
import org.change.v2.model.WorldModel
import org.change.v2.model.openflow.Action
import org.change.v2.model.openflow.ActionType
import org.change.v2.model.openflow.FlowEntry
import org.change.v2.model.openflow.actions.ApplyActionsAction
import org.change.v2.model.openflow.actions.DropAction
import org.change.v2.model.openflow.actions.LoadAction
import org.change.v2.model.openflow.actions.ModDlDstAction
import org.change.v2.model.openflow.actions.ModDlSrcAction
import org.change.v2.model.openflow.actions.ModNwDstAction
import org.change.v2.model.openflow.actions.ModNwSrcAction
import org.change.v2.model.openflow.actions.ModTpDstAction
import org.change.v2.model.openflow.actions.ModTpSrcAction
import org.change.v2.model.openflow.actions.ModVlanVidAction
import org.change.v2.model.openflow.actions.MoveAction
import org.change.v2.model.openflow.actions.NormalAction
import org.change.v2.model.openflow.actions.OutputAction
import org.change.v2.model.openflow.actions.PushVlanAction
import org.change.v2.model.openflow.actions.ResubmitAction
import org.change.v2.model.openflow.actions.SetFieldAction
import org.change.v2.model.openflow.actions.SetTunnelAction

import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.change.v2.model.openflow.actions.StripVlanAction
import org.change.v2.analysis.processingmodels.instructions.Deallocate
import org.change.v2.analysis.processingmodels.instructions.Constrain
import org.change.v2.analysis.processingmodels.instructions.{:==:, :<=:, :>=:, :&:}
import org.change.v2.analysis.processingmodels.instructions.:~:
import org.change.v2.analysis.processingmodels.instructions.If
import org.change.v2.analysis.processingmodels.instructions.Fork
import org.change.v2.analysis.memory.Value
import org.change.v2.analysis.processingmodels.instructions.CreateTag
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.util.canonicalnames._
import org.change.v2.analysis.expression.concrete.SymbolicValue
import org.change.v2.analysis.memory.Intable
import java.io.PrintStream
import org.change.v2.model.openflow.actions.LearnAction
import org.change.v2.analysis.processingmodels.instructions.Forward
import org.change.v2.abstractnet.linux.iptables.EnterIPInterface
import org.change.v2.util.conversion.RepresentationConversion._
import org.change.v2.abstractnet.linux.iptables.EnterIPTablesChain
import org.change.v2.abstractnet.linux.iptables.IPTablesConstants
import org.change.v2.abstractnet.linux.iptables.ConntrackTrack
import org.change.v2.abstractnet.linux.iptables.ConntrackUnDnat
import org.change.v2.abstractnet.linux.iptables.ConntrackUnSnat
import org.change.v2.analysis.expression.concrete.ConstantStringValue
import org.change.v2.abstractnet.linux.iptables.VariableNameExtensions._
import scala.util.matching.Regex._
import org.change.v2.model.openflow.actions.CTAction
import org.change.v2.abstractnet.linux.iptables.ConntrackZone
import org.change.v2.abstractnet.linux.iptables.ConntrackCommitZone
import org.change.v2.abstractnet.linux.iptables.ConntrackTrackZone
import org.change.v2.abstractnet.linux.ovs.PacketMirror
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.linux.ovs.EnterIface
import org.change.v2.abstractnet.neutron.elements.Experiment._
import org.change.v2.abstractnet.neutron.CSVBackedNetworking
import java.io.PrintWriter
import org.change.v2.analysis.z3.Z3Util
import org.change.v2.analysis.executor.DecoratedInstructionExecutor
import org.change.v2.analysis.executor.solvers.Z3Solver
import org.change.v2.analysis.executor.solvers.Solver
import org.change.v2.abstractnet.linux.Translatable
import org.change.v2.analysis.processingmodels.instructions.ConstrainRaw
import org.change.v2.analysis.processingmodels.instructions.ConstrainNamedSymbol
import org.change.v2.analysis.memory.TagExp

import org.change.v2.analysis.constraint._
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.analysis.memory.MemorySpace
import org.change.v2.analysis.expression.concrete.nonprimitive.Plus
import org.change.v2.analysis.expression.abst.Expression
import org.change.v2.analysis.memory.MemoryObject
import org.change.v2.analysis.expression.concrete.nonprimitive.Minus
import org.change.v2.analysis.memory.ValueStack

object IngressToMachine
{
  
  def main(argv : Array[String]) {
    val ipsrc = "8.8.8.8"
    val enterIface = "eth2"
    val pcName = "controller"
    val ipDst = "203.0.113.104"
    // ensure that we aim for qg...
    val ethDst = "fa:16:3e:9c:99:77"
    
    val expName = "fip"
    val dir = "stack-inputs/generated2"
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    val world = WorldModel.fromFolder(dir)
    psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
    init = System.currentTimeMillis()
    val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
          x.getNamespaces.map { y => InstructionBlock(
              Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
              Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
              Assign("IsTracked".scopeTo(y), ConstantValue(0))
            )
          }
        }
      } ++ world.getPcs.flatMap { x => {
          x.getCtZones.map { y => {
              val prefix = x.getName + "." + y.intValue
              InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
              )
            }
          }
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
        Assign(EtherDst, ConstantValue(macToNumber(ethDst), isMac = true))
      )(State.bigBang, true)._1
    init =System.currentTimeMillis()
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(enterIface)
    val ns = pc.getNamespaceForNic(enterIface)
    
    //ingress example
    val iib = new EnterIface(pcName, enterIface, world).generateInstruction
    val worldModel = world
    val iip = InstructionBlock(
      Assign("InputInterface", ConstantStringValue(enterIface)),
      Allocate("OutputInterface"),
      iib
    )
    val psCode = new PrintStream(s"$dir/generated-code-$expName.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    val psout = new PrintStream(s"$dir/generated-$expName.out")
    val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
    val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
    
    init = System.currentTimeMillis()
    val okProvider = initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
      System.out.println(System.currentTimeMillis() - init)
      psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Forward number of states: " + ok.size + "," + fail.size)
      psout.println("[" + ok.mkString(",") + "]")
      psFailed.println("[" + fail.mkString(",") + "]")
      val success = ok.filter { x => x.history.head.startsWith("DeliveredLocallyVM") }
      init = System.currentTimeMillis()
      val ib = 
          success.foldLeft((List[State](), List[State]())){ (acc, x) => 
            "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
                findAllIn(x.history.head).matchData.foldRight(acc) {
                  (r, acc2) => {
                    val tap = r.group(1)
                    val machine = r.group(2)
                    println(s"Entering $tap at $machine")
                    val ii = InstructionBlock(
                        PacketMirror(),
                        Assign("InputInterface", ConstantStringValue(tap)),
                        new EnterIface(machine, tap, world).generateInstruction()
                    )(x, true)
                    (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                }
            }
          }
      psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
      val (okRev, failedRev) = ib
      psOutRev.println("[" + okRev.mkString(",") + "]")
      psFailedRev.println("[" + failedRev.mkString(",") + "]")
      ok
    }
    psOutRev.close()
    psFailedRev.close()
    psStats.close()
    psout.close()
    psFailed.close()
//    
//    
//    val okTenant = runInbound(dir)
//    EquivalenceProver((l1, l2) => {
//      
//    }).areEquivalent(okTenant, )
//    
//    
  }
}




class OVSExecutor(solver : Solver) extends DecoratedInstructionExecutor(solver)
{
  override def executeExoticInstruction(instruction : Instruction, s : State, v : Boolean) : 
    (List[State], List[State]) = {
    if (instruction.isInstanceOf[Translatable])
    {
      this.execute(instruction.asInstanceOf[Translatable].generateInstruction(), s, v)
    }
    else
      throw new UnsupportedOperationException("Cannot handle this kind of instruction. Make it Translatable " + instruction)
  }
  
  
  override def executeConstrainRaw(instruction : ConstrainRaw, s : State, v : Boolean = false): 
    (List[State], List[State]) = { 
    val ConstrainRaw(a, dc, c) = instruction
    a(s) match {
      case Some(int) => c match {
          case None => dc instantiate s match {
            case Left(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc") (s => {
              val maybeNewMem = s.memory.addConstraint(int, c, true)
              getNewMemory(maybeNewMem, Left(int), c)
            })
            case Right(err) => Fail(err)(s, v)
          }
          case Some(c) => optionToStatePair(s, s"Memory object @ $a cannot $dc") (s => {
            val maybeNewMem = s.memory.addConstraint(int, c, true)
            getNewMemory(maybeNewMem, Left(int), c)
          })
        }
      case None => execute(Fail(TagExp.brokenTagExpErrorMessage), s, v)
    }
  }
  def mapMemory(mem : MemorySpace, id : Long, value : Long) : MemorySpace = {
    mem.copy(rawObjects = mem.rawObjects.map(x => {
        (x._1, mapMemoryObject(x._2, id, value))
      }).toMap,
      symbols = mem.symbols.map(x => {
        (x._1, mapMemoryObject(x._2, id, value))
      }).toMap
    )
  }
  
  def mapExpression(expr : Expression, id : Long, value : Long) : Expression = {
    expr match {
      case y : SymbolicValue if y.id == id => ConstantValue(value)
      case Plus(a, b) => Plus(mapValue(a, id, value), mapValue(b, id, value))
      case Minus(a, b) => Minus(mapValue(a, id, value), mapValue(b, id, value))
      case _ => expr
    }
  }
  
  def mapConstraint(ct : Constraint, id : Long, value : Long) : Constraint = {
    ct match {
      case OR(cs) => OR(cs.map(mapConstraint(_, id, value)))
      case AND(cs) => AND(cs.map(mapConstraint(_, id, value)))
      case LTE_E(expr) => LTE_E(mapExpression(expr, id, value))
      case GTE_E(expr) => GTE_E(mapExpression(expr, id, value))
      case GT_E(expr) => GT_E(mapExpression(expr, id, value))
      case LT_E(expr) => LT_E(mapExpression(expr, id, value))
      case EQ_E(expr) => EQ_E(mapExpression(expr, id, value))
      case NOT(c) => NOT(mapConstraint(c, id, value))
      case _ => ct
    }
  }
  
  def mapValue(v : Value, id : Long, value : Long) : Value = {
    v.copy(e = mapExpression(v.e, id, value), cts = v.cts.map(mapConstraint(_, id, value)))
  }
  def mapValueStack(vs : ValueStack, id : Long, value : Long) : ValueStack = {
    vs.vs match {
      case head :: tail => vs.copy(vs = mapValue(head, id, value) :: tail)
      case Nil => vs
    }
  }
  
  def mapMemoryObject(mo : MemoryObject, id : Long, value : Long) : MemoryObject = {
    mo.valueStack match {
      case head :: tail => {
        mo.copy(valueStack = mapValueStack(head, id, value) :: tail)
      }
      case Nil => mo
    }
  }

  protected def getNewMemory(maybeNewMem: Option[MemorySpace], a : Either[Int, String], c : Constraint) = {
    maybeNewMem match {
      case None => None
      case Some(m) => {
        val value = a match {
          case Right(s) => m.eval(s).get
          case Left(s) => m.eval(s).get
        }
        c match {
          case EQ_E(ConstantValue(v, _, _)) => {
            value.e match {
              case y : SymbolicValue => {
                Some(mapMemory(m, y.id, v))
              }
              case _ => {
                if (isSat(m))
                {
                  Some(m)
                }
                else
                {
                  None
                }
              }
            }
          }
          case _ => {
            if (isSat(m))
            {
              Some(m)
            }
            else
            {
              None
            }
          }
        }
        
      }
    }
  }
  
  override def executeConstrainNamedSymbol(instruction : ConstrainNamedSymbol, s : State, v : Boolean = false) : 
    (List[State], List[State]) = {
    val ConstrainNamedSymbol(id, dc, c) = instruction
    c match {
      case None => dc instantiate s match {
        case Left(c) => optionToStatePair(s, s"Symbol $id cannot $dc") (s => {
          val maybeNewMem = s.memory.addConstraint(id, c, true)
          getNewMemory(maybeNewMem, Right(id), c)
        })
        case Right(err) => Fail(err)(s, v)
      }
      case Some(c) => optionToStatePair(s, s"Symbol $id cannot $dc") (s => {
          val maybeNewMem = s.memory.addConstraint(id, c, true)
          getNewMemory(maybeNewMem, Right(id), c)
      })
    }
  }
  
}


object L2Connectivity
{
  
  def experiment(enterIface : String, 
      pcName : String,
      dir : String,
      codeFirst : Instruction = NoOp,
      exp : String = "eastwest") = {
    val expName = s"$exp-$enterIface-$pcName"
    try
    {
      var init = System.currentTimeMillis()
      val psStats = new PrintStream(s"$dir/stats-$expName.txt")
      val world = WorldModel.fromFolder(dir)
      psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
      init = System.currentTimeMillis()
      val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
            x.getNamespaces.map { y => InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
                Assign("IsTracked".scopeTo(y), ConstantValue(0)),
                Assign("SNAT.IsSNAT".scopeTo(y), ConstantValue(0)),
                Assign("DNAT.IsDNAT".scopeTo(y), ConstantValue(0))
              )
            }
          }
        } ++ world.getPcs.flatMap { x => {
            x.getCtZones.map { y => {
                val prefix = x.getName + "." + y.intValue
                InstructionBlock(
                  Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                  Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                  Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                  Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
                )
              }
            }
          }
        }
      )
      def decorated = new OVSExecutor(new Z3Solver())
      val initials = 
        InstructionBlock(
          State.eher,
          State.tunnel,
          initcode,
          codeFirst,
          Assign("IsUnicast", ConstantValue(1))
        )(State.bigBang, true)._1
      init =System.currentTimeMillis()
      val pc = world.getComputer(pcName)
      val nic = pc.getNic(enterIface)
      val ns = pc.getNamespaceForNic(enterIface)
      
      //ingress example
      val iib = new EnterIface(pcName, enterIface, world)
      val worldModel = world
  //    val iib = new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel).generateInstruction()
      val iip = InstructionBlock(
        Assign("InputInterface", ConstantStringValue(enterIface)),
        Allocate("OutputInterface"),
        iib
      )
      val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
      val psout = new PrintStream(s"$dir/generated-$expName.out")
      val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
      val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
      
      init = System.currentTimeMillis()
      initials.map { initial => 
        val (ok, fail) =  decorated.execute(iip, initial, true)
        System.out.println(System.currentTimeMillis() - init)
        psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Forward number of states: " + ok.size + "," + fail.size)
        psout.println("[" + ok.mkString(",") + "]")
        psFailed.println("[" + fail.mkString(",") + "]")
        val success = ok.filter { x => x.history.head.startsWith("DeliveredLocallyVM") }
        init = System.currentTimeMillis()
        val ib = 
            success.foldLeft((List[State](), List[State]())){ (acc, x) => 
              "DeliveredLocallyVM\\((.*),[ ]*(.*)\\)".r.
                  findAllIn(x.history.head).matchData.foldRight(acc) {
                    (r, acc2) => {
                      val tap = r.group(1)
                      val machine = r.group(2)
                      println(s"Entering $tap at $machine")
                      val ii = decorated.execute(InstructionBlock(
                          PacketMirror(),
                          Assign("InputInterface", ConstantStringValue(tap)),
                          new EnterIface(machine, tap, world)
                      ), x, true)
                      (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                  }
              }
            }
        psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
        psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
        val (okRev, failedRev) = ib
        psOutRev.println("[" + okRev.mkString(",") + "]")
        psFailedRev.println("[" + failedRev.mkString(",") + "]")
      }
      psOutRev.close()
      psFailedRev.close()
      psStats.close()
      psout.close()
      psFailed.close()
    }
    catch {
      case e : Exception => e.printStackTrace()
    }
    
  }
  
  
  def main(argv : Array[String]) {
    var ipsrc = "192.168.13.3"
    var enterIface = "tap72d5355f-c6"
    var pcName = "compute1"
    var dir = "stack-inputs/generated2"
    var neutroncfg = CSVBackedNetworking.loadFromFolder(dir)

    var ports = neutroncfg.getPorts.filter { x => x.getDeviceOwner == "compute:None" }
    
    ports.foreach { x => {
        ipsrc = x.getFixedIps.iterator().next().getIpAddress
        enterIface = "tap" + x.getId.take("72d5355f-c6".length())
        pcName = "compute1"
        val codeFirst = InstructionBlock(
          Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
//          Assign(IPDst, ConstantValue(ipToNumber("8.8.8.8"), isIp = true)),
//          Assign(EtherDst, ConstantValue(macToNumber("fa:16:3e:f8:03:e4"), isMac = true)),
          Assign(EtherSrc, ConstantValue(macToNumber(x.getMacAddress), isMac = true))
        )
        experiment(enterIface, pcName, dir, codeFirst = codeFirst)
        Z3Util.z3Context.finalize()
        Z3Util.refreshCache
      }
    }
    
    
    dir = "stack-inputs/generated4"
    neutroncfg = CSVBackedNetworking.loadFromFolder(dir)

    ports = neutroncfg.getPorts.filter { x => x.getDeviceOwner == "compute:None" }
    ports = ports.filter { x => x.getId == "d66e3d2e-2fe7-41ee-b1e9-2229b1a5cfd3" }
    ports.foreach { x => {
        ipsrc = x.getFixedIps.iterator().next().getIpAddress
        enterIface = "tap" + x.getId.take("72d5355f-c6".length())
        pcName = "compute1"
        val codeFirst = InstructionBlock(
          Assign(IPSrc, ConstantValue(ipToNumber(ipsrc), isIp = true)),
//          Assign(IPDst, ConstantValue(ipToNumber("8.8.8.8"), isIp = true)),
//          Assign(EtherDst, ConstantValue(macToNumber("fa:16:3e:f8:03:e4"), isMac = true)),
          Assign(EtherSrc, ConstantValue(macToNumber(x.getMacAddress), isMac = true))
        )
        experiment(enterIface, pcName, dir, codeFirst = codeFirst)
        Z3Util.z3Context.finalize()
        Z3Util.refreshCache
      }
    }
//    val ipDst = "192.168.13.4"
//    experiment(ipsrc, enterIface, pcName, dir)
  }
}






object EgressFromMachine
{
  def main(argv : Array[String]) {
    val ipsrc = "192.168.13.5"
    val enterIface = "tap72d5355f-c6"
    val pcName = "compute1"
    val ipDst = "8.8.8.8"
    
    val expName = "northbound"
    val dir = "stack-inputs/generated2"
    var init = System.currentTimeMillis()
    val psStats = new PrintStream(s"$dir/stats-$expName.txt")
    val world = WorldModel.fromFolder(dir)
    psStats.println("Parsing time: " + (System.currentTimeMillis() - init) + " ms")
    init = System.currentTimeMillis()
    val initcode = InstructionBlock(world.getPcs.flatMap { x => { 
          x.getNamespaces.map { y => InstructionBlock(
              Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(y), ConstantValue(0)),
              Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(y)),
              Assign(IPTablesConstants.CTMARK_TOP.scopeTo(y), ConstantValue(0)),
              Assign("IsTracked".scopeTo(y), ConstantValue(0))
            )
          }
        }
      } ++ world.getPcs.flatMap { x => {
          x.getCtZones.map { y => {
              val prefix = x.getName + "." + y.intValue
              InstructionBlock(
                Allocate(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_BOTTOM.scopeTo(prefix), ConstantValue(0)),
                Allocate(IPTablesConstants.CTMARK_TOP.scopeTo(prefix)),
                Assign(IPTablesConstants.CTMARK_TOP.scopeTo(prefix), ConstantValue(0)),
                Assign("IsTracked".scopeTo(prefix), ConstantValue(0))
              )
            }
          }
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
        Assign(IPDst, ConstantValue(ipToNumber(ipDst), isIp = true))
      )(State.bigBang, true)._1
    init =System.currentTimeMillis()
    val pc = world.getComputer(pcName)
    val nic = pc.getNic(enterIface)
    val ns = pc.getNamespaceForNic(enterIface)
    
    //ingress example
    val iib = new EnterIface(pcName, enterIface, world).generateInstruction
    val worldModel = world
//    val iib = new EnterIPTablesChain(pc, nic, "nat", "PREROUTING", worldModel).generateInstruction()
    val iip = InstructionBlock(
      Assign("InputInterface", ConstantStringValue(enterIface)),
      Allocate("OutputInterface"),
      iib
    )
    val psCode = new PrintStream(s"$dir/generated-code-$expName.out")
    psCode.println(iib)
    psCode.close
    val psFailed = new PrintStream(s"$dir/generated-fail-$expName.out")
    val psout = new PrintStream(s"$dir/generated-$expName.out")
    val psOutRev = new PrintStream(s"$dir/generated-reverse-$expName.out")
    val psFailedRev = new PrintStream(s"$dir/generated-reverse-fail-$expName.out")
    
    init = System.currentTimeMillis()
    initials.map { initial => 
      val (ok, fail) =  iip(initial, true)
      System.out.println(System.currentTimeMillis() - init)
      psStats.println("Forward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Forward number of states: " + ok.size + "," + fail.size)
      psout.println("[" + ok.mkString(",") + "]")
      psFailed.println("[" + fail.mkString(",") + "]")
      val success = ok.filter { x => x.history.head.startsWith("MaybeOutbound") }
      init = System.currentTimeMillis()
      val ib = 
          success.foldLeft((List[State](), List[State]())){ (acc, x) => 
            "MaybeOutbound\\((.*),(.*),(.*)\\)".r.
                findAllIn(x.history.head).matchData.foldRight(acc) {
                  (r, acc2) => {
                    val tap = r.group(1)
                    val br = r.group(2)
                    val machine = r.group(3)
                    println(s"Entering $tap at $machine")
                    val ii = InstructionBlock(
                        PacketMirror(),
                        Assign("InputInterface", ConstantStringValue(tap)),
                        new EnterIface(machine, tap, world).generateInstruction()
                    )(x, true)
                    (acc2._1 ++ ii._1, acc2._2 ++ ii._2)
                }
            }
          }
      psStats.println("Backward runtime: " + (System.currentTimeMillis() - init) + " ms")
      psStats.println("Backward number of states: " + ib._1.size + "," + ib._2.size)
      val (okRev, failedRev) = ib
      psOutRev.println("[" + okRev.mkString(",") + "]")
      psFailedRev.println("[" + failedRev.mkString(",") + "]")
    }
    psOutRev.close()
    psFailedRev.close()
    psStats.close()
    psout.close()
    psFailed.close()
  }
}