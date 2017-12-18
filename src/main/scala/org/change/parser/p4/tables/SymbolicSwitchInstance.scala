package org.change.parser.p4.tables

import java.util

import org.change.parser.p4.factories.FullTableFactory
import org.change.v2.p4.model.{ISwitchInstance, Switch}

import scala.collection.JavaConverters._

class SymbolicSwitchInstance(name : String,
                             ifaces : Map[Int, String],
                             cloneSpec : Map[Int, Int],
                             switch: Switch) extends ISwitchInstance {
  override lazy val getCloneSpec2EgressSpec: util.Map[Integer, Integer] = cloneSpec.map(r => {
    new Integer(r._1) -> new Integer(r._2)
  }).asJava

  override lazy val getIfaceSpec: util.Map[Integer, String] = ifaces.map(r => new Integer(r._1) -> r._2).asJava

  override def getName: String = name

  def getSwitch : Switch = switch

  def flowInstances() : List[P4FlowInstance] = ???

  def defaultAction() : ActionDefinition = ???
}

object SymbolicSwitchInstance {
  FullTableFactory.register(classOf[SymbolicSwitchInstance], (switchInstance: SymbolicSwitchInstance, tableName : String, id : String) => {
    new FullTableWithInstances(tableName = tableName,
      id = id,
      switch = switchInstance.getSwitch,
      flowDefinitions = switchInstance.flowInstances(),
      defaultAction = switchInstance.defaultAction(),
      switchInstance = switchInstance
    ).fullAction()
  })

  def apply(name: String,
            ifaces: Map[Int, String],
            cloneSpec: Map[Int, Int],
            switch: Switch): SymbolicSwitchInstance = new SymbolicSwitchInstance(name, ifaces, cloneSpec, switch)

}