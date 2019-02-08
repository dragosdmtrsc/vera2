package org.change.v2.plugins.click

import java.io.{File, FilenameFilter}

import org.change.parser.clickfile.ClickToAbstractNetwork
import org.change.parser.interclicklinks.InterClickLinksParser
import org.change.parser.startpoints.StartPointParser
import org.change.v2.abstractnet.optimized.router.OptimizedRouter
import org.change.v2.analysis.executor.{ClickAsyncExecutor, CodeAwareInstructionExecutor, DecoratedInstructionExecutor, TrivialTripleInstructionExecutor}
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.plugins.eq.{PluginBuilder, TopologyPlugin}
import org.change.v2.util.ToDot

class ClickPlugin(folder : String) extends TopologyPlugin {
  private val clicksFolder = new File(folder)
  private val clicks = clicksFolder.list(new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".click")
  }).sorted.map(clicksFolder.getPath + File.separatorChar + _)

  private val routers = clicksFolder.list(new FilenameFilter {
    override def accept(dir: File, name: String): Boolean = name.endsWith(".rt")
  }).sorted.map(clicksFolder.getPath + File.separatorChar + _)

  private val rawLinks = InterClickLinksParser.parseLinks(s"$folder/links.links")

  private val startElems: Option[Iterable[(String, String, String)]] = Some(StartPointParser.parseStarts
  (s"$folder/start.start"))
  private lazy val configs = clicks.map(ClickToAbstractNetwork.buildConfig(_, prefixedElements = true)) ++
    routers.map(f =>
      OptimizedRouter.bvRouterNetworkConfig(new File(f)))
  private lazy val (instrs, links) = ClickAsyncExecutor.buildTopo(
    configs,
    rawLinks)
  private lazy val instructions = CodeAwareInstructionExecutor.flattenProgram(instrs, links)

  private lazy val starts = ClickAsyncExecutor.getStartPoints(configs, startElems).toSet

  override def apply(): collection.Map[String, Instruction] = instructions

  override def startNodes(): collection.Set[String] = starts

  private lazy val ipcfg = ToDot.mkIPCG(instructions, startNodes.toSet)
  def terminals(): Set[String] = {
    ipcfg._2.filter(x => {
      !ipcfg._1.contains(x) || ipcfg._1(x).isEmpty
    })
  }
}

object ClickPlugin {
  class Builder extends PluginBuilder[ClickPlugin] {
    var folder : String = ""
    override def set(parm: String, value: String): PluginBuilder[ClickPlugin] = parm match {
      case "dir" => folder = value
        this
      case _ => throw new IllegalArgumentException(parm + " unknown argument, expecting dir")
    }

    override def build(): ClickPlugin = new ClickPlugin(folder)
  }
}
