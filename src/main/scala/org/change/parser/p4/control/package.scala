package org.change.parser.p4

package object control {
  val NORMAL : Int = 0
  val INGRESS_CLONE : Int = 1
  val EGRESS_CLONE : Int = 2
  val RECIRCULATED : Int = 3
  val RESUBMITED : Int = 4
  val MULTICAST : Int = 5

  val DROP_VALUE : Int = 511

  val FIELD_LIST_REF : String = "field_list_ref"
  val STANDARD_METADATA : String = "standard_metadata"
  val INTRINSIC_METADATA : String = "intrinsic_metadata"
  val CLONE_SPEC : String = "clone_spec"
  val RESUBMIT_FLAG : String = "resubmit_flag"
  val RECIRCULATE_FLAG : String = "recirculate_flag"
}
