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
        ether_src : 48;
        ether_dst : 48;
        nhop_ipv4 : 32;
        if_ipv4_addr : 32;
        if_mac_addr : 48;
        is_ext_if : 1;
        tcpLength : 16;
        if_index : 8;

    }
}
header ethernet_t ethernet;
header ipv4_t ipv4;
header tcp_t tcp;
metadata meta_t meta;
parser start {
    return parse_ethernet;
}
parser parse_ethernet {
    extract(ethernet);
    set_metadata(meta.ether_src, ethernet.srcAddr);
    set_metadata(meta.ether_dst, ethernet.dstAddr);
    return select(ethernet.etherType) {
        0x0800 : parse_ipv4;
        default: ingress;
    }
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
action _drop() {
    drop();
}
action _nop() {
}
action reverse() {
    modify_field(ethernet.srcAddr, meta.ether_dst);
    modify_field(ethernet.dstAddr, meta.ether_src);
    modify_field(ipv4.srcAddr, meta.ipv4_da);
    modify_field(ipv4.dstAddr, meta.ipv4_sa);
    modify_field(tcp.srcPort, meta.tcp_dp);
    modify_field(tcp.dstPort, meta.tcp_sp);
    modify_field(standard_metadata.egress_spec, 1);
}
table nop {
    reads { ethernet : valid; }
    actions { _nop; }
}
table reverse {
    reads { ethernet : valid; ipv4 : valid; tcp : valid; }
    actions { _drop; reverse; }
}
control ingress {
    apply(reverse);
}
control egress {
}