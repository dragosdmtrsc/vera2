package org.change.v2.plugins.eq

import org.change.v2.plugins.click.ClickPlugin

class ClickToposPlugin extends AbsIntegrationPlugin {
  private lazy val topo1 = new ClickPlugin(topologyLeftParms("dir"))
  private lazy val topo2 = new ClickPlugin(topologyRightParms("dir"))

  override def topologyLeft(): TopologyPlugin = topo1
  override def topologyRight(): TopologyPlugin = topo2

  override def inputMappings(): InputPortPlugin =
    new PortMapper(topo1.startNodes().intersect(topo2.startNodes()).map(h => h -> h).toMap)

  override def outputMappings(): OutputPortPlugin =
    new PortMapper(topo1.terminals().intersect(topo2.terminals()).map(h => h -> h).toMap)
}
object ClickToposPlugin {
  class Builder extends PluginBuilder[ClickToposPlugin] {
    var dir1 = ""
    var dir2 = ""
    override def set(parm: String, value: String): PluginBuilder[ClickToposPlugin] = this
    override def build(): ClickToposPlugin = new ClickToposPlugin()
  }
}