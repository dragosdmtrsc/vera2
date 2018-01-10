grammar P4Grammar;

// TODO: Watch for "[" or "]"

p4_program      :   p4_declaration+ ;

p4_declaration  :   header_type_declaration
                |   instance_declaration
                |   field_list_declaration
                |   field_list_calculation_declaration
                |   calculated_field_declaration
                |   value_set_declaration
                |   parser_function_declaration
                |   parser_exception_declaration
                |   counter_declaration
                |   meter_declaration
                |   register_declaration
                |   action_function_declaration
                |   action_profile_declaration
                |   action_selector_declaration
                |   table_declaration
                |   control_function_declaration
                ;

const_value     returns [Integer constValue]:
    ('+'|'-')? width_spec? unsigned_value ;
unsigned_value  returns [Integer unsignedValue]:
    Binary_value            #BinaryUValue
    | Decimal_value         #DecimalUValue
    | Hexadecimal_value     #HexadecimalUValue
    ;

Binary_value:
    BINARY_BASE Binary_digit+ ;
Decimal_value:
    Decimal_digit+ ;
Hexadecimal_value:
    HEXADECIMAL_BASE Hexadecimal_digit+ ;

BINARY_BASE     :   '0b' | '0B' ;
HEXADECIMAL_BASE    :   '0x' | '0X' ;

fragment Binary_digit    :  '_' | '0' | '1' ;
fragment Decimal_digit   :   Binary_digit | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
fragment Hexadecimal_digit   : Decimal_digit | 'a' | 'A' | 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' ;

width_spec  :   Decimal_value '\'' ;
field_value returns [Integer fieldValue] : const_value ;

// Section 2.1
header_type_declaration returns [org.change.parser.p4.HeaderDeclaration headerDeclaration, org.change.v2.p4.model.Header header]:
    'header_type' header_type_name '{' header_dec_body '}' ;
header_type_name : NAME ;

// Info: max_length is used for run-time checks (if the header is larger than this maximum)
header_dec_body returns [List<org.change.v2.p4.model.Field> fields,
    Integer length,
    Integer maxLength]:   'fields' '{' field_dec+ '}' ( 'length' ':' length_exp ';' )? ( 'max_length' ':' const_value ';' )? ;

field_dec returns [org.change.v2.p4.model.Field field]  :   field_name ':' bit_width ( '(' field_mod ')' )? ';' ;
field_name  :   NAME ;
field_mod   :   'signed' | 'saturating' | field_mod ',' field_mod ;
length_bin_op   :   '+' | '-' | '*' | '<<' | '>>' ;
//TODO: Only const_value is supported currenty.
length_exp  :   const_value | field_name | length_exp length_bin_op length_exp | '(' length_exp ')' ;
bit_width   :   const_value | '*' ;

// Section 2.2
//TODO: Support metadata instances.
instance_declaration returns [org.change.parser.p4.P4Instance instance]:
    header_instance     #HeaderInstance
    | metadata_instance #MetadataInstance
    ;

header_instance returns [org.change.parser.p4.HeaderInstance instance]:
    scalar_instance     #ScalarInstance
    | array_instance    #ArrayInstance
    ;

scalar_instance returns [org.change.parser.p4.ScalarHeader instance, org.change.v2.p4.model.HeaderInstance hdrInstance]:
    'header' header_type_name instance_name ';' ;
array_instance  returns [org.change.parser.p4.ArrayHeader instance, org.change.v2.p4.model.ArrayInstance arrInstance]:
    'header' header_type_name instance_name '[' const_value ']' ';' ;
instance_name   :   NAME ;

metadata_instance returns [org.change.parser.p4.MetadataInstance instance]:
   'metadata' header_type_name instance_name metadata_initializer? ';' ;
metadata_initializer returns [scala.collection.Map<String, Integer> inits]:
   '{' ( field_name ':' field_value ';' )+ '}' ;

header_ref returns [org.change.v2.analysis.memory.TagExp tagReference, String headerInstanceId] :
    instance_name | instance_name '[' index ']' ;
// TODO: Add support for `last`
index   :   const_value | 'last' ;
field_ref   returns [org.change.v2.analysis.memory.TagExp reference]:   header_ref '.' field_name ;

//TODO: All the field etc stuff.
field_list_declaration returns [java.util.List<String> entryList] :   'field_list' field_list_name '{' ( field_list_entry ';')+ '}' ;
field_list_name :   NAME ;

field_list_entry  returns [String entryName]  :   field_ref | header_ref | field_value | field_list_name | 'payload' ;
field_list_calculation_declaration :
    'field_list_calculation' field_list_calculation_name '{'
        'input' '{'
            ( field_list_name ';' )+
        '}'
        'algorithm' ':' stream_function_algorithm_name ';'
        'output_width' ':' const_value ';'
    '}'
    ;
field_list_calculation_name :   NAME ;
stream_function_algorithm_name  :   NAME ;
calculated_field_declaration    : 'calculated_field' field_ref '{' update_verify_spec+ '}' ;

update_verify_spec  :   update_or_verify field_list_calculation_name ( if_cond )? ';' ;
update_or_verify    :   'update' | 'verify' ;

if_cond :   'if' '(' calc_bool_cond ')' ;
calc_bool_cond :    'valid' '(' (header_ref | field_ref) ')' | field_ref '==' field_value ;
value_set_declaration : 'parser_value_set' value_set_name ';' ;
value_set_name  : NAME ;

// Section 4 Parser Specification
parser_function_declaration returns [org.change.parser.p4.ParserFunctionDeclaration functionDeclaration,
org.change.v2.p4.model.parser.State state] :
    'parser' parser_state_name '{' parser_function_body '}' ;

parser_state_name   :   NAME ;
parser_function_body returns [java.util.List<org.change.v2.p4.model.parser.Statement> statements]:
    extract_or_set_statement* return_statement ;
// TODO: Support set_statement.
extract_or_set_statement returns [org.change.parser.p4.ParserFunctionStatement functionStatement,
org.change.v2.p4.model.parser.Statement statement] :
    extract_statement | set_statement ;
extract_statement  returns [org.change.parser.p4.ExtractHeader extractStatement,
    org.change.v2.p4.model.parser.ExtractStatement statement] :
    'extract' '(' header_extract_ref ')' ';' ;
header_extract_ref returns [org.change.parser.p4.HeaderInstance headerInstance] :
    instance_name | instance_name '[' header_extract_index ']' ;
header_extract_index    : const_value | 'next' ;
set_statement returns [org.change.v2.p4.model.parser.SetStatement statement]  :
    'set_metadata' '(' field_ref',' metadata_expr ')' ';' ;
simple_metadata_expr returns [org.change.v2.p4.model.parser.Expression expression] : field_value | field_or_data_ref;
plus_metadata_expr returns [org.change.v2.p4.model.parser.Expression expression]: simple_metadata_expr '+' compound;
minus_metadata_expr returns [org.change.v2.p4.model.parser.Expression expression]: simple_metadata_expr '-' compound;
compound returns [org.change.v2.p4.model.parser.Expression expression]: minus_metadata_expr | plus_metadata_expr | simple_metadata_expr;
metadata_expr returns [org.change.v2.p4.model.parser.Expression expression]:   compound ;

return_statement  returns [org.change.v2.p4.model.parser.Statement statement]  :
    return_value_type | 'return select' '(' select_exp ')' '{' case_entry+ '}'  ;

return_value_type  returns [org.change.v2.p4.model.parser.ReturnStatement statement] :
    'return' parser_state_name ';' | 'return' control_function_name ';'
    | 'parse_error' parser_exception_name ';' ;
control_function_name   :   NAME ;
parser_exception_name   :   NAME ;

case_entry returns [org.change.v2.p4.model.parser.CaseEntry caseEntry] :
    value_list ':' case_return_value_type ';';
value_list returns [java.util.List<org.change.v2.p4.model.parser.Value> values]:
    value_or_masked ( ',' value_or_masked )* | 'default' ;

case_return_value_type  : parser_state_name | control_function_name | 'parse_error' parser_exception_name ;

value_or_masked returns [org.change.v2.p4.model.parser.Value v]:
    field_value | field_value 'mask' field_value | value_set_name ;


select_exp  returns [java.util.List<org.change.v2.p4.model.parser.Expression> expressions]:
    field_or_data_ref (',' field_or_data_ref)* ;
field_or_data_ref   :   field_ref | 'latest.'field_name | 'current' '(' const_value ',' const_value ')' ;
parser_exception_declaration    :   'parser_exception' parser_exception_name '{'
    set_statement*
    return_or_drop ';'
    '}'
    ;

return_or_drop :    return_to_control | 'parser_drop' ;
return_to_control   :   'return' control_function_name ;
counter_declaration :   'counter' counter_name '{'
    'type' ':' counter_type ';'
    ( direct_or_static ';' )?
    ( 'instance_count' ':' const_expr ';' )?
    ( 'min_width' ':' const_expr ';' )?
    ( 'saturating' ';' )?
    '}'
    ;

counter_type    : 'bytes' | 'packets' | 'packets_and_bytes' ;
direct_or_static returns [boolean isDirect, String directTable, boolean isStatic, String staticTable]: direct_attribute | static_attribute ;
direct_attribute returns [String table] : 'direct' ':' table_name ;
static_attribute returns [String table]: 'static' ':' table_name ;
meter_declaration : 'meter' meter_name '{'
    'type' ':' meter_type ';'
    ( 'result' ':' field_ref ';' )?
    ( direct_or_static ';' )?
    (' instance_count' ':' const_expr ';' )?
    '}'
    ;

//TODO: const_expr in not defined, I'll leave this as a const_value.
const_expr  : const_value ;

table_name  :   NAME ;
meter_name  :   NAME ;
counter_name    :   NAME ;
register_name   :   NAME ;

meter_type : 'bytes' | 'packets' ;

register_declaration returns [org.change.v2.p4.model.RegisterSpecification spec]: 'register' register_name '{'
    width_declaration ';'
    ( direct_or_static ';' )?
    ( 'instance_count' ':' const_value ';' )?
    ( attribute_list ';' )?
    '}'
    ;

width_declaration returns [Integer width]: 'width' ':' const_value ;
attribute_list : 'attributes' ':' attr_entry ;
attr_entry : 'signed' | 'saturating' | attr_entry ',' attr_entry ;
action_function_declaration:
        'action' action_header '{' action_statement* '}' ;

action_header : action_name '(' ( param_list )? ')' ;
param_list : param_name (',' param_name)* ;
action_statement : action_name '(' ( arg (',' arg)* )? ')' ';' ;
arg : param_name | field_value | field_ref | header_ref ;

action_profile_declaration : 'action_profile' action_profile_name '{'
    action_specification
    ( 'size' ':' const_value ';' )?
    ( 'dynamic_action_selection' ':' selector_name ';' )?
    '}'
    ;

action_name : NAME ;
param_name  : NAME ;
selector_name   : NAME ;
action_profile_name : NAME ;

action_specification : 'actions' '{' ( action_name ';' )+ '}' ;

action_selector_declaration : 'action_selector' selector_name '{'
    'selection_key' ':' field_list_calculation_name ';'
    '}'
    ;

table_declaration : 'table' table_name '{'
    ( 'reads' '{' field_match+ '}' )?
    table_actions
    ( 'min_size' ':' const_value ';' )?
    ( 'max_size' ':' const_value ';' )?
    ( 'size' ':' const_value ';' )?
    ( 'support_timeout' ':' ('true' | 'false') ';' )?
    '}'
    ;

field_match returns [org.change.v2.p4.model.table.TableMatch tableMatch, String tableName]: field_or_masked_ref ':' field_match_type ';' ;
field_or_masked_ref : header_ref | field_ref | field_ref 'mask' const_value ;
field_match_type returns [org.change.v2.p4.model.table.MatchKind matchKind]: 'exact' | 'ternary' | 'lpm' | 'range' | 'valid' ;
table_actions : action_specification | action_profile_specification ;
action_profile_specification : 'action_profile' ':' action_profile_name ;
control_function_declaration returns [String controlFunctionName] : 'control' control_fn_name control_block ;
control_fn_name : NAME;
control_block returns [String parent,
java.util.List<org.change.v2.analysis.processingmodels.Instruction> instructions] : '{' control_statement* '}' ;
control_statement returns [String parent,
org.change.v2.analysis.processingmodels.Instruction instruction]: apply_table_call |
apply_and_select_block |
if_else_statement |
control_fn_name '(' ')' ';' ;

apply_table_call returns [String parent, org.change.v2.analysis.processingmodels.Instruction instruction]: 'apply' '(' table_name ')' ';' ;

apply_and_select_block returns [String parent, org.change.v2.analysis.processingmodels.Instruction instruction]:
    'apply' '(' table_name ')' '{' ( case_list )? '}' ;

case_list returns [String parent,
    java.util.List<org.change.v2.analysis.processingmodels.Instruction> instructions]: action_case+ # case_list_action
          | hit_miss_case+  # case_list_hitmiss;

action_case returns [String parent,
    org.change.v2.analysis.processingmodels.Instruction instruction]: action_or_default control_block ;
action_or_default : action_name | 'default' ;
hit_miss_case returns [String parent,
      org.change.v2.analysis.processingmodels.Instruction instruction]: hit_or_miss control_block ;
hit_or_miss : 'hit' | 'miss' ;

if_else_statement returns [String parent,
      org.change.v2.analysis.processingmodels.Instruction instruction]: 'if' '(' bool_expr ')' control_block ( else_block )? ;
else_block returns [String parent,
     org.change.v2.analysis.processingmodels.Instruction instruction]: 'else' control_block | 'else' if_else_statement ;

bool_expr : 'valid' '(' header_ref ')' # valid_bool_expr
          | bool_expr bool_op bool_expr # compound_bool_expr
          | 'not' bool_expr # negated_bool_expr
          | '(' bool_expr ')' # par_bool_expr
          | exp rel_op exp #relop_bool_expr
          | 'true' # const_bool
          | 'false' # const_bool;

exp : exp bin_op exp # compound_exp
      | un_op exp # unary_exp
      | field_ref # field_red_exp
      | value # value_exp
      | '(' exp ')' # par_exp ;

//TODO: This is an assumption too
value   : const_value ;

bin_op : '+' | '*' | '-' | '<<' | '>>' | '&' | '|' | '^' ;
un_op : '~' | '-' ;
bool_op : 'or' | 'and' ;
rel_op : '>' | '>=' | '==' | '<=' | '<' | '!=' ;

NAME:   (SINGLELETTER|UNDERSCORE) (SINGLELETTER | UNDERSCORE | DOLLAR | NUMBER)*;

fragment SINGLELETTER   :   ( 'a'..'z' | 'A'..'Z');


fragment LOWERCASE  :   'a'..'z';
fragment UNDERSCORE :   '_';
fragment DOLLAR :   '$';
fragment NUMBER :   '0'..'9';
// Skip whitespaces
WS : [ \t\r\n]+ -> skip ;

