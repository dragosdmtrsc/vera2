header_type axon_head_t {
    fields {
        preamble : 64;
        axonType : 8;
        axonLength : 16;
        fwdHopCount : 8;
        revHopCount : 8;
    }
}
header_type axon_hop_t {
    fields {
        port : 8;
    }
}
header_type my_metadata_t {
    fields {
        fwdHopCount : 8;
        revHopCount : 8;
        headerLen : 16;
    }
}
header axon_head_t axon_head;
header axon_hop_t axon_fwdHop[64];
header axon_hop_t axon_revHop[64];
metadata my_metadata_t my_metadata;
parser start {
    return select(current(0, 64)) {
        0: parse_head;
        default: ingress;
    }
}
parser parse_head {
    extract(axon_head);
    set_metadata(my_metadata.fwdHopCount, latest.fwdHopCount);
    set_metadata(my_metadata.revHopCount, latest.revHopCount);
    set_metadata(my_metadata.headerLen, 2 + axon_head.fwdHopCount + axon_head.revHopCount);
    return select(latest.fwdHopCount) {
        0: ingress;
        default: parse_next_fwdHop;
    }
}
parser parse_next_fwdHop {
    return select(my_metadata.fwdHopCount) {
        0x0 : parse_next_revHop;
        default : parse_fwdHop;
    }
}
parser parse_fwdHop {
    extract(axon_fwdHop[next]);
    set_metadata(my_metadata.fwdHopCount,
                 my_metadata.fwdHopCount - 1);
    return parse_next_fwdHop;
}
parser parse_next_revHop {
    return select(my_metadata.revHopCount) {
        0x0 : ingress;
        default : parse_revHop;
    }
}
parser parse_revHop {
    extract(axon_revHop[next]);
    set_metadata(my_metadata.revHopCount,
                 my_metadata.revHopCount - 1);
    return parse_next_revHop;
}
action _drop() {
    drop();
}
action route() {
    modify_field(standard_metadata.egress_spec, axon_fwdHop[0].port);
    subtract(axon_head.fwdHopCount, axon_head.fwdHopCount, 1);
    pop(axon_fwdHop, 1);
    add(axon_head.revHopCount, axon_head.revHopCount, 1);
    push(axon_revHop, 1);
    modify_field(axon_revHop[0].port, standard_metadata.ingress_port);
}
table drop_pkt {
    actions {
        _drop;
    }
    size: 1;
}
table route_pkt {
    reads {
        axon_head: valid;
        axon_fwdHop[0]: valid;
    }
    actions {
        _drop;
        route;
    }
    size: 1;
}
control ingress {
    if (axon_head.axonLength != my_metadata.headerLen) {
        apply(drop_pkt);
    }
    else {
        apply(route_pkt);
    }
}
control egress {
}
