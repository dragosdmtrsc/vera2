#!/bin/sh

neutron port-list -f csv -F id -F mac_address > symnet-neutron-port-list.csv
neutron net-list -F id -F name -f csv > symnet-neutron-net-list.csv
neutron security-group-list -F id -F name -f csv > symnet-neutron-security-group-list.csv
neutron subnet-list -f csv > symnet-neutron-subnet-list.csv
neutron security-group-rule-list -f csv > symnet-neutron-security-group-rule-list.csv
neutron net-external-list -f csv > symnet-neutron-net-external-list.csv
neutron router-list -f csv > symnet-neutron-router-list.csv
neutron floatingip-list -f csv > symnet-neutron-floatingip-list.csv

openstack network trunk list -f csv > symnet-neutron-trunk-list.csv

for p in `cat symnet-neutron-port-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron port-show -f json $p > symnet-neutron-port-show-$p.txt; done
for p in `cat symnet-neutron-net-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron net-show -f json $p > symnet-neutron-net-show-$p.txt; done
for p in `cat symnet-neutron-subnet-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron subnet-show -f json $p > symnet-neutron-subnet-show-$p.txt; done
for p in `cat symnet-neutron-security-group-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron security-group-show -c id -c name -c description -c tenant_id -f json $p > symnet-neutron-security-group-show-$p.txt; done
for p in `cat symnet-neutron-security-group-rule-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron security-group-rule-show -f json $p > symnet-neutron-security-group-rule-show-$p.txt; done
for p in `cat symnet-neutron-router-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron router-show -f json $p > symnet-neutron-router-show-$p.txt; done
for p in `cat symnet-neutron-floatingip-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do neutron floatingip-show -f json $p > symnet-neutron-floatingip-show-$p.txt; done
for p in `cat symnet-neutron-trunk-list.csv | tail -n +2 | cut -d ',' -f 1 | cut -d '"' -f 2`; do openstack network trunk show -f json $p > symnet-neutron-trunk-show-$p.txt; done


