package org.change.p4.control

import java.lang

import com.microsoft.z3.{BitVecNum, Context}
import org.change.p4.control.queryimpl.{ScalarValue, StructObject, Value}
import org.change.p4.control.types.BVType

object PacketLinearize {
  def linearize(packValue: Value,
                len : Int,
                context : Context): String = {
    var crtString = ""
    if (!packValue.ofType.isInstanceOf[BVType])
      throw new IllegalArgumentException(
        s"packet type expected, got $packValue of type ${packValue.ofType}"
      )
    var ast = packValue.asInstanceOf[ScalarValue].AST
    ast match {
      case bvn : BitVecNum =>
        val stringBuilder = new StringBuilder()
        if (len % 8 == 0) {
          System.err.println(bvn.getBigInteger.toString(2).reverse)
          for (l <- bvn.getBigInteger.toByteArray.reverse) {
            stringBuilder.append("%02x".format(l))
          }
          System.err.println(bvn)
          System.err.println(stringBuilder.mkString)
          stringBuilder.mkString
        } else {
          bvn.getBigInteger.toString(2).take(len)
        }
      case _ => throw new IllegalArgumentException("can't deal with this kind of" +
        s"packet $ast")
    }
  }

}
