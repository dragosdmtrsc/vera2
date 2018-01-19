package org.change.parser.p4

/**
  * P4 Section 1.5.1 Value Specifications
  */
object ValueSpecificationParser {
  def binaryToInt: (String => Long) = reprToInt(2)
  def hexToInt: (String => Long) = reprToInt(16)
  def decimalToInt: (String => Long) = reprToInt(10)

  def reprToInt(base: Int)(repr: String): Long =
    java.lang.Long.parseLong(repr.replaceAll("-", ""), base).longValue()
}
