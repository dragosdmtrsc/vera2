package org.change.parser.p4

import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}

/**
  * A small gift from radu to symnetic.
  */
class P4GrammarListener extends P4GrammarBaseListener {

  // Section 1.5 Begin
  override def exitField_value(ctx: P4GrammarParser.Field_valueContext): Unit =
    ctx.fieldValue = ctx.const_value().constValue

  override def exitConst_value(ctx: P4GrammarParser.Const_valueContext): Unit =
  //TODO: Support the width field
    ctx.constValue = (if (ctx.getText().startsWith("-")) -1 else 1) *
      ctx.unsigned_value().unsignedValue

  override def exitHexadecimalUValue(ctx: P4GrammarParser.HexadecimalUValueContext): Unit =
    ctx.unsignedValue = ctx.hexadecimal_value().parsedValue

  override def exitDecimalUValue(ctx: P4GrammarParser.DecimalUValueContext): Unit =
    ctx.unsignedValue = ctx.decimal_value().parsedValue

  override def exitBinaryUValue(ctx: P4GrammarParser.BinaryUValueContext): Unit =
    ctx.unsignedValue = ctx.binary_value().parsedValue

  override def exitBinary_value(ctx: P4GrammarParser.Binary_valueContext): Unit =
    ctx.parsedValue = ValueSpecificationParser.binaryToInt(ctx.getText)

  override def exitHexadecimal_value(ctx: P4GrammarParser.Hexadecimal_valueContext): Unit =
    ctx.parsedValue = ValueSpecificationParser.hexToInt(ctx.getText)

  override def exitDecimal_value(ctx: P4GrammarParser.Decimal_valueContext): Unit =
    ctx.parsedValue = ValueSpecificationParser.decimalToInt(ctx.getText)
  // Section 1.5 End


  // Section 2.1 Begin
  import scala.collection.JavaConversions._

  // Section 2.1
  import scala.collection.mutable.{Map => MutableMap}
  // Declared headers are collected here.
  val declaredHeaders: MutableMap[String, HeaderDeclaration] = MutableMap()

  override def exitHeader_type_declaration(ctx: P4GrammarParser.Header_type_declarationContext): Unit = {
    val declaredHeaderName = ctx.header_type_name().getText
    //TODO: Support for other header lengths.
    var headerSize = if (ctx.header_dec_body().length_exp() != null &&
      ctx.header_dec_body().length_exp().const_value() != null)
        Some(ctx.header_dec_body().length_exp().const_value().constValue)
      else
        None

    val fields = ctx.header_dec_body().field_dec().toList.map { h =>
      val width: Option[Int] = Option(h.bit_width().const_value()).map(_.constValue)
      val name: String = h.field_name().getText
      (name, width)
    }

    val fieldsWithSizes = {
      val total = fields.foldLeft(0)(_ + _._2.getOrElse(0))
      // If not defined, then it is computed.
      if (headerSize.isEmpty) headerSize = Some(total)
      // By this point there should be a value for the size of the header.
      fields.map(field => (field._1, field._2.getOrElse(headerSize.get - total)))
    }

    ctx.headerDeclaration = HeaderDeclaration(
      declaredHeaderName,
      fieldsWithSizes.scanLeft(0)(_ + _._2).zip(fieldsWithSizes).toMap,
      headerSize.get
    )

    declaredHeaders.put(declaredHeaderName,ctx.headerDeclaration)
  }
  // Exit Section 2.1

  // Section 2.2
  private val headerInstances: MutableMap[String, HeaderInstance] = MutableMap()

  override def exitScalarInstance(ctx: P4GrammarParser.ScalarInstanceContext): Unit =
    ctx.instance = ctx.scalar_instance().instance

  override def exitArrayInstance(ctx: P4GrammarParser.ArrayInstanceContext): Unit =
    ctx.instance = ctx.array_instance().instance

  override def exitScalar_instance(ctx: P4GrammarParser.Scalar_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText
    val headerType = ctx.instance_name().getText
    ctx.instance = new ScalarHeader(instanceName, declaredHeaders(headerType))
    headerInstances.put(instanceName, ctx.instance)
  }

  override def exitArray_instance(ctx: P4GrammarParser.Array_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText;
    val index = ctx.const_value().constValue;
    val headerType = ctx.header_type_name().getText
    ctx.instance = new ArrayHeader(instanceName, index, declaredHeaders(headerType))
    headerInstances.put(instanceName + index, ctx.instance)
  }

  override def exitMetadata_initializer(ctx: P4GrammarParser.Metadata_initializerContext): Unit = {
    import scala.collection.JavaConverters._
    ctx.inits = (ctx.field_name().asScala zip ctx.field_value().asScala).map( nv => {
      nv._1.getText -> nv._2.fieldValue
    }).toMap
  }

  override def exitMetadata_instance(ctx: P4GrammarParser.Metadata_instanceContext) = {
    val instanceName = ctx.instance_name().getText
    val headerType = ctx.instance_name().getText
    val initializer: Map[String, Int] = if (ctx.metadata_initializer() != null)
      ctx.metadata_initializer().inits.map(mv => mv._1.toString -> mv._2.intValue()).toMap
    else
      Map.empty
    ctx.instance = new MetadataInstance(instanceName, declaredHeaders(headerType), initializer)
    headerInstances.put(instanceName, ctx.instance)
  }
  // Exit Section 2.2

  // Section 4
  override def exitHeader_extract_ref(ctx: P4GrammarParser.Header_extract_refContext): Unit = {
    ctx.headerInstance = headerInstances(
      ctx.instance_name().getText + {
        if (ctx.header_extract_index() != null)
          if (ctx.header_extract_index().getText != "next")
            ctx.header_extract_index().getText
          else "" //TODO: Support next
        else ""
      }
    )
  }

  val declaredFunctions: MutableMap[String, ParserFunctionDeclaration] = MutableMap()

  override def exitParser_function_declaration(ctx: P4GrammarParser.Parser_function_declarationContext): Unit = {
    ctx.functionDeclaration = ParserFunctionDeclaration(
      ctx.parser_state_name().getText,
      ctx.parser_function_body().extract_or_set_statement().toList.map(_.functionStatement)
    )
  }

  override def exitExtract_or_set_statement(ctx: P4GrammarParser.Extract_or_set_statementContext): Unit = {
    ctx.functionStatement = Option(ctx.extract_statement()).map(_.extractStatement).orElse(
        throw new Exception("Set not supported")
      ).get
  }

  override def enterExtract_statement(ctx: P4GrammarParser.Extract_statementContext): Unit = {
    ctx.extractStatement = ExtractHeader(ctx.header_extract_ref().headerInstance)
  }

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
}
