header_type local_metadata_t {
    fields {
        cpu_code : 16;
        port_type : 4;
        ingress_error : 1;
        was_mtagged : 1;
        copy_to_cpu : 1;
        bad_packet : 1;
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

header vlan_t inner_vlan_tag ;

metadata local_metadata_t local_metadata { bad_packet : 1 ; };