package org.change.parser.p4

/**
  * P4 Section 1.5.1 Value Specifications
  */
object ValueSpecificationParser {
  def binaryToInt: String => BigInt = reprToInt(2)
  def hexToInt: String => BigInt = reprToInt(16)
  def decimalToInt: String => BigInt = reprToInt(10)

  def reprToInt(base: Int)(repr: String): BigInt =
    BigInt.apply(repr.replaceAll("-", ""), base)
}
