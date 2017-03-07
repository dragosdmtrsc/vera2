package org.change.parser

/**
  * A small gift from radu to symnetic.
  */
package object p4 {

  def binaryToInt: (String => Int) = reprToInt(2)
  def hexToInt: (String => Int) = reprToInt(16)
  def decimalToInt: (String => Int) = reprToInt(10)

  def reprToInt(base: Int)(repr: String): Int = Integer.parseInt(repr.replaceAll("-", ""), base)

}
