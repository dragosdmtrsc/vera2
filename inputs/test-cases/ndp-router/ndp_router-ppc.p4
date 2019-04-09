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
header_type ndp_t {
    fields {
        flags : 16;
        checksum : 16;
        sport : 32;
        dport : 32;
        seqpull : 32;
        pacerecho : 32;
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
header ndp_t ndp;
register buffersense {
    width:16;
    static : forward;
    instance_count : 4;
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
    return select(latest.protocol) {
        199 : parse_ndp;
        default: ingress;
    }
}
parser parse_ndp {
    extract(ndp);
    return ingress;
}
action _drop() {
    drop();
}
header_type routing_metadata_t {
    fields {
        nhop_ipv4 : 32;
    }
}
header_type meta_t {
    fields {
        register_tmp : 16;
        ndpflags : 16;
        egress_priority : 16;
    }
}
metadata meta_t meta;
metadata routing_metadata_t routing_metadata;
action set_nhop(nhop_ipv4, port) {
    modify_field(routing_metadata.nhop_ipv4, nhop_ipv4);
    modify_field(standard_metadata.egress_port, port);
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
    register_read(meta.register_tmp, buffersense, standard_metadata.egress_port);
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
action decreasereg() {
    register_read(meta.register_tmp,
                    buffersense,
                    standard_metadata.egress_port);
    subtract_from_field(meta.register_tmp, 1);
    add_to_field(meta.register_tmp, meta.egress_priority);
    register_write(buffersense,
        standard_metadata.egress_port,
        meta.register_tmp);
}
action cont() {
}
table dec_counter {
    reads {
        meta.ndpflags: range;
    }
    actions {
        decreasereg;
        cont;
    }
    size: 2;
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
action setpriolow() {
    modify_field(meta.egress_priority, 0);
    register_read(meta.register_tmp, buffersense, standard_metadata.egress_port);
    add_to_field(meta.register_tmp, 1);
    register_write(buffersense, standard_metadata.egress_port, meta.register_tmp);
}
action setpriohigh() {
    truncate(67);
    modify_field(meta.egress_priority, 1);
}
action directpriohigh() {
    modify_field(meta.egress_priority, 1);
    modify_field(meta.ndpflags, ndp.flags);
}
table directtoprio {
    reads {
        meta.register_tmp : range;
    }
    actions{
        directpriohigh;
    }
    size:2;
}
table setprio {
    reads {
        meta.register_tmp : range;
    }
    actions {
        setpriolow;
        setpriohigh;
    }
    size:2;
}
control ingress {
    if(valid(ipv4) and ipv4.ttl > 0) {
        apply(ipv4_lpm);
        if (valid(ndp) and (ndp.flags > 1))
        {
            apply(directtoprio);
        }
        else
        {
            apply(setprio);
        }
        apply(forward);
    }
}
control egress {
    apply(dec_counter);
    apply(send_frame);
}
