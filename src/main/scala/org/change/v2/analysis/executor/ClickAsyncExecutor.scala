package org.change.v2.analysis.executor

import java.util.concurrent.{ExecutorService, Executors}

import org.change.v2.abstractnet.generic.NetworkConfig
import org.change.v2.analysis.memory.State
import org.change.v2.analysis.processingmodels.{Instruction, _}

class ClickAsyncExecutor(
                          instructions: Map[LocationId, Instruction],
                          links: Map[LocationId, LocationId],
                          maxLevel: Int = 1000,
                          executor: InstructionExecutor = InstructionExecutor(),
                          nr: Int = 4,
                          service: Option[ExecutorService] = None
                        ) extends AsyncExecutor(executor, instructions, maxLevel,
  ClickAsyncExecutor.getExecutor(nr, service)) {

  override def getInstruction(loc: LocationId): Option[Instruction] = {
    val i1 = instructions.get(loc)
    if (i1.isEmpty) {
      if (links contains loc)
        getInstruction(links(loc))
      else
        None
    }
    else
      i1
  }

}

object ClickAsyncExecutor {

  def getExecutor(nr: Int, service: Option[ExecutorService]) = {
    service match {
      case Some(x) => x
      case None => Executors.newFixedThreadPool(nr)
    }
  }

  def buildAggregated(
                       configs: Iterable[NetworkConfig],
                       interClickLinks: Iterable[(String, String, String, String, String, String)],
                       startElems: Option[Iterable[(String, String, String)]] = None,
                       nrThreads: Int = 1,
                       instrExec: InstructionExecutor = InstructionExecutor()):
  (ClickAsyncExecutor, List[State]) = {
    // Create a context for every network config.
    val ctxes = configs.map(networkModel => networkModel.elements.values.
      foldLeft(Map[LocationId, Instruction]())(_ ++ _.instructions))
    val intraLinks = configs.map(networkModel => {
      networkModel.paths.flatMap(_.sliding(2).map(pcp => {
        val src = pcp.head
        val dst = pcp.last
        networkModel.elements(src._1).outputPortName(src._3) -> networkModel.elements(dst._1).inputPortName(dst._2)
      })).toMap
    }).foldLeft(Map[String, String]())(_ ++ _)
    // Keep the configs for name resolution.
    val configMap: Map[String, NetworkConfig] = configs.map(c => c.id.get -> c).toMap
    // Add forwarding links between click files.
    val links = interClickLinks.map(l => {
      val ela = l._1 + "-" + l._2
      val elb = l._4 + "-" + l._5
      configMap(l._1).elements(ela).outputPortName(l._3) -> configMap(l._4).elements(elb).inputPortName(l._6)
    }).toMap ++ intraLinks
    // Create initial states
    val startStates = startElems match {
      case Some(initialPoints) => initialPoints.map(ip => {
        val st = State.bigBang()
        st.copy(history = configMap(ip._1).elements(ip._1 + "-" + ip._2).inputPortName(ip._3) :: st.history)
      })
      case None => {
        val st = State.bigBang()
        List(st.copy(history = configs.head.entryLocationId :: st.history))
      }
    }
    val instrs = ctxes.foldLeft(Map[String, Instruction]())(_ ++ _)

    val executor = new ClickAsyncExecutor(instrs, links, executor = instrExec, nr = nrThreads)
    (executor, startStates.toList)
  }
}
