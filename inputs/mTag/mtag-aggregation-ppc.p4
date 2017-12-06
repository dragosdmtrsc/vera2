header_type ethernet_t {
    fields {
        dst_addr : 48;
        src_addr : 48;
        ethertype : 16;
    }
}
header_type vlan_t {
    fields {
        pcp : 3;
        cfi : 1;
        vid : 12;
        ethertype : 16;
    }
}
header_type mTag_t {
    fields {
        up1 : 8;
        up2 : 8;
        down1 : 8;
        down2 : 8;
        ethertype : 16;
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
        dstAddr : 32;
        options : *;
    }
    length : ihl * 4;
    max_length : 60;
}
header_type local_metadata_t {
    fields {
        cpu_code : 16;
        port_type : 4;
        ingress_error : 1;
        was_mtagged : 1;
        copy_to_cpu : 1;
        bad_packet : 1;
        color : 8;
    }
}
header ethernet_t ethernet;
header vlan_t vlan;
header mTag_t mtag;
header ipv4_t ipv4;
metadata local_metadata_t local_metadata;
parser start {
    return ethernet;
}
parser ethernet {
    extract(ethernet);
    return select(latest.ethertype) {
        0x8100: vlan;
        0x800: ipv4;
        default: ingress;
    }
}
parser vlan {
    extract(vlan);
    return select(latest.ethertype) {
        0xaaaa: mtag;
        0x800: ipv4;
        default: ingress;
    }
}
parser mtag {
    extract(mtag);
    return select(latest.ethertype) {
        0x800: ipv4;
        default: ingress;
    }
}
parser ipv4 {
    extract(ipv4);
    return ingress;
}
action common_copy_pkt_to_cpu(cpu_code, bad_packet) {
    modify_field(local_metadata.copy_to_cpu, 1);
    modify_field(local_metadata.cpu_code, cpu_code);
    modify_field(local_metadata.bad_packet, bad_packet);
}
action common_drop_pkt(do_copy, cpu_code, bad_packet) {
    modify_field(local_metadata.copy_to_cpu, do_copy);
    modify_field(local_metadata.cpu_code, cpu_code);
    modify_field(local_metadata.bad_packet, bad_packet);
    drop();
}
action common_set_port_type(port_type, ingress_error) {
    modify_field(local_metadata.port_type, port_type);
    modify_field(local_metadata.ingress_error, ingress_error);
}
table check_mtag {
    reads {
        mtag : valid;
    }
    actions {
        common_drop_pkt;
        common_copy_pkt_to_cpu;
        no_op;
    }
    size : 1;
}
table identify_port {
    reads {
        standard_metadata.ingress_port : exact;
    }
    actions {
        common_set_port_type;
        common_drop_pkt;
        no_op;
    }
    max_size : 64;
}
action use_mtag_up1() {
    modify_field(standard_metadata.egress_spec, mtag.up1);
}
action use_mtag_up2() {
    modify_field(standard_metadata.egress_spec, mtag.up2);
}
action use_mtag_down1() {
    modify_field(standard_metadata.egress_spec, mtag.down1);
}
action use_mtag_down2() {
    modify_field(standard_metadata.egress_spec, mtag.down2);
}
table select_output_port {
    reads {
        local_metadata.port_type : exact;
    }
    actions {
        use_mtag_up1;
        use_mtag_up2;
        use_mtag_down1;
        use_mtag_down2;
        no_op;
    }
    max_size : 4;
}
control ingress {
    apply(check_mtag);
    apply(identify_port);
    apply(select_output_port);
}
