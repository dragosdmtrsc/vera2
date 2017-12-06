grammar OpenFlowGrammar;

@header {
package generated.parse.openflow;
}

HEADLINE : 'NXST_FLOW' .*? NL -> skip;
flows : HEADLINE? flow* EOF;
flow : matches NL;
matches : match? (match)*;


matchField : varName (EQUALS value ('/' value)?)?;

varName : fieldName ('[' (NUMBER '..' NUMBER)? ']')?;

fieldName : 'in_port'
   | 'dl_vlan'
   | 'dl_vlan_pcp'
   | 'dl_src'
   | 'dl_dst'
   | 'dl_type'
   | 'nw_src'
   | 'nw_dst'
   | 'nw_proto'
   | 'nw_tos'
   | 'nw_ecn'
   | 'nw_ttl'
   | 'tp_src'
   | 'tp_dst'
   | 'icmp_type'
   | 'icmp_code'
   | 'metadata'
   | 'ip'
   | 'icmp'
   | 'tcp'
   | 'udp'
   | 'sctp'
   | 'arp'
   | 'rarp'
   | 'vlan_tci'
   | 'ip_frag'
   | 'arp_sha'
   | 'arp_tha'
   | 'arp_spa'
   | 'ipv6_src'
   | 'ipv6_dst'
   | 'ipv6_label'
   | 'nd_target'
   | 'nd_sll'
   | 'nd_tll'
   | 'tun_id'
   | 'tun_src'
   | 'tun_dst'
   | 'regidx'
   | 'pkt_mark'
   | 'ipv6'
   | 'tcp6'
   | 'udp6'
   | 'sctp6'
   | 'icmp6'
   | 'send_flow_rem'
   | 'check_overlap'
   | 'out_port'
   | nxm_reg;

value : MAC | IP | NUMBER | IP6 | varName;

flowMetadata :    'idle_timeout' EQUALS NUMBER   #idle_timeout
   | 'hard_timeout' EQUALS NUMBER   #hard_timeout
//   | 'send_flow_rem'   #send_flow_rem
//   | 'check_overlap'   #check_overlap
   | 'table' EQUALS NUMBER #table
   | 'cookie' EQUALS NUMBER #cookie
   | 'priority' EQUALS NUMBER  #priority
   | 'duration' EQUALS Seconds   #duration
   | 'n_packets' EQUALS NUMBER   #n_packets
   | 'n_bytes'  EQUALS NUMBER  #n_bytes
   | 'hard_age' EQUALS NUMBER   #hard_age
   | 'idle_age' EQUALS NUMBER   #idle_age;

match : matchField
   | flowMetadata
   | action;

action: 'actions' EQUALS actionset;
actionset : target? (target)*;


Seconds : INT '.' INT 's';

ipv6 : NUMBER ':' NUMBER ':' NUMBER ':' NUMBER ':'
NUMBER ':' NUMBER ':' NUMBER ':' NUMBER;

ctrlParam : 'max_len' EQUALS NUMBER #maxLenParam
| 'reason' EQUALS reason #reasonParam
| 'id' EQUALS NUMBER #controllerIdParam;
 reason : 'action' | 'no_match' | 'invalid_ttl';

port : 'LOCAL' | 'CONTROLLER' | 'ALL' | 'NORMAL' | 'FLOOD' | NUMBER;

nxm_reg :
'NXM_OF_IN_PORT' #nxInPort
| 'NXM_OF_ETH_DST' #ethDst
| 'NXM_OF_ETH_SRC' #ethSrc
| 'NXM_OF_ETH_TYPE' #ethType
| 'NXM_OF_VLAN_TCI' #vlanTci
| 'NXM_OF_IP_TOS' #ipTos
| 'NXM_OF_IP_PROTO' #ipProto
| 'NXM_OF_IP_SRC' #ipSrc
| 'NXM_OF_IP_DST' #ipDst
| 'NXM_OF_TCP_SRC' #tcpSrc
| 'NXM_OF_TCP_DST' #tcpDst
| 'NXM_OF_UDP_SRC' #udpSrc
| 'NXM_OF_UDP_DST' #udpDst
| 'NXM_OF_ICMP_TYPE' #icmpType
| 'NXM_OF_ICMP_CODE' #icmpCode
| 'NXM_OF_ARP_OP' #arpOp
| 'NXM_OF_ARP_SPA' #arpSpa
| 'NXM_OF_ARP_TPA' #arpTpa
| 'NXM_NX_TUN_ID' #tunId
| 'NXM_NX_ARP_SHA' #arpSha
| 'NXM_NX_ARP_THA' #arpTha
| 'NXM_NX_ICMPV6_TYPE' #icmp6Type
| 'NXM_NX_ICMPV6_CODE' #icmp6Code
| 'NXM_NX_ND_SLL' #ndSll
| 'NXM_NX_ND_TLL' #ndTll
| 'NXM_OF_VLAN_TCI' #vlanTci
| 'NXM_NX_REG' NUMBER  #nxRegIdx;//for idx in the switch's accepted range.

target : 'output:' port #outputPort
|  'output:' fieldName '[' (NUMBER '..' NUMBER)? ']' #outputReg
|  'enqueue:' port ':' NAME #enqueue
|  'normal' #normal
|  'flood' #flood
|  'all' #all
|  'controller('ctrlParam? (ctrlParam)* ')' #controllerWithParams
|  'controller' #controller
|  'controller:' NUMBER? #controllerWithId
|  'local' #local
|  'in_port' #inPort
|  'drop' #drop
|  'mod_vlan_vid:' NUMBER #modVlanVid
|  'mod_vlan_pcp:' NUMBER #modVlanPcp
|  'strip_vlan' #stripVlan
|  'push_vlan:' NUMBER #pushVlan
|  'push_mpls:' NUMBER #pushMpls
|  'pop_mpls:' NUMBER #popMpls
|  'mod_dl_src:' MAC #setDlSrc
|  'mod_dl_dst:' MAC #setDlDst
|  'mod_nw_src:' IP #setNwSrc
|  'mod_nw_dst:' IP #setNwDst
|  'mod_tp_src:' NUMBER #setTpSrc
|  'mod_tp_dst:' NUMBER #setTpDst
|  'mod_nw_tos:' NUMBER #setNwTos
|  'resubmit:' NUMBER #resubmit
|  'resubmit(,' NUMBER ')' #resubmitSecond
|  'resubmit(' port NUMBER ')' #resubmitTable
|  'set_tunnel:' NUMBER #setTunnel
|  'set_tunnel64:' NUMBER #setTunnel64
|  'set_queue:' NUMBER #setQueue
|  'pop_queue' #popQueue
|  'dec_ttl' #decTTL
|  'dec_ttl' ('(' NUMBER NUMBER ')')? #decTTLWithParams
|  'set_mpls_ttl:' NUMBER #setMplsTTL
|  'dec_mpls_ttl' #decMplsTTL
|  'move:' fieldName '[' (NUMBER '..' NUMBER)? ']' '->' fieldName '[' NUMBER '..' NUMBER ']' #move
|  'load:' value '->' varName #load
|  'push:' fieldName '[' (NUMBER '..' NUMBER)? ']' #push
|  'pop:' fieldName '[' (NUMBER '..' NUMBER)? ']' #pop
|  'set_field:' NAME '->' fieldName ('[' (NUMBER '..' NUMBER)? ']')? #setField
|  'apply_actions(' actionset ')' #applyActions
|  'clear_actions' #clearActions
|  'write_metadata:' NUMBER ('/' NUMBER)? #writeMetadata
|  'goto_table:' NUMBER #goto
|  'fin_timeout('timeoutParam timeoutParam ')' #finTimeout
|  'sample(' sampleParam+ ')' #sample
|  'learn' '(' (match | argument | flowMetadata | target)*  ')'  #learn
|  'exit' #exit
|  port #forwardToPortTarget
;


argument : 'fin_idle_timeout' EQUALS NUMBER #learnFinIdleTo
| 'fin_hard_timeout' EQUALS NUMBER #learnFinHardTo
| varName EQUALS value #learnAssign
| nxm_reg '[' (NUMBER '..' NUMBER)? ']' #learnAssignSelf;
field : NAME;

timeoutParam : 'idle_timeout' EQUALS NUMBER #idleTimeoutParam
| 'hard_timeout' EQUALS NUMBER #hardTimeoutParam;

sampleParam : 'probability' EQUALS NUMBER #probabilityParam
| 'collector_set_id' EQUALS NUMBER #collectorSetParam
| 'obs_domain_id' EQUALS NUMBER #obsDomainParam
| 'obs_point_id' EQUALS NUMBER #obsPointParam;


frag_type : 'no'   #noFrag //Matches only non-fragmented packets.
| 'yes'  #yesFrag  //Matches all fragments.
| 'first'  #firstFrag //Matches only fragments with offset 0.
| 'later' #laterFrag //Matches only fragments with nonzero offset.
| 'not_later' #notLaterFrag;



mask : INT | IP;
IP : INT '.' INT '.' INT '.' INT;

IP6   :                             ( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |                       '::' ( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |                H16?  '::' ( H16 ':'  H16 ':'  H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':' )? H16)?  '::' ( H16 ':'  H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':'  H16 ':' )? H16)?  '::' ( H16 ':'  H16 ':' ) Ls32
                |  (( H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'    H16 ':'   Ls32
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'              Ls32
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::'              H16
                |  (( H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':'  H16 ':' )? H16)?  '::';

 fragment H16           : HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
 fragment Ls32          : ( H16 ':' H16 ) | IP;

MAC : HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT ':'
	HEX_DIGIT HEX_DIGIT;
NUMBER : INT | ('0x' HEX_DIGIT+);
INT : DIGIT+;
NAME : (ALPHA) (ALPHA | DIGIT)*;
WS : [, \t\r\n]+ -> skip ;
NL : '\r'? '\n';
EQUALS : '=';
fragment DIGIT : [0-9];
fragment ALPHA : UPPER | LOWER | SIGNS;
fragment LOWER : [a-z];
fragment UPPER : [A-Z];
fragment SIGNS : [-_];
HEX_DIGIT : [0-9A-Fa-f];
fragment OCT_DIGIT : [0-8] ;