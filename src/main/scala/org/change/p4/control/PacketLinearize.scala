package org.change.p4.control

import com.microsoft.z3.{BitVecNum, Context}
import org.change.p4.control.queryimpl.{PacketWrapper, ScalarValue, Value}
import org.change.p4.control.types.PacketType

object PacketLinearize {
  //TODO: make the linearize thing look better - looking strange for the moment
  def linearize(packValue: Value, context : Context): String = {
    var crtString = ""
    if (packValue.ofType != PacketType)
      throw new IllegalArgumentException(
        s"packet type expected, got $packValue"
      )
    var ast = packValue.asInstanceOf[ScalarValue].AST
    val packet = PacketWrapper(context)
    var break = false
    while (!packet.isNil(ast) && !break) {
      val idx = packet.takeKind(ast)
      if (idx.nonEmpty) {
        val (prev, now) = packet.unwrap(ast, idx.get)
        val str = if (now.isBVNumeral) {
          val bvnum = now.asInstanceOf[BitVecNum]
          val sz = idx.get
          bvnum.getBigInteger.toString(2)
        } else {
          s"#bv${idx.get}[${now.toString}]"
        }
        crtString = crtString + str
        ast = prev
      } else {
        break = true
        System.err.println(
          "still having some symbolic value... how do we handle this"
        )
      }
    }
    crtString
  }

}
