#!/bin/bash

cat ~/GitHub/symnet-neutron/inputs/big-switch/dump-tables.txt | \
sudo python ~/GitHub/symnet-neutron/scripts/sswitch_pretty.py --thrift-port 10001 | \
tail -n +3 | \
grep -v "Error:" > \
~/GitHub/symnet-neutron/inputs/big-switch/table_dump_full.txt