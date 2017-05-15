package org.change.parser.p4

import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}

/**
  * A small gift from radu to symnetic.
  */
class P4GrammarListener extends P4GrammarBaseListener {

  // Section 2.1
  import scala.collection.mutable.{Map => MutableMap}
  private val declaredHeaders: MutableMap[String, HeaderDeclaration] = MutableMap()

  override def exitHeader_type_declaration(ctx: P4GrammarParser.Header_type_declarationContext): Unit = {
    val declaredHeaderName = ctx.header_type_name().getText
    //TODO: Support for other header lengths.
    val headerSize = ctx.header_dec_body().length_exp().const_value().constValue

    import scala.collection.JavaConversions._
    val fields = ctx.header_dec_body().field_dec().toList.map { h =>
      val width: Option[Int] = Option(h.bit_width().const_value()).map(_.constValue)
      val name: String = h.field_name().getText
      (name, width)
    }

    val fieldsWithSizes = {
      val total = fields.foldLeft(0)(_ + _._2.getOrElse(0))
      fields.map(field => (field._1, field._2.getOrElse(headerSize - total)))
    }

    declaredHeaders.put(declaredHeaderName,
      HeaderDeclaration(
        declaredHeaderName,
        fieldsWithSizes.scanLeft(0)(_ + _._2).zip(fieldsWithSizes).toMap,
        headerSize
      )
    )
  }
  // Exit Section 2.1

  // Section 2.2
  private val headerInstances: MutableMap[String, HeaderInstance] = MutableMap()

  override def exitScalar_instance(ctx: P4GrammarParser.Scalar_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText;
    val headerType = ctx.instance_name().getText;
    headerInstances.put(instanceName, ScalarHeader(instanceName, declaredHeaders(headerType)))
  }

  override def exitArray_instance(ctx: P4GrammarParser.Array_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText;
    val index = ctx.const_value().constValue;
    val headerType = ctx.header_type_name().getText
    headerInstances.put(instanceName + index, ArrayHeader(instanceName, index, declaredHeaders(headerType)))
  }
  // Exit Section 2.2

  override def exitHeader_ref(ctx: P4GrammarParser.Header_refContext): Unit = {
    ctx.headerInstanceId = if (ctx.index() != null)
        if (! (ctx.index().getText == "last"))
          ctx.instance_name().getText + ctx.index().getText
        else throw new UnsupportedOperationException("'last' not yet supported")
      else
        ctx.instance_name().getText

    ctx.tagReference = headerInstances(ctx.headerInstanceId).getTagExp
  }

  override def exitField_ref(ctx: P4GrammarParser.Field_refContext): Unit = {
    ctx.reference = ctx.header_ref().tagReference +
      headerInstances(ctx.header_ref().headerInstanceId).layout.indexOf(ctx.field_name().getText)
  }

  override def exitField_value(ctx: P4GrammarParser.Field_valueContext): Unit = {
    ctx.fieldValue = ctx.const_value().constValue
  }

  override def exitConst_value(ctx: P4GrammarParser.Const_valueContext): Unit = {
    //TODO: Support the width field
    ctx.constValue = (if (ctx.getText.startsWith("-")) -1 else 1) * ctx.unsigned_value().unsignedValue
  }

  override def exitUnsigned_value(ctx: P4GrammarParser.Unsigned_valueContext): Unit = {
    ctx.unsignedValue = {
        Option(ctx.decimal_value()).map(_.parsedValue) orElse
        Option(ctx.binary_value()).map(_.parsedValue) orElse
        Option(ctx.hexadecimal_value()).map(_.parsedValue)
    }.get
  }

  override def exitBinary_value(ctx: P4GrammarParser.Binary_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.binaryToInt(ctx.getText)
  }

  override def exitHexadecimal_value(ctx: P4GrammarParser.Hexadecimal_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.hexToInt(ctx.getText)
  }

  override def exitDecimal_value(ctx: P4GrammarParser.Decimal_valueContext): Unit = {
    ctx.parsedValue = ValueSpecificationParser.decimalToInt(ctx.getText)
  }
}
