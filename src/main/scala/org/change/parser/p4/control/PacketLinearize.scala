package org.change.parser.p4.control

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
        crtString = now match {
          case Left(id) =>
            val sz = idx.get
            var nr = id
            for (i <- 0 until sz) {
              if (nr % 2 == 0)
                crtString = crtString + '0'
              else crtString = crtString + '1'
              nr = nr >> 1
            }
            crtString
          case Right(x) =>
            crtString + "[#" + idx.get + ":" + x.toString() + "]"
        }
        ast = prev
      } else {
        break = true
        System.err.println("still having some symbolic value... how do we handle this")
      }
    }
    crtString
  }

}
