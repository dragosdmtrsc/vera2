header_type ethernet_t {
    fields {
        dstAddr : 48;
        srcAddr : 48;
        etherType : 16;
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

header_type meta_t {
    fields {
        register_tmp : 32;
    }
}

metadata meta_t meta;

register my_register {
    width : 32;
    static : m_table;
    instance_count : 16384;
}
header ethernet_t ethernet;
metadata intrinsic_metadata_t intrinsic_metadata;


parser start {
    return parse_ethernet;
}

parser parse_ethernet {
    extract(ethernet);
    return ingress;
}

action _drop() {
    drop();
}

action _nop() {
}


action m_action(register_idx) {
    register_read(meta.register_tmp, my_register, register_idx);
}

table m_table {
    reads {
        ethernet.srcAddr : exact;
    }
    actions {
        m_action; _nop;
    }
    size : 16384;
}

control ingress {
    apply(m_table);
}

control egress {
}
