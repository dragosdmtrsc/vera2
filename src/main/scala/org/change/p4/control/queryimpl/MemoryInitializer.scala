package org.change.p4.control.queryimpl

import org.change.p4.control._
import org.change.p4.control.types._
import org.change.p4.model.actions.P4ActionType
import org.change.p4.model.{ArrayInstance, Header, HeaderInstance, Switch}
import com.microsoft.z3.Context

import scala.collection.JavaConverters._
object MemoryInitializer {
  def mkStruct(layout: Header): StructType = {
    StructType(
      layout.getFields.asScala
        .filter(_.getLength != 0)
        .map(f => {
          f.getName -> BVType(f.getLength)
        })
        .toMap ++ Map("IsValid" -> BoolType)
    )
  }

  def helperStuff(switch: Switch): Map[String, P4Type] = {
    Map(
      "packet" -> PacketKind(switch),
      "errorCause" -> UnboundedInt,
      "packetLength" -> BVType(16),
      FIELD_LIST_REF -> BoundedInt()
    )
  }

  def populateHelpers(switch: Switch, ctx: P4RootMemory)(
    implicit context: Context
  ): P4RootMemory = {
    ctx
      .update(ctx.field(FIELD_LIST_REF), ctx.int(0))
      .update(ctx.field("errorCause"), ctx.int(0))
      .as[P4RootMemory]
  }

  def tableStructures(
    switch: Switch
  )(implicit context: Context): Map[String, P4Type] = {
    switch
      .tables()
      .asScala
      .map(tableDeclaration => {
        val lastQName = tableDeclaration.getName + "_lastQuery"
        lastQName -> FlowStruct(switch, tableDeclaration, context)
      })
      .toMap
  }

  def actionStructures(
    switch: Switch
  )(implicit context: Context): Map[String, P4Type] = {
    switch
      .actions()
      .asScala
      .filter(_.getActionType == P4ActionType.Complex)
      .flatMap(x => {
        val aname = x.getActionName
        x.getParameterList.asScala.map(ap => {
          aname + "_" + ap.getParamName -> ap.getSort
        })
      })
      .toMap
  }

  def populateHeaders(switch: Switch,
                      crt: P4Memory)(implicit context: Context): P4Memory = {
    switch.getInstances.asScala.foldLeft(crt)(
      (crt, h) =>
        h match {
          case ai: ArrayInstance =>
            val inst = crt.field(ai.getName)
            (0 until ai.getLength).foldLeft(crt)((acc, x) => {
              val h = inst(inst.next().int(x))
              val valid = h.valid()
              val m = if (!ai.isMetadata) {
                acc.update(valid, h.valid().int(0))
              } else {
                val rm = acc.asInstanceOf[P4RootMemory]
                rm.copy(
                  rootMemory = rm.rootMemory.set(
                    List(Left("IsValid"), Right(x), Left(ai.getName)),
                    ImmutableValue(rm.rootMemory.mkBool(true))
                  )
                )
              }
              if (ai.isMetadata) {
                ai.getLayout.getFields.asScala.foldLeft(m)((acc, fld) => {
                  acc.update(
                    h.field(fld.getName),
                    h.field(fld.getName)
                      .int(ai.getInitializer.getOrDefault(fld.getName, 0))
                  )
                })
              } else {
                m
              }
            })
          case hi: HeaderInstance =>
            val inst = crt.field(hi.getName)
            val h = inst
            val valid = h.valid()
            val m =
              if (!hi.isMetadata)
                crt.update(valid, h.valid().int(0))
              else {
                val rm = crt.asInstanceOf[P4RootMemory]
                rm.copy(
                  rootMemory = rm.rootMemory.set(
                    List(Left("IsValid"), Left(hi.getName)),
                    ImmutableValue(rm.rootMemory.mkBool(true))
                  )
                )
              }
            if (hi.isMetadata)
              hi.getLayout.getFields.asScala.foldLeft(m)((acc, fld) => {
                if (hi.getName == "standard_metadata" &&
                    (fld.getName == "ingress_port" ||
                    fld.getName == "packet_length")) {
                  acc
                } else {
                  acc.update(
                    h.field(fld.getName),
                    h.field(fld.getName)
                      .int(hi.getInitializer.getOrDefault(fld.getName, 0))
                  )
                }
              })
            else m
      }
    )
  }

  def initialize(switch: Switch)(implicit context: Context): P4RootMemory = {
    val st = StructType(
      switch.getInstances.asScala
        .map(
          x =>
            x.getName -> (x match {
              case arrayInstance: ArrayInstance =>
                FixedArrayType(
                  mkStruct(arrayInstance.getLayout),
                  arrayInstance.getLength
                )
              case hi: HeaderInstance =>
                mkStruct(hi.getLayout)
            })
        )
        .toMap ++ helperStuff(switch) ++ tableStructures(switch) ++ actionStructures(
        switch
      )
    )
    val tm = TypeMapper()(context)
    val structObject = tm.fresh(st, "input").asInstanceOf[StructObject]
    val fresh = P4RootMemory(
      switch,
      RootMemory(structObject = structObject, tm.literal(BoolType, 1))
    )
    val r = populateHelpers(
      switch,
      populateHeaders(switch, fresh)(context).as[P4RootMemory]
    )
    val constrained =
      r.where(r.ingressAllowed(r.standardMetadata().field("ingress_port")))
    assert(r.err().as[P4RootMemory].rootMemory.isEmpty())
    constrained.as[P4RootMemory]
  }
}
