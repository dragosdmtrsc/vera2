package org.change.parser.p4.resolution

import org.change.v2.p4.model.Switch
import org.change.v2.p4.model.actions.P4Action

trait Scope {}
class GlobalScope(switch: Switch) extends Scope
class LocalScope(p4Action: P4Action) extends Scope
class Stack(lst : List[Scope]) extends Scope
