package org.change.parser.p4.control

import org.change.v2.p4.model.Switch

class BufferMechanism(switch : Switch) {
  def apply(context : P4Memory): (P4Memory, P4Memory, P4Memory, P4Memory) = {
    def egressPort = context.standardMetadata().field("egress_port")
    def instance = context.standardMetadata().field("instance_type")
    val espec = context.standardMetadata().field("egress_spec")
    val recirc = context.standardMetadata().field("recirculate_flag")
    val cloneSpec = context.standardMetadata().field("clone_spec")
    val dropped = context.where(espec != espec.int(511))
    val recirculated = context.where(recirc != recirc.int(0))
    val cloned = context.where(cloneSpec != cloneSpec.int(0))
      .update(cloneSpec, cloneSpec.int(0))
      .update(instance, instance.int(1))
      .update(egressPort, egressPort.fresh())
    val continued = context.where(!(
      espec != espec.int(511) ||
        recirc != recirc.int(0) ||
          cloneSpec != cloneSpec.int(0)
    )).update(egressPort, egressPort.fresh())
    (dropped, recirculated, cloned, (continued || cloned).as[P4Memory])
  }

}
