package org.change.p4

import org.change.p4.control.SolveTables
import org.change.p4.control.queryimpl.{PacketWrapper, StructureInitializer}
import org.change.p4.control.vera.ParserHelper
import org.change.p4.model.Switch
import z3.scala.{Z3AST, Z3Context}
import z3.scala.Z3Context.AstPrintMode

package object tools {
  class Initializer(switch: Switch) {
    def init(context: Z3Context): Switch = {
      val s = SolveTables(switch)
      context.setAstPrintMode(AstPrintMode.Z3_PRINT_SMTLIB2_COMPLIANT)
      StructureInitializer(switch)(context)
      val helper = new ParserHelper(switch)
      declarePacketAxioms(context, helper)
      switch
    }
    private def declarePacketAxioms(context : Z3Context, helper: ParserHelper): Unit = {
      val refs = helper.resolveDataRefs(helper.mkUnrolledLabeledGraph)
      val packetWrapper = PacketWrapper(context)
      val iniPacket = context.mkFreshConst("packet", packetWrapper.packetSort._1)
      val axioms = refs.toList.map(ref => {
        val offendingStat = ref._1
        val (initstart, inilen) = (offendingStat._2.getStart.toInt, offendingStat._2.getEnd.toInt)
        val pcs = ref._2.map(path => {
          var (intstart, len) = (initstart, inilen)
          var crtPacket = iniPacket
          var pathCondition = context.mkTrue()
          var currentValue: Z3AST = null
          for (loc <- path) {
            val es = loc._1
            val layOfTheLand = es.getExpression.getInstance().getLayout
            val IT = layOfTheLand.getFields.iterator()
            while (IT.hasNext && len > 0) {
              val field = IT.next()
              val widthIdx = packetWrapper.indexing(field.getLength)
              val (nextstart, consumes) = if (intstart < field.getLength) {
                (0, field.getLength - intstart)
              } else {
                (intstart - field.getLength, 0)
              }
              if (consumes != 0) {
                val x = packetWrapper.packetSort._4(widthIdx)(1)(crtPacket)
                val actualConsume = if (consumes <= len) {
                  consumes
                } else {
                  len
                }
                val whatComes =
                  if (intstart == 0 && actualConsume == field.getLength)
                    x
                  else
                    context.mkExtract(intstart + actualConsume - 1, intstart, x)
                if (currentValue != null) {
                  currentValue = context.mkConcat(currentValue, whatComes)
                } else {
                  currentValue = whatComes
                }
              }
              pathCondition = context.mkAnd(pathCondition, packetWrapper.packetSort._3(widthIdx)(crtPacket))
              crtPacket = packetWrapper.packetSort._4(widthIdx).head(crtPacket)
              intstart = nextstart
              len = len - consumes
            }
          }
          (pathCondition, currentValue)
        })
        (offendingStat._2.getStart, offendingStat._2.getEnd) -> pcs
      }).groupBy(x => x._1).mapValues(u => {
        u.flatMap(_._2)
      })
      axioms.foreach(r => {
        val bitwidth = r._1._2.toInt
        val forall = context.mkForAllConst(0, Seq(), Seq(iniPacket),
          context.mkEq(
            packetWrapper.take(iniPacket, r._1._1.toInt, r._1._2.toInt),
            r._2.foldLeft(context.mkInt(0, context.mkBVSort(bitwidth)))((acc, v) => {
              context.mkITE(v._1, v._2, acc)
            })
          )
        )
        packetWrapper.declareAxiom(forall)
      })
    }
  }
  implicit def apply(switch: Switch): Initializer = new Initializer(switch)
}
