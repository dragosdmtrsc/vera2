#!/bin/bash


for x in `ovs-vsctl list-br`; 
do
ovs-ofctl dump-ports-desc $x > ports-$x-`hostname`.txt
ovs-ofctl dump-flows $x > flows-$x-`hostname`.txt
done