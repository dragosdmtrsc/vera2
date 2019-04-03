package org.change.parser.p4.control

import java.util.IllegalFormatException

import org.change.parser.p4.control.queryimpl.{PacketWrapper, ScalarValue, Value}
import org.change.plugins.vera.PacketType

import scala.collection.mutable


object PacketLinearize {
  def linearize(packValue : Value) : String = {
    var crtString = ""
    if (packValue.ofType != PacketType)
      throw new IllegalArgumentException(s"packet type expected, got $packValue")
    var ast = packValue.asInstanceOf[ScalarValue].z3AST
    val packet = PacketWrapper(ast.context)
    var break = false
    while (!packet.isNil(ast) && !break) {
      val idx = packet.takeKind(ast)
      if (idx.nonEmpty) {
        val (prev, now) = packet.unwrap(ast, idx.get)
        val astString = now.context.astToString(now)
        val str = if (astString.startsWith("#x")) {
          val nr = astString.substring(2)
          var wholeValue = ""
          for (c <- nr) {
            var bitval = ""
            var nibValue = if (c >= '0' && c <= '9') {
              c - '0'
            } else {
              c - 'a'
            }
            for (_ <- 0 until 4) {
              if (nibValue % 2 == 0) {
                bitval = '0' + bitval
              } else {
                bitval = '1' + bitval
              }
              nibValue = nibValue >> 1
            }
            wholeValue = wholeValue + bitval
          }
          wholeValue
        } else if (astString.startsWith("#b")) {
          astString.substring(2)
        } else {
          throw new IllegalArgumentException("expecting z3 bitvector in #x or #b format")
        }
        crtString = crtString + str
        ast = prev
      } else {
        break = true
        System.err.println("still having some symbolic value... how do we handle this")
      }
    }
    crtString
  }

}
