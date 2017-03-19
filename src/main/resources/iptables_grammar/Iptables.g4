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
		| NL)*;
rle : '-A' chainname match* target;
chain : '-N' chainname;
policy : '-P' chainname targetName;

target : '-j' targetName;
targetName : (acceptTarget
	| dropTarget
	| queueTarget
	| returnTarget
	| checksumTarget
	| connmarkTarget
	| ctTarget
	| dnatTarget
	| markTarget
	| masqueradeTarget
	| notrackTarget
	| redirectTarget
	| rejectTarget 
	| setTarget
	| snatTarget
);

acceptTarget : 'ACCEPT';
dropTarget : 'DROP' ;
queueTarget : 'QUEUE' ;
returnTarget : 'RETURN' ;
checksumTarget : 'CHECKSUM'  checksumTargetOpts;
checksumTargetOpts : ('--checksum-fill')?;
connmarkTarget : 'CONNMARK'  connmarkTargetOpts;
connmarkTargetOpts : ('--set-xmark' value=INT('/' cmask=INT)?
| '--save-mark' ('--nfmask' nfCtMask | '--ctmask' nfCtMask)*
| '--restore-mark' ('--nfmask' nfCtMask | '--ctmask' nfCtMask)*
| '--and-mark' bits=nfCtMask
| '--or-mark' bits=nfCtMask
| '--xor-mark' bits=nfCtMask
| '--set-mark' value=INT('/' cmask=INT)?
| '--save-mark' ('--mask' cmask=INT)?
| '--restore-mark' ('--mask' cmask=INT)?)*;

nfCtMask : ('0x' HEX_DIGIT+);
ctTarget : 'CT'  ctTargetOpts;
ctTargetOpts : ('--notrack'
| '--helper' name=NAME
| '--ctevents' event*
| '--expevents' event*
| '--zone' id=INT
| '--timeout' name=NAME
 )*;
 
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
 
dnatTarget : 'DNAT'  dnatTargetOpts;
dnatTargetOpts : (
'--to-destination' (fromip=address ('-' toip=address)?)?(':' fromport=INT('-' toport=INT)?)?
| '--random'
| '--persistent')*;

markTarget : 'MARK'  markTargetOpts;
markTargetOpts : ('--set-xmark' value=INT('/' cmask=INT)?
| '--set-mark' value=INT('/' cmask=INT)?
| '--and-mark' bits=nfCtMask
| '--or-mark' bits=nfCtMask
| '--xor-mark' bits=nfCtMask);
masqueradeTarget : 'MASQUERADE'  masqueradeTargetOpts;
masqueradeTargetOpts : ('--to-ports' startPort=INT('-' endPort=INT)?
| '--random')*;
notrackTarget : 'NOTRACK';
redirectTarget : 'REDIRECT'  redirectTargetOpts;
redirectTargetOpts : ('--to-ports' startPort=INT('-' endPort=INT)?
| '--random');
rejectTarget : 'REJECT'  rejectTargetOpts;
rejectTargetOpts : '--reject-with' type=INT;
setTarget : 'SET'  setTargetOpts;
setTargetOpts : (
'--add-set' setname=NAME flag(',' flag)*
'--del-set' setname=NAME flag(',' flag)*
'--timeout' value=INT
'--exist')*;

snatTarget : 'SNAT'  snatTargetOpts;
snatTargetOpts : ('--to-source' (address ('-' address)?)?(':' fromport=INT('-' toport=INT)?)?
| '--random'
| '--persistent')*;

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
	| icmpopts // TODO : Do this please
	| connmarkopts // FIXME: Reanalizeaza asta
	| markopts // FIXME: Reanalizeaza asta
	| conntrackopts // TODO
	| commentopts // TODO
	| icmp6opts // TODO
	| macopts // TODO
	| physdevopts // TODO
	| setopts // TODO
	| stateopts // TODO
); 

connmarkopts : 'connmark' connmarkvars;
connmarkvars : not='!'? '--mark' value=INT '/' mask;

conntrackopts : 'conntrack' conntrackvars+;
conntrackvars : '!'? '--ctstate' statelist;

address : IP;
mask : INT;

statuslist : status (',' status)*;
statelist : state (',' state)*;

markopts : 'mark' '!'? '--mark' value=INT ('/' mask)?;
commentopts : 'comment' '--comment' comment=NAME+;
icmp6opts : 'icmp6' '!'? '--icmpv6-type' type=INT ('/' code=INT)? |typename=NAME;
macopts : 'mac' '!'? '--mac-source' macaddress;
macaddress : HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT ':' 
	HEX_DIGIT HEX_DIGIT;
physdevopts: 'physdev' physdevvars+;
physdevvars : '!'? '--physdev-in' name=NAME
| '!'? '--physdev-out' name=NAME
| '!'? '--physdev-is-bridged';
setopts : 'set' setvars+;
setvars : '!'? '--match-set' setname=NAME flagset;
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

stateopts : 'state' '!'? '--state' state;
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
IP : INT '.' INT '.' INT '.' INT {System.out.println("IP");};
INT : DIGIT+ {System.out.println("INT");};
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

