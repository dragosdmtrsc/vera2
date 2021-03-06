grammar P4Commands;

statements: statement+;
statement : table_default | table_add | mirroring_add | act_prof_create_member;
table_default returns [org.change.p4.control.TableFlow tableFlow]:
    'table_set_default' id act_spec ('=>' action_parm*)?;
table_add returns [org.change.p4.control.TableFlow tableFlow]:
    'table_add' id act_spec match_key* ('=>' action_parm*)?;
mirroring_add : 'mirroring_add' unsigned_value unsigned_value;

act_prof_create_member: 'act_prof_create_member' NAME unsigned_value
    NAME ('=>' action_parm*)?;

action_parm returns [org.change.p4.control.ActionParam actionParam]: unsigned_value;
match_key returns [org.change.p4.control.MatchParam matchParam]:
    unsigned_value '&&&' unsigned_value #Masked
    | unsigned_value '/' unsigned_value #Prefix
    | unsigned_value ',' unsigned_value #Range
    | unsigned_value #Exact;

act_spec returns [org.change.p4.control.ActionSpec actionSpec]:
 'member' '(' unsigned_value ')' #memberAction
    | id #namedAction
    | EMPTY_ACTION_SPEC #emptyAction;

unsigned_value  returns [scala.math.BigInt unsignedValue]:
    Binary_value            #BinaryUValue
    | Decimal_value         #DecimalUValue
    | Hexadecimal_value     #HexadecimalUValue
    | MAC #MacUValue
    | IP #IPUValue
    | IP6 #IP6UValue
    ;
id : NAME;
Binary_value:
    BINARY_BASE Binary_digit+ ;
Decimal_value:
    Decimal_digit+ ;
Hexadecimal_value:
    HEXADECIMAL_BASE Hexadecimal_digit+ ;

fragment BINARY_BASE     :   '0b' | '0B' ;
fragment HEXADECIMAL_BASE    :   '0x' | '0X' ;

fragment Binary_digit    :  '_' | '0' | '1' ;
fragment Decimal_digit   :   Binary_digit | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
fragment Hexadecimal_digit   : Decimal_digit | 'a' | 'A' | 'b' | 'B' | 'c' | 'C' | 'd' | 'D' | 'e' | 'E' | 'f' | 'F' ;

fragment SINGLELETTER   :   ( LOWERCASE | 'A'..'Z');

IP : Decimal_value '.' Decimal_value '.' Decimal_value '.' Decimal_value;

IP6   :                             ( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |                       '::' ( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |                H16?  '::' ( H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':' )? H16)?  '::' ( H16 ':'  H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':'  H16 ':' )? H16)?  '::' ( H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'    H16 ':'   Ls32
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'              Ls32
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'              H16
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::';

 fragment H16           : Hexadecimal_digit Hexadecimal_digit Hexadecimal_digit Hexadecimal_digit;
 fragment Ls32          : ( H16 ':' H16 ) | IP;

MAC : Hexadecimal_digit Hexadecimal_digit ':'
	Hexadecimal_digit Hexadecimal_digit ':'
	Hexadecimal_digit Hexadecimal_digit ':'
	Hexadecimal_digit Hexadecimal_digit ':'
	Hexadecimal_digit Hexadecimal_digit ':'
	Hexadecimal_digit Hexadecimal_digit;
fragment LOWERCASE  :   'a'..'z';
fragment UNDERSCORE :   '_';
fragment DOLLAR :   '$';
fragment NUMBER :   '0'..'9';
EMPTY_ACTION_SPEC : 'EMPTY' | 'empty' | 'emptyEMPTY';
NAME:   (SINGLELETTER|UNDERSCORE) (SINGLELETTER|UNDERSCORE|NUMBER)*;

TABLE_SET: 'table_bla';
// Skip whitespaces
WS : [ \r\n\t]+ -> skip ;

