package org.change.parser.p4

import generated.parse.p4.P4GrammarListener
import generated.parse.p4.P4GrammarParser._
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.{ErrorNode, TerminalNode}

/**
  * A small gift from radu to symnetic.
  */
class P4ToSEFL extends P4GrammarListener {
  override def enterP4_program(ctx: P4_programContext): Unit = ???

  override def exitP4_program(ctx: P4_programContext): Unit = ???

  override def enterP4_declaration(ctx: P4_declarationContext): Unit = ???

  override def exitP4_declaration(ctx: P4_declarationContext): Unit = ???

  override def enterConst_value(ctx: Const_valueContext): Unit = ???

  override def exitConst_value(ctx: Const_valueContext): Unit = ???

  override def enterUnsigned_value(ctx: Unsigned_valueContext): Unit = ???

  override def exitUnsigned_value(ctx: Unsigned_valueContext): Unit = ???

  override def enterBinary_value(ctx: Binary_valueContext): Unit = ???

  override def exitBinary_value(ctx: Binary_valueContext): Unit = ???

  override def enterDecimal_value(ctx: Decimal_valueContext): Unit = ???

  override def exitDecimal_value(ctx: Decimal_valueContext): Unit = ???

  override def enterHexadecimal_value(ctx: Hexadecimal_valueContext): Unit = ???

  override def exitHexadecimal_value(ctx: Hexadecimal_valueContext): Unit = ???

  override def enterBinary_digit(ctx: Binary_digitContext): Unit = ???

  override def exitBinary_digit(ctx: Binary_digitContext): Unit = ???

  override def enterDecimal_digit(ctx: Decimal_digitContext): Unit = ???

  override def exitDecimal_digit(ctx: Decimal_digitContext): Unit = ???

  override def enterHexadecimal_digit(ctx: Hexadecimal_digitContext): Unit = ???

  override def exitHexadecimal_digit(ctx: Hexadecimal_digitContext): Unit = ???

  override def enterWidth_spec(ctx: Width_specContext): Unit = ???

  override def exitWidth_spec(ctx: Width_specContext): Unit = ???

  override def enterField_value(ctx: Field_valueContext): Unit = ???

  override def exitField_value(ctx: Field_valueContext): Unit = ???

  override def enterHeader_type_declaration(ctx: Header_type_declarationContext): Unit = ???

  override def exitHeader_type_declaration(ctx: Header_type_declarationContext): Unit = ???

  override def enterHeader_type_name(ctx: Header_type_nameContext): Unit = ???

  override def exitHeader_type_name(ctx: Header_type_nameContext): Unit = ???

  override def enterHeader_dec_body(ctx: Header_dec_bodyContext): Unit = ???

  override def exitHeader_dec_body(ctx: Header_dec_bodyContext): Unit = ???

  override def enterField_dec(ctx: Field_decContext): Unit = ???

  override def exitField_dec(ctx: Field_decContext): Unit = ???

  override def enterField_name(ctx: Field_nameContext): Unit = ???

  override def exitField_name(ctx: Field_nameContext): Unit = ???

  override def enterField_mod(ctx: Field_modContext): Unit = ???

  override def exitField_mod(ctx: Field_modContext): Unit = ???

  override def enterLength_bin_op(ctx: Length_bin_opContext): Unit = ???

  override def exitLength_bin_op(ctx: Length_bin_opContext): Unit = ???

  override def enterLength_exp(ctx: Length_expContext): Unit = ???

  override def exitLength_exp(ctx: Length_expContext): Unit = ???

  override def enterBit_width(ctx: Bit_widthContext): Unit = ???

  override def exitBit_width(ctx: Bit_widthContext): Unit = ???

  override def enterInstance_declaration(ctx: Instance_declarationContext): Unit = ???

  override def exitInstance_declaration(ctx: Instance_declarationContext): Unit = ???

  override def enterHeader_instance(ctx: Header_instanceContext): Unit = ???

  override def exitHeader_instance(ctx: Header_instanceContext): Unit = ???

  override def enterScalar_instance(ctx: Scalar_instanceContext): Unit = ???

  override def exitScalar_instance(ctx: Scalar_instanceContext): Unit = ???

  override def enterArray_instance(ctx: Array_instanceContext): Unit = ???

  override def exitArray_instance(ctx: Array_instanceContext): Unit = ???

  override def enterInstance_name(ctx: Instance_nameContext): Unit = ???

  override def exitInstance_name(ctx: Instance_nameContext): Unit = ???

  override def enterMetadata_instance(ctx: Metadata_instanceContext): Unit = ???

  override def exitMetadata_instance(ctx: Metadata_instanceContext): Unit = ???

  override def enterMetadata_initializer(ctx: Metadata_initializerContext): Unit = ???

  override def exitMetadata_initializer(ctx: Metadata_initializerContext): Unit = ???

  override def enterHeader_ref(ctx: Header_refContext): Unit = ???

  override def exitHeader_ref(ctx: Header_refContext): Unit = ???

  override def enterIndex(ctx: IndexContext): Unit = ???

  override def exitIndex(ctx: IndexContext): Unit = ???

  override def enterField_ref(ctx: Field_refContext): Unit = ???

  override def exitField_ref(ctx: Field_refContext): Unit = ???

  override def enterField_list_declaration(ctx: Field_list_declarationContext): Unit = ???

  override def exitField_list_declaration(ctx: Field_list_declarationContext): Unit = ???

  override def enterField_list_name(ctx: Field_list_nameContext): Unit = ???

  override def exitField_list_name(ctx: Field_list_nameContext): Unit = ???

  override def enterField_list_entry(ctx: Field_list_entryContext): Unit = ???

  override def exitField_list_entry(ctx: Field_list_entryContext): Unit = ???

  override def enterField_list_calculation_declaration(ctx: Field_list_calculation_declarationContext): Unit = ???

  override def exitField_list_calculation_declaration(ctx: Field_list_calculation_declarationContext): Unit = ???

  override def enterField_list_calculation_name(ctx: Field_list_calculation_nameContext): Unit = ???

  override def exitField_list_calculation_name(ctx: Field_list_calculation_nameContext): Unit = ???

  override def enterStream_function_algorithm_name(ctx: Stream_function_algorithm_nameContext): Unit = ???

  override def exitStream_function_algorithm_name(ctx: Stream_function_algorithm_nameContext): Unit = ???

  override def enterCalculated_field_declaration(ctx: Calculated_field_declarationContext): Unit = ???

  override def exitCalculated_field_declaration(ctx: Calculated_field_declarationContext): Unit = ???

  override def enterUpdate_verify_spec(ctx: Update_verify_specContext): Unit = ???

  override def exitUpdate_verify_spec(ctx: Update_verify_specContext): Unit = ???

  override def enterUpdate_or_verify(ctx: Update_or_verifyContext): Unit = ???

  override def exitUpdate_or_verify(ctx: Update_or_verifyContext): Unit = ???

  override def enterIf_cond(ctx: If_condContext): Unit = ???

  override def exitIf_cond(ctx: If_condContext): Unit = ???

  override def enterCalc_bool_cond(ctx: Calc_bool_condContext): Unit = ???

  override def exitCalc_bool_cond(ctx: Calc_bool_condContext): Unit = ???

  override def enterValue_set_declaration(ctx: Value_set_declarationContext): Unit = ???

  override def exitValue_set_declaration(ctx: Value_set_declarationContext): Unit = ???

  override def enterValue_set_name(ctx: Value_set_nameContext): Unit = ???

  override def exitValue_set_name(ctx: Value_set_nameContext): Unit = ???

  override def enterParser_function_declaration(ctx: Parser_function_declarationContext): Unit = ???

  override def exitParser_function_declaration(ctx: Parser_function_declarationContext): Unit = ???

  override def enterParser_state_name(ctx: Parser_state_nameContext): Unit = ???

  override def exitParser_state_name(ctx: Parser_state_nameContext): Unit = ???

  override def enterParser_function_body(ctx: Parser_function_bodyContext): Unit = ???

  override def exitParser_function_body(ctx: Parser_function_bodyContext): Unit = ???

  override def enterExtract_or_set_statement(ctx: Extract_or_set_statementContext): Unit = ???

  override def exitExtract_or_set_statement(ctx: Extract_or_set_statementContext): Unit = ???

  override def enterExtract_statement(ctx: Extract_statementContext): Unit = ???

  override def exitExtract_statement(ctx: Extract_statementContext): Unit = ???

  override def enterHeader_extract_ref(ctx: Header_extract_refContext): Unit = ???

  override def exitHeader_extract_ref(ctx: Header_extract_refContext): Unit = ???

  override def enterHeader_extract_index(ctx: Header_extract_indexContext): Unit = ???

  override def exitHeader_extract_index(ctx: Header_extract_indexContext): Unit = ???

  override def enterSet_statement(ctx: Set_statementContext): Unit = ???

  override def exitSet_statement(ctx: Set_statementContext): Unit = ???

  override def enterMetadata_expr(ctx: Metadata_exprContext): Unit = ???

  override def exitMetadata_expr(ctx: Metadata_exprContext): Unit = ???

  override def enterReturn_statement(ctx: Return_statementContext): Unit = ???

  override def exitReturn_statement(ctx: Return_statementContext): Unit = ???

  override def enterReturn_value_type(ctx: Return_value_typeContext): Unit = ???

  override def exitReturn_value_type(ctx: Return_value_typeContext): Unit = ???

  override def enterControl_function_name(ctx: Control_function_nameContext): Unit = ???

  override def exitControl_function_name(ctx: Control_function_nameContext): Unit = ???

  override def enterParser_exception_name(ctx: Parser_exception_nameContext): Unit = ???

  override def exitParser_exception_name(ctx: Parser_exception_nameContext): Unit = ???

  override def enterCase_entry(ctx: Case_entryContext): Unit = ???

  override def exitCase_entry(ctx: Case_entryContext): Unit = ???

  override def enterValue_list(ctx: Value_listContext): Unit = ???

  override def exitValue_list(ctx: Value_listContext): Unit = ???

  override def enterCase_return_value_type(ctx: Case_return_value_typeContext): Unit = ???

  override def exitCase_return_value_type(ctx: Case_return_value_typeContext): Unit = ???

  override def enterValue_or_masked(ctx: Value_or_maskedContext): Unit = ???

  override def exitValue_or_masked(ctx: Value_or_maskedContext): Unit = ???

  override def enterSelect_exp(ctx: Select_expContext): Unit = ???

  override def exitSelect_exp(ctx: Select_expContext): Unit = ???

  override def enterField_or_data_ref(ctx: Field_or_data_refContext): Unit = ???

  override def exitField_or_data_ref(ctx: Field_or_data_refContext): Unit = ???

  override def enterParser_exception_declaration(ctx: Parser_exception_declarationContext): Unit = ???

  override def exitParser_exception_declaration(ctx: Parser_exception_declarationContext): Unit = ???

  override def enterReturn_or_drop(ctx: Return_or_dropContext): Unit = ???

  override def exitReturn_or_drop(ctx: Return_or_dropContext): Unit = ???

  override def enterReturn_to_control(ctx: Return_to_controlContext): Unit = ???

  override def exitReturn_to_control(ctx: Return_to_controlContext): Unit = ???

  override def enterCounter_declaration(ctx: Counter_declarationContext): Unit = ???

  override def exitCounter_declaration(ctx: Counter_declarationContext): Unit = ???

  override def enterCounter_type(ctx: Counter_typeContext): Unit = ???

  override def exitCounter_type(ctx: Counter_typeContext): Unit = ???

  override def enterDirect_or_static(ctx: Direct_or_staticContext): Unit = ???

  override def exitDirect_or_static(ctx: Direct_or_staticContext): Unit = ???

  override def enterDirect_attribute(ctx: Direct_attributeContext): Unit = ???

  override def exitDirect_attribute(ctx: Direct_attributeContext): Unit = ???

  override def enterStatic_attribute(ctx: Static_attributeContext): Unit = ???

  override def exitStatic_attribute(ctx: Static_attributeContext): Unit = ???

  override def enterMeter_declaration(ctx: Meter_declarationContext): Unit = ???

  override def exitMeter_declaration(ctx: Meter_declarationContext): Unit = ???

  override def enterConst_expr(ctx: Const_exprContext): Unit = ???

  override def exitConst_expr(ctx: Const_exprContext): Unit = ???

  override def enterTable_name(ctx: Table_nameContext): Unit = ???

  override def exitTable_name(ctx: Table_nameContext): Unit = ???

  override def enterMeter_name(ctx: Meter_nameContext): Unit = ???

  override def exitMeter_name(ctx: Meter_nameContext): Unit = ???

  override def enterCounter_name(ctx: Counter_nameContext): Unit = ???

  override def exitCounter_name(ctx: Counter_nameContext): Unit = ???

  override def enterRegister_name(ctx: Register_nameContext): Unit = ???

  override def exitRegister_name(ctx: Register_nameContext): Unit = ???

  override def enterMeter_type(ctx: Meter_typeContext): Unit = ???

  override def exitMeter_type(ctx: Meter_typeContext): Unit = ???

  override def enterRegister_declaration(ctx: Register_declarationContext): Unit = ???

  override def exitRegister_declaration(ctx: Register_declarationContext): Unit = ???

  override def enterWidth_declaration(ctx: Width_declarationContext): Unit = ???

  override def exitWidth_declaration(ctx: Width_declarationContext): Unit = ???

  override def enterAttribute_list(ctx: Attribute_listContext): Unit = ???

  override def exitAttribute_list(ctx: Attribute_listContext): Unit = ???

  override def enterAttr_entry(ctx: Attr_entryContext): Unit = ???

  override def exitAttr_entry(ctx: Attr_entryContext): Unit = ???

  override def enterAction_function_declaration(ctx: Action_function_declarationContext): Unit = ???

  override def exitAction_function_declaration(ctx: Action_function_declarationContext): Unit = ???

  override def enterAction_header(ctx: Action_headerContext): Unit = ???

  override def exitAction_header(ctx: Action_headerContext): Unit = ???

  override def enterParam_list(ctx: Param_listContext): Unit = ???

  override def exitParam_list(ctx: Param_listContext): Unit = ???

  override def enterAction_statement(ctx: Action_statementContext): Unit = ???

  override def exitAction_statement(ctx: Action_statementContext): Unit = ???

  override def enterArg(ctx: ArgContext): Unit = ???

  override def exitArg(ctx: ArgContext): Unit = ???

  override def enterAction_profile_declaration(ctx: Action_profile_declarationContext): Unit = ???

  override def exitAction_profile_declaration(ctx: Action_profile_declarationContext): Unit = ???

  override def enterAction_name(ctx: Action_nameContext): Unit = ???

  override def exitAction_name(ctx: Action_nameContext): Unit = ???

  override def enterParam_name(ctx: Param_nameContext): Unit = ???

  override def exitParam_name(ctx: Param_nameContext): Unit = ???

  override def enterSelector_name(ctx: Selector_nameContext): Unit = ???

  override def exitSelector_name(ctx: Selector_nameContext): Unit = ???

  override def enterAction_profile_name(ctx: Action_profile_nameContext): Unit = ???

  override def exitAction_profile_name(ctx: Action_profile_nameContext): Unit = ???

  override def enterAction_specification(ctx: Action_specificationContext): Unit = ???

  override def exitAction_specification(ctx: Action_specificationContext): Unit = ???

  override def enterAction_selector_declaration(ctx: Action_selector_declarationContext): Unit = ???

  override def exitAction_selector_declaration(ctx: Action_selector_declarationContext): Unit = ???

  override def enterTable_declaration(ctx: Table_declarationContext): Unit = ???

  override def exitTable_declaration(ctx: Table_declarationContext): Unit = ???

  override def enterField_match(ctx: Field_matchContext): Unit = ???

  override def exitField_match(ctx: Field_matchContext): Unit = ???

  override def enterField_or_masked_ref(ctx: Field_or_masked_refContext): Unit = ???

  override def exitField_or_masked_ref(ctx: Field_or_masked_refContext): Unit = ???

  override def enterField_match_type(ctx: Field_match_typeContext): Unit = ???

  override def exitField_match_type(ctx: Field_match_typeContext): Unit = ???

  override def enterTable_actions(ctx: Table_actionsContext): Unit = ???

  override def exitTable_actions(ctx: Table_actionsContext): Unit = ???

  override def enterAction_profile_specification(ctx: Action_profile_specificationContext): Unit = ???

  override def exitAction_profile_specification(ctx: Action_profile_specificationContext): Unit = ???

  override def enterControl_function_declaration(ctx: Control_function_declarationContext): Unit = ???

  override def exitControl_function_declaration(ctx: Control_function_declarationContext): Unit = ???

  override def enterControl_fn_name(ctx: Control_fn_nameContext): Unit = ???

  override def exitControl_fn_name(ctx: Control_fn_nameContext): Unit = ???

  override def enterControl_block(ctx: Control_blockContext): Unit = ???

  override def exitControl_block(ctx: Control_blockContext): Unit = ???

  override def enterControl_statement(ctx: Control_statementContext): Unit = ???

  override def exitControl_statement(ctx: Control_statementContext): Unit = ???

  override def enterApply_table_call(ctx: Apply_table_callContext): Unit = ???

  override def exitApply_table_call(ctx: Apply_table_callContext): Unit = ???

  override def enterApply_and_select_block(ctx: Apply_and_select_blockContext): Unit = ???

  override def exitApply_and_select_block(ctx: Apply_and_select_blockContext): Unit = ???

  override def enterCase_list(ctx: Case_listContext): Unit = ???

  override def exitCase_list(ctx: Case_listContext): Unit = ???

  override def enterAction_case(ctx: Action_caseContext): Unit = ???

  override def exitAction_case(ctx: Action_caseContext): Unit = ???

  override def enterAction_or_default(ctx: Action_or_defaultContext): Unit = ???

  override def exitAction_or_default(ctx: Action_or_defaultContext): Unit = ???

  override def enterHit_miss_case(ctx: Hit_miss_caseContext): Unit = ???

  override def exitHit_miss_case(ctx: Hit_miss_caseContext): Unit = ???

  override def enterHit_or_miss(ctx: Hit_or_missContext): Unit = ???

  override def exitHit_or_miss(ctx: Hit_or_missContext): Unit = ???

  override def enterIf_else_statement(ctx: If_else_statementContext): Unit = ???

  override def exitIf_else_statement(ctx: If_else_statementContext): Unit = ???

  override def enterElse_block(ctx: Else_blockContext): Unit = ???

  override def exitElse_block(ctx: Else_blockContext): Unit = ???

  override def enterBool_expr(ctx: Bool_exprContext): Unit = ???

  override def exitBool_expr(ctx: Bool_exprContext): Unit = ???

  override def enterExp(ctx: ExpContext): Unit = ???

  override def exitExp(ctx: ExpContext): Unit = ???

  override def enterValue(ctx: ValueContext): Unit = ???

  override def exitValue(ctx: ValueContext): Unit = ???

  override def enterBin_op(ctx: Bin_opContext): Unit = ???

  override def exitBin_op(ctx: Bin_opContext): Unit = ???

  override def enterUn_op(ctx: Un_opContext): Unit = ???

  override def exitUn_op(ctx: Un_opContext): Unit = ???

  override def enterBool_op(ctx: Bool_opContext): Unit = ???

  override def exitBool_op(ctx: Bool_opContext): Unit = ???

  override def enterRel_op(ctx: Rel_opContext): Unit = ???

  override def exitRel_op(ctx: Rel_opContext): Unit = ???

  override def visitTerminal(node: TerminalNode): Unit = ???

  override def visitErrorNode(node: ErrorNode): Unit = ???

  override def exitEveryRule(ctx: ParserRuleContext): Unit = ???

  override def enterEveryRule(ctx: ParserRuleContext): Unit = ???
}
