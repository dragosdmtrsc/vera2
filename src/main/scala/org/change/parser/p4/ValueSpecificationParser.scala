package org.change.parser.p4

/**
  * A small gift from radu to symnetic.
  */
/**
  * 1.5.1 Value Specifications
  */
object ValueSpecificationParser {
  def binaryToInt: (String => Int) = reprToInt(2)
  def hexToInt: (String => Int) = reprToInt(16)
  def decimalToInt: (String => Int) = reprToInt(10)

  def reprToInt(base: Int)(repr: String): Int = Integer.parseInt(repr.replaceAll("-", ""), base)
}
