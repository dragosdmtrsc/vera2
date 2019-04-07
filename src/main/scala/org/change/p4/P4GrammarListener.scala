package org.change.p4

import java.util

import generated.parser.p4.P4GrammarParser._
import generated.parser.p4.{P4GrammarBaseListener, P4GrammarParser}
import org.change.p4.model._
import org.change.p4.model.actions._
import org.change.p4.model.control._
import org.change.p4.model.control.exp._
import org.change.p4.model.fieldlists.FieldList.PayloadEntry
import org.change.p4.model.parser._
import org.change.p4.model.table.{MatchKind, TableDeclaration, TableMatch}

/**
  * A small gift from radu to symnetic.
  */
class P4GrammarListener extends P4GrammarBaseListener {

  // Section 1.5 Begin
  override def exitField_value(
    ctx: P4GrammarParser.Field_valueContext
  ): Unit = {
    ctx.fieldValue = ctx.const_value().constValue
    ctx.width = ctx.const_value().width
  }
  override def exitConst_value(
    ctx: P4GrammarParser.Const_valueContext
  ): Unit = {
    if (ctx.width_spec() != null) {
      ctx.width = Integer.parseInt(ctx.width_spec().Decimal_value().getText)
    } else {
      ctx.width = -1
    }
    ctx.constValue = (if (ctx.getText.startsWith("-")) -1 else 1) *
      ctx.unsigned_value().unsignedValue
  }

  override def exitHexadecimalUValue(
    ctx: P4GrammarParser.HexadecimalUValueContext
  ): Unit =
    ctx.unsignedValue = ValueSpecificationParser.hexToInt(
      ctx.Hexadecimal_value().getText.substring(2)
    )

  override def exitDecimalUValue(
    ctx: P4GrammarParser.DecimalUValueContext
  ): Unit =
    ctx.unsignedValue =
      ValueSpecificationParser.decimalToInt(ctx.Decimal_value().getText)

  override def exitBinaryUValue(
    ctx: P4GrammarParser.BinaryUValueContext
  ): Unit =
    ctx.unsignedValue =
      ValueSpecificationParser.binaryToInt(ctx.Binary_value().getText)
  // Section 1.5 End
  // Section 2.1 Begin
  import scala.collection.JavaConversions._

  // Section 2.1
  override def exitHeader_type_declaration(
    ctx: P4GrammarParser.Header_type_declarationContext
  ): Unit = {
    val declaredHeaderName = ctx.header_type_name().getText
    var headerSize =
      if (ctx.header_dec_body().length_exp() != null &&
          ctx.header_dec_body().length_exp().const_value() != null)
        Some(ctx.header_dec_body().length_exp().const_value().constValue)
      else
        None

    val fields = ctx.header_dec_body().field_dec().toList.map { h =>
      val width: Option[BigInt] =
        Option(h.bit_width().const_value()).map(_.constValue)
      val name: String = h.field_name().getText
      (name, width)
    }

    val fieldsWithSizes = {
      val total = fields.foldLeft(BigInt(0))(_ + _._2.getOrElse(0l))
      // If not defined, then it is computed.
      if (headerSize.isEmpty) headerSize = Some(total)
      // By this point there should be a value for the size of the header.
      fields.map(
        field => (field._1, field._2.getOrElse(headerSize.get - total))
      )
    }
    ctx.header = new Header()
      .setName(declaredHeaderName)
      .setMaxLength(ctx.header_dec_body().maxLength)
    for (f <- ctx.header_dec_body().fields) {
      ctx.header = ctx.header.addField(f)
    }
    switch.declareHeader(ctx.header, false)
  }
  // Exit Section 2.1

  // Section 2.2

  override def exitScalar_instance(
    ctx: P4GrammarParser.Scalar_instanceContext
  ): Unit = {
    val instanceName = ctx.instance_name().getText
    val headerType = ctx.header_type_name().getText
    ctx.hdrInstance = new model.HeaderInstance(headerType, instanceName)
    switch.declareHeaderInstance(ctx.hdrInstance, false)
  }

  override def exitArray_instance(
    ctx: P4GrammarParser.Array_instanceContext
  ): Unit = {
    val instanceName = ctx.instance_name().getText
    val index = ctx.const_value().constValue
    val headerType = ctx.header_type_name().getText
    ctx.arrInstance =
      new ArrayInstance(headerType, instanceName, index.intValue())
    switch.declareHeaderInstance(ctx.arrInstance, false)
  }

  override def exitMetadata_initializer(
    ctx: P4GrammarParser.Metadata_initializerContext
  ): Unit = {
    import scala.collection.JavaConverters._
    ctx.inits = (ctx.field_name().asScala zip ctx.field_value().asScala)
      .map(nv => {
        nv._1.getText -> nv._2.fieldValue
      })
      .toMap
  }

  override def exitMetadataInstance(
    ctx: P4GrammarParser.MetadataInstanceContext
  ): Unit = {
    val headerType = ctx.metadata_instance().header_type_name().getText
    val instanceName = ctx.metadata_instance().instance_name().getText
    val initializer: Map[String, BigInt] =
      if (ctx.metadata_instance().metadata_initializer() != null)
        ctx
          .metadata_instance()
          .metadata_initializer()
          .inits
          .map(mv => mv._1.toString -> mv._2)
          .toMap
      else
        Map.empty
    switch.declareHeaderInstance(
      initializer.foldLeft(
        new model.HeaderInstance(headerType, instanceName).setMetadata(true)
      )((acc, x) => {
        acc.addInitializer(x._1, x._2)
      }),
      true
    )
  }

  override def exitField_list_calculation_declaration(
    ctx: Field_list_calculation_declarationContext
  ): Unit = {
    switch.declareCalculation(
      new Calculation(ctx.field_list_calculation_name().getText),
      false
    )
  }

  override def exitCounter_declaration(
    ctx: Counter_declarationContext
  ): Unit = {
    ctx.spec = new RegisterSpecification()
      .setDirect(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isDirect
      } else {
        false
      })
      .setStatic(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isStatic
      } else {
        false
      })
      .setDirectTable(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().directTable
      } else {
        null
      })
      .setStaticTable(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().staticTable
      } else {
        null
      })
      .setCounter(true)
      .setName(ctx.counter_name().getText)
    //TODO: more fields in counter, not sure they are needed
    switch.declareRegister(ctx.spec, false)
  }

  // Section 4
  override def exitHeader_extract_ref(
    ctx: P4GrammarParser.Header_extract_refContext
  ): Unit = {
    ctx.expression =
      (if (ctx.header_extract_index() != null)
         new IndexedHeaderRef()
           .setIndex(ctx.header_extract_index().expression.intValue())
       else new HeaderRef()).setPath(ctx.instance_name().getText)
  }

  override def exitHeader_extract_index(
    ctx: Header_extract_indexContext
  ): Unit =
    if (ctx.const_value() != null)
      ctx.expression = ctx.const_value().constValue.intValue()
    else ctx.expression = -2

  override def exitParser_function_declaration(
    ctx: P4GrammarParser.Parser_function_declarationContext
  ): Unit = {
    ctx.state = ctx
      .parser_function_body()
      .statements
      .foldLeft(new State().setName(ctx.parser_state_name().getText))(
        (acc, x) => {
          acc.add(x)
        }
      )
    switch.declareParserState(ctx.state, false)
  }

  override def exitExtract_or_set_statement(
    ctx: P4GrammarParser.Extract_or_set_statementContext
  ): Unit = {
    ctx.statement =
      if (ctx.set_statement() != null)
        ctx.set_statement().statement
      else
        ctx.extract_statement().statement
  }

  override def exitIndex(ctx: IndexContext): Unit = {
    if (ctx.const_value() == null) ctx.idx = -1
    else ctx.idx = ctx.const_value().constValue.intValue()
  }
  override def exitHeader_ref(ctx: P4GrammarParser.Header_refContext): Unit = {
    if (ctx.index() != null)
      ctx.expression = new IndexedHeaderRef()
        .setIndex(ctx.index().idx)
        .setPath(ctx.instance_name().getText)
    else
      ctx.expression = new HeaderRef().setPath(ctx.instance_name().getText)
  }

  override def exitField_ref(ctx: P4GrammarParser.Field_refContext): Unit = {
    ctx.expression = new FieldRef()
      .setHeaderRef(ctx.header_ref().expression)
      .setField(ctx.field_name().getText)
  }
  override def exitAction_function_declaration(
    ctx: Action_function_declarationContext
  ): Unit = {
    val complexAction = new P4ComplexAction(
      ctx.action_header().action_name().getText
    )
    if (ctx.action_header() != null && ctx
          .action_header()
          .param_list() != null &&
        ctx.action_header().param_list().param_name() != null)
      for (parm <- ctx.action_header().param_list().param_name())
        complexAction.getParameterList.add(new P4ActionParameter(parm.getText))
    for (act <- ctx.action_statement())
      complexAction.getActionList.add(act.actionCall)
    switch.declareAction(complexAction)
  }

  override def exitAction_statement(ctx: Action_statementContext): Unit = {
    ctx.actionCall = new P4ActionCall(
      new P4Action(P4ActionType.UNKNOWN, ctx.action_name().getText)
    )
    for (a <- ctx.arg())
      ctx.actionCall.addParameter(a.expr)
  }

  override def exitArg(ctx: ArgContext): Unit = {
    if (ctx.field_ref() != null) {
      ctx.expr = ctx.field_ref().expression
    } else if (ctx.field_value() != null) {
      ctx.expr =
        new LiteralExpr(ctx.field_value().fieldValue, ctx.field_value().width)
    } else if (ctx.header_ref() != null) {
      if (ctx.header_ref().expression.isArray)
        ctx.expr = ctx.header_ref().expression
      else
        ctx.expr = new StringRef(ctx.header_ref().expression.getPath)
    } else if (ctx.param_name() != null) {
      ctx.expr = new StringRef(ctx.param_name().getText)
    }
  }

  override def enterP4_program(ctx: P4_programContext): Unit = {
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
      0 -> ("ingress_port", 9),
      9 -> ("packet_length", 16),
      25 -> ("egress_spec", 9),
      34 -> ("egress_port", 9),
      43 -> ("egress_instance", 64),
      109 -> ("instance_type", 32),
      141 -> ("parser_status", 9),
      150 -> ("parser_error_location", 9),
      159 -> ("field_list_ref", 16),
      175 -> ("clone_spec", 16)
    )
    ctx.switchSpec = new Switch()
    switch = ctx.switchSpec
    val metadataInstancej =
      new model.HeaderInstance("standard_metadata_t", "standard_metadata")
        .setMetadata(true)
    switch
      .declareHeader(
        hOffs.foldLeft(
          new Header().setName("standard_metadata_t").setLength(512)
        )((acc, x) => {
          acc.addField(new Field().setLength(x._2._2).setName(x._2._1))
        }),
        false
      )
      .declareHeaderInstance(metadataInstancej, false)
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

  override def exitMeter_declaration(ctx: Meter_declarationContext): Unit = {
    ctx.spec = new RegisterSpecification()
      .setName(ctx.meter_name().getText)
      .setMeter(true)
    switch.declareRegister(ctx.spec, false)
  }
  override def exitRegister_declaration(
    ctx: Register_declarationContext
  ): Unit = {
    ctx.spec = new RegisterSpecification()
      .setDirect(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isDirect
      } else {
        false
      })
      .setStatic(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().isStatic
      } else {
        false
      })
      .setDirectTable(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().directTable
      } else {
        null
      })
      .setStaticTable(if (ctx.direct_or_static() != null) {
        ctx.direct_or_static().staticTable
      } else {
        null
      })
      .setName(ctx.register_name().NAME().getText)
      .setWidth(ctx.width_declaration().width)
      .setCount(
        if (ctx.const_value() != null) ctx.const_value().constValue.intValue()
        else 1
      )
    switch.declareRegister(ctx.spec, false)
  }

  override def exitField_list_declaration(
    ctx: Field_list_declarationContext
  ): Unit = {
    ctx.fieldList = new fieldlists.FieldList(ctx.field_list_name().getText)
    for (entry <- ctx.field_list_entry())
      ctx.fieldList.add(entry.entry)
    switch.declareFieldList(ctx.fieldList, false)
  }

  override def exitField_list_entry(ctx: Field_list_entryContext): Unit = {
    if (ctx.field_ref() != null)
      ctx.entry =
        new fieldlists.FieldList.FieldRefEntry(ctx.field_ref().expression)
    else if (ctx.header_ref() != null) {
      //need to disambiguate on reference solving
      ctx.entry = new fieldlists.FieldList.StringRefEntry(
        ctx.header_ref().expression.getPath
      )
    } else if (ctx.field_list_name() != null) {
      //need to disambiguate on reference solving
      ctx.entry =
        new fieldlists.FieldList.StringRefEntry(ctx.field_list_name().getText)
    } else if (ctx.field_value() != null) {
      ctx.entry = new fieldlists.FieldList.FieldValueEntry(
        ctx.field_value().fieldValue,
        ctx.field_value().width
      )
    } else {
      ctx.entry = new PayloadEntry
    }
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
    ctx.field = new Field()
      .setLength(
        if (ctx.bit_width().const_value() != null)
          ctx.bit_width().const_value().constValue.intValue()
        else
          -1
      )
      .setName(ctx.field_name().NAME().getText)
    if (ctx.field_mod() != null && ctx
          .field_mod()
          .getText
          .contains("saturating"))
      ctx.field.setSaturating()
    if (ctx.field_mod() != null && ctx.field_mod().getText.contains("signed"))
      ctx.field.setSigned()
  }

  override def exitField_match_type(ctx: Field_match_typeContext): Unit = {
    ctx.matchKind =
      if (ctx.getText == "range")
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
        throw new UnsupportedOperationException(
          s"Unknown match type ${ctx.getText}"
        )
  }

  override def exitTable_declaration(ctx: Table_declarationContext): Unit = {
    val tableName = ctx.table_name().NAME().getText
    if (ctx.field_match() != null) {
      for (fm <- ctx.field_match()) {
        ctx.tableDeclaration.addMatch(fm.tableMatch)
      }
    }
    if (ctx.table_actions().action_specification() != null &&
        ctx.table_actions().action_specification().action_name() != null) {
      for (an <- ctx.table_actions().action_specification().action_name()) {
        ctx.tableDeclaration.addAction(an.getText)
      }
    } else if (ctx.table_actions().action_profile_specification() != null &&
               ctx
                 .table_actions()
                 .action_profile_specification()
                 .action_profile_name()
                 .NAME()
                 .getText != null) {
      ctx.tableDeclaration.setProfile(
        ctx
          .table_actions()
          .action_profile_specification()
          .action_profile_name()
          .getText
      )
    }
    switch.declareTable(ctx.tableDeclaration, false)
  }
  override def enterTable_declaration(ctx: Table_declarationContext): Unit = {
    val tableName = ctx.table_name().NAME().getText
    ctx.tableDeclaration = new TableDeclaration(tableName)
    if (ctx.field_match() != null) {
      for (fm <- ctx.field_match()) {
        fm.tableName = tableName
      }
    }
  }

  override def exitField_or_masked_ref(
    ctx: Field_or_masked_refContext
  ): Unit = {
    if (ctx.const_value() != null)
      ctx.mask = ctx.const_value().constValue
    else
      ctx.mask = -1l
    if (ctx.field_ref() != null)
      ctx.field = ctx.field_ref().getText
    else
      ctx.field = ctx.header_ref().getText

    if (ctx.mask != -1) {
      assert(ctx.field_ref() != null)
      ctx.expression = new MaskedFieldRef(ctx.field_ref().expression, ctx.mask)
    } else {
      if (ctx.field_ref() != null)
        ctx.expression = ctx.field_ref().expression
      else ctx.expression = ctx.header_ref().expression
    }
  }

  override def exitField_match(ctx: Field_matchContext): Unit = {
    ctx.tableMatch = new TableMatch(
      ctx.tableName,
      ctx.field_or_masked_ref().field,
      ctx.field_match_type().matchKind,
      ctx.field_or_masked_ref().mask
    ).setReferenceKey(ctx.field_or_masked_ref().expression)
  }

  override def enterControl_function_declaration(
    ctx: Control_function_declarationContext
  ) {
    ctx.controlFunctionName = ctx.control_fn_name().NAME().getText
    ctx.controlBlock = new ControlBlock(ctx.controlFunctionName)
    currentControl = ctx.controlBlock
    ctx.control_block().parent = s"control.${ctx.controlFunctionName}"
  }

  override def exitControl_function_declaration(
    ctx: Control_function_declarationContext
  ) {
    switch.declareControlBlock(currentControl, false)
    if (ctx.control_block() != null)
      ctx.controlBlock.setStatement(ctx.control_block().blockStatement)
    else ctx.controlBlock.setStatement(new BlockStatement)
    currentControl = null
  }

  override def exitControl_block(ctx: Control_blockContext) {
    var i = 0
    ctx.blockStatement = new BlockStatement()
    if (ctx.control_statement() != null && ctx.control_statement().size() > 0)
      for (cs <- ctx.control_statement()) {
        ctx.blockStatement.addStatement(cs.statement)
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

  override def exitControl_statement(ctx: Control_statementContext) {
    if (ctx.control_fn_name != null) {
      val cfName = ctx.control_fn_name.getText
      val execId = currentControl.newControlCall(cfName)
      ctx.statement = new ApplyControlStatement(cfName, execId)
    } else if (ctx.apply_table_call() != null) {
      ctx.statement = ctx.apply_table_call().statement
    } else if (ctx.apply_and_select_block() != null) {
      ctx.statement = ctx.apply_and_select_block().statement
    } else if (ctx.if_else_statement() != null) {
      ctx.statement = ctx.if_else_statement().ifelseStatement
    } else {
      throw new IllegalStateException("Got null for all statements")
    }
  }
  override def exitApply_table_call(ctx: Apply_table_callContext) {
    assert(currentControl != null)
    val execId = currentControl.newTableCall(ctx.table_name().getText)
    ctx.statement = new ApplyTableStatement(ctx.table_name().getText, execId)
  }
  override def enterApply_and_select_block(ctx: Apply_and_select_blockContext) {
    ctx.case_list().parent = ctx.parent
    val crt = currentControl.newTableCall(ctx.table_name().getText)
    ctx.statement =
      new ApplyAndSelectTableStatement(ctx.table_name().getText, crt)
    ctx.case_list().statement = ctx.statement
  }

  override def exitAction_profile_declaration(
    ctx: Action_profile_declarationContext
  ): Unit = {
    ctx.actionProfile = new P4ActionProfile(
      ctx.action_profile_name().NAME().getText
    )
    ctx.actionProfile.getActions.addAll(ctx.action_specification().actions)
    if (ctx.selector_name() != null)
      ctx.actionProfile.setDynamicActionSelector(
        ctx.selector_name().NAME().getText
      )
    if (ctx.const_value() != null)
      ctx.actionProfile.setSize(ctx.const_value().constValue.intValue())
    switch.declareActionProfile(ctx.actionProfile, false)
  }

  override def exitAction_specification(
    ctx: Action_specificationContext
  ): Unit = {
    ctx.actions = new util.ArrayList[String]()
    if (ctx.action_name() != null)
      for (ac <- ctx.action_name())
        ctx.actions.add(ac.NAME().getText)
  }

  override def exitCase_list_action(ctx: Case_list_actionContext): Unit = {
    for (ac <- ctx.action_case()) {
      ctx.statement.addEntry(ac.caseEntry)
    }
  }

  override def enterCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    for (ac <- ctx.hit_miss_case()) {
      ac.parent = s"${ctx.parent}[$i]"
      i = i + 1
    }
  }

  override def exitCase_list_hitmiss(ctx: Case_list_hitmissContext): Unit = {
    var i = 0
    for (ac <- ctx.hit_miss_case()) {
      ctx.statement.addEntry(ac.caseEntry)
      i = i + 1
    }
  }

  override def enterHit_miss_case(ctx: Hit_miss_caseContext): Unit = {
    ctx.control_block().parent = ctx.parent
  }

  override def exitHit_miss_case(ctx: Hit_miss_caseContext) {
    ctx.caseEntry = TableCaseEntry.from(
      ctx.hit_or_miss().getText,
      ctx.control_block().blockStatement
    )
  }

  override def enterAction_case(ctx: Action_caseContext) {
    //must make room for control block instructions!
    //this will create a new control block with a specific port.
    ctx.control_block().parent = ctx.parent
  }

  override def exitAction_case(ctx: Action_caseContext) {
    ctx.caseEntry = TableCaseEntry.from(
      ctx.action_or_default().getText,
      ctx.control_block().blockStatement
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

  override def exitIf_else_statement(ctx: If_else_statementContext) {
    //bool_expr should be a constrain instruction we can use in the IF.
    if (ctx.else_block() != null) {
      ctx.ifelseStatement = new IfElseStatement(
        ctx.bool_expr().bexpr,
        ctx.control_block().blockStatement,
        ctx.else_block().statement
      )
    } else {
      ctx.ifelseStatement = new IfElseStatement(
        ctx.bool_expr().bexpr,
        ctx.control_block().blockStatement
      )
    }
  }

  override def enterElse_block(ctx: Else_blockContext): Unit = {
    if (ctx.control_block() != null) {
      ctx.control_block().parent = ctx.parent
    } else if (ctx.if_else_statement() != null) {
      ctx.if_else_statement().parent = ctx.parent
    }
  }

  override def exitElse_block(ctx: Else_blockContext) {
    if (ctx.control_block() != null) {
      //Nothing to do here, the control_block() will place
      // instructions in the table correctlys
      //      ctx.control_block().parent = ctx.parent
      ctx.statement = ctx.control_block().blockStatement
    } else if (ctx.if_else_statement() != null) {
      ctx.statement = ctx.if_else_statement().ifelseStatement
      ctx.if_else_statement().parent = ctx.parent
    }
  }

  //exp : exp bin_op exp # compound_exp
  //      | un_op exp # unary_exp
  //      | field_ref # field_red_exp
  //      | value # value_exp
  //      | '(' exp ')' # par_exp ;

  override def exitCompound_exp(ctx: Compound_expContext): Unit = {
    ctx.bvexpr =
      BinExpr.from(ctx.bin_op().getText, ctx.exp(0).bvexpr, ctx.exp(1).bvexpr)
  }

  override def exitUnary_exp(ctx: Unary_expContext): Unit = {
    ctx.bvexpr = UnOpExpr.from(ctx.un_op().getText, ctx.exp().bvexpr)
  }

  override def exitField_red_exp(ctx: Field_red_expContext) {
    ctx.bvexpr = new FieldRefExpr(ctx.field_ref().expression)
  }

  override def exitValue_exp(ctx: Value_expContext) {
    ctx.bvexpr = new LiteralExpr(
      ctx.value().const_value().constValue,
      ctx.value().const_value().width
    )
  }

  override def exitPar_exp(ctx: Par_expContext) {
    ctx.bvexpr = ctx.exp().bvexpr
  }

  //bool_expr : 'valid' '(' header_ref ')' # valid_bool_expr
  //	  | bool_expr bool_op bool_expr # compound_bool_expr
  //	  | 'not' bool_expr # negated_bool_expr
  //	  | '(' bool_expr ')' # par_bool_expr
  //	  | exp rel_op exp # relop_bool_expr
  //	  | 'true' # const_bool
  //  | 'false' # const_bool;

  override def exitValid_bool_expr(ctx: Valid_bool_exprContext) {
    ctx.bexpr = new ValidRef(ctx.header_ref().expression)
  }

  override def exitCompound_bool_expr(ctx: Compound_bool_exprContext) {
    ctx.bexpr = BinBExpr.from(
      ctx.bool_op().getText,
      ctx.bool_expr(0).bexpr,
      ctx.bool_expr(1).bexpr
    )
  }

  override def exitPar_bool_expr(ctx: Par_bool_exprContext) {
    ctx.bexpr = ctx.bool_expr().bexpr
  }

  override def exitRelop_bool_expr(ctx: Relop_bool_exprContext) {
    ctx.bexpr =
      RelOp.from(ctx.rel_op().getText, ctx.exp(0).bvexpr, ctx.exp(1).bvexpr)
  }
  override def exitNegated_bool_expr(ctx: Negated_bool_exprContext) {
    ctx.bexpr = new NegBExpr(ctx.bool_expr().bexpr)
  }
  override def exitConst_bool(ctx: Const_boolContext) {
    ctx.bexpr = LiteralBool.fromValue("true".equals(ctx.getText))
  }
  override def exitParser_function_body(
    ctx: Parser_function_bodyContext
  ): Unit = {
    ctx.statements = ctx
      .extract_or_set_statement()
      .map(x => {
        x.statement
      }) :+ ctx.return_statement().statement
  }

  override def exitExtract_statement(ctx: Extract_statementContext): Unit = {
    val extractWhere = ctx.header_extract_ref().expression
    ctx.statement = new ExtractStatement(extractWhere)
  }

  override def exitLatest_field_ref_expr(
    ctx: Latest_field_ref_exprContext
  ): Unit = {
    ctx.bvexpr = new FieldRefExpr(ctx.latest_field_ref().expression)
  }

  override def exitSet_statement(ctx: Set_statementContext): Unit = {
    val dst = ctx.field_ref().expression
    val src = ctx.exp().bvexpr
    ctx.statement = new SetStatement(dst, null)
      .setRightE(src)
  }

  override def exitMetadata_expr(ctx: Metadata_exprContext): Unit = {
    ctx.expression = ctx.compound().expression
    super.exitMetadata_expr(ctx)
  }

  override def exitCompound(ctx: CompoundContext): Unit = {
    ctx.expression =
      if (ctx.minus_metadata_expr() != null)
        ctx.minus_metadata_expr().expression
      else if (ctx.plus_metadata_expr() != null)
        ctx.plus_metadata_expr().expression
      else if (ctx.simple_metadata_expr() != null)
        ctx.simple_metadata_expr().expression
      else
        throw new IllegalStateException(
          "Got compound metadata expression " + ctx.getText
        )
  }

  override def exitPlus_metadata_expr(ctx: Plus_metadata_exprContext): Unit = {
    ctx.expression = new CompoundExpression(
      true,
      ctx.simple_metadata_expr().expression,
      ctx.compound().expression
    )
  }

  override def exitMinus_metadata_expr(
    ctx: Minus_metadata_exprContext
  ): Unit = {
    ctx.expression = new CompoundExpression(
      false,
      ctx.simple_metadata_expr().expression,
      ctx.compound().expression
    )
  }

  override def exitLatest_field_ref(ctx: Latest_field_refContext): Unit =
    ctx.expression = new FieldRef().setField(ctx.field_name().getText)

  override def exitData_ref(ctx: Data_refContext): Unit = {
    ctx.expression = new DataRef(
      ctx.const_value(0).constValue.toLong,
      ctx.const_value(1).constValue.toLong
    )
  }
  override def exitField_or_data_ref(ctx: Field_or_data_refContext): Unit = {
    ctx.expression =
      if (ctx.data_ref() != null) ctx.data_ref().expression
      else if (ctx.field_ref() != null) ctx.field_ref().expression
      else if (ctx.latest_field_ref() != null) ctx.latest_field_ref().expression
      else { assert(false); null }
  }
  override def exitSimple_metadata_expr(
    ctx: Simple_metadata_exprContext
  ): Unit = {
    if (ctx.field_value() != null) {
      ctx.expression = new ConstantExpression(
        java.lang.Long.decode(ctx.field_value.getText)
      )
    } else if (ctx.field_or_data_ref() != null) {
      ctx.expression = ctx.field_or_data_ref().expression
    } else {
      assert(false)
    }
  }

  override def exitReturn_value_type(ctx: Return_value_typeContext): Unit = {
    if (ctx.parser_exception_name() != null) {
      ctx.statement = new ReturnStatement("")
        .setError(true)
        .setMessage(ctx.parser_exception_name().getText)
    } else if (ctx.control_function_name() != null) {
      ctx.statement = new ReturnStatement(ctx.control_function_name().getText)
    } else if (ctx.parser_state_name() != null) {
      ctx.statement = new ReturnStatement(ctx.parser_state_name().getText)
    }
  }

  override def exitData_ref_exp(ctx: Data_ref_expContext): Unit = {
    ctx.bvexpr = new DataRefExpr(ctx.data_ref().expression)
  }

  override def exitReturn_statement(ctx: Return_statementContext): Unit = {
    if (ctx.return_value_type() != null)
      ctx.statement = ctx.return_value_type().statement
    else {
      ctx.statement = ctx
        .case_entry()
        .foldLeft(new ReturnSelectStatement())((acc, x) => {
          acc.add(
            ctx
              .select_exp()
              .bvexpressions
              .foldLeft(x.caseEntry)((acc2, y) => {
                acc2.addExpression(y)
              })
          )
        })
    }
  }

  override def exitValue_or_masked(ctx: Value_or_maskedContext): Unit = {
    val fv0 = ctx.field_value(0)
    ctx.bvValue = new LiteralExpr(fv0.fieldValue, fv0.width)
    if (ctx.field_value().size() > 1) {
      ctx.bvMask =
        new LiteralExpr(ctx.field_value(1).fieldValue, ctx.field_value(1).width)
    } else {
      val oldWidth = fv0.width
      ctx.bvMask = new LiteralExpr(-1, oldWidth)
    }
  }

  override def exitValue_list(ctx: Value_listContext): Unit = {
    ctx.bvValues = ctx.value_or_masked().map(_.bvValue)
    ctx.bvMasks = ctx.value_or_masked().map(_.bvMask)
    ctx.isDefault = ctx.value_or_masked().isEmpty
  }

  override def exitCase_entry(ctx: Case_entryContext): Unit = {
    val retst =
      if (ctx.case_return_value_type().parser_exception_name() != null) {
        new ReturnStatement("")
          .setError(true)
          .setMessage(
            ctx.case_return_value_type().parser_exception_name().getText
          )
      } else {
        new ReturnStatement(ctx.case_return_value_type().getText)
      }
    if (ctx.value_list().isDefault) {
      ctx.caseEntry = new CaseEntry().setDefault()
    } else {
      assert(
        ctx.value_list().bvMasks.size() == ctx.value_list().bvValues.size()
      )
      ctx.caseEntry = new CaseEntry()
        .setBvMasks(ctx.value_list().bvMasks)
        .setBvValues(ctx.value_list().bvValues)
    }
    ctx.caseEntry.setReturnStatement(retst)
  }
  override def exitSelect_exp(ctx: Select_expContext): Unit = {
    ctx.bvexpressions = ctx.exp().map(_.bvexpr)
  }
  private var currentControl: ControlBlock = null
  private val tableDecls: util.Set[TableDeclaration] =
    new util.HashSet[TableDeclaration]()
  private var switch: Switch = null
  def actionRegistrar(): ActionRegistrar = switch.getActionRegistrar
}
