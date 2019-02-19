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
action _strip_mtag() {
    remove_header(mtag);
    modify_field(local_metadata.was_mtagged, 1);
}
table strip_mtag {
    reads {
        mtag : valid;
    }
    actions {
        _strip_mtag;
        no_op;
    }
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
action set_egress(egress_spec) {
    modify_field(standard_metadata.egress_spec, egress_spec);
}
table local_switching {
    reads {
        vlan.vid : exact;
        ipv4.dstAddr : exact;
    }
    actions {
        set_egress;
        no_op;
    }
}
action add_mTag(up1, up2, down1, down2) {
    add_header(mtag);
    modify_field(mtag.ethertype, vlan.ethertype);
    modify_field(vlan.ethertype, 0xaaaa);
    modify_field(mtag.up1, up1);
    modify_field(mtag.up2, up2);
    modify_field(mtag.down1, down1);
    modify_field(mtag.down2, down2);
    modify_field(standard_metadata.egress_spec, up1);
}
counter pkts_by_dest {
    type : packets;
    direct : mTag_table;
}
counter bytes_by_dest {
    type : bytes;
    direct : mTag_table;
}
table mTag_table {
    reads {
        ethernet.dst_addr : exact;
        vlan.vid : exact;
    }
    actions {
        add_mTag;
        common_copy_pkt_to_cpu;
        no_op;
    }
    max_size : 20000;
}
table egress_check {
    reads {
        standard_metadata.ingress_port : exact;
        local_metadata.was_mtagged : exact;
    }
    actions {
        common_drop_pkt;
        no_op;
    }
    max_size : 64;
}
meter per_dest_by_source {
    type : bytes;
    result : local_metadata.color;
    instance_count : 64 * 64;
}
action meter_pkt(meter_idx) {
    meter(per_dest_by_source, meter_idx, local_metadata.color);
}
table egress_meter {
    reads {
        standard_metadata.ingress_port : exact;
        mtag.up1 : exact;
    }
    actions {
        meter_pkt;
        no_op;
    }
    size : 64 * 64;
}
counter per_color_drops {
    type : packets;
    direct : meter_policy;
}
table meter_policy {
    reads {
        local_metadata.color : exact;
    }
    actions {
        drop;
        no_op;
    }
}
control ingress {
    apply(strip_mtag);
    apply(identify_port);
    if (local_metadata.ingress_error == 0) {
        apply(local_switching);
        if (standard_metadata.egress_spec == 0) {
            apply(mTag_table);
        }
     }
}
control egress {
    apply(egress_check);
    apply(egress_meter) {
        hit {
            apply(meter_policy);
        }
    }
}
