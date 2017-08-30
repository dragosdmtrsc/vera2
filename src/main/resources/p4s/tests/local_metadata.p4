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