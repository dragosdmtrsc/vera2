package org.change.parser

import org.change.v2.p4.model.{ISwitchInstance, SwitchInstance}

package object p4 {
  GlobalInitFactory.register(classOf[SwitchInstance], (r : ISwitchInstance) => {
    new InstanceBasedInitFactory(r.asInstanceOf[SwitchInstance]).initCode()
  })
  FullTableFactory.register(classof = classOf[SwitchInstance], (switchInstance : ISwitchInstance, tabName : String, id : String) => {
    new FullTable(tabName, switchInstance.asInstanceOf[SwitchInstance], id).fullAction()
  })
}
