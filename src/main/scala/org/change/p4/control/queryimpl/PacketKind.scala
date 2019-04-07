package org.change.p4.control.queryimpl
import java.util.logging.Logger

import org.change.p4.control.types.{BVType, StructType}
import org.change.p4.control.vera.ParserHelper
import org.change.p4.model.Switch
import org.change.p4.model.control.{ControlStatement, EndOfControl}
import org.change.p4.model.parser.ExtractStatement

import scala.collection.mutable


class PacketKind(val maxLen : Int) extends StructType(
  Map("pack" -> BVType(maxLen), "len" -> BVType(32)))

object PacketKind {
  val packetMap : mutable.Map[Switch, PacketKind] = mutable.Map.empty
  def apply(switch: Switch) : PacketKind = {
    packetMap.getOrElseUpdate(switch, {
      val ph = ParserHelper(switch)
      val unr = ph.mkUnrolledLabeledGraph.graphView
      val propagated = unr
        .scc()
        .reverse
        .foldLeft(Map.empty[ControlStatement, Int])((crt, x) => {
          val node = x.head
          val nowat = crt.getOrElse(node, 0)
          val add = node match {
            case es: ExtractStatement =>
              es.getExpression.getInstance().getLayout.getLength
            case _ => 0
          }
          val prop = nowat + add
          unr.edges.getOrElse(node, Nil).foldLeft(crt)((acc, nxt) => {
            val n = acc.getOrElse(nxt, 0)
            acc + (nxt -> math.max(n, prop))
          })
        })
      val maxlen = propagated(new EndOfControl("parser"))
      Logger.getLogger(getClass.getSimpleName).info(s"max packet size $maxlen")
      new PacketKind(maxlen)
    })
  }
}