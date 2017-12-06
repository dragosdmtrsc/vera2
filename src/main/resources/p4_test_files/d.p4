control ingress {
        if(valid(ipv4) or ipv4.ttl > 0) {
              apply(modulo);
        }
	else {
	     apply(div);
	}
    }
}
