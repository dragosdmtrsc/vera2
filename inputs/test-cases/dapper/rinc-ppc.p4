header_type ethernet_t {
    fields {
        dstAddr : 48;
        srcAddr : 48;
        etherType : 16;
    }
}
header_type ipv4_t {
    fields {
        version : 4;
        ihl : 4;
        diffserv : 8;
        totalLen : 16;
        identification : 16;
        flags : 3;
        fragOffset : 13;
        ttl : 8;
        protocol : 8;
        hdrChecksum : 16;
        srcAddr : 32;
        dstAddr: 32;
    }
}
header_type tcp_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        seqNo : 32;
        ackNo : 32;
        dataOffset : 4;
        res : 3;
        ecn : 3;
        urg : 1;
 ack : 1;
 push : 1;
 rst : 1;
 syn : 1;
 fin : 1;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}
header_type options_end_t {
 fields {
  kind :8;
 }
}
header_type options_nop_t {
   fields {
 kind: 8;
    }
}
header_type options_mss_t {
   fields {
 kind : 8;
 len : 8;
 MSS : 16;
   }
}
header_type options_wscale_t {
   fields {
 kind : 8;
 len : 8;
 wscale : 8;
   }
}
header_type options_sack_t {
   fields {
 kind: 8;
 len : 8;
   }
}
header_type options_ts_t {
   fields {
 kind: 8;
 len : 8;
 ttee : 64;
   }
}
parser start {
    return parse_ethernet;
}
header ethernet_t ethernet;
parser parse_ethernet {
    extract(ethernet);
    return select(latest.etherType) {
        0x0800 : parse_ipv4;
        default: ingress;
    }
}
header ipv4_t ipv4;
field_list ipv4_checksum_list {
        ipv4.version;
        ipv4.ihl;
        ipv4.diffserv;
        ipv4.totalLen;
        ipv4.identification;
        ipv4.flags;
        ipv4.fragOffset;
        ipv4.ttl;
        ipv4.protocol;
        ipv4.srcAddr;
        ipv4.dstAddr;
}
field_list_calculation ipv4_checksum {
    input {
        ipv4_checksum_list;
    }
    algorithm : csum16;
    output_width : 16;
}
calculated_field ipv4.hdrChecksum {
    verify ipv4_checksum;
    update ipv4_checksum;
}
parser parse_ipv4 {
    extract(ipv4);
    return select(latest.protocol) {
        6 : parse_tcp;
        default: ingress;
    }
}
header tcp_t tcp;
header_type my_metadata_t {
    fields {
 parse_tcp_options_counter : 8;
    }
}
metadata my_metadata_t my_metadata;
parser parse_tcp {
    extract(tcp);
    set_metadata(my_metadata.parse_tcp_options_counter, tcp.dataOffset * 4 - 20);
    return select(latest.syn) {
     1: parse_tcp_options;
 default : ingress;
    }
}
parser parse_tcp_options {
 return select(my_metadata.parse_tcp_options_counter, current(0,8)) {
  0x0000 mask 0xff00 : ingress;
  0x0000 mask 0x00ff : parse_end;
  0x0001 mask 0x00ff : parse_nop;
  0x0002 mask 0x00ff : parse_mss;
  0x0003 mask 0x00ff : parse_wscale;
  0x0004 mask 0x00ff : parse_sack;
  0x0008 mask 0x00ff : parse_ts;
 }
}
header options_end_t options_end;
parser parse_end {
 extract(options_end);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-1);
 return parse_tcp_options;
}
header options_nop_t options_nop[3];
parser parse_nop {
 extract(options_nop[next]);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-1);
 return parse_tcp_options;
}
header options_mss_t options_mss;
parser parse_mss {
 extract(options_mss);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-4);
 return parse_tcp_options;
}
header options_wscale_t options_wscale;
parser parse_wscale {
 extract(options_wscale);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-3);
 return parse_tcp_options;
}
header options_sack_t options_sack;
parser parse_sack {
 extract(options_sack);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-2);
 return parse_tcp_options;
}
header options_ts_t options_ts;
parser parse_ts {
 extract(options_ts);
 set_metadata(my_metadata.parse_tcp_options_counter, my_metadata.parse_tcp_options_counter-10);
 return parse_tcp_options;
}
header_type intrinsic_metadata_t {
    fields {
        ingress_global_timestamp : 48;
        lf_field_list : 32;
        mcast_grp : 16;
        egress_rid : 16;
    }
}
metadata intrinsic_metadata_t intrinsic_metadata;
header_type stats_metadata_t {
    fields {
 dummy : 32;
 dummy2: 32;
 flow_map_index : 2;
 senderIP : 32;
 seqNo : 32;
 ackNo : 32;
 sample_rtt_seq : 32;
 rtt_samples : 32;
 mincwnd : 32;
 dupack : 32;
   }
}
metadata stats_metadata_t stats_metadata;
register check_map {
    width : 2;
    instance_count : 2;
}
field_list l3_hash_fields {
    ipv4.srcAddr;
    ipv4.dstAddr;
    ipv4.protocol;
    tcp.srcPort;
    tcp.dstPort;
}
field_list l3_hash_fields_reverse {
    ipv4.dstAddr;
    ipv4.srcAddr;
    ipv4.protocol;
    tcp.dstPort;
    tcp.srcPort;
}
field_list_calculation flow_map_hash {
    input {
        l3_hash_fields;
    }
    algorithm : crc32;
    output_width : 2;
}
field_list_calculation flow_map_hash_reverse {
    input {
        l3_hash_fields_reverse;
    }
    algorithm : crc32;
    output_width : 2;
}
action lookup_flow_map() {
    modify_field_with_hash_based_offset(stats_metadata.flow_map_index, 0, flow_map_hash, 2);
    register_write(check_map, 0, stats_metadata.flow_map_index);
}
action lookup_flow_map_reverse() {
    modify_field_with_hash_based_offset(stats_metadata.flow_map_index, 0, flow_map_hash_reverse, 2);
    register_write(check_map, 1, stats_metadata.flow_map_index);
}
table lookup{
 actions {
  lookup_flow_map;
 }
}
table lookup_reverse{
 actions {
  lookup_flow_map_reverse;
 }
}
register MSS {
    width : 16;
    instance_count : 4;
}
register wscale {
    width : 8;
    instance_count : 4;
}
register sendIP {
    width : 32;
    instance_count : 4;
}
action record_IP() {
 register_write(sendIP, stats_metadata.flow_map_index, ipv4.dstAddr);
 register_read(stats_metadata.senderIP, sendIP, stats_metadata.flow_map_index);
 register_write(MSS, stats_metadata.flow_map_index, options_mss.MSS);
 register_write(wscale, stats_metadata.flow_map_index, options_wscale.wscale);
 register_write(mincwnd, stats_metadata.flow_map_index, 14600);
}
table init{
 actions {
  record_IP;
 }
}
action get_sender_IP(){
 register_read(stats_metadata.senderIP, sendIP, stats_metadata.flow_map_index);
 register_read(stats_metadata.seqNo, flow_last_seq_sent, stats_metadata.flow_map_index);
 register_read(stats_metadata.ackNo, flow_last_ack_rcvd, stats_metadata.flow_map_index);
 register_read(stats_metadata.sample_rtt_seq, flow_rtt_sample_seq, stats_metadata.flow_map_index);
 register_read(stats_metadata.rtt_samples, rtt_samples, stats_metadata.flow_map_index);
 register_read(stats_metadata.mincwnd, mincwnd, stats_metadata.flow_map_index);
 register_read(stats_metadata.dupack, flow_pkts_dup, stats_metadata.flow_map_index);
}
table direction{
 actions {
 get_sender_IP;
 }
}
register mincwnd {
 width : 32;
 instance_count : 4;
}
register flight_size {
 width : 32;
 instance_count : 4;
}
action increase_mincwnd(){
 register_write(mincwnd, stats_metadata.flow_map_index, stats_metadata.dummy);
}
table increase_cwnd{
 actions{
  increase_mincwnd;
 }
}
register flow_rwnd{
    width : 16;
    instance_count : 4;
}
register flow_last_ack_rcvd {
    width : 32;
    instance_count : 4;
}
register flow_last_seq_sent {
    width : 32;
    instance_count : 4;
}
register flow_pkts_sent {
    width : 32;
    instance_count : 4;
}
register flow_pkts_rcvd {
    width : 32;
    instance_count : 4;
}
register flow_pkts_retx {
    width : 32;
    instance_count : 4;
}
register flow_pkts_dup {
    width : 32;
    instance_count : 4;
}
register ack_time{
 width: 32;
 instance_count : 4;
}
register app_reaction_time {
 width : 32;
 instance_count : 4;
}
action update_flow_sent() {
 register_read(stats_metadata.dummy, flow_pkts_sent, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(flow_pkts_sent, stats_metadata.flow_map_index, stats_metadata.dummy );
 register_write(flow_last_seq_sent, stats_metadata.flow_map_index, tcp.seqNo);
 modify_field(stats_metadata.dummy, intrinsic_metadata.ingress_global_timestamp);
 register_read(stats_metadata.dummy2, ack_time, stats_metadata.flow_map_index);
 subtract_from_field(stats_metadata.dummy, stats_metadata.dummy2 );
 register_write(app_reaction_time, stats_metadata.flow_map_index, stats_metadata.dummy );
 register_read(stats_metadata.dummy, flow_last_seq_sent, stats_metadata.flow_map_index);
 register_read(stats_metadata.dummy2, flow_last_ack_rcvd, stats_metadata.flow_map_index);
 subtract_from_field(stats_metadata.dummy, stats_metadata.dummy2);
 register_write(flight_size, stats_metadata.flow_map_index, stats_metadata.dummy);
}
action update_flow_rcvd() {
 register_read(stats_metadata.dummy, flow_pkts_rcvd, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(flow_pkts_rcvd, stats_metadata.flow_map_index, stats_metadata.dummy );
 register_write(flow_last_ack_rcvd, stats_metadata.flow_map_index, tcp.ackNo);
 register_write(flow_pkts_dup, stats_metadata.flow_map_index, 0);
 register_write(flow_rwnd, stats_metadata.flow_map_index, tcp.window );
 register_write(ack_time, stats_metadata.flow_map_index, intrinsic_metadata.ingress_global_timestamp);
}
action update_flow_retx_3dupack() {
 register_read(stats_metadata.dummy, flow_pkts_retx, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(flow_pkts_retx, stats_metadata.flow_map_index, stats_metadata.dummy );
 register_write(flow_rtt_sample_seq,stats_metadata.flow_map_index,0);
 register_write(flow_rtt_sample_time,stats_metadata.flow_map_index,0);
 register_read(stats_metadata.dummy, mincwnd, stats_metadata.flow_map_index);
 shift_left(stats_metadata.dummy, stats_metadata.dummy, 1);
 register_write(mincwnd, stats_metadata.flow_map_index,stats_metadata.dummy );
}
action update_flow_retx_timeout() {
 register_read(stats_metadata.dummy, flow_pkts_retx, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(flow_pkts_retx, stats_metadata.flow_map_index, stats_metadata.dummy );
 register_write(flow_rtt_sample_seq,stats_metadata.flow_map_index,0);
 register_write(flow_rtt_sample_time,stats_metadata.flow_map_index,0);
 register_write(mincwnd, stats_metadata.flow_map_index, 14600);
}
action update_flow_dupack() {
 register_read(stats_metadata.dummy, flow_pkts_dup, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(flow_pkts_dup, stats_metadata.flow_map_index, stats_metadata.dummy );
}
table flow_sent {
 actions {
  update_flow_sent;
 }
}
table flow_retx_3dupack {
 actions {
  update_flow_retx_3dupack;
 }
}
table flow_retx_timeout {
 actions {
  update_flow_retx_timeout;
 }
}
table flow_rcvd {
 actions {
  update_flow_rcvd;
 }
}
table flow_dupack{
 actions{
  update_flow_dupack;
 }
}
register flow_srtt{
    width : 32;
    instance_count : 4;
}
register rtt_samples{
    width : 32;
    instance_count : 4;
}
register flow_rtt_sample_seq{
    width : 32;
    instance_count : 4;
}
register flow_rtt_sample_time{
    width : 32;
    instance_count : 4;
}
action sample_new_rtt(){
 register_write(flow_rtt_sample_seq, stats_metadata.flow_map_index, tcp.seqNo);
 register_write(flow_rtt_sample_time, stats_metadata.flow_map_index, intrinsic_metadata.ingress_global_timestamp);
}
action use_sample_rtt(){
 register_read(stats_metadata.dummy, flow_rtt_sample_time , stats_metadata.flow_map_index);
 modify_field(stats_metadata.dummy2, intrinsic_metadata.ingress_global_timestamp);
 subtract_from_field(stats_metadata.dummy2, stats_metadata.dummy);
 register_write(flow_rtt_sample_seq, stats_metadata.flow_map_index,0);
 register_read(stats_metadata.dummy, flow_srtt, stats_metadata.flow_map_index);
 add(stats_metadata.dummy, stats_metadata.dummy, stats_metadata.dummy2);
 shift_right(stats_metadata.dummy, stats_metadata.dummy, 3);
 register_write(flow_srtt, stats_metadata.flow_map_index, stats_metadata.dummy);
 register_read(stats_metadata.dummy, rtt_samples, stats_metadata.flow_map_index);
 add_to_field(stats_metadata.dummy, 1);
 register_write(rtt_samples, stats_metadata.flow_map_index, stats_metadata.dummy);
}
action use_sample_rtt_first(){
 register_read(stats_metadata.dummy, flow_rtt_sample_time , stats_metadata.flow_map_index);
 modify_field(stats_metadata.dummy2, intrinsic_metadata.ingress_global_timestamp);
 subtract_from_field(stats_metadata.dummy2, stats_metadata.dummy);
 register_write(flow_rtt_sample_seq, stats_metadata.flow_map_index,0);
 register_write(flow_srtt, stats_metadata.flow_map_index, stats_metadata.dummy2);
 register_write(rtt_samples, stats_metadata.flow_map_index, 1);
}
table sample_rtt_sent{
 actions{
  sample_new_rtt;
 }
}
table sample_rtt_rcvd{
 actions{
  use_sample_rtt;
 }
}
table first_rtt_sample{
 actions{
  use_sample_rtt_first;
 }
}
action _drop() {
    drop();
}
header_type routing_metadata_t {
    fields {
        nhop_ipv4 : 32;
    }
}
metadata routing_metadata_t routing_metadata;
action set_nhop(nhop_ipv4, port) {
    modify_field(routing_metadata.nhop_ipv4, nhop_ipv4);
    modify_field(standard_metadata.egress_spec, port);
    add_to_field(ipv4.ttl, -1);
}
table ipv4_lpm {
    reads {
        ipv4.dstAddr : lpm;
    }
    actions {
        set_nhop;
        _drop;
    }
    size: 1024;
}
action set_dmac(dmac) {
    modify_field(ethernet.dstAddr, dmac);
}
table forward {
    reads {
        routing_metadata.nhop_ipv4 : exact;
    }
    actions {
        set_dmac;
        _drop;
    }
    size: 512;
}
action rewrite_mac(smac) {
    modify_field(ethernet.srcAddr, smac);
}
table send_frame {
    reads {
        standard_metadata.egress_port: exact;
    }
    actions {
        rewrite_mac;
        _drop;
    }
    size: 256;
}
register srcIP{
 width : 32;
 instance_count : 4;
}
register dstIP{
 width : 32;
 instance_count : 4;
}
register metaIP{
 width : 32;
 instance_count : 4;
}
action save_source_IP() {
 register_write(srcIP, stats_metadata.flow_map_index, ipv4.srcAddr);
 register_write(dstIP, stats_metadata.flow_map_index, ipv4.dstAddr);
 register_write(metaIP, stats_metadata.flow_map_index, stats_metadata.senderIP);
}
table debug {
 actions{
  save_source_IP;
 }
}
control ingress {
 if ( ipv4.protocol == 0x06) {
  if( ipv4.srcAddr > ipv4.dstAddr ) {
   apply(lookup);
  }else{
   apply(lookup_reverse);
  }
  if ( (tcp.syn == 1) and (tcp.ack == 0) ) {
   apply(init);
  }
  else{
   apply(direction);
  }
  if (ipv4.srcAddr == stats_metadata.senderIP){
   if( tcp.seqNo > stats_metadata.seqNo )
   {
    apply(flow_sent);
    if(stats_metadata.sample_rtt_seq == 0){
     apply(sample_rtt_sent);
    }
    if(stats_metadata.dummy > stats_metadata.mincwnd)
    {
     apply(increase_cwnd);
    }
   }else{
    if(stats_metadata.dupack == 3) {
     apply(flow_retx_3dupack);
    } else {
     apply(flow_retx_timeout);
    }
   }
  }
  else if(ipv4.dstAddr == stats_metadata.senderIP ) {
   if( tcp.ackNo > stats_metadata.ackNo ){
    apply(flow_rcvd);
    if( tcp.ackNo >= stats_metadata.sample_rtt_seq and stats_metadata.sample_rtt_seq>0){
     if(stats_metadata.rtt_samples ==0){
     apply(first_rtt_sample);
     }else{
     apply(sample_rtt_rcvd);
     }
    }
   }else{
    apply(flow_dupack);
   }
  }
  else{
   apply(debug);
  }
 }
 apply(ipv4_lpm);
        apply(forward);
}
control egress {
    apply(send_frame);
}
