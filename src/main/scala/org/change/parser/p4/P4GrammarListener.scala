package org.change.parser.p4

import java.util
import java.util.{Collections, UUID}

import generated.parse.p4.P4GrammarParser._
import generated.parse.p4.{P4GrammarBaseListener, P4GrammarParser}
import org.antlr.v4.runtime.tree.ParseTreeProperty
import org.change.v2.abstractnet.generic._
import org.change.v2.analysis.expression.abst.FloatingExpression
import org.change.v2.analysis.expression.concrete.ConstantValue
import org.change.v2.analysis.expression.concrete.nonprimitive.{:@, Address, Symbol}
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
      448 -> ("parser_error_location", 64),
      512 -> ("egress_priority", 64)
    )
    declaredHeaders.put("standard_metadata_t", HeaderDeclaration("standard_metadata_t", hOffs, 512))
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


  override def exitField_match(ctx : Field_matchContext) : Unit = {
    ctx.tableMatch = new TableMatch(ctx.tableName,
      ctx.field_or_masked_ref().getText,
      ctx.field_match_type().matchKind)

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
    println("Enter control function "+ ctx.control_fn_name.getText)
    ctx.controlFunctionName = ctx.control_fn_name().NAME().getText
    ctx.control_block().parent = s"control.${ctx.controlFunctionName}"
  }

  override def exitControl_function_declaration(ctx:Control_function_declarationContext){
    ports += (ctx.control_fn_name.getText -> blocks.get(ctx.control_block))

    this.instructions.put(s"control.${ctx.controlFunctionName}", Forward(s"control.${ctx.controlFunctionName}" + "[0]"))

    println("\n\n------------------------------\nGenerated SEFL CODE for function "+ ctx.control_fn_name.getText +"\n------------------------------\n")
    for ((x,y) <- ports){
      println(x+":")
      for ( z <- y)
        println("\t"+z)
    }

    if(currentInstructions.length!=0){
      System.out.println("Not expecting instructions at exit of ctrl function!\n"+currentInstructions)
    }
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

  val instructions = mutable.Map[String, Instruction]()
  val links = mutable.Map[String, String]()
  override def exitControl_block(ctx:Control_blockContext){
    blocksLast.put(ctx,currentInstructions.head)
    currentInstructions.remove(0)
    var  i = 0
    for (cs <- ctx.control_statement()) {
      ctx.instructions.add(cs.instruction)
      // the parent should add the mappings to get them straight
      this.instructions.put(s"${ctx.parent}[$i]", cs.instruction)
      if (i + 1 < ctx.control_statement().size()) {
        this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}[${i + 1}]")
      } else {
        this.links.put(s"${ctx.parent}[$i].out", s"${ctx.parent}.out")
      }
      i = i + 1
    }
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
      ctx.instruction = Forward(s"$cfName.$execId")
      this.links.put(s"control.$cfName.out.$execId", ctx.parent + ".out")
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
    val portName = s"table.${ctx.table_name().getText}.in.$execId"
    println("Apply matched " + ctx.table_name.getText)

    ctx.instruction = Forward(s"table.${ctx.table_name().getText}.in.$execId")
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", ctx.parent + ".out")

    currentInstructions.head.append(Forward(portName))

    //here we should call Radu's action parsing code instead
    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(portName+"_output"))

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName+"_output" -> currentInstructions.head)

    currentTableInvocation+=1
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
    val portName = s"table.${ctx.table_name().getText}.in.${UUID.randomUUID().toString}"
    ctx.case_list().parent = ctx.parent

    currentInstructions.head.append(Forward(portName))
    currentTableName.prepend(portName)

    currentInstructions.remove(0)

    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (portName -> currentInstructions.head)
    currentInstructions.head.append(Forward(currentTableName.head+"_output"))

    currentTableInvocation+=1
    //here we should call Radu's action parsing code instead
    //currentInstructions.head.append(Forward(ctx.table_name.getText()+"_output"));
  }


  override def exitApply_and_select_block(ctx:Apply_and_select_blockContext){
    println("Apply and select bmatched " + ctx.table_name.getText)

    //adding fork if there are multiple forward instructions
    // TODO: Wire it up
    val execId = UUID.randomUUID().toString
    this.instructions.put(ctx.parent, Forward(s"table.${ctx.table_name().getText}.in.$execId"))
    this.links.put(s"table.${ctx.table_name().getText}.out.$execId", s"${ctx.parent}.select")
    this.instructions.put(s"${ctx.parent}.select",
      ctx.case_list().instructions.map(_.asInstanceOf[If]).foldRight(NoOp : Instruction)((x, acc) => {
        If (x.testInstr, x.thenWhat, acc)
      })
    )
    this.links.put(s"${ctx.parent}[${ctx.case_list().instructions.size() - 1}].out", s"${ctx.parent}.out")
    val z = ports(currentTableName.head).reverse.takeWhile(
      _ match {
        case x:Forward => true
        case _ => false
      })

    if (z.length>1){
      ports(currentTableName.head).trimEnd(z.length)
      ports(currentTableName.head).append(Fork(z))
    }

    currentInstructions.remove(0)
    currentInstructions.prepend(new ListBuffer[Instruction])
    ports += (currentTableName.head+"_output" -> currentInstructions.head)
    currentTableName.remove(0)
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
    var i = 0
    for (ac <- ctx.action_case()) {
      ctx.instructions.add(ac.instruction)
      i = i + 1
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
    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName))

    ctx.hit_or_miss.getText match {
      case "hit" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case "miss" =>
        ports += (portName -> blocks.get(ctx.control_block))
    }

    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
  }

  override def enterAction_case(ctx:Action_caseContext){
    //must make room for control block instructions!
    //this will create a new control block with a specific port.
    ctx.control_block().parent = ctx.parent
  }

  override def exitAction_case(ctx:Action_caseContext){
    val portName = currentTableName.head + "_" + ctx.action_or_default().getText
    ctx.instruction = If (Constrain(ctx.action_or_default().getText + ".Fired", :==:(ConstantValue(1))),
      Forward(ctx.parent + "[0]")
    )
    //currentInstructions.head.append(Forward(portName));
    ports(currentTableName.head).append(Forward(portName))
    //currentInstructions.prepend(new ListBuffer[Instruction])

    ctx.action_or_default.getText match {
      case "default" =>
        ports += (portName -> blocks.get(ctx.control_block))
      case _ =>
        //lookup action name in the table, if found add the following
        ports += (portName  -> blocks.get(ctx.control_block))
    }

    //currentInstructions.head.append(Forward(currentTableName.head+"_output"))
    blocksLast.get(ctx.control_block).append(Forward(currentTableName.head+"_output"))
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

    ctx.instruction = If (
      InstructionBlock(
        blocks.get(ctx.bool_expr)
      ),
      Forward(ctx.parent + "[if][0]"),
      if (ctx.else_block() != null)
        Forward(s"${ctx.parent}[else][0]")
      else
        Forward(s"${ctx.parent}.out")
    )

    this.links.put(ctx.parent + "[if].out", s"${ctx.parent}.out")
    if (ctx.else_block() != null)
      this.links.put(ctx.parent + "[else].out", s"${ctx.parent}.out")

//    val constr = blocks.get(ctx.bool_expr).head
//    blocks.get(ctx.bool_expr).remove(0)
//
//    val negated = for (x <- blocks.get(ctx.bool_expr)) yield {
//      x match {
//        case ConstrainNamedSymbol(a,c,d) => ConstrainNamedSymbol(a, :~:(c),d)
//        case ConstrainRaw(a,c,d) => ConstrainRaw(a, :~:(c),d)
//      }
//    }
//
//    currentInstructions.head.append(
//      If (constr,
//        InstructionBlock(blocks.get(ctx.bool_expr) ++ blocks.get(ctx.control_block)),
//        if (ctx.else_block==null) NoOp
//        else InstructionBlock(negated ++ blocks.get(ctx.else_block))
//      ))
//
//    blocksLast.get(ctx.control_block).append(Forward(labelName))
//    if (ctx.else_block!=null)
//      blocksLast.get(ctx.else_block.control_block).append(Forward(labelName))
//
//    currentInstructions.remove(0)
//    currentInstructions.prepend(new ListBuffer[Instruction])
//    ports += (labelName -> currentInstructions.head)
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

  override def exitCompound_exp(ctx:Compound_expContext){
    println("FIXME Compound exp "+ctx.getText)
  }

  override def exitUnary_exp(ctx:Unary_expContext){
    println("FIXME Unary exp "+ctx.getText)
  }

  override def exitField_red_exp(ctx:Field_red_expContext){
    //need to convert the field reference to a number; the current code will issue metadata accesses
    expressions.put(ctx,:@(ctx.getText))
  }

  override def exitValue_exp(ctx:Value_expContext){
    expressions.put(ctx,ConstantValue(ctx.getText.toInt))
  }

  override def exitPar_exp(ctx:Par_expContext){
    println("Matched par expression "+ctx.getText)
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
  }

  override def exitCompound_bool_expr(ctx:Compound_bool_exprContext){
    println("Matched compound bool expr. FIXME ")

    // bool_op can be "and" or "or"
    blocks.put(ctx,new ListBuffer[Instruction])


    ctx.bool_op.getText match {
      case "and" =>
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(0)))
        blocks.get(ctx).appendAll(blocks.get(ctx.bool_expr(1)))
      case "or" =>
        blocks.get(ctx).append(
          Fork(
            List[Instruction](InstructionBlock(blocks.get(ctx.bool_expr(0))),InstructionBlock(blocks.get(ctx.bool_expr(1)))
            )
          )
        )
    }
  }

  override def exitPar_bool_expr(ctx:Par_bool_exprContext){
    blocks.put(ctx,blocks.get(ctx.bool_expr))
  }

  override def exitRelop_bool_expr(ctx:Relop_bool_exprContext){
    println("Matched relop bool expr " + expressions.get(ctx.exp(0))+ s"${ctx.rel_op.getText}" + expressions.get(ctx.exp(1)))

    val exp1 = expressions.get(ctx.exp(0))
    val exp2 = expressions.get(ctx.exp(1))

    blocks.put(ctx,new ListBuffer[Instruction])

    exp1 match {
      case Symbol(x) =>
        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" =>
            constraints.put(ctx, :~:(:==:(exp2)))
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" =>
            constraints.put(ctx, :<:(exp2))
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))
          case "<=" =>
            constraints.put(ctx, :<:(exp2))
            blocks.get(ctx).append(Constrain(x,:<=:(exp2)))

          case ">" =>
            constraints.put(ctx, :>:(exp2))
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))
          case ">=" =>
            constraints.put(ctx, :>:(exp2))
            blocks.get(ctx).append(Constrain(x,:>=:(exp2)))
          case _ => println("Unknown relop "+ctx.rel_op.getText);
        }

      case Address(x) =>
        ctx.rel_op.getText match {
          case "==" =>
            constraints.put(ctx, :==:(exp2))
            blocks.get(ctx).append(Constrain(x,:==:(exp2)))

          case "!=" => println ("!= relop")
            constraints.put(ctx, :~:(:==:(exp2)))
            blocks.get(ctx).append(Constrain(x,:~:(:==:(exp2))))

          case "<" => println ("< relop")
            constraints.put(ctx, :<:(exp2))
            blocks.get(ctx).append(Constrain(x,:<:(exp2)))

          case ">" => println ("> relop")
            constraints.put(ctx, :>:(exp2))
            blocks.get(ctx).append(Constrain(x,:>:(exp2)))

          case _ => println("Unknown relop "+ctx.rel_op);
        }

      case _ => println ("FIXME RELOP_EXPR")
    }
    //exp1 must be either metadata or field ref
    //exp2 can be either constant, etc.
  }

  override def exitNegated_bool_expr(ctx:Negated_bool_exprContext){
    val exp = constraints.get(ctx.bool_expr)
    constraints.put(ctx, :~:(exp))
  }

  override def exitConst_bool(ctx:Const_boolContext){
    println("Matched const bool expr.")

    values.put(ctx, (if (ctx.getText.equalsIgnoreCase("true")) 1 else 0))
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
    val src = ctx.metadata_expr().getText
    ctx.statement = new SetStatement(ParserInterpreter.parseExpression(dst),
      ParserInterpreter.parseExpression(src))
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
    if (ctx.value_or_masked() != null)
      ctx.values = ctx.value_or_masked().map(x => x.v)
    else
      ctx.values = util.Arrays.asList(new Value().setValue(0).setMask(0))
  }

  override def exitCase_entry(ctx: Case_entryContext): Unit = {
    val retVal = ctx.case_return_value_type().getText
    val retst = if (ctx.case_return_value_type().parser_exception_name() != null) {
      new ReturnStatement("").setError(true).setMessage(ctx.case_return_value_type().parser_exception_name().getText)
    } else  {
      new ReturnStatement(ctx.case_return_value_type().getText)
    }
    ctx.caseEntry = ctx.value_list().values.foldLeft(new CaseEntry())((acc, x) => {
      acc.addValue(x)
    }).setReturnStatement(retst)
  }

  override def exitSelect_exp(ctx: Select_expContext): Unit = {
    ctx.expressions = ctx.field_or_data_ref().map(x => ParserInterpreter.parseExpression(x.getText))
  }

  def buildNetworkConfig() = new NetworkConfig(None, elements.map(element => (element.name, element)).toMap, foundPaths.toList)
}
