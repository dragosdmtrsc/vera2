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
header ethernet_t ethernet;
header ipv4_t ipv4;
header tcp_t tcp;
parser start {
    return parse_ethernet;
}
parser parse_ethernet {
    extract(ethernet);
    return select(latest.etherType) {
        0x0800 : parse_ipv4;
        default: ingress;
    }
}
parser parse_ipv4 {
    extract(ipv4);
    return select(ipv4.protocol) {
        0x06 : parse_tcp;
        default : ingress;
    }
}
parser parse_tcp {
    extract(tcp);
    return ingress;
}
action _drop() {
    drop();
}
action nat_miss_int_to_ext() {
    modify_field(standard_metadata.egress_spec, 3);
}
action nat_hit_int_to_ext(srcAddr, srcPort) {
    modify_field(ipv4.srcAddr, srcAddr);
    modify_field(tcp.srcPort, srcPort);
    modify_field(standard_metadata.egress_spec, 2);
}
action nat_hit_ext_to_int(dstAddr, dstPort) {
    modify_field(ipv4.dstAddr, dstAddr);
    modify_field(tcp.dstPort, dstPort);
    modify_field(standard_metadata.egress_spec, 1);
}
action nat_no_nat() {
}
table nat {
    reads {
        standard_metadata.ingress_port : exact;
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
        nat_hit_int_to_ext;
        nat_hit_ext_to_int;
        nat_no_nat;
    }
    size : 128;
}
control ingress {
    if (valid(ipv4) and valid(tcp)) {
        apply(nat);
    }
}
control egress {
}