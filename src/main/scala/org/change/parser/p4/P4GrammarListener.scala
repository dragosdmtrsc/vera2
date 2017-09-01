package org.change.parser.p4

import java.util

import generated.parse.p4.P4GrammarParser._
import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}
import org.change.v2.analysis.memory.{Tag, TagExp}
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.p4.model._
//import org.change.v2.analysis.memory.TagExp.IntImprovements
import org.change.v2.p4.model.actions._
import org.change.v2.analysis.memory.Intable
/**
  * A small gift from radu to symnetic.
  */
class P4GrammarListener extends P4GrammarBaseListener {

  // Section 1.5 Begin
  override def exitField_value(ctx: P4GrammarParser.Field_valueContext): Unit = {
    ctx.fieldValue = ctx.const_value().constValue
    if (p4ActionCall != null) {
      val value = ctx.fieldValue.toString
      val p4Instance = new P4ParameterInstance().setParameter(new P4ActionParameter(P4ActionParameterType.VAL.x, "")).setValue(value)
      p4ActionCall.addParameter(p4Instance)
    }
  }

  override def exitConst_value(ctx: P4GrammarParser.Const_valueContext): Unit =
  //TODO: Support the width field
    ctx.constValue = (if (ctx.getText().startsWith("-")) -1 else 1) *
      ctx.unsigned_value().unsignedValue

  override def exitHexadecimalUValue(ctx: P4GrammarParser.HexadecimalUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.hexToInt(ctx.Hexadecimal_value().getText.substring(2))

  override def exitDecimalUValue(ctx: P4GrammarParser.DecimalUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.decimalToInt(ctx.Decimal_value().getText)

  override def exitBinaryUValue(ctx: P4GrammarParser.BinaryUValueContext): Unit =
    ctx.unsignedValue = ValueSpecificationParser.binaryToInt(ctx.Binary_value().getText)

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
    ctx.header = new Header().setName(declaredHeaderName).setMaxLength(ctx.header_dec_body().maxLength)
    for (f <- ctx.header_dec_body().fields) {
      ctx.header = ctx.header.addField(f)
    }
    headers.put(declaredHeaderName, ctx.header)
    declaredHeaders.put(declaredHeaderName,ctx.headerDeclaration)
  }
  // Exit Section 2.1

  // Section 2.2
  val headerInstances: MutableMap[String, HeaderInstance] = MutableMap()

  override def exitScalarInstance(ctx: P4GrammarParser.ScalarInstanceContext): Unit =
    ctx.instance = ctx.scalar_instance().instance

  override def exitArrayInstance(ctx: P4GrammarParser.ArrayInstanceContext): Unit =
    ctx.instance = ctx.array_instance().instance

  override def exitScalar_instance(ctx: P4GrammarParser.Scalar_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText
    val headerType = ctx.header_type_name().getText
    ctx.instance = new ScalarHeader(instanceName, declaredHeaders(headerType))
    ctx.hdrInstance = new org.change.v2.p4.model.HeaderInstance(headers(headerType), instanceName)
    instances.put(instanceName, ctx.hdrInstance)
    headerInstances.put(instanceName, ctx.instance)
  }

  val headers = new util.HashMap[String, Header]()
  val instances = new util.HashMap[String, org.change.v2.p4.model.HeaderInstance]()

  override def exitArray_instance(ctx: P4GrammarParser.Array_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText
    val index = ctx.const_value().constValue
    val headerType = ctx.header_type_name().getText
    ctx.arrInstance = new ArrayInstance(headers(headerType), instanceName, index)
    instances.put(instanceName, ctx.arrInstance)
    for (i <- 0 until index) {
      ctx.instance = new ArrayHeader(instanceName, i, declaredHeaders(headerType))
      headerInstances.put(instanceName + i, ctx.instance)
    }
  }

  override def exitMetadata_initializer(ctx: P4GrammarParser.Metadata_initializerContext): Unit = {
    import scala.collection.JavaConverters._
    ctx.inits = (ctx.field_name().asScala zip ctx.field_value().asScala).map( nv => {
      nv._1.getText -> nv._2.fieldValue
    }).toMap
  }

  override def exitMetadataInstance(ctx: P4GrammarParser.MetadataInstanceContext) = {
    val headerType = ctx.metadata_instance().header_type_name().getText
    val instanceName = ctx.metadata_instance().instance_name().getText
    val initializer: Map[String, Int] = if (ctx.metadata_instance().metadata_initializer() != null)
      ctx.metadata_instance().metadata_initializer().inits.map(mv => mv._1.toString -> mv._2.intValue()).toMap
    else
      Map.empty
    val metadataInstance = new MetadataInstance(instanceName, declaredHeaders(headerType), initializer)
    ctx.instance = metadataInstance
    headerInstances.put(instanceName, metadataInstance)
    instances.put(instanceName,
      initializer.foldLeft(new org.change.v2.p4.model.HeaderInstance(headers(headerType), instanceName).setMetadata(true))((acc, x) => {
        acc.addInitializer(x._1, x._2.toLong)
      })
    )
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
        Some(SetFunction(ctx.set_statement().field_ref().getText, ctx.set_statement().metadata_expr().getText))
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
      (if (headerInstances.contains(ctx.header_ref().headerInstanceId))
        headerInstances(ctx.header_ref().headerInstanceId).layout.indexOf(ctx.field_name().getText)
      else
        0)
    if (complexAction != null && actionFieldRef) {
      actionFieldRef = false
    }
  }

  val actionRegistrar = new ActionRegistrar()
  private var complexAction : P4ComplexAction = null

  override def enterAction_function_declaration(ctx: Action_function_declarationContext): Unit =  {
    super.enterAction_function_declaration(ctx)
    val text = ctx.action_header().action_name().NAME().getText
    complexAction = new P4ComplexAction(text)
    actionRegistrar.register(complexAction)
  }

  override def exitAction_function_declaration(ctx: Action_function_declarationContext): Unit = {
    complexAction = null
  }


  override def enterParam_list(ctx: Param_listContext): Unit = {
    if (p4ActionCall == null && complexAction != null) {
      for (x <- ctx.param_name()) {
        val param = new P4ActionParameter(x.NAME().getText)
        complexAction.getParameterList.add(param)
      }
    } else if (p4ActionCall != null) {

    }
  }

  override def enterParam_name(ctx: Param_nameContext): Unit = {
    if (p4ActionCall != null) {
      val p4Instance = new P4ParameterInstance().setParameter(new P4ActionParameter("")).setValue(ctx.NAME().getText)
      p4ActionCall.addParameter(p4Instance)
    }
  }

  override def enterHeader_ref(ctx: Header_refContext): Unit = {
    if (p4ActionCall != null && !actionFieldRef) {
      val value = ctx.getText
      val p4Instance = new P4ParameterInstance().setParameter(new P4ActionParameter(P4ActionParameterType.HDR.x, "")).setValue(value)
      p4ActionCall.addParameter(p4Instance)
    }
  }

  private var actionFieldRef = false
  override def enterField_ref(ctx: Field_refContext): Unit = {
    if (p4ActionCall != null) {
      val value = ctx.getText
      val p4Instance = new P4ParameterInstance().setParameter(new P4ActionParameter(P4ActionParameterType.FLD.x, "")).setValue(value)
      p4ActionCall.addParameter(p4Instance)
      actionFieldRef = true
    }
  }


  private var p4ActionCall : P4ActionCall = null
  override def enterAction_statement(ctx: Action_statementContext): Unit = {
    if (complexAction != null) {
      val theName = ctx.action_name().NAME().getText
      // will defer type inference until needed
      val p4act = new P4Action(P4ActionType.UNKNOWN, theName)
      p4ActionCall = new P4ActionCall(p4act)
      complexAction.getActionList.add(p4ActionCall)
    }
  }

  override def exitAction_statement(ctx: Action_statementContext): Unit = {
    p4ActionCall = null
  }

  override def exitP4_program(ctx: P4_programContext): Unit = {
    // ok, let's do type inference now
    for (x <- actionRegistrar.getDeclaredActions) {

    }
  }

  override def enterP4_program(ctx : P4_programContext) : Unit = {
    //    ingress_port
    //    packet_length
    //    egress_spec
    //    egress_port
    //    egress_instance
    //    instance_type
    //    parser_status
    //    parser_error_location
    // Standard metadata intrinsic
    val hOffs = Map[Int, (String, Int)](
      0 -> ("ingress_port", 64),
      64 -> ("packet_length", 64),
      128 -> ("egress_spec", 64),
      192 -> ("egress_port", 64),
      256 -> ("egress_instance", 64),
      320 -> ("instance_type", 64),
      384 -> ("parser_status", 64),
      448 -> ("parser_error_location", 64)
    )
    declaredHeaders.put("standard_metadata_t", new HeaderDeclaration("standard_metadata_t", hOffs, 512))
    headers.put("standard_metadata_t", hOffs.foldLeft(new Header().setName("standard_metadata_t").setLength(512))((acc, x) => {
      acc.addField(new Field().setLength(x._2._2).setName(x._2._1))
    }))
    // one day, initialize the standard metadata as per spec
    val metadataInstance = new MetadataInstance("standard_metadata", declaredHeaders("standard_metadata_t"), Map[String, Int]())
    headerInstances.put("standard_metadata", metadataInstance)
    val metadataInstancej = new org.change.v2.p4.model.HeaderInstance(headers("standard_metadata_t"), "standard_metadata").setMetadata(true)
    instances.put("standard_metadata", metadataInstancej)
  }

  def resolveField(fieldSpec : String) : Either[Intable, String] = {
    val split = fieldSpec.split("\\.")
    val theHeader = headerInstances(split(0))
    if (theHeader.isInstanceOf[MetadataInstance])
      Right(fieldSpec)
    else
      Left(theHeader.getTagOfField(split(1)))
  }


  override def exitDirect_or_static(ctx: Direct_or_staticContext): Unit = {
    if (ctx.direct_attribute() != null) {
      ctx.isDirect = true
      ctx.directTable = ctx.direct_attribute().table
    } else if (ctx.static_attribute() != null) {
      ctx.isStatic = true
      ctx.staticTable = ctx.static_attribute().table
    }
  }

  override def exitDirect_attribute(ctx: Direct_attributeContext): Unit = {
    ctx.table = ctx.table_name().NAME().getText
  }

  override def exitStatic_attribute(ctx: Static_attributeContext): Unit = {
    ctx.table = ctx.table_name().NAME().getText
  }

  override def exitWidth_declaration(ctx: Width_declarationContext): Unit = {
    ctx.width = ctx.const_value().constValue
  }

  val registerMap = new util.HashMap[String, RegisterSpecification]()

  override def exitRegister_declaration(ctx: Register_declarationContext): Unit = {
    ctx.spec = new RegisterSpecification().setDirect(
      if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isDirect
      } else {
        false
      }).setStatic(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isStatic
      } else {
        false
      }).setDirectTable(
        if (ctx.direct_or_static() != null) {
          ctx.direct_or_static().directTable
        } else {
          null
        }
      ).setStaticTable(
        if (ctx.direct_or_static() != null) {
          ctx.direct_or_static().staticTable
        } else {
          null
        }
      ).setName(ctx.register_name().NAME().getText).setWidth(ctx.width_declaration().width).
      setCount(if (ctx.const_value() != null) ctx.const_value().constValue else 1  )
    registerMap.put(ctx.spec.getName, ctx.spec)
  }

  val fieldLists = new java.util.HashMap[String, FieldList]()

  override def exitField_list_declaration(ctx: Field_list_declarationContext): Unit = {
    ctx.entryList = new util.ArrayList[String]()
    for (x <- ctx.field_list_entry()) {
      ctx.entryList.add(x.entryName)
    }
    fieldLists.put(ctx.field_list_name().NAME().getText, new FieldList(ctx.field_list_name().NAME().getText, ctx.entryList))
  }

  override def exitField_list_entry(ctx: Field_list_entryContext): Unit = {
    ctx.entryName = ctx.getText
  }

  override def exitHeader_dec_body(ctx: Header_dec_bodyContext): Unit = {
    if (ctx.const_value() != null) {
      ctx.maxLength = ctx.const_value().constValue
    } else {
      ctx.maxLength = -1
    }
    ctx.fields = new util.ArrayList[Field]()
    for (f <- ctx.field_dec()) {
      ctx.fields.add(f.field)
    }
    if (ctx.length_exp() != null && ctx.length_exp().const_value() != null)
      ctx.length = ctx.length_exp().const_value().constValue
    else ctx.length = -1
  }

  override def exitField_dec(ctx: Field_decContext): Unit = {
    ctx.field = new Field().setLength(
      if (ctx.bit_width().const_value() != null)
        ctx.bit_width().const_value().constValue
      else
        -1).setName(ctx.field_name().NAME().getText)
    if (ctx.field_mod() != null && ctx.field_mod().getText.contains("saturating"))
      ctx.field.setSaturating()
    if (ctx.field_mod() != null && ctx.field_mod().getText.contains("signed"))
      ctx.field.setSigned()
  }

}
