#!/bin/bash 

for i in `seq 1 8`;
do

    if [ ! -f /etc/network/interfaces.d/eth$i ]
    then
        sudo bash -c "echo \"auto eth$i\" > /etc/network/interfaces.d/eth$i.cfg" &> /dev/null
        sudo bash -c "echo \"iface eth$i inet dhcp\" >> /etc/network/interfaces.d/eth$i.cfg" &> /dev/null
        sudo ifdown eth$i &> /dev/null
        sudo ifup eth$i &> /dev/null
    fi
done


for i in `seq 1 8`
do
    actual_i=$((i+15))
    ip1=`ifconfig eth$i | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`

    for j in `seq 1 8`
    do
        actual_j=$((j+15))
        ip2=`ifconfig eth$j | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'`
        
        if ping -c 1 $ip2 -I eth$i &> /dev/null
        then
            echo "$actual_i,$actual_j,1"
        else
            echo "$actual_i,$actual_j,0"
        fi
    done
done
