package org.change.p4.control.queryimpl
import org.change.p4.control.STANDARD_METADATA
import org.change.p4.control.types.{BVType, BoolType}
import org.change.p4.model.Switch
import com.microsoft.z3.Context

object StructureInitializer {
  implicit def apply(switch: Switch)(implicit context: Context): Switch = {
    val tm = TypeMapper.apply()(context)
    val stdMeta = switch.getInstance("standard_metadata")
    val clSpec = stdMeta.getLayout.getField("clone_spec")
    val cloneSpecLen = if (clSpec != null) {
      clSpec.getLength
    } else {
      // default to 16, because this means that clone session
      // will never get called
      16
    }
    tm.mkFunction("clone_session", BVType(cloneSpecLen), BVType(9))
    val w = switch.mcastGrpWidth()
    if (w != 0)
      tm.mkFunction("mgid_session", BVType(w), BVType(9))
    val igPort =
      switch.getInstance(STANDARD_METADATA).getLayout.getField("ingress_port")
    tm.mkFunction(
      "constrain_ingress_port",
      BVType(igPort.getLength),
      BoolType
    )
    val egPort =
      switch.getInstance(STANDARD_METADATA).getLayout.getField("egress_port")
    tm.mkFunction("constrain_egress_port", BVType(egPort.getLength), BoolType)
    switch
  }
}
