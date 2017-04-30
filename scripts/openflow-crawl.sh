#!/bin/bash
ns="root"
ip netns > namespaces-`hostname`.txt
ip address show > ifaces-$ns-`hostname`.txt
route -n > routes-$ns-`hostname`.txt
iptables -t raw -S > iptables-raw-$ns-`hostname`.txt
iptables -t nat -S > iptables-nat-$ns-`hostname`.txt
iptables -t filter -S > iptables-filter-$ns-`hostname`.txt
iptables -t mangle -S > iptables-mangle-$ns-`hostname`.txt
brctl show > linuxbridges-$ns-`hostname`.txt
for ns in `ip netns`;
do
    ip netns exec $ns ip address show > ifaces-$ns-`hostname`.txt
    ip netns exec $ns route -n > routes-$ns-`hostname`.txt
    ip netns exec $ns iptables -t raw -S > iptables-raw-$ns-`hostname`.txt
    ip netns exec $ns iptables -t nat -S > iptables-nat-$ns-`hostname`.txt
    ip netns exec $ns iptables -t filter -S > iptables-filter-$ns-`hostname`.txt
    ip netns exec $ns iptables -t mangle -S > iptables-mangle-$ns-`hostname`.txt
    ip netns exec $ns brctl show > linuxbridges-$ns-`hostname`.txt
done

for x in `ovs-vsctl list-br`; 
do
ovsdb-client dump -f json --pretty > ovs-`hostname`.txt
ovs-ofctl dump-ports-desc $x > ports-$x-`hostname`.txt
ovs-ofctl dump-flows $x > flows-$x-`hostname`.txt
done