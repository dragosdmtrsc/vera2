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

parser start {
    return parse_ethernet;
}

header ethernet_t ethernet;

parser parse_ethernet {
    extract(ethernet);
    return select(latest.etherType) {
        0x800 : parse_ipv4;
        default: ingress;
    }
}

header ipv4_t ipv4;

parser parse_ipv4 {
    extract(ipv4);
    return select(ipv4.protocol) {
        0x06 : parse_tcp;
        default : ingress;
    }
}

header tcp_t tcp;

parser parse_tcp {
    extract(tcp);
    return ingress;
}

action drp() {
    drop();
}

action _nop() {
    modify_field(standard_metadata.egress_spec, 1);
    no_op();
}

table filter {
    reads {
        ipv4.dstAddr : range;
        ipv4.srcAddr : range;
        tcp.srcPort : range;
        tcp.srcPort : range;
    }
    actions {
        drp;
        _nop;
    }
    size: 1024;
}

table nop {
    reads {
        ipv4.dstAddr : range;
    }
    actions {
        _nop;
    }
    size: 1;
}

control ingress {
    apply(filter);
}

control egress {
    apply(nop);
}


