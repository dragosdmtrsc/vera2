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
header_type cpu_header_t {
    fields {
        preamble: 96;
        device: 8;
        reason: 8;
        if_index: 8;
    }
}
header_type tcp_t {
    fields {
        srcPort : 16;
        dstPort : 16;
        seqNo : 32;
        ackNo : 32;
        dataOffset : 4;
        res : 4;
        flags : 8;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}
header_type meta_t {
    fields {
        do_forward : 1;
        ipv4_sa : 32;
        ipv4_da : 32;
        tcp_sp : 16;
        tcp_dp : 16;
        nhop_ipv4 : 32;
        if_ipv4_addr : 32;
        if_mac_addr : 48;
        is_ext_if : 1;
        tcpLength : 16;
        if_index : 8;
    }
}
header_type intrinsic_metadata_t {
    fields {
        mcast_grp : 4;
        egress_rid : 4;
        mcast_hash : 16;
        lf_field_list: 32;
    }
}
header ethernet_t ethernet;
header cpu_header_t cpu_header;
header ipv4_t ipv4;
header tcp_t tcp;
metadata intrinsic_metadata_t intrinsic_metadata;
metadata meta_t meta;
parser start {
    set_metadata(meta.if_index, standard_metadata.ingress_port);
    return select(current(0, 96)) {
        0 : parse_cpu_header;
        default: parse_ethernet;
    }
}
parser parse_cpu_header {
    extract(cpu_header);
    set_metadata(meta.if_index, cpu_header.if_index);
    return parse_ethernet;
}
parser parse_ethernet {
    extract(ethernet);
    return select(latest.etherType) {
        0x0800 : parse_ipv4;
        default: ingress;
    }
}
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
    set_metadata(meta.ipv4_sa, ipv4.srcAddr);
    set_metadata(meta.ipv4_da, ipv4.dstAddr);
    set_metadata(meta.tcpLength, ipv4.totalLen);
    return select(ipv4.protocol) {
        0x06 : parse_tcp;
        default : ingress;
    }
}
parser parse_tcp {
    extract(tcp);
    set_metadata(meta.tcp_sp, tcp.srcPort);
    set_metadata(meta.tcp_dp, tcp.dstPort);
    return ingress;
}
field_list tcp_checksum_list {
        ipv4.srcAddr;
        ipv4.dstAddr;
        8'0;
        ipv4.protocol;
        meta.tcpLength;
        tcp.srcPort;
        tcp.dstPort;
        tcp.seqNo;
        tcp.ackNo;
        tcp.dataOffset;
        tcp.res;
        tcp.flags;
        tcp.window;
        tcp.urgentPtr;
        payload;
}
field_list_calculation tcp_checksum {
    input {
        tcp_checksum_list;
    }
    algorithm : csum16;
    output_width : 16;
}
calculated_field tcp.checksum {
    verify tcp_checksum if(valid(tcp));
    update tcp_checksum if(valid(tcp));
}
action _drop() {
    drop();
}
action set_if_info(ipv4_addr, mac_addr, is_ext) {
    modify_field(meta.if_ipv4_addr, ipv4_addr);
    modify_field(meta.if_mac_addr, mac_addr);
    modify_field(meta.is_ext_if, is_ext);
}
table if_info {
    reads {
        meta.if_index : exact;
    }
    actions {
        _drop;
        set_if_info;
    }
}
action nat_miss_ext_to_int() {
    modify_field(meta.do_forward, 0);
    drop();
}
field_list copy_to_cpu_fields {
    standard_metadata;
}
action nat_miss_int_to_ext() {
    clone_ingress_pkt_to_egress(250, copy_to_cpu_fields);
}
action nat_hit_int_to_ext(srcAddr, srcPort) {
    modify_field(meta.do_forward, 1);
    modify_field(meta.ipv4_sa, srcAddr);
    modify_field(meta.tcp_sp, srcPort);
}
action nat_hit_ext_to_int(dstAddr, dstPort) {
    modify_field(meta.do_forward, 1);
    modify_field(meta.ipv4_da, dstAddr);
    modify_field(meta.tcp_dp, dstPort);
}
action nat_no_nat() {
    modify_field(meta.do_forward, 1);
}
table nat {
    reads {
        meta.is_ext_if : exact;
        ipv4 : valid;
        tcp : valid;
        ipv4.srcAddr : ternary;
        ipv4.dstAddr : ternary;
        tcp.srcPort : ternary;
        tcp.dstPort : ternary;
    }
    actions {
        _drop;
        nat_miss_int_to_ext;
        nat_miss_ext_to_int;
        nat_hit_int_to_ext;
        nat_hit_ext_to_int;
        nat_no_nat;
    }
    size : 128;
}
action set_nhop(nhop_ipv4, port) {
    modify_field(meta.nhop_ipv4, nhop_ipv4);
    modify_field(standard_metadata.egress_spec, port);
    subtract_from_field(ipv4.ttl, 1);
}
table ipv4_lpm {
    reads {
        meta.ipv4_da : lpm;
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
        meta.nhop_ipv4 : exact;
    }
    actions {
        set_dmac;
        _drop;
    }
    size: 512;
}
action do_rewrites(smac) {
    remove_header(cpu_header);
    modify_field(ethernet.srcAddr, smac);
    modify_field(ipv4.srcAddr, meta.ipv4_sa);
    modify_field(ipv4.dstAddr, meta.ipv4_da);
    modify_field(tcp.srcPort, meta.tcp_sp);
    modify_field(tcp.dstPort, meta.tcp_dp);
}
table send_frame {
    reads {
        standard_metadata.egress_port: exact;
    }
    actions {
        do_rewrites;
        _drop;
    }
    size: 256;
}
action do_cpu_encap() {
    add_header(cpu_header);
    modify_field(cpu_header.preamble, 0);
    modify_field(cpu_header.device, 0);
    modify_field(cpu_header.reason, 0xab);
    modify_field(cpu_header.if_index, meta.if_index);
}
table send_to_cpu {
    actions { do_cpu_encap; }
    size : 0;
}
control ingress {
    apply(if_info);
    apply(nat);
    if (meta.do_forward == 1 and ipv4.ttl > 0) {
        apply(ipv4_lpm);
        apply(forward);
    }
}
control egress {
    if (standard_metadata.instance_type == 0) {
        apply(send_frame);
    } else {
        apply(send_to_cpu);
    }
}
