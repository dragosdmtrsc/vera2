/**
 * Define a grammar called Hello
 */
grammar Iptables;
@header {
package generated.iptables_grammar;
}

table : (rle NL 
		| chain NL
		| policy NL
		| NL
		| EOF)* ; 
rle : '-A' chainname match* target;
chain : '-N' chainname;
policy : '-P' chainname targetName;

target : '-j' targetName;
targetName : (acceptTarget // TODO
	| dropTarget // TODO 
	| returnTarget // TODO
	| checksumTarget // TODO
	| connmarkTarget  // TODO
	| ctTarget // TODO 
	| dnatTarget // TODO
	| markTarget // TODO 
	| notrackTarget // TODO
	| redirectTarget // TODO
	| rejectTarget  // TODO 
	| setTarget // TODO
	| snatTarget // TODO
	| jumpyTarget
); 
 
jumpyTarget : NAME; 
acceptTarget : 'ACCEPT';
dropTarget : 'DROP' ;
returnTarget : 'RETURN' ;

checksumTarget : 'CHECKSUM'  checksumTargetOpts;
checksumTargetOpts : ('--checksum-fill')?;
connmarkTarget : 'CONNMARK'  connmarkTargetOpts*; 
connmarkTargetOpts : '--save-mark' maskingOption #saveCtMark
| '--restore-mark' maskingOption #restoreCtMark;
 
maskingOption : '--nfmask' nfCtMask #nfMask
| '--ctmask' nfCtMask #ctMask;

nfCtMask : ('0x' HEX_DIGIT+);
ctTarget : 'CT'  ctTargetOpts;
ctTargetOpts : (ctNotrack |
ctZone
 )*; 
  
ctZone : '--zone' id=INT;
ctNotrack : '--notrack';
 
event : 'new' 
| 'related' 
| 'destroy' 
| 'reply' 
| 'assured' 
| 'protoinfo' 
| 'helper' 
| 'mark' 
| 'natseqinfo' 
| 'secmark';
  
dnatTarget : 'DNAT'  '--to-destination' IP;
dnatTargetOpts : '--to-destination' IP;

markTarget : 'MARK'  '--set-xmark' INT('/' INT)?;
markTargetOpts : '--set-xmark' INT('/' INT)?;

notrackTarget : 'NOTRACK';
redirectTarget : 'REDIRECT'  '--to-ports' INT('-' INT)?;
//redirectTargetOpts : ('--to-ports' INT('-' INT)?);
rejectTarget : 'REJECT'  rejectTargetOpts;
rejectTargetOpts : '--reject-with' type=INT;
setTarget : 'SET'  setTargetOpts;
setTargetOpts : (
'--add-set' setname=NAME flag(',' flag)*
'--del-set' setname=NAME flag(',' flag)*
'--timeout' value=INT
'--exist')*;

snatTarget : 'SNAT'  '--to-source' IP;
snatTargetOpts : ('--to-source' );

chainname : NAME;
// parse match
match : (module // TODO
	| protocol  // OK
	| sourceaddr  // OK
	| dstaddr //OK
	| ipv4 // TODO : Optional
	| ipv6 // TODO : Optional
	| iniface // OK
	| outiface // OK
	| frgm // OK
	| ctrs // TODO : Optiona l
);
ipv4 : '-4';
ipv6 : '-6';

frgm : '!'? ('-f' | '--fragment');
ctrs : '!'? ('-c' | '--set-counters') packets=INT bytes=INT;

iniface : neg='!'?  ('-i' | '--in-interface') iface=NAME;
outiface : neg='!'? ('-o' | '--out-interface') iface=NAME;

module : '-m' (tcpopts // OK
	| udpopts // OK
	| icmpopts // OK
	| connmarkopts // FIXME: Reanalizeaza asta
	| markopts // FIXME: Reanalizeaza asta
	| conntrackopts // OK
	| commentopts // TODO: Optional, maybe unneeded
	| icmp6opts // TODO : Optional(for the moment)
	| macopts // OK
	| physdevopts // OK
	| setopts // TODO: Deffered - need also ipset parameters
	| stateopts // OK
); 

connmarkopts : 'connmark' neg='!'? '--mark' INT ('/' INT)?;
conntrackopts : 'conntrack' conntrackvars+;
conntrackvars : neg='!'? '--ctstate' statelist;

address : IP; 
mask : INT;

statuslist : status (',' status)*;
statelist : state (',' state)*;

markopts : 'mark' neg='!'? '--mark' INT ('/' INT)?;
commentopts : 'comment' '--comment' comment=NAME+;
icmp6opts : 'icmp6' '!'? '--icmpv6-type' type=INT ('/' code=INT)? |typename=NAME;
macopts : 'mac' neg='!'? '--mac-source' macaddress;
macaddress : HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT;
physdevopts: 'physdev' physdevvars+;  
physdevvars : neg='!'? '--physdev-in' NAME #physdevIn
| neg='!'? '--physdev-out' NAME #physdevOut
| neg='!'? '--physdev-is-bridged' #physdevIsBridged; 
setopts : 'set' setvars+;
setvars : neg='!'? '--match-set' setname=NAME flagset;
flagset : (flag (',' flag)*)?;
flag : NAME;

status : 'NONE'
| 'EXPECTED' 
| 'SEEN_REPLY'
| 'ASSURED'
| 'CONFIRMED';

state : 'INVALID'
| 'NEW' 
| 'ESTABLISHED'
| 'RELATED'
| 'UNTRACKED'
| 'SNAT' 
| 'DNAT';

stateopts : 'state' neg='!'? '--state' state;
udpopts : 'udp' (dport | sport)*;
// FIXME: icmp rtfd and add options 
icmpopts : 'icmp' neg='!'? '--icmp-type' icmptype; 

icmptype : INT #bareType
	| INT '/' INT #typeCode
	| NAME #codeName;
	 

 
tcpopts : 'tcp' (dport | sport)*;
dport  : neg='!'? '--dport' portno;
sport  : neg='!'? '--sport' portno;
portno : INT;
protocol : neg='!'? ('-p' | '--protocol') proto=('tcp' | 'udp' | 'icmp' | INT);

sourceaddr : neg='!'? ('-s' | '--source') addressExpression; 
addressExpression : IP_MASK # ipMasked
	| IP # ipNormal
	| NAME # hostName;
dstaddr : neg='!'? ('-d' | '--destination') addressExpression;
 
MATCH : '-m';

IP_MASK : IP '/' INT;
IP : INT '.' INT '.' INT '.' INT ;
INT : DIGIT+;
NAME : (ALPHA) (ALPHA | DIGIT)*;
WS : [ \t]+ -> skip ; // skip spaces, tabs, newlines
NL : '\r'? '\n';

fragment DIGIT : [0-9]; 
fragment ALPHA : UPPER | LOWER | SIGNS;
fragment LOWER : [a-z];
fragment UPPER : [A-Z];
fragment SIGNS : [-_];
HEX_DIGIT : [0-9A-Fa-f] ;
fragment OCT_DIGIT : [0-8] ;

