grammar Openflow;
@header {
package generated.openflow_grammar;
}


flow : matches actions?;
matches : match? (',' match)*;
match : ;
actions : 'actions=' actionset;
actionset : target? (',' target)*;
target : ;
table : ;

mask : INT | ip;
ip : INT '.' INT '.' INT '.' INT;
mac : HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT;
NUMBER : INT | '0x' HEX_DIGIT+;
INT : DIGIT+;
NAME : (ALPHA) (ALPHA | DIGIT)* {System.out.println("NAME");};
WS : [ \t]+ -> skip ; // skip spaces, tabs, newlines
NL : '\r'? '\n';

fragment DIGIT : [0-9]; 
fragment ALPHA : UPPER | LOWER | SIGNS;
fragment LOWER : [a-z];
fragment UPPER : [A-Z];
fragment SIGNS : [-_];
fragment HEX_DIGIT : [0-9A-Fa-f] ;
fragment OCT_DIGIT : [0-8] ;