control ingress {
    apply(egress_meter) {	
    			hit { // If egress meter table matched, apply policy; create two ports here!
			       apply(meter_policy);
			}
    }
}
