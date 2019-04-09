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
        res : 3;
        ecn : 3;
        urg : 1;
	ack : 1;
	push : 1;
	rst : 1;
	syn : 1;
	fin : 1;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}
#define TCP_OPTIONS_END 0x00
header_type options_end_t {
	fields {
		kind :8;
	}
}
#define TCP_OPTIONS_NOP 0x01
header_type options_nop_t {
   fields {
	kind: 8;
    }
}
#define TCP_OPTIONS_MSS 0x02
header_type options_mss_t {
   fields {
	kind : 8;	
	len : 8;
	MSS : 16;
   }
}
#define TCP_OPTIONS_WSCALE 0x03
header_type options_wscale_t {
   fields {
	kind : 8;
	len : 8;
	wscale : 8;
   }
}

#define TCP_OPTIONS_SACK 0x04
header_type options_sack_t {
   fields {
	kind: 8;
	len : 8;
   }
}
#define TCP_OPTIONS_TS 0x08
header_type options_ts_t {
   fields {
	kind: 8;
	len : 8;
	ttee : 64;
   }
}
