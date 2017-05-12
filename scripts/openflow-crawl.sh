#!/bin/bash
ns="root"
ip netns > symnet-namespaces-`hostname`.txt
ip address show > symnet-ifaces-$ns-`hostname`.txt
route -n > symnet-routes-$ns-`hostname`.txt
iptables -t raw -S > symnet-iptables-raw-$ns-`hostname`.txt
iptables -t nat -S > symnet-iptables-nat-$ns-`hostname`.txt
iptables -t filter -S > symnet-iptables-filter-$ns-`hostname`.txt
iptables -t mangle -S > symnet-iptables-mangle-$ns-`hostname`.txt
brctl show > symnet-linuxbridges-$ns-`hostname`.txt
for ns in `ip netns`;
do
    ip netns exec $ns ip address show > symnet-ifaces-$ns-`hostname`.txt
    ip netns exec $ns route -n > symnet-routes-$ns-`hostname`.txt
    ip netns exec $ns iptables -t raw -S > symnet-iptables-raw-$ns-`hostname`.txt
    ip netns exec $ns iptables -t nat -S > symnet-iptables-nat-$ns-`hostname`.txt
    ip netns exec $ns iptables -t filter -S > symnet-iptables-filter-$ns-`hostname`.txt
    ip netns exec $ns iptables -t mangle -S > symnet-iptables-mangle-$ns-`hostname`.txt
    ip netns exec $ns brctl show > symnet-linuxbridges-$ns-`hostname`.txt
done


ovsdb-client dump -f json > symnet-ovs-`hostname`.txt

for x in `ovs-vsctl list-br`; 
do
ovs-ofctl dump-ports-desc $x > symnet-ports-$x-`hostname`.txt
ovs-ofctl dump-flows $x > symnet-flows-$x-`hostname`.txt
done