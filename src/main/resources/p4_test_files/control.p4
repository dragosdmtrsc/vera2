// The ingress control function
control ingress {
    apply(check_mtag);
    apply(identify_port);
    apply(select_output_port);

    apply(egress_meter) {
    			hit { // If egress meter table matched, apply policy; create two ports here!
			        apply(meter_policy);
				}
    }

    apply(routing_table) { // Create one port for each action, one for default. 
     ipv4_route_action { // IPv4 action was used
        apply(v4_rpf);	 
        apply(v4_acl);
     }
     ipv6_route_action { // IPv6 action was used
        apply(v6_option_check);
        apply(v6_option_acl);
     }
     default {
    	    if (standard_metadata.ingress_port != 1) {
	       apply(cpu_ingress_check);
	    }    
     }
    }
    egress();
}