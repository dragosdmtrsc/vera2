package org.change.p4.control

object SMInstantiator {
  implicit def apply(ctx: P4Memory): RichContext = RichContext(ctx)
}
