package org.change.parser.p4

import java.util
import java.util.{Collections, UUID}

import generated.parse.p4.P4GrammarParser._
import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.change.parser.p4.tables.P4Utils
import org.change.v2.abstractnet.generic._
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive._
import org.change.v2.analysis.memory.{Tag, TagExp}
import org.change.v2.analysis.memory.TagExp._
import org.change.v2.analysis.processingmodels.Instruction
import org.change.v2.analysis.processingmodels.instructions._
import org.change.v2.p4.model._
import org.change.v2.p4.model.parser._
import org.change.v2.p4.model.table.{MatchKind, TableMatch}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
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
      val width: Option[Long] = Option(h.bit_width().const_value()).map(_.constValue)
      val name: String = h.field_name().getText
      (name, width)
    }

    val fieldsWithSizes = {
      val total = fields.foldLeft(0l)(_ + _._2.getOrElse(0l))
      // If not defined, then it is computed.
      if (headerSize.isEmpty) headerSize = Some(total)
      // By this point there should be a value for the size of the header.
      fields.map(field => (field._1, field._2.getOrElse(headerSize.get - total)))
    }

//    ctx.headerDeclaration = HeaderDeclaration(
//      declaredHeaderName,
//      fieldsWithSizes.scanLeft(0)(_ + _._2).zip(fieldsWithSizes).toMap,
//      headerSize.get
//    )
    ctx.header = new Header().setName(declaredHeaderName).setMaxLength(ctx.header_dec_body().maxLength)
    for (f <- ctx.header_dec_body().fields) {
      ctx.header = ctx.header.addField(f)
    }
    headers.put(declaredHeaderName, ctx.header)
    declaredHeaders.put(declaredHeaderName,ctx.headerDeclaration)
  }
  // Exit Section 2.1

  // Section 2.2

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
  }

  val headers = new util.HashMap[String, Header]()
  val instances = new util.HashMap[String, org.change.v2.p4.model.HeaderInstance]()
  val actionProfiles = new util.HashMap[String, P4ActionProfile]()

  override def exitArray_instance(ctx: P4GrammarParser.Array_instanceContext): Unit = {
    val instanceName = ctx.instance_name().getText
    val index = ctx.const_value().constValue
    val headerType = ctx.header_type_name().getText
    ctx.arrInstance = new ArrayInstance(headers(headerType), instanceName, index.intValue())
    instances.put(instanceName, ctx.arrInstance)
    for (i <- 0 until index.intValue()) {
      ctx.instance = new ArrayHeader(instanceName, i, declaredHeaders(headerType))
    }
  }

  override def exitMetadata_initializer(ctx: P4GrammarParser.Metadata_initializerContext): Unit = {
    import scala.collection.JavaConverters._
    ctx.inits = (ctx.field_name().asScala zip ctx.field_value().asScala).map( nv => {
      nv._1.getText -> new Integer(nv._2.fieldValue.intValue())
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
    instances.put(instanceName,
      initializer.foldLeft(new org.change.v2.p4.model.HeaderInstance(headers(headerType), instanceName).setMetadata(true))((acc, x) => {
        acc.addInitializer(x._1, x._2.toLong)
      })
    )
  }
  // Exit Section 2.2

  // Section 4
  override def exitHeader_extract_ref(ctx: P4GrammarParser.Header_extract_refContext): Unit = {
  }

  val declaredFunctions: MutableMap[String, ParserFunctionDeclaration] = MutableMap()

  val parserFunctions  = new util.HashMap[String, State]()


  override def exitParser_function_declaration(ctx: P4GrammarParser.Parser_function_declarationContext): Unit = {
    ctx.functionDeclaration = ParserFunctionDeclaration(
      ctx.parser_state_name().getText,
      ctx.parser_function_body().extract_or_set_statement().toList.map(_.functionStatement)
    )
    ctx.state = ctx.parser_function_body().statements.foldLeft(new State().
      setName(ctx.parser_state_name().getText))((acc, x) => {
      acc.add(x)
    })
    parserFunctions.put(ctx.state.getName, ctx.state)
  }

  override def exitExtract_or_set_statement(ctx: P4GrammarParser.Extract_or_set_statementContext): Unit = {
    ctx.functionStatement = Option(ctx.extract_statement()).map(_.extractStatement).orElse(
        Some(SetFunction(ctx.set_statement().field_ref().getText, ctx.set_statement().metadata_expr().getText))
      ).get
    ctx.statement = if (ctx.set_statement() != null)
      ctx.set_statement().statement
    else
      ctx.extract_statement().statement
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

  }

  override def exitField_ref(ctx: P4GrammarParser.Field_refContext): Unit = {
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
      448 -> ("parser_error_location", 64),
      512 -> ("egress_priority", 64)
    )
//    declaredHeaders.put("standard_metadata_t", HeaderDeclaration("standard_metadata_t", hOffs, 512))
    headers.put("standard_metadata_t", hOffs.foldLeft(new Header().setName("standard_metadata_t").setLength(512))((acc, x) => {
      acc.addField(new Field().setLength(x._2._2).setName(x._2._1))
    }))
    // one day, initialize the standard metadata as per spec
    val metadataInstancej = new org.change.v2.p4.model.HeaderInstance(headers("standard_metadata_t"), "standard_metadata").setMetadata(true)
    instances.put("standard_metadata", metadataInstancej)
  }

  def resolveField(fieldSpec : String) : Either[Intable, String] = {
//    val split = fieldSpec.split("\\.")
//    val theHeader = headerInstances(split(0))
//    if (theHeader.isInstanceOf[MetadataInstance])
      Right(fieldSpec)
    //DD: Removed header addressing through tags - might not need those anymore
//    else
//      Left(theHeader.getTagOfField(split(1)))
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
    ctx.width = ctx.const_value().constValue.intValue()
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
      setCount(if (ctx.const_value() != null) ctx.const_value().constValue.intValue() else 1  )
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
      ctx.maxLength = ctx.const_value().constValue.intValue()
    } else {
      ctx.maxLength = -1
    }
    ctx.fields = new util.ArrayList[Field]()
    for (f <- ctx.field_dec()) {
      ctx.fields.add(f.field)
    }
    if (ctx.length_exp() != null && ctx.length_exp().const_value() != null)
      ctx.length = ctx.length_exp().const_value().constValue.intValue()
    else ctx.length = -1
  }

  override def exitField_dec(ctx: Field_decContext): Unit = {
    ctx.field = new Field().setLength(
      if (ctx.bit_width().const_value() != null)
        ctx.bit_width().const_value().constValue.intValue()
      else
        -1).setName(ctx.field_name().NAME().getText)
    if (ctx.field_mod() != null && ctx.field_mod().getText.contains("saturating"))
      ctx.field.setSaturating()
    if (ctx.field_mod() != null && ctx.field_mod().getText.contains("signed"))
      ctx.field.setSigned()
  }



  override def exitField_match_type(ctx : Field_match_typeContext) : Unit = {
    ctx.matchKind = if (ctx.getText == "range")
      MatchKind.Range
    else if (ctx.getText == "lpm")
      MatchKind.Lpm
    else if (ctx.getText == "ternary")
      MatchKind.Ternary
    else if (ctx.getText == "exact")
      MatchKind.Exact
    else if (ctx.getText == "valid")
      MatchKind.Valid
    else
      throw new UnsupportedOperationException(s"Unknown match type ${ctx.getText}")
  }

  var tables = new util.HashSet[String]()
  var tableDeclarations = new util.HashMap[String, util.List[TableMatch]]()
  var tableAllowedActions = new util.HashMap[String, util.List[String]]()
  override def exitTable_declaration(ctx: Table_declarationContext): Unit = {
    val tableName = ctx.table_name().NAME().getText
    if (ctx.field_match() != null) {
      for (fm <- ctx.field_match()) {
        tableDeclarations.get(tableName).add(fm.tableMatch)
      }
    }
    if (ctx.table_actions().action_specification() != null &&
      ctx.table_actions().action_specification().action_name() != null) {
      for (an <- ctx.table_actions().action_specification().action_name()) {
        tableAllowedActions.get(ctx.table_name().getText).add(an.getText)
      }
    } else if (ctx.table_actions().action_profile_specification() != null &&
      ctx.table_actions().action_profile_specification().action_profile_name().NAME().getText != null) {
      tableAllowedActions.get(tableName).add(
        ctx.table_actions().action_profile_specification().action_profile_name().NAME().getText
      )
    }

  }
  override def enterTable_declaration(ctx: Table_declarationContext): Unit = {
    val tableName = ctx.table_name().NAME().getText
    tables.add(tableName)
    //TODO: Do something with these allowed actions - i.e. populate them
    tableAllowedActions.put(tableName, new util.ArrayList[String]())
    tableDeclarations.put(tableName, new util.ArrayList[TableMatch]())
    if (ctx.field_match() != null) {
      for (fm <- ctx.field_match()) {
        fm.tableName = tableName
      }
    }
  }


  override def exitField_or_masked_ref(ctx: Field_or_masked_refContext): Unit = {
    if (ctx.const_value() != null)
      ctx.mask =ctx.const_value().constValue
    else
      ctx.mask = -1l
    if (ctx.field_ref() != null)
      ctx.field = ctx.field_ref().getText
    else
      ctx.field = ctx.header_ref().getText
  }

  override def exitField_match(ctx : Field_matchContext) : Unit = {
    ctx.tableMatch = new TableMatch(ctx.tableName,
      ctx.field_or_masked_ref().field,
      ctx.field_match_type().matchKind,
      ctx.field_or_masked_ref().mask)
  }



  private val currentPort = -1
  private var currentTableName:ListBuffer[String] = new ListBuffer[String]
  private var currentInstructions:ListBuffer[ListBuffer[Instruction]] = new ListBuffer[ListBuffer[Instruction]]
  private var currentTableInvocation = 0

  private val elements = new ArrayBuffer[GenericElement]()
  private var pathBuilder: ArrayBuffer[PathComponent] = _
  private val foundPaths = ArrayBuffer[List[PathComponent]]()
  private val ports:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]()
  private val portsLiveBlocks:scala.collection.mutable.Map[String,ListBuffer[Instruction]] = new scala.collection.mutable.LinkedHashMap[String, ListBuffer[Instruction]]()

  private val values:ParseTreeProperty[Integer] = new ParseTreeProperty[Integer]()
  private val constraints:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]()
  private val symbols:ParseTreeProperty[FloatingConstraint] = new ParseTreeProperty[FloatingConstraint]()
  private val expressions:ParseTreeProperty[FloatingExpression] = new ParseTreeProperty[FloatingExpression]()

  private val blocks:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]()
  private val blocksLast:ParseTreeProperty[ListBuffer[Instruction]] = new ParseTreeProperty[ListBuffer[Instruction]]()

  override def enterControl_function_declaration(ctx:Control_function_declarationContext){
    ctx.controlFunctionName = ctx.control_fn_name().NAME().getText
    ctx.control_block().parent = s"control.${ctx.controlFunctionName}"
  }

  override def exitControl_function_declaration(ctx:Control_function_declarationContext){
    ports += (ctx.control_fn_name.getText -> blocks.get(ctx.control_block))
    if (ctx.control_block() == null || ctx.control_block().instructions == null || ctx.control_block().instructions.isEmpty)
      this.instructions.put(s"control.${ctx.controlFunctionName}", Forward(s"control.${ctx.controlFunctionName}.out"))
    else
      this.instructions.put(s"control.${ctx.controlFunctionName}", Forward(s"control.${ctx.controlFunctionName}" + "[0]"))
  }

  override def enterControl_block(ctx:Control_blockContext) {
    currentInstructions.prepend(new ListBuffer[Instruction])
    //associate current instructions to the context
    blocks.put(ctx,currentInstructions.head)
    var  i =0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (cs <- ctx.control_statement()) {
      cs.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  val instructions: MutableMap[String, Instruction] = mutable.Map[String, Instruction]()
  val links: MutableMap[String, String] = mutable.Map[String, String]()
  val tableSelectors: MutableMap[(String, String), java.lang.Boolean] = mutable.Map[(String, String), java.lang.Boolean]()

  override def exitControl_block(ctx:Control_blockContext){
    var  i = 0
    if (ctx.control_statement() != null && ctx.control_statement().size() > 0)
      for (cs <- ctx.control_statement()) {
        ctx.instructions.add(cs.instruction)
        // the parent should add the mappings to get them straight
        assert(cs.instruction != null, s"In control block ${cs.getText} got null instruction")
        this.instructions.put(s"${ctx.parent}[$i]", cs.instruction)
        if (i + 1 < ctx.control_statement().size()) {
          this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}[${i + 1}]")
        } else {
          this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}.out")
        }
        i = i + 1
      }
    else
      this.links.put(s"${ctx.parent}[0]", s"${ctx.parent}.out")
  }

  override def enterControl_statement(ctx: Control_statementContext): Unit = {
    if (ctx.apply_table_call() != null) {
      ctx.apply_table_call().parent = ctx.parent
    } else if (ctx.apply_and_select_block() != null) {
      ctx.apply_and_select_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent
    }
  }

  override def exitControl_statement(ctx:Control_statementContext){
    if (ctx.control_fn_name!=null){
      val cfName = ctx.control_fn_name.getText
      val execId = UUID.randomUUID().toString
      ctx.instruction = Forward(s"control.$cfName")
      this.links.put(s"control.$cfName.out", ctx.parent + ".out")
      currentInstructions.head.append(ctx.instruction)
    } else if (ctx.apply_table_call() != null) {
      ctx.instruction = ctx.apply_table_call().instruction
    } else if (ctx.apply_and_select_block() != null) {
      ctx.instruction = ctx.apply_and_select_block().instruction
    } else if (ctx.if_else_statement() != null) {
      ctx.instruction = ctx.if_else_statement().instruction
    } else {
      throw new IllegalStateException("Got null for all statements")
    }
  }


  override def exitApply_table_call(ctx:Apply_table_callContext){
    val execId = UUID.randomUUID().toString
    ctx.instruction = Forward(s"table.${ctx.table_name().getText}.in.$execId")
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", ctx.parent + ".out")
    this.tableSelectors((ctx.table_name().getText, execId)) = false
  }

  /*
   apply_and_select_block : 'apply' '(' table_name ')' '{' ( case_list )? '}' ;
   case_list : action_case+ # case_list_action
            | hit_miss_case+  # case_list_hitmiss;
   action_case : action_or_default control_block ;
   action_or_default : action_name | 'default' ;
   hit_miss_case : hit_or_miss control_block ;
   hit_or_miss : 'hit' | 'miss' ;
  */
  override def enterApply_and_select_block(ctx:Apply_and_select_blockContext){
    ctx.case_list().parent = ctx.parent
    //here we should call Radu's action parsing code instead
    //currentInstructions.head.append(Forward(ctx.table_name.getText()+"_output"));
  }


  override def exitApply_and_select_block(ctx:Apply_and_select_blockContext){
    val execId = UUID.randomUUID().toString
    this.instructions.put(ctx.parent, Forward(s"table.${ctx.table_name().getText}.in.$execId"))
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", s"${ctx.parent}.select")
    this.tableSelectors((ctx.table_name().getText, execId)) = true
    val defaultEntry = ctx.case_list().instructions.collect({
      case v @ If(ConstrainNamedSymbol(what, _, _), b, _) if what == "default.Fired" => b
    }).headOption.getOrElse(Forward(s"${ctx.parent}.out"))

    this.instructions.put(s"${ctx.parent}.select",
      ctx.case_list().instructions.map(_.asInstanceOf[If]).filter(r => r match {
        case If(ConstrainNamedSymbol(what, _, _), _, _) => what != "default.Fired"
        case _ => true
      }).foldRight(defaultEntry)((x, acc) => {
        If (x.testInstr, x.thenWhat, acc)
      })
    )
    var j = 0
    for (i <- ctx.case_list().instructions) {
      this.links.put(s"${ctx.parent}[$j].out", s"${ctx.parent}.out")
      j = j + 1
    }

    ctx.instruction = Forward(s"table.${ctx.table_name().getText}.in.$execId")
  }

  override def exitAction_profile_declaration(ctx: Action_profile_declarationContext): Unit = {
    ctx.actionProfile = new P4ActionProfile(ctx.action_profile_name().NAME().getText)
    ctx.actionProfile.getActions.addAll(ctx.action_specification().actions)
    if (ctx.selector_name() != null)
      ctx.actionProfile.setDynamicActionSelector(ctx.selector_name().NAME().getText)
    if (ctx.const_value() != null)
      ctx.actionProfile.setSize(ctx.const_value().constValue.intValue())
    this.actionProfiles.put(ctx.actionProfile.getName, ctx.actionProfile)
  }

  override def exitAction_specification(ctx: Action_specificationContext): Unit = {
    ctx.actions = new util.ArrayList[String]()
    if (ctx.action_name() != null)
      for (ac <- ctx.action_name())
        ctx.actions.add(ac.NAME().getText)
  }

  override def enterCase_list_action(ctx: Case_list_actionContext): Unit = {
    var i = 0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (ac <- ctx.action_case()) {
      ac.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  override def exitCase_list_action(ctx: Case_list_actionContext): Unit = {
    for (ac <- ctx.action_case()) {
      ctx.instructions.add(ac.instruction)
    }
  }

  override def enterCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    ctx.instructions = new util.ArrayList[Instruction]()
    for (ac <- ctx.hit_miss_case()) {
      ac.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  override def exitCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    for (ac <- ctx.hit_miss_case()) {
      ctx.instructions.add(ac.instruction)
      i = i + 1
    }
  }

  override def enterHit_miss_case(ctx: Hit_miss_caseContext): Unit = {
    ctx.control_block().parent = ctx.parent
  }

  override def exitHit_miss_case(ctx:Hit_miss_caseContext){
    val portName = currentTableName.head + "_" + ctx.hit_or_miss.getText

    ctx.instruction = If (Constrain(currentTableName.head + ".Hit", :==:(ConstantValue(if (ctx.hit_or_miss().getText == "hit") 1 else 0))),
      Forward(ctx.parent + "[0]")
    )
    ports(currentTableName.head).append(Forward(portName))

    ctx.hit_or_miss.getText match {
      case "hit" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case "miss" =>
        ports += (portName -> blocks.get(ctx.control_block))
    }

    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
  }

  override def enterAction_case(ctx:Action_caseContext){
    //must make room for control block instructions!
    //this will create a new control block with a specific port.
    ctx.control_block().parent = ctx.parent
  }

  override def exitAction_case(ctx:Action_caseContext){
    ctx.instruction = If (Constrain(ctx.action_or_default().getText + ".Fired", :==:(ConstantValue(1))),
      Forward(ctx.parent + "[0]")
    )
  }



  /*if_else_statement : 'if' '(' bool_expr ')' control_block ( else_block )? ;

  else_block : 'else' control_block
             | 'else' if_else_statement ;
   */

  override def enterIf_else_statement(ctx: If_else_statementContext): Unit = {
    ctx.control_block().parent = ctx.parent + "[if]"
    if (ctx.else_block() != null) {
      ctx.else_block().parent = ctx.parent + "[else]"
    }
  }

  override def exitIf_else_statement(ctx:If_else_statementContext){
    //bool_expr should be a constrain instruction we can use in the IF.
    val labelName = "if_"+currentTableInvocation
    currentTableInvocation += 1
    assert(ctx.bool_expr.instruction != null, s"in if else ${ctx.bool_expr().getText}")
//    ctx.instruction = If (
//      InstructionBlock(
//        blocks.get(ctx.bool_expr)
//      ),
//      Forward(ctx.parent + "[if][0]"),
//      if (ctx.else_block() != null)
//        Forward(s"${ctx.parent}[else][0]")
//      else
//        Forward(s"${ctx.parent}.out")
//    )
    ctx.instruction = InstructionBlock(
      ctx.bool_expr().alsoAdd,
      If (
      ctx.bool_expr().instruction,
      Forward(ctx.parent + "[if][0]"),
      if (ctx.else_block() != null)
        Forward(s"${ctx.parent}[else][0]")
      else
        Forward(s"${ctx.parent}.out")
    ))
    this.links.put(ctx.parent + "[if].out", s"${ctx.parent}.out")
    if (ctx.else_block() != null)
      this.links.put(ctx.parent + "[else].out", s"${ctx.parent}.out")
  }

  override def enterElse_block(ctx: Else_blockContext): Unit = {
    if (ctx.control_block() != null) {
      ctx.control_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent

    }
  }

  override def exitElse_block(ctx:Else_blockContext){
    if (ctx.control_block() != null) {
      //Nothing to do here, the control_block() will place
      // instructions in the table correctlys
      //      ctx.control_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent
      //TODO: Here, please insert into the instruction table parent[0]
      //referencing the if_else_statement().instruction() and some links perhaps
      this.instructions.put(ctx.parent + "[0]", ctx.if_else_statement().instruction)
    }
    blocks.put(ctx,blocks.get(ctx.control_block))
  }


  override def exitField_name(ctx:Field_nameContext) {
//    println("Matched field name "+ctx.getText)
  }

  //exp : exp bin_op exp # compound_exp
  //      | un_op exp # unary_exp
  //      | field_ref # field_red_exp
  //      | value # value_exp
  //      | '(' exp ')' # par_exp ;

  override def exitCompound_exp(ctx:Compound_expContext): Unit ={
    ctx.expr = ctx.bin_op().getText match  {
      case "+" => :+:(ctx.exp(0).expr, ctx.exp(1).expr)
      case "-" => :-:(ctx.exp(0).expr, ctx.exp(1).expr)
      case "&" => :&&:(ctx.exp(0).expr, ctx.exp(1).expr)
    }
  }

  override def exitUnary_exp(ctx:Unary_expContext): Unit ={
    ctx.expr = ctx.un_op().getText match {
      case "-" => :-:(ConstantValue(0), ctx.exp().expr)
      case "~" => :!:(ctx.exp().expr)
    }
  }

  override def exitField_red_exp(ctx:Field_red_expContext){
    //need to convert the field reference to a number; the current code will issue metadata accesses
    expressions.put(ctx,:@(ctx.getText))
    ctx.expr = :@(ctx.getText)
  }

  override def exitValue_exp(ctx:Value_expContext){
    ctx.expr = ConstantValue(P4Utils.toNumber(ctx.getText))
    expressions.put(ctx,ConstantValue(P4Utils.toNumber(ctx.getText)))
  }

  override def exitPar_exp(ctx:Par_expContext){
    println("Matched par expression "+ctx.getText)
    ctx.expr = ctx.exp().expr
    expressions.put(ctx,expressions.get(ctx.exp))
  }

  //bool_expr : 'valid' '(' header_ref ')' # valid_bool_expr
  //	  | bool_expr bool_op bool_expr # compound_bool_expr
  //	  | 'not' bool_expr # negated_bool_expr
  //	  | '(' bool_expr ')' # par_bool_expr
  //	  | exp rel_op exp # relop_bool_expr
  //	  | 'true' # const_bool
  //  | 'false' # const_bool;

  override def exitValid_bool_expr(ctx:Valid_bool_exprContext){
    //check with radu - need valid instruction
    blocks.put(ctx,new ListBuffer[Instruction])

    //the header parsing code should create the metadata named as the header (including [index]!?))
    blocks.get(ctx).append(Constrain(ctx.header_ref.getText + ".IsValid",:==:(ConstantValue(1))))
    ctx.alsoAdd = NoOp
    ctx.instruction = Constrain(ctx.header_ref.getText + ".IsValid",:==:(ConstantValue(1)))
  }

  override def exitCompound_bool_expr(ctx:Compound_bool_exprContext){
    // bool_op can be "and" or "or"
    blocks.put(ctx,new ListBuffer[Instruction])
    ctx.bool_op.getText match {
      case "and" =>
        ctx.instruction = InstructionBlock(ctx.bool_expr(0).instruction, ctx.bool_expr(1).instruction)
        ctx.alsoAdd = InstructionBlock(ctx.bool_expr(0).alsoAdd, ctx.bool_expr(1).alsoAdd)
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(0)))
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(1)))
      case "or" =>
        ctx.instruction = Fork(ctx.bool_expr(0).instruction, ctx.bool_expr(1).instruction)
        ctx.alsoAdd = InstructionBlock(ctx.bool_expr(0).alsoAdd, ctx.bool_expr(1).alsoAdd)
        blocks.get(ctx).append(
          Fork(
            List[Instruction](InstructionBlock(blocks.get(ctx.bool_expr(0))),InstructionBlock(blocks.get(ctx.bool_expr(1)))
            )
          )
        )
    }
  }

  override def exitPar_bool_expr(ctx:Par_bool_exprContext){
    constraints.put(ctx, constraints.get(ctx.bool_expr()))
    blocks.put(ctx, blocks.get(ctx.bool_expr))
    ctx.alsoAdd = ctx.bool_expr().alsoAdd
    ctx.instruction = ctx.bool_expr().instruction
  }

  override def exitRelop_bool_expr(ctx:Relop_bool_exprContext){
    val tmp = UUID.randomUUID().toString
    ctx.alsoAdd = NoOp
    val exp1 = ctx.exp.head.expr
    val exp2 = ctx.exp(1).expr

    blocks.put(ctx,new ListBuffer[Instruction]())

    exp1 match {
      case Symbol(x) =>
        ctx.rel_op.getText match {
          case "==" =>
            ctx.instruction = Constrain(x,:==:(exp2))
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))
          case "!=" =>
            ctx.instruction = Constrain(x,:~:(:==:(exp2)))
            constraints.put(ctx, :~:(:==:(exp2)))
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))
          case "<" =>
            constraints.put(ctx, :<:(exp2))
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))
            ctx.instruction = Constrain(x,:<:(exp2))
          case "<=" =>
            constraints.put(ctx, :<:(exp2))
            blocks.get(ctx).append(Constrain(x,:<=:(exp2)))
            ctx.instruction = Constrain(x,:<=:(exp2))
          case ">" =>
            constraints.put(ctx, :>:(exp2))
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))
            ctx.instruction = Constrain(x,:>:(exp2))
          case ">=" =>
            constraints.put(ctx, :>:(exp2))
            blocks.get(ctx).append(Constrain(x,:>=:(exp2)))
            ctx.instruction = Constrain(x,:>=:(exp2))
          case _ => println("Unknown relop "+ctx.rel_op.getText);
        }
      case _ =>
        assert(exp1 != null, s"Cannot translate ${ctx.exp(0).getText}")
        ctx.instruction = ConstrainFloatingExpression(exp1, ctx.rel_op.getText match {
          case "==" => :==:(exp2)
          case "!=" => :~:(:==:(exp2))
          case "<" => :<:(exp2)
          case "<=" => :<=:(exp2)
          case ">" => :>:(exp2)
          case ">=" => :>=:(exp2)
          case _ => throw new Exception("Unknown relop "+ctx.rel_op.getText);
        })
    }
    //exp1 must be either metadata or field ref
    //exp2 can be either constant, etc.
  }
  private def not(instruction : Instruction): Instruction = instruction match {
    case cns : ConstrainNamedSymbol => cns.not()
    case cr : ConstrainRaw => cr.not()
    case InstructionBlock(instrs) => Fork(instrs.map(not))
    case Fork(instrs) => InstructionBlock(instrs.map(not))
    case _ => ???
  }

  override def exitNegated_bool_expr(ctx:Negated_bool_exprContext){
    val exp = constraints.get(ctx.bool_expr)
    println(s"Exit negated bool expr of $exp")
    constraints.put(ctx, :~:(exp))
    ctx.alsoAdd = ctx.bool_expr().alsoAdd
    ctx.instruction = not(ctx.bool_expr().instruction)
  }

  override def exitConst_bool(ctx:Const_boolContext){
    println("Matched const bool expr.")
    ctx.alsoAdd = Assign("__CONSTANT_1__", ConstantValue(1))
    ctx.instruction = Constrain("__CONSTANT_1__", :==:(ConstantValue(if (ctx.getText.equalsIgnoreCase("true")) 1 else 0)))
    values.put(ctx, if (ctx.getText.equalsIgnoreCase("true")) 1 else 0)
  }



  override def exitParser_function_body(ctx: Parser_function_bodyContext) : Unit = {
    ctx.statements = ctx.extract_or_set_statement().map(x => {
      x.statement
    }) :+ ctx.return_statement().statement
  }

  override def exitExtract_statement(ctx: Extract_statementContext): Unit = {
    val extractWhere = ctx.header_extract_ref().getText
    ctx.statement = new ExtractStatement(ParserInterpreter.parseExpression(extractWhere))
  }

  override def exitSet_statement(ctx: Set_statementContext): Unit = {
    val dst = ctx.field_ref().getText
    val src = ctx.metadata_expr().expression
    ctx.statement = new SetStatement(ParserInterpreter.parseExpression(dst),
      src)
  }

  override def exitMetadata_expr(ctx: Metadata_exprContext): Unit = {
    ctx.expression = ctx.compound().expression
    super.exitMetadata_expr(ctx)
  }

  override def exitCompound(ctx: CompoundContext): Unit = {
    ctx.expression = if (ctx.minus_metadata_expr() != null)
      ctx.minus_metadata_expr().expression
    else if (ctx.plus_metadata_expr() != null)
      ctx.plus_metadata_expr().expression
    else if (ctx.simple_metadata_expr() != null)
      ctx.simple_metadata_expr().expression
    else
      throw new IllegalStateException("Got compound metadata expression " + ctx.getText)
  }

  override def exitPlus_metadata_expr(ctx: Plus_metadata_exprContext): Unit = {
    ctx.expression = new CompoundExpression(true, ctx.simple_metadata_expr().expression, ctx.compound().expression)
  }

  override def exitMinus_metadata_expr(ctx: Minus_metadata_exprContext): Unit = {
    ctx.expression = new CompoundExpression(false, ctx.simple_metadata_expr().expression, ctx.compound().expression)
  }
  override def exitSimple_metadata_expr(ctx: Simple_metadata_exprContext): Unit = {
    ctx.expression = ParserInterpreter.parseExpression(ctx.getText)
  }


  override def exitReturn_value_type(ctx: Return_value_typeContext): Unit = {
    if (ctx.parser_exception_name() != null) {
      ctx.statement = new ReturnStatement("").setError(true).setMessage(ctx.parser_exception_name().getText)
    } else if (ctx.control_function_name() != null) {
      ctx.statement = new ReturnStatement(ctx.control_function_name().getText)
    } else if (ctx.parser_state_name() != null) {
      ctx.statement = new ReturnStatement(ctx.parser_state_name().getText)
    }
  }

  override def exitReturn_statement(ctx: Return_statementContext): Unit = {
    if (ctx.return_value_type() != null)
      ctx.statement = ctx.return_value_type().statement
    else {
      ctx.statement = ctx.case_entry().foldLeft(new ReturnSelectStatement())((acc, x) => {
        acc.add(ctx.select_exp().expressions.foldLeft(x.caseEntry)((acc2, y) => {
          acc2.addExpression(y)
        }))
      })
    }
  }

  override def exitValue_or_masked(ctx : Value_or_maskedContext) : Unit = {
    ctx.v = new Value(ctx.field_value(0).const_value().constValue.toLong,
      if (ctx.field_value().size() > 1)
        ctx.field_value(1).const_value().constValue.toLong
      else
        -1l
      )
  }

  override def exitValue_list(ctx: Value_listContext): Unit = {
    if (ctx.value_or_masked() != null && !ctx.value_or_masked().isEmpty)
      ctx.values = ctx.value_or_masked().map(x => x.v)
    else
      ctx.values = null
  }

  override def exitCase_entry(ctx: Case_entryContext): Unit = {
    val retVal = ctx.case_return_value_type().getText
    val retst = if (ctx.case_return_value_type().parser_exception_name() != null) {
      new ReturnStatement("").setError(true).setMessage(ctx.case_return_value_type().parser_exception_name().getText)
    } else  {
      new ReturnStatement(ctx.case_return_value_type().getText)
    }
    ctx.caseEntry = (if (ctx.value_list().values == null) {
      // default
      new CaseEntry().setDefault()
    } else {
      // useful
      ctx.value_list().values.foldLeft(new CaseEntry())((acc, x) => {
        acc.addValue(x)
      })
    }).setReturnStatement(retst)
  }

  override def exitSelect_exp(ctx: Select_expContext): Unit = {
    ctx.expressions = ctx.field_or_data_ref().map(x => ParserInterpreter.parseExpression(x.getText))
  }

  def buildNetworkConfig() = new NetworkConfig(None, elements.map(element => (element.name, element)).toMap, foundPaths.toList)
}
