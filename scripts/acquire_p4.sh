#!/bin/bash

if [[ ! $# == 1 ]];
then
    echo "Usage $0 <file_name>"
    exit 1
fi

cat ~/GitHub/symnet-neutron/inputs/big-switch/dump-tables.txt | \
sudo python ~/GitHub/symnet-neutron/scripts/sswitch_pretty.py --thrift-port 10001 | \
tail -n +3 | \
grep -v "Error:" > ~/GitHub/symnet-neutron/inputs/big-switch/$1.txt